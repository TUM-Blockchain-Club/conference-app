-- Indoor venue map: venues, venue_levels, map_features.
--
-- Geometry is stored as GeoJSON in jsonb rather than PostGIS. The venue is a
-- handful of rooms in a *venue-local metric CRS* (metres from a venue origin,
-- X right / Y down), not a place on the globe: there is no projection to do, no
-- spatial index worth building at this row count, and no geo query the client
-- makes. jsonb keeps the whole thing importable from a QGIS GeoJSON export and
-- readable in the dashboard. See docs/VENUE-MAP.md.

create table public.venues (
  id uuid primary key default gen_random_uuid(),
  slug text not null unique,
  name text not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table public.venue_levels (
  id uuid primary key default gen_random_uuid(),
  venue_id uuid not null references public.venues(id) on delete cascade,
  slug text not null,
  name text not null,
  ordinal int not null default 0,              -- 0 = ground, 1 = first, ... ; display order
  outline jsonb,                               -- GeoJSON Polygon: the floor's footprint
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  -- Scoped, not global: "ground" is a reasonable slug for every venue.
  constraint venue_levels_slug_unique unique (venue_id, slug)
);

create table public.map_features (
  id uuid primary key default gen_random_uuid(),
  level_id uuid not null references public.venue_levels(id) on delete cascade,
  slug text not null,
  name text not null,
  category text not null default 'other',
  -- The join that makes the map schedule-aware: it is what lets "Main Stage" on
  -- the map know which talk is on right now. Nullable and `set null`, because a
  -- restroom has no location row and deleting a location must not delete the
  -- room it points at.
  location_id uuid references public.locations(id) on delete set null,
  geometry jsonb not null,                     -- GeoJSON Polygon (a room) or Point (a POI)
  label_anchor jsonb,                          -- GeoJSON Point; falls back to the centroid
  sort_order int not null default 0,           -- draw order within a level
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint map_features_slug_unique unique (level_id, slug),
  constraint map_features_category_valid check (
    category in (
      'stage', 'room', 'food', 'restroom', 'booth',
      'entrance', 'stairs', 'elevator', 'corridor', 'other'
    )
  )
);

create index venue_levels_venue_id_idx  on public.venue_levels (venue_id);
create index map_features_level_id_idx  on public.map_features (level_id);
create index map_features_location_id_idx on public.map_features (location_id);  -- PG does not auto-index FKs

create trigger set_updated_at before update on public.venues
  for each row execute function public.set_updated_at();
create trigger set_updated_at before update on public.venue_levels
  for each row execute function public.set_updated_at();
create trigger set_updated_at before update on public.map_features
  for each row execute function public.set_updated_at();

-- Grants are a layer separate from RLS: RLS restricts, it does not grant.
grant select on public.venues, public.venue_levels, public.map_features
  to anon, authenticated;

alter table public.venues        enable row level security;
alter table public.venue_levels  enable row level security;
alter table public.map_features  enable row level security;

-- No draft concept here: a venue map is either seeded or it is not, and an
-- unfinished one simply has no rows. Unconditional read, like tracks/locations.
create policy venues_public_read on public.venues
  for select to anon, authenticated using (true);

create policy venue_levels_public_read on public.venue_levels
  for select to anon, authenticated using (true);

create policy map_features_public_read on public.map_features
  for select to anon, authenticated using (true);

-- No insert/update/delete policies: writes are service_role only, through
-- public.import_venue().
