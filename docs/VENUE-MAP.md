# Authoring the venue map

The Map tab draws vector geometry, not an image. This is how that geometry gets
made, from a floor plan PDF to `supabase/seed/venue.json`.

Everything in the toolchain is open source: **QGIS** (GPL-2.0) to trace,
`scripts/venue-from-geojson.mjs` to fold the exports into the seed file,
`make seed-venue` to import it.

---

## The coordinate system

**Venue-local metres. Origin at the venue's north-west corner, X increasing
right (east), Y increasing *down* (south).**

Not longitude/latitude. Three reasons:

1. Euclidean distance in these coordinates *is* distance in metres. No
   projection maths anywhere in the app.
2. The client renders with a plain affine transform — `x * scale + offset` —
   which is unit-testable arithmetic rather than a mapping library.
3. Y-down matches the screen, so a traced plan is not upside down in Compose.

If the venue ever needs to sit on a real basemap, a single affine transform
converts the whole dataset. Nothing about this choice forecloses that.

> **Watch the Y axis.** QGIS's canvas is Y-up. Set the layer CRS to a local
> engineering CRS and *negate Y on export*, or trace with the raster placed so
> that increasing Y goes south. The quickest check: the room you traced at the
> top of the plan must have the **smallest** Y in the exported GeoJSON.

---

## 1. Set up the QGIS project

1. **Project → Properties → CRS**: pick a projected metric CRS. Any of them
   works because the geometry never leaves this project — the layer is treated
   as venue-local metres downstream regardless. `EPSG:3857` is a fine default.
2. **Layer → Add Layer → Add Raster Layer**: the floor plan PDF or image.
3. Georeference it so that one traced metre is one real metre — use the plan's
   printed scale bar, or a known door width, with the **Georeferencer**
   (Raster → Georeferencer) and a linear transform.

Do this once per floor, in one project with one raster per level group.

> **The plan is a tracing reference, not a shipped asset.** A white
> architectural drawing under dark-mode vectors looks wrong on `#111111`, so the
> app ships vectors only. Nothing you import here includes the raster.

---

## 2. Digitize two layers per floor

**Level outline** — one polygon, the floor's footprint.

**Features** — the rooms and points of interest. Create it as a *polygon* layer
for rooms and a second *point* layer for POIs, or one layer of each geometry
type; the export script accepts both.

Give the feature layer these attribute-table fields:

| Field | Type | Required | Notes |
|---|---|---|---|
| `slug` | text | ✅ | `^[a-z0-9]+(-[a-z0-9]+)*$`, unique within the level |
| `name` | text | | The label drawn on the map. Defaults to `slug` |
| `category` | text | | One of the ten below. Defaults to `other` |
| `location` | text | | Slug of a `locations` row in `schedule.json` |
| `sort_order` | integer | | Draw order within the level |
| `label_x`, `label_y` | double | | Label position; defaults to the centroid |

`category` is one of `stage`, `room`, `food`, `restroom`, `booth`, `entrance`,
`stairs`, `elevator`, `corridor`, `other`. Set it up as a **Value Map** widget
in the layer's Attributes Form so it cannot be typed wrong.

### `location` is the important one

It is the only field that connects the map to the programme. A feature with
`location = "main-stage"` is what lets the map show which talk is on in Main
Stage right now, and what "Show on map" on a session jumps to. Rooms that host
sessions need it; restrooms and stairs do not.

The slug must match a `locations[].slug` in `supabase/seed/schedule.json`.
`make seed-venue` fails on a slug that does not.

### Rings and holes

A room is one polygon. Its first ring is the exterior; further rings are holes —
a courtyard, a lift core, an atrium. Draw a hole with QGIS's **Add Ring** tool,
not as a second feature.

A room that is genuinely two disconnected shapes must be two features with two
slugs. The export script rejects multi-part geometry rather than guessing.

---

## 3. Export

For each layer: **right-click → Export → Save Features As… → GeoJSON**, into one
directory. Then write a `manifest.json` beside them:

```json
{
  "venue":  { "slug": "tbc-conference", "name": "TBC Conference Venue" },
  "levels": [
    { "slug": "ground", "name": "Ground", "ordinal": 0,
      "outline": "ground-outline.geojson", "features": "ground-features.geojson" },
    { "slug": "first", "name": "1st Floor", "ordinal": 1,
      "outline": "first-outline.geojson", "features": "first-features.geojson" }
  ]
}
```

`ordinal` is the display order of the floor tabs: 0 is ground, 1 is the first
floor, and a basement is -1.

Then fold it into the seed document:

```bash
node scripts/venue-from-geojson.mjs path/to/export-dir
```

This **writes** `supabase/seed/venue.json`; it does not import anything. Read
the diff first — it is the artifact that gets reviewed, not the QGIS project.

---

## 4. Import

```bash
export SUPABASE_URL=https://<your-project-ref>.supabase.co
export SUPABASE_SERVICE_ROLE_KEY=<service-role-key>
make seed-venue
```

`make seed-venue` validates `venue.json` against `venue.schema.json`, checks
that every ring closes and encloses an area, and resolves every `location` slug
against `schedule.json` — all before the first network call. It then calls the
transactional `import_venue()` RPC, which upserts by slug and prunes levels and
features no longer in the file.

