#!/usr/bin/env node
// Validates supabase/seed/venue.json against venue.schema.json, checks its
// geometry and its cross-references into schedule.json locally, then imports it
// via the transactional public.import_venue() RPC. All validation happens
// before any network call so a bad file never causes a partial write.
//
// Sibling of seed-supabase.mjs, deliberately not merged with it: the schedule
// and the map are seeded on different cadences by different people, and one
// script that always pushes both would make editing a room name require
// re-importing the whole programme.
import { readFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import Ajv2020 from "ajv/dist/2020.js";
import addFormats from "ajv-formats";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const seedDir = path.join(__dirname, "..", "supabase", "seed");
const schemaPath = path.join(seedDir, "venue.schema.json");
const dataPath = path.join(seedDir, "venue.json");
const schedulePath = path.join(seedDir, "schedule.json");

function fail(message) {
  console.error(`error: ${message}`);
  process.exit(1);
}

// Node's fetch reports every connect-layer problem as a bare "TypeError: fetch
// failed" and hides the real reason (ECONNREFUSED, ENOTFOUND, TLS) in
// err.cause, which can itself be nested. Walk the whole chain.
function describeError(err) {
  let out = err.stack ?? String(err);
  for (let cause = err.cause; cause; cause = cause.cause) {
    const code = cause.code ? `${cause.code} ` : "";
    out += `\n  cause: ${code}${cause.message ?? cause}`;
  }
  return out;
}

async function loadJson(filePath) {
  const raw = await readFile(filePath, "utf-8");
  return JSON.parse(raw);
}

/** Ring must close, and must enclose an area — three collinear points do not. */
function checkRing(ring, label, errors) {
  const [firstX, firstY] = ring[0];
  const [lastX, lastY] = ring[ring.length - 1];
  if (firstX !== lastX || firstY !== lastY) {
    errors.push(`${label}: ring is not closed (first ${ring[0]}, last ${ring[ring.length - 1]})`);
  }
  // Shoelace. Signed, so the winding direction does not matter here.
  let twiceArea = 0;
  for (let i = 0, j = ring.length - 1; i < ring.length; j = i++) {
    twiceArea += ring[j][0] * ring[i][1] - ring[i][0] * ring[j][1];
  }
  if (Math.abs(twiceArea) < 1e-6) {
    errors.push(`${label}: ring encloses no area (degenerate or collinear)`);
  }
}

function validateGeometry(data, locationSlugs) {
  const errors = [];
  const levelSlugs = new Set();

  for (const level of data.levels) {
    if (levelSlugs.has(level.slug)) errors.push(`duplicate level slug: ${level.slug}`);
    levelSlugs.add(level.slug);

    if (level.outline) {
      level.outline.coordinates.forEach((ring, i) =>
        checkRing(ring, `level "${level.slug}" outline ring ${i}`, errors)
      );
    }

    const featureSlugs = new Set();
    for (const feature of level.features ?? []) {
      const label = `feature "${level.slug}/${feature.slug}"`;
      if (featureSlugs.has(feature.slug)) {
        errors.push(`duplicate feature slug in level "${level.slug}": ${feature.slug}`);
      }
      featureSlugs.add(feature.slug);

      if (feature.geometry.type === "Polygon") {
        feature.geometry.coordinates.forEach((ring, i) =>
          checkRing(ring, `${label} ring ${i}`, errors)
        );
      }
      // A dangling location slug is not a hard failure server-side (it imports
      // as NULL), but it silently breaks the schedule cross-link, which is the
      // whole point of the column — so it fails here, where it is cheap to fix.
      if (feature.location && !locationSlugs.has(feature.location)) {
        errors.push(`${label} references unknown location "${feature.location}"`);
      }
    }
  }

  return errors;
}

async function main() {
  const [schema, data] = await Promise.all([loadJson(schemaPath), loadJson(dataPath)]);

  const ajv = new Ajv2020({ allErrors: true, strict: true });
  addFormats(ajv);
  const validate = ajv.compile(schema);

  if (!validate(data)) {
    console.error(`schema validation failed with ${validate.errors.length} error(s):`);
    for (const err of validate.errors) {
      console.error(`  ${err.instancePath || "/"}: ${err.message}`);
    }
    process.exit(1);
  }

  // The locations live in the schedule seed, not here: one table, one owner.
  const schedule = await loadJson(schedulePath);
  const locationSlugs = new Set((schedule.locations ?? []).map((l) => l.slug));

  const geometryErrors = validateGeometry(data, locationSlugs);
  if (geometryErrors.length > 0) {
    console.error(`geometry validation failed with ${geometryErrors.length} error(s):`);
    for (const err of geometryErrors) console.error(`  ${err}`);
    process.exit(1);
  }

  const featureCount = data.levels.reduce((sum, l) => sum + (l.features?.length ?? 0), 0);
  const linked = data.levels.reduce(
    (sum, l) => sum + (l.features ?? []).filter((f) => f.location).length,
    0
  );
  console.log(
    `validated venue "${data.venue.slug}": ${data.levels.length} levels, ` +
      `${featureCount} features (${linked} linked to a schedule location)`
  );

  const supabaseUrl = process.env.SUPABASE_URL;
  const serviceRoleKey = process.env.SUPABASE_SERVICE_ROLE_KEY;
  if (!supabaseUrl) fail("SUPABASE_URL is not set");
  if (!serviceRoleKey) fail("SUPABASE_SERVICE_ROLE_KEY is not set");

  const endpoint = new URL("/rest/v1/rpc/import_venue", supabaseUrl);
  let response;
  try {
    response = await fetch(endpoint, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        apikey: serviceRoleKey,
        Authorization: `Bearer ${serviceRoleKey}`,
      },
      body: JSON.stringify({ payload: data }),
    });
  } catch (err) {
    fail(`could not reach ${endpoint} — is Supabase running? ${describeError(err)}`);
  }

  const body = await response.text();
  if (!response.ok) {
    fail(`import_venue RPC failed with ${response.status}: ${body}`);
  }

  console.log("import succeeded:");
  console.log(JSON.stringify(JSON.parse(body), null, 2));
}

main().catch((err) => fail(describeError(err)));
