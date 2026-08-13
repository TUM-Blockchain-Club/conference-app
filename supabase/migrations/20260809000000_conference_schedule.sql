-- Event schedule data model: tracks, locations, speakers, events, event_speakers.

create table public.tracks (
  id uuid primary key default gen_random_uuid(),
  slug text not null unique,
  name text not null,
  description text,
  color text,                                   -- hex, for UI chips
  sort_order int not null default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table public.locations (
  id uuid primary key default gen_random_uuid(),
  slug text not null unique,
  name text not null,
  floor text,
  capacity int,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table public.speakers (
  id uuid primary key default gen_random_uuid(),
  slug text not null unique,
  name text not null,
  title text,
  company text,
  bio text,
  photo_url text,
  links jsonb not null default '{}'::jsonb,     -- {"x": "...", "linkedin": "..."} — extensible w/o migration
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table public.events (
  id uuid primary key default gen_random_uuid(),
  slug text not null unique,
  title text not null,
  description text,
  start_time timestamptz not null,
  end_time timestamptz not null,
  track_id uuid references public.tracks(id) on delete set null,
  location_id uuid references public.locations(id) on delete set null,
  event_type text not null default 'talk',      -- talk | panel | workshop | keynote | break
  is_published boolean not null default false,  -- draft by default
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint events_time_valid check (end_time > start_time)
);

create table public.event_speakers (
  event_id uuid not null references public.events(id) on delete cascade,
  speaker_id uuid not null references public.speakers(id) on delete cascade,
  role text not null default 'speaker',         -- speaker | moderator | host
  sort_order int not null default 0,
  primary key (event_id, speaker_id)
);

create index events_start_time_idx      on public.events (start_time);
create index events_track_id_idx        on public.events (track_id);
create index events_location_id_idx     on public.events (location_id);   -- PG does not auto-index FKs
create index event_speakers_speaker_idx on public.event_speakers (speaker_id);

-- updated_at trigger, shared by the four base tables.
create function public.set_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

create trigger set_updated_at before update on public.tracks
  for each row execute function public.set_updated_at();
create trigger set_updated_at before update on public.locations
  for each row execute function public.set_updated_at();
create trigger set_updated_at before update on public.speakers
  for each row execute function public.set_updated_at();
create trigger set_updated_at before update on public.events
  for each row execute function public.set_updated_at();

-- Grants are a layer separate from RLS: RLS restricts, it does not grant.
revoke all on all tables in schema public from anon, authenticated;
grant select on public.tracks, public.locations, public.speakers,
                public.events, public.event_speakers to anon, authenticated;

alter table public.tracks          enable row level security;
alter table public.locations       enable row level security;
alter table public.speakers        enable row level security;
alter table public.events          enable row level security;
alter table public.event_speakers  enable row level security;

-- tracks, locations: no draft content, unconditional read.
create policy tracks_public_read on public.tracks
  for select to anon, authenticated using (true);

create policy locations_public_read on public.locations
  for select to anon, authenticated using (true);

create policy events_public_read on public.events
  for select to anon, authenticated using (is_published);

-- Published-gating also applies to the join and speaker tables, so a direct
-- table query cannot leak draft event associations.
create policy event_speakers_public_read on public.event_speakers
  for select to anon, authenticated using (
    exists (select 1 from public.events e where e.id = event_id and e.is_published)
  );

create policy speakers_public_read on public.speakers
  for select to anon, authenticated using (
    exists (
      select 1 from public.event_speakers es
      join public.events e on e.id = es.event_id
      where es.speaker_id = speakers.id and e.is_published
    )
  );

-- No insert/update/delete policies: writes are service_role only until the
-- admin app adds its own.
