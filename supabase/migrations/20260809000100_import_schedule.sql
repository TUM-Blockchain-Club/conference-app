-- Transactional, idempotent, reconciling import of the schedule seed JSON.
-- One function call = one transaction: partial writes and stale junction
-- rows are both ruled out by construction.
create function public.import_schedule(payload jsonb)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  tracks_before      int;
  tracks_after       int;
  locations_before   int;
  locations_after    int;
  speakers_before    int;
  speakers_after     int;
  events_before      int;
  events_after       int;
  event_speakers_before int;
  event_speakers_after  int;
  result jsonb;
begin
  select count(*) into tracks_before    from public.tracks;
  select count(*) into locations_before from public.locations;
  select count(*) into speakers_before  from public.speakers;
  select count(*) into events_before    from public.events;
  select count(*) into event_speakers_before from public.event_speakers;

  -- 1. Upsert tracks, locations, speakers.
  insert into public.tracks (slug, name, description, color, sort_order)
  select
    t->>'slug', t->>'name', t->>'description', t->>'color',
    coalesce((t->>'sort_order')::int, 0)
  from jsonb_array_elements(coalesce(payload->'tracks', '[]'::jsonb)) as t
  on conflict (slug) do update set
    name = excluded.name,
    description = excluded.description,
    color = excluded.color,
    sort_order = excluded.sort_order;

  insert into public.locations (slug, name, floor, capacity)
  select
    l->>'slug', l->>'name', l->>'floor', (l->>'capacity')::int
  from jsonb_array_elements(coalesce(payload->'locations', '[]'::jsonb)) as l
  on conflict (slug) do update set
    name = excluded.name,
    floor = excluded.floor,
    capacity = excluded.capacity;

  insert into public.speakers (slug, name, title, company, bio, photo_url, links)
  select
    s->>'slug', s->>'name', s->>'title', s->>'company', s->>'bio', s->>'photo_url',
    coalesce(s->'links', '{}'::jsonb)
  from jsonb_array_elements(coalesce(payload->'speakers', '[]'::jsonb)) as s
  on conflict (slug) do update set
    name = excluded.name,
    title = excluded.title,
    company = excluded.company,
    bio = excluded.bio,
    photo_url = excluded.photo_url,
    links = excluded.links;

  -- 2. Upsert events, resolving track/location slugs to UUIDs.
  insert into public.events (
    slug, title, description, start_time, end_time,
    track_id, location_id, event_type, is_published
  )
  select
    e->>'slug', e->>'title', e->>'description',
    (e->>'start_time')::timestamptz, (e->>'end_time')::timestamptz,
    (select id from public.tracks tr where tr.slug = e->>'track'),
    (select id from public.locations loc where loc.slug = e->>'location'),
    coalesce(e->>'event_type', 'talk'),
    coalesce((e->>'is_published')::boolean, false)
  from jsonb_array_elements(coalesce(payload->'events', '[]'::jsonb)) as e
  on conflict (slug) do update set
    title = excluded.title,
    description = excluded.description,
    start_time = excluded.start_time,
    end_time = excluded.end_time,
    track_id = excluded.track_id,
    location_id = excluded.location_id,
    event_type = excluded.event_type,
    is_published = excluded.is_published;

  -- 3. Replace event_speakers for every event present in the payload.
  delete from public.event_speakers es
  using public.events ev
  where es.event_id = ev.id
    and ev.slug in (
      select e->>'slug' from jsonb_array_elements(coalesce(payload->'events', '[]'::jsonb)) as e
    );

  insert into public.event_speakers (event_id, speaker_id, role, sort_order)
  select
    ev.id,
    sp.id,
    coalesce(es->>'role', 'speaker'),
    coalesce((es->>'sort_order')::int, 0)
  from jsonb_array_elements(coalesce(payload->'events', '[]'::jsonb)) as e
  join public.events ev on ev.slug = e->>'slug'
  cross join lateral jsonb_array_elements(coalesce(e->'speakers', '[]'::jsonb)) as es
  join public.speakers sp on sp.slug = es->>'slug';

  -- 4. Prune rows whose slug is absent from the payload: full reconciliation,
  -- so the JSON really is the source of truth. Order matters for FKs.
  delete from public.events
  where slug not in (
    select coalesce(e->>'slug', '') from jsonb_array_elements(coalesce(payload->'events', '[]'::jsonb)) as e
  );

  delete from public.speakers
  where slug not in (
    select coalesce(s->>'slug', '') from jsonb_array_elements(coalesce(payload->'speakers', '[]'::jsonb)) as s
  );

  delete from public.locations
  where slug not in (
    select coalesce(l->>'slug', '') from jsonb_array_elements(coalesce(payload->'locations', '[]'::jsonb)) as l
  );

  delete from public.tracks
  where slug not in (
    select coalesce(t->>'slug', '') from jsonb_array_elements(coalesce(payload->'tracks', '[]'::jsonb)) as t
  );

  select count(*) into tracks_after    from public.tracks;
  select count(*) into locations_after from public.locations;
  select count(*) into speakers_after  from public.speakers;
  select count(*) into events_after    from public.events;
  select count(*) into event_speakers_after from public.event_speakers;

  result := jsonb_build_object(
    'tracks', jsonb_build_object('total', tracks_after),
    'locations', jsonb_build_object('total', locations_after),
    'speakers', jsonb_build_object('total', speakers_after),
    'events', jsonb_build_object('total', events_after),
    'event_speakers', jsonb_build_object('total', event_speakers_after)
  );

  return result;
end;
$$;

revoke all on function public.import_schedule(jsonb) from public, anon, authenticated;
grant execute on function public.import_schedule(jsonb) to service_role;