Reconciliation is **scoped to the venue in the payload**. Importing
`tbc-conference` never touches another venue's rows.

The RPC's response reports `unresolved_locations`; it should be `0`.

---

## The überlab dataset

The venue in `supabase/seed/venue.json` is **überlab, House of Communication
München** (August-Everding-Str. 25). It was *not* traced in QGIS — it was
hand-authored from the venue's own brochure,
`Ueberlab_Conferencing-und-Events_Januar.pdf`, whose **page 7** carries the
ground-floor plan ("Überblick buchbaren Flächen EG") and **page 8** the first
floor ("1. OG"), each with the room table that gives every space its area and
capacity. The QGIS round trip above stays the documented path for a re-trace;
for forty rectilinear rooms off an orthographic plan it cost more than it saved.

Things worth knowing before editing it:

**The venue is three detached houses**, JOIN, HEART and LAB, left to right,
about 190 m end to end. They are separate buildings — there is no single
building outline, so neither level defines `outline`; each house is a feature of
its own in category `other` and the floors' extent comes from the features. The
two bridges that join them exist only on the 1st floor, which is why the ground
floor has none.

**Both plans are raster images inside the PDF**, and the two are drawn at
*different* scales — 33.9 px/m on page 7, 32.9 px/m on page 8 at a 300 dpi
render — with different gaps between the houses. So the floors cannot share one
transform: each house is registered separately, its traced centre placed on the
metre box the 1. OG sheet gives it. Calibration was cross-checked against the
printed dimensions of Konferenz 3, 5, 6 and 9+10, Flex, Studio and Worklab 1+2;
every traced room lands within ~8 % of its printed `Größe`.

**The purple fills on the plans are bookable areas, not walled rooms.** JOIN's
is Workcafé and Join Lounge in one envelope; HEART's is the whole GERN hall with
SPRESS and the PS5 Gaming Lounge as zones inside it; LAB's runs Worklab 1 into
Worklab 2. Each envelope is kept as the traced polygon and the named zones are
drawn over it with a higher `sort_order`, so the zones label and tap correctly
while the hall keeps its real shape. A zone's rectangle is sized to its printed
m², not traced — there is no line on the plan to trace.

**The conference's own layout comes from the markup on page 8**, which somebody
added over the 1. OG plan: Ticketing at the JOIN-side mouth of the bridge, an
arrow east into HEART, four unlabelled markers along that route (carried as
`booth-1`…`booth-4`), "Talk rooms 1-2" pointing at Konferenz 9 and 10, and
"Dining area (downstairs) / Talk rooms 3-4" against the HEART stair. That last
one is the one inference in the dataset: it says those three are one floor down
from that stair, and the spaces down there are GERN, SPRESS and the PS5 Gaming
Lounge, so they carry `dining-area`, `talk-room-3` and `talk-room-4`.

Two conflicts inside the brochure were resolved in favour of the plan tables:
GERN is 900 m² on p32 but 2 000 m² in the EG table (900 is the hall, 2 000 is
GERN *gesamt*, including the Holztafel and the Biergarten), and Opie Green is
32 m² on p36 but 50 m² in the 1. OG table. The traced GERN envelope comes out at
about 1 200 m², which is the hall plus the SPRESS and PS5 zones — consistent
with the 900 m² reading.

**Not in the dataset:** a main entrance marker. The outer walls carry door
symbols in several places and the brochure never says which one the public
enters by, and a guessed entrance is worse than none. Add it once someone
who has been there can point at it.

---

## Editing without QGIS

`supabase/seed/venue.json` is plain JSON and hand-editing it is fine for a room
rename, a category change or a nudged label. Re-run `make seed-venue` after.

Re-running `venue-from-geojson.mjs` overwrites the file, so a hand edit that
must survive belongs in the QGIS attribute table instead.

---

## Alternatives that were considered

**JOSM + the `indoorhelper` plugin** (GPL) is the right tool if the venue should
also live in OpenStreetMap under Simple Indoor Tagging. It is a heavier workflow
and only worth it to contribute the data back.

**Open Location Stack `floorplan-editor`** is the closest purpose-built thing —
browser-based, MapLibre, exports GeoJSON/IMDF — but at the time of writing it
has one star and **no declared licence**. Worth watching; not worth depending
on.

**OpenIndoorMaps** is pre-alpha and has no editor yet.

---

## Why the app renders this itself

`maplibre-compose` would give pan, zoom and label-collision for free, and it was
the serious alternative. It was rejected because `iosApp.xcodeproj` has no SPM
packages and no CocoaPods, so MapLibre Native on iOS means restructuring the iOS
build for tens of megabytes of binary — to draw a dozen rooms that need no
tiles, in a style that would have to be hand-written anyway to match `#111111`.

A Compose `Canvas` keeps the whole feature in `commonMain` and turns the
geometry into plain unit-testable Kotlin (`ui/map/MapGeometry.kt`,
`MapGeometryTest`). The data is GeoJSON either way, so switching later remains
open.
