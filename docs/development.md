# Development guide

Native Android Canvas game, version **0.16**, with 90 sequential stages across Original, Frontier and Wild. This repository contains the current implementation, runtime assets, terrain authoring tools and tests. The decrypted IPA and reference workspace are kept outside the repository.

## Build

Run commands from the repository root.

Requires `rg`, a JDK with `javac --release 8`, Android SDK platform 33 and build-tools 33.0.1. Set `ANDROID_SDK_ROOT` or `ANDROID_HOME`; macOS defaults to `~/Library/Android/sdk`. `ANDROID_PLATFORM_VERSION` and `BUILD_TOOLS_VERSION` override the SDK defaults.

```sh
./build.sh
```

Output: `build/Castle-Wars-Rebuilt.apk`. No Gradle or network access is required.

The existing signing key is retained locally at `.local/signing/debug.jks` and ignored by Git. `CASTLE_SIGNING_KEY` can select another key. Keep this key to install future builds over the existing app. A fresh clone without a key generates its own debug key; it cannot replace an installed app signed with the retained key.

## Asset layout

```text
assets/
  audio/
    music/                  Menu and battle tracks
    effects/                Movement, combat, spells and result sounds
  fonts/                    Number bitmap and JSON glyph metadata
  map/
    backgrounds/            Map background artwork
    campaigns.json          Campaign navigation markers
  sprites/
    units/                  Army atlas and JSON frame metadata
    effects/                Combat and spell atlas and JSON metadata
    scenery/                Scenery atlas and JSON metadata
  terrain/
    original/               Original overlays, worlds.json and manifest.json
    frontier/               Frontier overlays, worlds.json and manifest.json
    wild/                   Wild overlays, worlds.json and manifest.json
  ui/
    atlases/                Interface atlases and JSON frame metadata
    help/                   Help screens and control atlas
    loading/                Loading artwork
    screens/                Standalone interface backgrounds
  manifest.json             Runtime atlas, font and world entry points
```

Each terrain campaign uses the same two-file format. `manifest.json` retains the established Frontier/Wild format: `schema_version` and `overlays` (geometry fingerprints). `worlds.json` stores `schema_version`, `campaign`, `first_stage` and 30 battlefield layouts. The root manifest links both files for each campaign; `map/campaigns.json` holds navigation markers only.

Runtime metadata uses JSON schema version 1. Atlas and font entries share `frame: [x, y, width, height]`; fonts additionally define advances and line height. Image paths are complete, asset-relative paths. There are no runtime `.conf`, `.font` or legacy GUI XML files. Artwork and WAV bytes are preserved during relocation.

## Source and code map

`src/castle/Main.java` is the executable entry point outside the grouped supporting folders. `build.sh` remains at the repository root and recursively discovers Java sources. The supporting folders are `domain/` (with `stages/`), `application/`, `presentation/` and `infrastructure/android/`; all retain the shared `castle` package and package-private APIs.

See [code-map.md](code-map.md) for the current module responsibilities, map loading, simulation, rendering and audio flow. `Campaign` owns campaign boundaries and AI spell policy. `StageDefinition` parses the compact authored map data once into immutable stage definitions; each new game receives independent mutable objects.

## Validate

Requires Python 3 and `rg` in addition to the JDK:

```sh
./tests/run-checks.sh
```

This recursively enforces the architecture/comment boundaries, checks profile encapsulation and application use cases, runs the simulation, campaign, terrain, audio-event, raid-wave and stage-catalog checks, validates JSON metadata and asset bounds, and verifies deterministic terrain generation. Generation checks regenerate the authored data in place; unchanged inputs must produce unchanged tracked files.

For Android UI checks, start an Android emulator or connect a test device:

```sh
./build.sh
adb -s emulator-5556 install --no-incremental -r build/Castle-Wars-Rebuilt.apk
./tests/run-ui-check.sh ProfileUiCheck
./tests/run-ui-check.sh UiCheck
./tests/run-ui-check.sh CampaignInfoUiCheck
./tests/run-ui-check.sh RaidUiCheck
./tests/run-ui-check.sh AudioUiCheck
./tests/run-ui-check.sh PresentationUiCheck
```

Set `ANDROID_SERIAL` to use another device. Screenshots and baked terrain are written to the app's external files directory on that test device; keep retrieved previews under ignored `outputs/previews/`.

## Terrain authoring

```sh
python3 tools/generate_frontier.py
python3 tools/generate_wilds.py
```

These update the corresponding authored Java tables, the campaign’s `terrain/<campaign>/worlds.json` and `tools/data/*-design.json`, while preserving other terrain campaigns and navigation markers. Frontier control plans are in `tools/data/frontier-region-plans.json`. The committed PNG overlays are tied to their geometry through `assets/terrain/{frontier,wild}/manifest.json`.

When changing terrain geometry, use `TerrainBakeCheck` on Android to regenerate and validate the PNGs, then retrieve the emitted images and manifest into the matching terrain folder. Terrain baking uses the current `StageDefinition` through `Engine`, so collision geometry and visual silhouettes share the same regions.

## Current gameplay policy

- All castles start at 500 troops. Recruitment is 50 per 1.25 seconds and stops during combat; siege casualties tick every second.
- Troop movement is 50 units per second, with the existing archer-origin bonus and terrain/upgrade modifiers. Deployment sends half the garrison.
- Campaign I has no AI spells. Campaign II uses level 0 spells at 1.75 times cost; Campaign III uses level 2 spells at 1.4 times cost.
- Reinforcements appear 100 units from the chosen castle; their tier totals are 100, 240, 280, 480, 540 and 800.
- Music and effects play at 100% in-app volume. Movement and battle-start variants use an independent RNG. Combined raid groups announce once per siege; friendly reinforcements are silent on arrival.
- Stage unlocks are sequential. Upgrade levels stop at tier 5.

Build products, emulator state, keys, credentials and previews are ignored. This is a local Git repository; no remote publication is configured.
