-- Transactional, idempotent, reconciling import of the venue seed JSON, plus
-- the single-round-trip read the app uses.
--
-- Unlike import_schedule(), the payload is *one venue*: levels and features are
-- nested under it, because their slugs are only unique within their parent and
-- because that is the shape QGIS exports fold into. Reconciliation is therefore
-- scoped to the venue named in the payload — importing "tbc-conference" never
-- touches another venue's rows.
create function public.import_venue(payload jsonb)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  venue_slug_in    text := payload->'venue'->>'slug';
  venue_id_in      uuid;
  levels_before    int;
  levels_after     int;
  features_before  int;
  features_after   int;
  unresolved       int;
begin
  if venue_slug_in is null or venue_slug_in = '' then
    raise exception 'payload.venue.slug is required';
  end if;

  insert into public.venues (slug, name)
  values (venue_slug_in, coalesce(payload->'venue'->>'name', venue_slug_in))
  on conflict (slug) do update set name = excluded.name
  returning id into venue_id_in;

  select count(*) into levels_before
    from public.venue_levels where venue_id = venue_id_in;
  select count(*) into features_before
    from public.map_features f
    join public.venue_levels l on l.id = f.level_id
   where l.venue_id = venue_id_in;

  -- 1. Upsert levels.
  insert into public.venue_levels (venue_id, slug, name, ordinal, outline)
  select
    venue_id_in,
    lv->>'slug',
    coalesce(lv->>'name', lv->>'slug'),
    coalesce((lv->>'ordinal')::int, 0),
    lv->'outline'
  from jsonb_array_elements(coalesce(payload->'levels', '[]'::jsonb)) as lv
  on conflict (venue_id, slug) do update set
    name = excluded.name,
    ordinal = excluded.ordinal,
    outline = excluded.outline;

  -- 2. Upsert features, resolving the location slug to a UUID. An unknown slug
  --    resolves to NULL rather than failing the import; step 5 reports how many.
  insert into public.map_features (
    level_id, slug, name, category, location_id, geometry, label_anchor, sort_order
  )
  select
    l.id,
    f->>'slug',
    coalesce(f->>'name', f->>'slug'),
    coalesce(f->>'category', 'other'),
    (select loc.id from public.locations loc where loc.slug = f->>'location'),
    f->'geometry',
    f->'label_anchor',
    coalesce((f->>'sort_order')::int, 0)
  from jsonb_array_elements(coalesce(payload->'levels', '[]'::jsonb)) as lv
  join public.venue_levels l
    on l.venue_id = venue_id_in and l.slug = lv->>'slug'
  cross join lateral jsonb_array_elements(coalesce(lv->'features', '[]'::jsonb)) as f
  on conflict (level_id, slug) do update set
    name = excluded.name,
    category = excluded.category,
    location_id = excluded.location_id,
    geometry = excluded.geometry,
    label_anchor = excluded.label_anchor,
    sort_order = excluded.sort_order;

  -- 3. Prune features whose slug is absent from their level in the payload.
  delete from public.map_features f
  using public.venue_levels l
  where f.level_id = l.id
    and l.venue_id = venue_id_in
    and not exists (
      select 1
      from jsonb_array_elements(coalesce(payload->'levels', '[]'::jsonb)) as lv
      cross join lateral jsonb_array_elements(coalesce(lv->'features', '[]'::jsonb)) as pf
      where lv->>'slug' = l.slug and pf->>'slug' = f.slug
    );

  -- 4. Prune levels absent from the payload. Cascades to their features, which
  --    is why it runs after the feature prune rather than instead of it.
  delete from public.venue_levels l
  where l.venue_id = venue_id_in
    and l.slug not in (
      select coalesce(lv->>'slug', '')
      from jsonb_array_elements(coalesce(payload->'levels', '[]'::jsonb)) as lv
    );

  select count(*) into levels_after
    from public.venue_levels where venue_id = venue_id_in;
  select count(*) into features_after
    from public.map_features f
    join public.venue_levels l on l.id = f.level_id
   where l.venue_id = venue_id_in;

  -- 5. A feature that names a location which does not exist is a typo in the
  --    seed file, not a hard error — but it silently breaks the schedule
  --    cross-link, so surface the count.
  select count(*) into unresolved
    from jsonb_array_elements(coalesce(payload->'levels', '[]'::jsonb)) as lv
    cross join lateral jsonb_array_elements(coalesce(lv->'features', '[]'::jsonb)) as f
   where f->>'location' is not null
     and not exists (select 1 from public.locations loc where loc.slug = f->>'location');

  return jsonb_build_object(
    'venue', jsonb_build_object('slug', venue_slug_in, 'id', venue_id_in),
    'levels', jsonb_build_object('before', levels_before, 'total', levels_after),
    'features', jsonb_build_object('before', features_before, 'total', features_after),
    'unresolved_locations', unresolved
  );
end;
$$;

revoke all on function public.import_venue(jsonb) from public, anon, authenticated;
grant execute on function public.import_venue(jsonb) to service_role;

-- The read side: levels and their features assembled into one document, one
-- round-trip. Same spirit as the single nested SCHEDULE_COLUMNS select — the
-- client wants the whole map or none of it, and three PostgREST calls stitched
-- together on-device would be three chances to be half-loaded.
--
-- security invoker (the default) on purpose: this runs under the caller's role,
-- so the public-read RLS policies still apply.
create function public.get_venue_map(venue_slug text)
returns jsonb
language sql
stable
set search_path = public
as $$
  select jsonb_build_object(
    'id', v.id,
    'slug', v.slug,
    'name', v.name,
    'levels', coalesce(
      (
        select jsonb_agg(lvl.doc order by lvl.ordinal, lvl.slug)
        from (
          select
            l.ordinal,
            l.slug,
            jsonb_build_object(
              'id', l.id,
              'slug', l.slug,
              'name', l.name,
              'ordinal', l.ordinal,
              'outline', l.outline,
              'features', coalesce(
                (
                  select jsonb_agg(
                    jsonb_build_object(
                      'id', f.id,
                      'slug', f.slug,
                      'name', f.name,
                      'category', f.category,
                      'location_id', f.location_id,
                      'geometry', f.geometry,
                      'label_anchor', f.label_anchor,
                      'sort_order', f.sort_order
                    )
                    order by f.sort_order, f.name
                  )
                  from public.map_features f
                  where f.level_id = l.id
                ),
                '[]'::jsonb
              )
            ) as doc
          from public.venue_levels l
          where l.venue_id = v.id
        ) as lvl
      ),
      '[]'::jsonb
    )
  )
  from public.venues v
  where v.slug = venue_slug;
$$;

grant execute on function public.get_venue_map(text) to anon, authenticated, service_role;
