-- Announcements: schedule changes, keynote reminders and emergency notices,
-- pushed to attendees while the conference is running (#7).
--
-- The app reads this table into its offline cache exactly as it reads the
-- schedule. A push notification only wakes the app so that it refreshes; the
-- feed itself never depends on push having been delivered.

create table public.announcements (
  id uuid primary key default gen_random_uuid(),
  slug text not null unique,                     -- stable key for the seed file, as with events
  title text not null,
  body text not null,
  -- Kept in step with AnnouncementCategory on the client, the same arrangement
  -- map_features.category has with MapCategory. A check constraint rather than
  -- a lookup table: four fixed values the client renders as toggles anyway, and
  -- a lookup table would mean a second fetch and a join to draw a chip.
  -- `emergency` is a category, not a severity: one axis, not two. It is the
  -- one category a device cannot opt out of.
  category text not null default 'general',
  published_at timestamptz not null default now(),
  is_published boolean not null default false,   -- draft by default, like events
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint announcements_category_valid check (
    category in ('general', 'schedule', 'keynote', 'emergency')
  )
);

create index announcements_published_at_idx
  on public.announcements (published_at desc) where is_published;

create trigger set_updated_at before update on public.announcements
  for each row execute function public.set_updated_at();

-- Grants are a layer separate from RLS: RLS restricts, it does not grant.
grant select on public.announcements to anon, authenticated;

alter table public.announcements enable row level security;

create policy announcements_public_read on public.announcements
  for select to anon, authenticated using (is_published);

-- No insert/update/delete policies: writes are service_role only, through
-- public.import_announcements().
