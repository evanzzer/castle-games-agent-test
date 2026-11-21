# Current code map

## Map and campaign data

| File | Responsibility |
| --- | --- |
| `src/castle/Campaign.java` | Campaign boundaries, labels, marker keys, strategic AI selection and enemy spell prices/levels. |
| `src/castle/StageDefinition.java` | Validated, cached definitions for all 90 stages; immutable building, region and altar records; creation of independent match objects. |
| `src/castle/CampaignInfo.java` | Original campaign names/descriptions and uniform stage title formatting. |
| `src/castle/Missions.java`, `Altars.java` | Checked-in original building, terrain and altar data. No reference-file import is required to build or run. |
| `src/castle/Frontier.java`, `Wilds.java` | Generated data for stages 31–60 and 61–90, including names, descriptions, buildings, regions and altars. |
| `assets/map/worlds.json` | World dimensions, background overlays, scenery and three campaign marker lists. Overlay entries point directly to files below `assets/terrain/`. |
| `tools/generate_frontier.py`, `generate_wilds.py` | Deterministic terrain/campaign authoring. Metadata for authoring and regression checks lives under `tools/data/`. |

`Engine(stage, rank)` selects a `StageDefinition`, obtains its campaign policy and populates fresh forts, region point arrays and altars. Stage data is parsed once. Game state never mutates the shared stage definitions. The original, Frontier and Wild tables use this same path instead of separate loading branches.

## Runtime and presentation

| File | Responsibility |
| --- | --- |
| `src/castle/Engine.java` | Pure Java simulation: deployment, recruitment, multi-team sieges, field encounters, spell effects, terrain sampling, altar buffs, AI and victory/defeat state. |
| `src/castle/Main.java` | Android lifecycle, Canvas rendering, selection/drag commands, campaign navigation, upgrades, result screens and audio playback. |
| `src/castle/AssetMetadata.java` | Versioned JSON loading and validated sprite/font frame rectangles. |
| `src/castle/TerrainArt.java` | Terrain PNG baking from the simulation's actual region polygons. |
| `src/castle/UpgradeRules.java` | Shared upgrade limits, purchase eligibility and saved-state normalization. |

`assets/manifest.json` identifies every runtime atlas, the number font and the world metadata. Atlases and fonts use the same JSON frame rectangle format. The renderer follows complete asset-relative image paths, so folder names do not encode engine behavior. No legacy configuration parser remains in the runtime.

## Combat and audio flow

An `Engine.March` retains its origin building kind, strength, destination, buffs and shared raid token. Field encounters combine survivors under a lightweight token. A siege tracks which tokens have already been announced, preventing duplicate battle-start sound when the same combined army arrives in several groups. Friendly reinforcement arrivals add strength without announcing an attack.

Engine spell and volley events enqueue `AudioCue` entries. The Android layer waits for SoundPool samples to load, owns and stops meteor/tornado streams, and pauses effects with the game. Background music is managed separately. Audio RNG is independent from simulation RNG.

Building combat uses the four combat-atlas cloud frames, with horizontal scale 0.70, vertical scale 0.90 and a center 16 units above the building ground anchor. Field encounters use their separate cloud frames. Counts are drawn above buildings and stack vertically during sieges.

## Validation and build

`tests/run-checks.sh` compiles the pure Java engine without Android dependencies and runs all `*Check.java` checks except Android instrumentation. `StageCatalogCheck` verifies all campaign boundaries and checks that shared definitions cannot leak mutable state between games. Python checks validate every JSON atlas/font frame and world overlay, then verify deterministic terrain regeneration and overlay fingerprints.

`tests/run-ui-check.sh` builds instrumentation using the same SDK and signing key as `build.sh`. Gameplay, campaign information, terrain, spell audio and raid grouping each have focused UI checks. `scripts/android-env.sh` is the shared SDK/signing configuration. Generated build files remain under ignored `build/`; the retained key stays outside that disposable directory at `.local/signing/debug.jks`.
