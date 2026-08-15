#!/usr/bin/env node
// Folds a directory of QGIS GeoJSON exports into supabase/seed/venue.json.
//
//   node scripts/venue-from-geojson.mjs <export-dir> [--out <path>] [--precision 3]
//
// <export-dir> must contain a manifest.json naming the venue and, per level,
// the outline and feature layers exported from QGIS:
//
//   {
//     "venue":  { "slug": "tbc-conference", "name": "TBC Conference Venue" },
//     "levels": [
//       { "slug": "ground", "name": "Ground", "ordinal": 0,
//         "outline": "ground-outline.geojson", "features": "ground-features.geojson" }
//     ]
//   }
//
// Each feature layer is a GeoJSON FeatureCollection whose per-feature
// properties carry slug / name / category / location / sort_order. Those are
// QGIS attribute-table columns; see docs/VENUE-MAP.md for the field list.
//
// The output is written, not imported — review the diff, then `make seed-venue`.
import { readFile, writeFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const defaultOut = path.join(__dirname, "..", "supabase", "seed", "venue.json");

function fail(message) {
  console.error(`error: ${message}`);
  process.exit(1);
}

function parseArgs(argv) {
  const positional = [];
  const flags = {};
  for (let i = 0; i < argv.length; i++) {
    if (argv[i].startsWith("--")) flags[argv[i].slice(2)] = argv[++i];
    else positional.push(argv[i]);
  }
  if (positional.length !== 1) {
    fail("usage: venue-from-geojson.mjs <export-dir> [--out <path>] [--precision <n>]");
  }
  return {
    dir: positional[0],
    out: flags.out ? path.resolve(flags.out) : defaultOut,
    precision: flags.precision === undefined ? 3 : Number(flags.precision),
  };
}

async function loadJson(filePath) {
  return JSON.parse(await readFile(filePath, "utf-8"));
}

/** Millimetre precision is already more than a traced floor plan can justify. */
function roundPosition(position, precision) {
  const factor = 10 ** precision;
  return [
    Math.round(position[0] * factor) / factor,
    Math.round(position[1] * factor) / factor,
  ];
}

function roundGeometry(geometry, precision) {
  if (geometry.type === "Point") {
    return { type: "Point", coordinates: roundPosition(geometry.coordinates, precision) };
  }
  if (geometry.type === "Polygon") {
    return {
      type: "Polygon",
      coordinates: geometry.coordinates.map((ring) =>
        ring.map((position) => roundPosition(position, precision))
      ),
    };
  }
  // MultiPolygon is what QGIS writes when a layer was created as multi-part,
  // even for a single-part room. Unwrap the one-part case rather than making
  // the author redo the layer; anything genuinely multi-part is an error,
  // because a room that is two disjoint shapes needs two features.
  if (geometry.type === "MultiPolygon") {
    if (geometry.coordinates.length !== 1) {
      throw new Error(
        `MultiPolygon with ${geometry.coordinates.length} parts — split it into one feature per part`
      );
    }
    return roundGeometry({ type: "Polygon", coordinates: geometry.coordinates[0] }, precision);
  }
  throw new Error(`unsupported geometry type "${geometry.type}"`);
}

/** QGIS writes absent attributes as null, and the seed schema forbids nulls. */
function putIfPresent(target, key, value) {
  if (value !== null && value !== undefined && value !== "") target[key] = value;
}

function toSeedFeature(geoFeature, index, layerLabel, precision) {
  const properties = geoFeature.properties ?? {};
  const slug = properties.slug;
  if (!slug) {
    throw new Error(`${layerLabel}: feature ${index} has no "slug" property`);
  }
  // Key order is fixed, and matches venue.schema.json, so that re-running the
  // script produces a reviewable diff rather than a reshuffled file.
  const feature = { slug, name: properties.name ?? slug };
  putIfPresent(feature, "category", properties.category);
  putIfPresent(feature, "location", properties.location);
  feature.geometry = roundGeometry(geoFeature.geometry, precision);
  if (properties.label_x !== null && properties.label_x !== undefined) {
    feature.label_anchor = {
      type: "Point",
      coordinates: roundPosition([Number(properties.label_x), Number(properties.label_y)], precision),
    };
  }
  if (properties.sort_order !== null && properties.sort_order !== undefined) {
    feature.sort_order = Number(properties.sort_order);
  }
  return feature;
}

/**
 * `JSON.stringify(_, null, 2)` puts every coordinate on its own line, which
 * turns a nudged wall into a hundred-line diff and defeats the "review the
 * diff" step this script exists to enable. Positions are collapsed back onto
 * one line each; everything else keeps standard formatting.
 */
function formatDocument(document) {
  return JSON.stringify(document, null, 2).replace(
    /\[\s*\n\s*(-?\d+(?:\.\d+)?),\s*\n\s*(-?\d+(?:\.\d+)?)\s*\n\s*\]/g,
    "[$1, $2]"
  );
}

async function main() {
  const { dir, out, precision } = parseArgs(process.argv.slice(2));
  const manifest = await loadJson(path.join(dir, "manifest.json"));

  if (!manifest.venue?.slug) fail("manifest.json: venue.slug is required");

  const levels = [];
  for (const [index, level] of (manifest.levels ?? []).entries()) {
    if (!level.slug) fail(`manifest.json: levels[${index}].slug is required`);

    const seedLevel = {
      slug: level.slug,
      name: level.name ?? level.slug,
      ordinal: level.ordinal ?? index,
    };

    if (level.outline) {
      const outline = await loadJson(path.join(dir, level.outline));
      // The outline layer holds one polygon. Accept a bare geometry, a Feature
      // or a one-item FeatureCollection, because QGIS writes all three
      // depending on how the layer was created.
      const geometry =
        outline.type === "FeatureCollection"
          ? outline.features[0]?.geometry
          : outline.type === "Feature"
            ? outline.geometry
            : outline;
      if (!geometry) fail(`${level.outline}: no geometry found`);
      seedLevel.outline = roundGeometry(geometry, precision);
    }

    if (level.features) {
      const collection = await loadJson(path.join(dir, level.features));
      const layerLabel = `${level.features}`;
      seedLevel.features = (collection.features ?? []).map((feature, i) => {
        try {
          return toSeedFeature(feature, i, layerLabel, precision);
        } catch (err) {
          fail(err.message);
        }
      });
    }

    levels.push(seedLevel);
  }

  const document = {
    venue: { slug: manifest.venue.slug, name: manifest.venue.name ?? manifest.venue.slug },
    levels: levels.sort((a, b) => a.ordinal - b.ordinal),
  };

  await writeFile(out, `${formatDocument(document)}\n`, "utf-8");

  const featureCount = levels.reduce((sum, l) => sum + (l.features?.length ?? 0), 0);
  console.log(`wrote ${out}: ${levels.length} levels, ${featureCount} features`);
  console.log("review the diff, then: make seed-venue");
}

main().catch((err) => fail(err.stack ?? String(err)));
