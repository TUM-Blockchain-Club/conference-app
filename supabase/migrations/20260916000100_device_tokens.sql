-- Push registration: one row per device that can be sent a notification.
--
-- READ THIS BEFORE CHANGING ANYTHING HERE. Until this migration every table in
-- `public` was read-only to `anon`. Registering for push without accounts
-- breaks that: an unauthenticated client has to be able to *write*. The design
-- keeps the RLS audit's invariants anyway by giving `anon` no table privilege
-- at all — not even SELECT — and exposing exactly one `security definer` RPC
-- that can only upsert, and returns nothing.
--
-- Security properties, stated so a reviewer can check them:
--
--   * register_device_token returns void. It never reads anything back, so it
--     cannot be used to enumerate tokens or confirm that one exists. `anon` has
--     no select path to this table in any form.
--   * Rows are keyed on the SHA-256 of the token. A caller can only touch the
--     row for a token it already holds — and an FCM token *is* the capability
--     to push to that device, so the RPC grants nothing an attacker did not
--     already have.
--   * Write amplification is the real residual risk: anyone with the public
--     anon key can insert junk rows. Bounded, not eliminated, by the length
--     and platform checks, Supabase's per-IP rate limiting on the API, and
--     pruning by `last_seen_at` (see prune_device_tokens below). That is
--     inherent to push registration without accounts, and accepted.
--   * The raw token is stored next to its hash, because the send path needs it:
--     a hash cannot be pushed to. The hash buys idempotent upsert keying, NOT
--     secrecy at rest. What actually protects this table is that only
--     service_role can read it.

-- digest() for the hash. Supabase installs extensions into `extensions`, which
-- is not on the function's pinned search_path, so calls are schema-qualified.
create extension if not exists pgcrypto with schema extensions;

create table public.device_tokens (
  token_hash text primary key,              -- sha256(token), hex
  token text not null,                      -- needed to actually send; see above
  platform text not null,
  -- The categories this device opted *into*. Emergency is sent regardless of
  -- this list — the client always includes it, and the send path ignores the
  -- list for it anyway.
  categories text[] not null default '{}',
  created_at timestamptz not null default now(),
  last_seen_at timestamptz not null default now(),
  constraint device_tokens_platform_valid check (platform in ('android', 'ios'))
);

create index device_tokens_last_seen_idx on public.device_tokens (last_seen_at);

-- No grants to anon/authenticated at all, not even SELECT, and RLS on with no
-- policy: deny-all to every role that is not service_role. Both layers say
-- no; the RPC below is the only door. Section 2 of supabase/checks/rls-audit.sql
-- names this table as its one intended exception.
alter table public.device_tokens enable row level security;
revoke all on public.device_tokens from anon, authenticated;

create function public.register_device_token(
  p_token text,
  p_platform text,
  p_categories text[]
) returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if p_token is null or length(p_token) not between 32 and 4096 then
    raise exception 'invalid token';
  end if;
  if p_platform is null or p_platform not in ('android', 'ios') then
    raise exception 'invalid platform';
  end if;
  -- Unknown categories are dropped rather than rejected: a newer client may
  -- know one this server does not yet, and refusing its whole registration
  -- would cost it emergencies too.
  insert into public.device_tokens (token_hash, token, platform, categories)
  values (
    encode(extensions.digest(p_token, 'sha256'), 'hex'),
    p_token,
    p_platform,
    coalesce(
      array(
        select distinct c from unnest(coalesce(p_categories, '{}')) as c
        where c in ('general', 'schedule', 'keynote', 'emergency')
      ),
      '{}'
    )
  )
  on conflict (token_hash) do update
    set categories   = excluded.categories,
        platform     = excluded.platform,
        last_seen_at = now();
end;
$$;

revoke all on function public.register_device_token(text, text, text[])
  from public, anon, authenticated;
grant execute on function public.register_device_token(text, text, text[])
  to anon, authenticated, service_role;

-- Housekeeping for the junk-row risk above, and for devices that uninstalled
-- without the send path ever seeing an UNREGISTERED. Every app launch
-- re-registers, so a live device's last_seen_at is never older than its last
-- launch. Not scheduled here: run it after the conference, or from pg_cron
-- once a hosted project exists.
create function public.prune_device_tokens(older_than interval default interval '30 days')
returns int
language sql
security definer
set search_path = public
as $$
  with deleted as (
    delete from public.device_tokens where last_seen_at < now() - older_than returning 1
  )
  select count(*)::int from deleted;
$$;

revoke all on function public.prune_device_tokens(interval) from public, anon, authenticated;
grant execute on function public.prune_device_tokens(interval) to service_role;
