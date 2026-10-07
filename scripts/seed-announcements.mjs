#!/usr/bin/env node
// Validates supabase/seed/announcements.json against announcements.schema.json,
// checks it locally, then imports it via the transactional
// public.import_announcements() RPC. All validation happens before any network
// call so a bad file never causes a partial write.
//
// Publishing a row here is what sends its push notification once push is
// configured (see supabase/functions/send-announcement). Without FCM
// credentials the send is a logged no-op and the feed still updates.
import { readFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import Ajv2020 from "ajv/dist/2020.js";
import addFormats from "ajv-formats";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const seedDir = path.join(__dirname, "..", "supabase", "seed");
const schemaPath = path.join(seedDir, "announcements.schema.json");
const dataPath = path.join(seedDir, "announcements.json");

// Must match the announcements_category_valid check constraint and
// AnnouncementCategory in the app. Checked here as well as by the schema's
// enum so that a schema edit cannot quietly widen the set.
const CATEGORIES = new Set(["general", "schedule", "keynote", "emergency"]);

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

function validateLocally(data) {
  const errors = [];
  const seen = new Set();
  for (const announcement of data.announcements) {
    if (seen.has(announcement.slug)) errors.push(`duplicate announcement slug: ${announcement.slug}`);
    seen.add(announcement.slug);
    if (!CATEGORIES.has(announcement.category)) {
      errors.push(`announcement "${announcement.slug}" has unknown category "${announcement.category}"`);
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

  const localErrors = validateLocally(data);
  if (localErrors.length > 0) {
    console.error(`validation failed with ${localErrors.length} error(s):`);
    for (const err of localErrors) console.error(`  ${err}`);
    process.exit(1);
  }

  const published = data.announcements.filter((a) => a.is_published).length;
  console.log(`validated ${data.announcements.length} announcements (${published} published)`);

  const supabaseUrl = process.env.SUPABASE_URL;
  const serviceRoleKey = process.env.SUPABASE_SERVICE_ROLE_KEY;
  if (!supabaseUrl) fail("SUPABASE_URL is not set");
  if (!serviceRoleKey) fail("SUPABASE_SERVICE_ROLE_KEY is not set");

  const endpoint = new URL("/rest/v1/rpc/import_announcements", supabaseUrl);
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
    fail(`import_announcements RPC failed with ${response.status}: ${body}`);
  }

  console.log("import succeeded:");
  console.log(JSON.stringify(JSON.parse(body), null, 2));
}

main().catch((err) => fail(describeError(err)));
