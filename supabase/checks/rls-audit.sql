-- Read-only audit of the RLS / grants posture of schema `public`.
--
-- Nothing here writes. Run it against the local stack with `make rls-audit`,
-- and against a hosted project the day one exists:
--
--   psql "$DB_URL" -f supabase/checks/rls-audit.sql
--
-- Sections 1, 2 and 4 must come back empty. The rest are inventories — read
-- them, don't just check the row count; 6 and 7 both have rows that are
-- expected and rows that are a finding, and each labels which is which.
--
-- Privileges are read from the catalog (`aclexplode`, `has_function_privilege`)
-- rather than from `information_schema`, whose grant views are filtered by the
-- roles the connected user belongs to and can under-report.

\pset pager off
\pset null '(null)'

\echo
\echo '=== 1. Tables in public with RLS DISABLED (expect zero rows) ==============='
select c.relname as table_name,
       c.relkind
  from pg_class c
  join pg_namespace n on n.oid = c.relnamespace
 where n.nspname = 'public'
   and c.relkind in ('r', 'p')          -- ordinary + partitioned tables
   and not c.relrowsecurity
 order by 1;

\echo
\echo '=== 2. Tables with RLS ON but ZERO policies (deny-all — expect zero rows) =='
select c.relname as table_name
  from pg_class c
  join pg_namespace n on n.oid = c.relnamespace
 where n.nspname = 'public'
   and c.relkind in ('r', 'p')
   and c.relrowsecurity
   and not exists (select 1 from pg_policy p where p.polrelid = c.oid)
 order by 1;

\echo
\echo '=== 3. Every policy on public (inventory — read the quals) ================'
select tablename,
       policyname,
       cmd,
       roles,
       qual,
       with_check
  from pg_policies
 where schemaname = 'public'
 order by tablename, policyname;

\echo
\echo '=== 4. Grants to anon/authenticated beyond SELECT (expect zero rows) ======'
select c.relname as table_name,
       r.rolname as grantee,
       a.privilege_type
  from pg_class c
  join pg_namespace n on n.oid = c.relnamespace
 cross join lateral aclexplode(c.relacl) a
  join pg_roles r on r.oid = a.grantee
 where n.nspname = 'public'
   and c.relkind in ('r', 'p', 'v', 'm', 'f')
   and r.rolname in ('anon', 'authenticated')
   and a.privilege_type <> 'SELECT'
 order by 1, 2, 3;

\echo
\echo '=== 5. SECURITY DEFINER functions without a pinned search_path (zero) ====='
select p.proname as function_name,
       pg_get_function_identity_arguments(p.oid) as args,
       p.proconfig
  from pg_proc p
  join pg_namespace n on n.oid = p.pronamespace
 where n.nspname = 'public'
   and p.prosecdef
   and not exists (
     select 1 from unnest(coalesce(p.proconfig, '{}')) as cfg
      where cfg like 'search\_path=%'
   )
 order by 1;

\echo
\echo '=== 6. Functions in public that anon can EXECUTE (READ THIS ONE) ========='
-- Expect exactly get_venue_map(text). import_schedule/import_venue must NOT
-- appear: they are security definer and service_role only.
--
-- This is the section that catches a new RPC, because nothing in the schema
-- prevents one. Postgres grants EXECUTE on every new function to PUBLIC, and
-- `alter default privileges` cannot revoke that (see 20260815000200) — so a
-- function added without an explicit `revoke all … from public` in its own
-- migration is callable by `anon` from the moment it exists, silently. The
-- `via` column names the reason a row is here.
select p.proname as function_name,
       pg_get_function_identity_arguments(p.oid) as args,
       case when p.proacl is null then 'PUBLIC (implicit — probably unintended)'
            else 'explicit grant' end as via,
       p.prosecdef as security_definer,
       has_function_privilege('authenticated', p.oid, 'execute') as authenticated_too
  from pg_proc p
  join pg_namespace n on n.oid = p.pronamespace
 where n.nspname = 'public'
   and p.prokind = 'f'
   and has_function_privilege('anon', p.oid, 'execute')
 order by 3, 1;

\echo
\echo '=== 7. Default ACLs for public granting to anon/authenticated (inventory) ='
-- Confirms 20260815000200 took. Default privileges follow the role that
-- *creates* an object, so only the rows whose `created_by_role` can create
-- objects here matter — that is `postgres`, which owns every migration on both
-- the local CLI and a hosted project. Those rows must be gone.
--
-- `supabase_admin` rows are expected and are NOT a finding: nothing in this
-- repo creates objects as that role, and `postgres` is not a member of it, so
-- the grants can neither apply to our tables nor be revoked by our migrations.
select pg_get_userbyid(d.defaclrole) as created_by_role,
       case when pg_get_userbyid(d.defaclrole) = 'postgres'
            then '*** REGRESSION ***' else 'informational' end as verdict,
       case d.defaclobjtype
         when 'r' then 'tables'
         when 'S' then 'sequences'
         when 'f' then 'functions'
         when 'T' then 'types'
         when 'n' then 'schemas'
       end as object_type,
       coalesce(r.rolname, 'PUBLIC') as grantee,
       a.privilege_type
  from pg_default_acl d
  join pg_namespace n on n.oid = d.defaclnamespace
 cross join lateral aclexplode(d.defaclacl) a
  left join pg_roles r on r.oid = a.grantee
 where coalesce(r.rolname, 'PUBLIC') in ('anon', 'authenticated', 'PUBLIC')
   and n.nspname = 'public'
 order by 2, 1, 3, 4, 5;

\echo
\echo '=== 8. Read path smoke test, as anon (schedule + venue map) ==============='
-- Row counts only. Draft events are excluded by the events RLS policy, so
-- `events` here is the published count, not the table count.
set role anon;
select 'events (published)' as source, count(*) from public.events
union all select 'speakers (linked to published)', count(*) from public.speakers
union all select 'tracks', count(*) from public.tracks
union all select 'locations', count(*) from public.locations
union all select 'venues', count(*) from public.venues
union all select 'venue_levels', count(*) from public.venue_levels
union all select 'map_features', count(*) from public.map_features;
reset role;
