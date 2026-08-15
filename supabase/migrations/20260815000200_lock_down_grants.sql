-- Lock down the schema-wide default grants.
--
-- Supabase ships a stock `alter default privileges in schema public grant all
-- on tables to anon, authenticated`. That is a *standing* rule: every table
-- created in `public` from then on is automatically world-writable over
-- PostgREST unless something else stops it. The `revoke all on all tables in
-- schema public` in 20260809000000 is point-in-time — it cleaned up the tables
-- that existed at that moment and did nothing for the venue tables added in
-- 20260815000000, which have carried INSERT/UPDATE/DELETE grants to `anon`
-- ever since. Only RLS-with-no-write-policy has been stopping those writes.
--
-- After this migration the standing pattern for anything new in `public` is:
--
--   * every table needs `enable row level security` *and* an explicit
--     `grant select ... to anon, authenticated`. Forgetting is loud —
--     "permission denied" at the client — instead of a silently writable table;
--   * every function needs an explicit `revoke all ... from public` in the same
--     migration that creates it, then a `grant execute` to whoever should
--     actually call it. This one is *not* enforced by the defaults below; see
--     block 1. Forgetting is silent, so it is what section 6 of
--     `supabase/checks/rls-audit.sql` exists to catch.

-- 1. Retract the stock default privileges.
--
-- `alter default privileges` only affects objects created by the role it is
-- attached to, and the role that owns migrations differs between the local CLI
-- and a hosted project — so emit both the bare form (current role) and the
-- explicit `for role postgres` one. `service_role` and `postgres` are left
-- alone: the seed scripts and the dashboard run as those.
--
-- `supabase_admin` carries the same stock defaults for `public` and they are
-- deliberately not touched here: `postgres` is not a member of that role, so
-- the statement would fail outright, and it does not apply to us anyway —
-- default privileges follow the *creating* role, and every object in these
-- migrations is created by `postgres`. Section 7 of `supabase/checks/rls-audit.sql`
-- reports it as informational for that reason.
alter default privileges in schema public
  revoke all on tables from anon, authenticated;
alter default privileges in schema public
  revoke all on sequences from anon, authenticated;
-- Functions are a partial fix only, and deliberately so. Postgres grants
-- EXECUTE on every new function to PUBLIC out of the box, and that built-in
-- default cannot be revoked here: `alter default privileges` is merged
-- *additively* onto it at creation time, so a stored default of
-- `{postgres=X/postgres}` unions back to the built-in and the new function ends
-- up with `proacl` null — PUBLIC, and therefore `anon`, can execute it.
-- (Verified on PG 17.6, the version the CLI runs.) This statement still strips
-- the stock explicit anon/authenticated entries, which is worth doing, but the
-- only real guard on a new RPC is an explicit `revoke all on function … from
-- public` in the migration that creates it. `import_schedule` and
-- `import_venue` already do that; block 3 retrofits the two that did not.
alter default privileges in schema public
  revoke all on functions from public, anon, authenticated;

alter default privileges for role postgres in schema public
  revoke all on tables from anon, authenticated;
alter default privileges for role postgres in schema public
  revoke all on sequences from anon, authenticated;
alter default privileges for role postgres in schema public
  revoke all on functions from public, anon, authenticated;

-- 2. Re-tighten the tables created after the point-in-time revoke.
--
-- Defense in depth: their RLS policies are select-only and already block the
-- writes. This removes the grant that RLS has been compensating for, so the two
-- layers agree instead of one covering for the other.
revoke all on public.venues, public.venue_levels, public.map_features
  from anon, authenticated;

grant select on public.venues, public.venue_levels, public.map_features
  to anon, authenticated;

-- 3. The per-function revoke that block 1 cannot do for us.
--
-- `set_updated_at` and `get_venue_map` were created without one, so they carry
-- Postgres's built-in `execute to public` and `anon` can call them by
-- inheritance rather than by decision. Retract that and hand back exactly the
-- one grant the app needs.
--
-- `set_updated_at` stays callable by its triggers: EXECUTE is checked when a
-- trigger is created, not each time it fires.
revoke all on function public.set_updated_at() from public, anon, authenticated;
revoke all on function public.get_venue_map(text) from public;

grant execute on function public.get_venue_map(text) to anon, authenticated, service_role;
