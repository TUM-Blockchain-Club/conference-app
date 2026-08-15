#!/usr/bin/env node
// Validates supabase/seed/schedule.json against schedule.schema.json, checks
// its cross-references locally, then imports it via the transactional
// public.import_schedule() RPC. All validation happens before any network
// call so a bad file never causes a partial write.
import { readFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import Ajv2020 from "ajv/dist/2020.js";
import addFormats from "ajv-formats";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const seedDir = path.join(__dirname, "..", "supabase", "seed");
const schemaPath = path.join(seedDir, "schedule.schema.json");
const dataPath = path.join(seedDir, "schedule.json");

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

function validateCrossReferences(data) {
  const trackSlugs = new Set(data.tracks.map((t) => t.slug));
  const locationSlugs = new Set(data.locations.map((l) => l.slug));
  const speakerSlugs = new Set(data.speakers.map((s) => s.slug));
  const errors = [];

  const dupe = (items, label) => {
    const seen = new Set();
    for (const item of items) {
      if (seen.has(item.slug)) errors.push(`duplicate ${label} slug: ${item.slug}`);
      seen.add(item.slug);
    }
  };
  dupe(data.tracks, "track");
  dupe(data.locations, "location");
  dupe(data.speakers, "speaker");
  dupe(data.events, "event");

  for (const event of data.events) {
    if (event.track && !trackSlugs.has(event.track)) {
      errors.push(`event "${event.slug}" references unknown track "${event.track}"`);
    }
    if (event.location && !locationSlugs.has(event.location)) {
      errors.push(`event "${event.slug}" references unknown location "${event.location}"`);
    }
    if (new Date(event.end_time) <= new Date(event.start_time)) {
      errors.push(`event "${event.slug}" has end_time <= start_time`);
    }
    for (const speaker of event.speakers ?? []) {
      if (!speakerSlugs.has(speaker.slug)) {
        errors.push(`event "${event.slug}" references unknown speaker "${speaker.slug}"`);
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

  const crossRefErrors = validateCrossReferences(data);
  if (crossRefErrors.length > 0) {
    console.error(`cross-reference validation failed with ${crossRefErrors.length} error(s):`);
    for (const err of crossRefErrors) console.error(`  ${err}`);
    process.exit(1);
  }

  console.log(
    `validated ${data.tracks.length} tracks, ${data.locations.length} locations, ` +
      `${data.speakers.length} speakers, ${data.events.length} events`
  );

  const supabaseUrl = process.env.SUPABASE_URL;
  const serviceRoleKey = process.env.SUPABASE_SERVICE_ROLE_KEY;
  if (!supabaseUrl) fail("SUPABASE_URL is not set");
  if (!serviceRoleKey) fail("SUPABASE_SERVICE_ROLE_KEY is not set");

  const endpoint = new URL("/rest/v1/rpc/import_schedule", supabaseUrl);
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
    fail(`import_schedule RPC failed with ${response.status}: ${body}`);
  }

  console.log("import succeeded:");
  console.log(JSON.stringify(JSON.parse(body), null, 2));
}

main().catch((err) => fail(describeError(err)));
