-- Publishing an announcement sends its push, via the send-announcement Edge
-- Function (supabase/functions/send-announcement).
--
-- Inert until configured, twice over:
--   1. The trigger only calls out when a Vault secret named
--      `send_announcement_url` exists. Without it, publishing just publishes.
--   2. The function itself is a logged no-op without FCM credentials.
-- So nothing here needs a credential to apply, and the feed never depends on
-- any of it.
--
-- To enable on a hosted project (see README.md, "Push notifications"):
--   select vault.create_secret(
--     'https://<project-ref>.supabase.co/functions/v1/send-announcement',
--     'send_announcement_url');
-- The URL is not a secret; Vault is used as the one config store both the
-- local stack and a hosted project already have.

create extension if not exists pg_net with schema extensions;

-- Idempotency: stamped by the function when it *claims* the row, before it
-- sends. Re-publishing an already-pushed announcement does not re-send;
-- clearing pushed_at by hand re-arms it.
alter table public.announcements add column pushed_at timestamptz;

create function public.send_announcement_push()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  function_url text;
begin
  select decrypted_secret into function_url
    from vault.decrypted_secrets
   where name = 'send_announcement_url';

  if function_url is null then
    return new;  -- push not configured
  end if;

  -- Asynchronous: pg_net queues the request and returns immediately, so a
  -- slow or failing function never holds up the publishing transaction.
  perform net.http_post(
    url := function_url,
    body := jsonb_build_object('announcement_id', new.id),
    headers := jsonb_build_object('Content-Type', 'application/json'),
    timeout_milliseconds := 10000
  );
  return new;
exception when others then
  -- Push is best-effort. Publishing an evacuation notice must never fail
  -- because the push plumbing did.
  raise warning 'send_announcement_push: %', sqlerrm;
  return new;
end;
$$;

revoke all on function public.send_announcement_push() from public, anon, authenticated;

-- What the Edge Function needs, and nothing more. It connects as service_role,
-- which bypasses RLS but not grants — and on this schema service_role holds no
-- read or write privilege by default (the seed paths all go through security
-- definer RPCs instead). Column-level on announcements: the function may stamp
-- pushed_at and change nothing else.
grant select on public.announcements to service_role;
grant update (pushed_at) on public.announcements to service_role;
grant select, delete on public.device_tokens to service_role;

-- `update of is_published`, not every update: an edit to a published
-- announcement's text is not a new announcement. The function's claim on
-- pushed_at makes a redundant firing (an import re-asserting is_published)
-- harmless.
create trigger announcements_send_push
  after insert or update of is_published on public.announcements
  for each row
  when (new.is_published and new.pushed_at is null)
  execute function public.send_announcement_push();
