-- Transactional, idempotent, reconciling import of the announcements seed
-- JSON. Modelled on import_schedule: one call = one transaction, upsert by
-- slug, prune whatever the payload no longer mentions.
create function public.import_announcements(payload jsonb)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  announcements_after int;
  published_after     int;
begin
  insert into public.announcements (slug, title, body, category, published_at, is_published)
  select
    a->>'slug', a->>'title', a->>'body',
    coalesce(a->>'category', 'general'),
    coalesce((a->>'published_at')::timestamptz, now()),
    coalesce((a->>'is_published')::boolean, false)
  from jsonb_array_elements(coalesce(payload->'announcements', '[]'::jsonb)) as a
  on conflict (slug) do update set
    title = excluded.title,
    body = excluded.body,
    category = excluded.category,
    published_at = excluded.published_at,
    is_published = excluded.is_published;

  -- Full reconciliation, so the JSON really is the source of truth.
  delete from public.announcements
  where slug not in (
    select coalesce(a->>'slug', '')
    from jsonb_array_elements(coalesce(payload->'announcements', '[]'::jsonb)) as a
  );

  select count(*) into announcements_after from public.announcements;
  select count(*) into published_after     from public.announcements where is_published;

  return jsonb_build_object(
    'announcements', jsonb_build_object(
      'total', announcements_after,
      'published', published_after
    )
  );
end;
$$;

revoke all on function public.import_announcements(jsonb) from public, anon, authenticated;
grant execute on function public.import_announcements(jsonb) to service_role;
