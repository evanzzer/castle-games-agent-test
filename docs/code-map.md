# Current code map

## Map and campaign data

| File | Responsibility |
| --- | --- |
| `src/castle/domain/stages/Campaign.java` | Campaign boundaries, labels, campaign IDs, strategic AI selection and enemy spell prices/levels. |
| `src/castle/domain/stages/StageDefinition.java` | Validated, cached definitions for all 90 stages; immutable building, region and altar records; creation of independent match objects. |
| `src/castle/domain/stages/CampaignInfo.java` | Shared name/description lookup and uniform stage title formatting; no campaign data. |
| `src/castle/domain/stages/original/Original.java` | All Original campaign names, descriptions, buildings, terrain regions and altars. No reference-file import is required to build or run. |
| `src/castle/domain/stages/frontier/Frontier.java`, `src/castle/domain/stages/wild/Wilds.java` | Generated data for stages 31–60 and 61–90, including names, descriptions, buildings, regions and altars. |
| `assets/map/campaigns.json` | Campaign navigation markers only. |
| `assets/terrain/{original,frontier,wild}/worlds.json` | Each campaign’s 30 battlefield dimensions, overlays and scenery placements. Overlay entries point to PNGs in the same terrain category. |
| `assets/terrain/{original,frontier,wild}/manifest.json` | Geometry-fingerprint indexes with the established `schema_version` and `overlays` format, identical in structure across the three campaigns. |
| `tools/generate_frontier.py`, `generate_wilds.py` | Deterministic terrain/campaign authoring. Metadata for authoring and regression checks lives under `tools/data/`. |

`Engine(stage, rank)` selects a `StageDefinition`, obtains its campaign policy and populates fresh forts, region point arrays and altars. Battlefield visual layouts live beside their terrain assets, while campaign navigation stays under `assets/map/`. Stage data is parsed once. Game state never mutates the shared stage definitions. The original, Frontier and Wild tables use this same path instead of separate loading branches.

## Runtime and presentation

| File | Responsibility |
| --- | --- |
| `src/castle/domain/Engine.java` | Pure Java simulation: deployment, recruitment, multi-team sieges, field encounters, spell effects, terrain sampling, altar buffs and victory/defeat state. |
| `src/castle/Main.java` | Executable entry point, Android lifecycle and dependency composition only. |
| `src/castle/domain/AiCommander.java` | Enemy timing, target selection and spell tactics, extracted from the match aggregate. |
| `src/castle/domain/PlayerProfile.java` | Encapsulated progress, upgrade purchases, settings and rewards. |
| `src/castle/domain/WorldLayout.java`, `MapMarker.java` | Immutable typed battlefield and navigation data. |
| `src/castle/application/MatchFactory.java` | Stage access validation and upgraded match creation. |
| `src/castle/application/MatchAudioEvents.java` | Once-only event queue delivery and raid announcement cursor. |
| `src/castle/application/ProfileRepository.java` | Save storage port. |
| `src/castle/infrastructure/android/AndroidProfileRepository.java` | Current and legacy preferences, normalized saves and migration mapping. |
| `src/castle/infrastructure/android/AndroidGameResources.java` | Atlas, font, world and marker parsing and bitmap caching. |
| `src/castle/presentation/GameResources.java` | Typed resource interface consumed by presentation. |
| `src/castle/presentation/GameView.java` | Screens, input, selection, camera and presentation coordination. |
| `src/castle/presentation/CanvasPainter.java` | Canvas image, sprite, font and panel primitives. |
| `src/castle/presentation/ArmyRenderer.java` | Formations, flags, anchors, buffs and siege/encounter visuals. |
| `src/castle/application/GameAudio.java` | Platform-free audio interface used by presentation. |
| `src/castle/infrastructure/android/AndroidGameAudio.java` | Android SoundPool/MediaPlayer adapter, sample loading, stream ownership and lifecycle. |
| `src/castle/infrastructure/android/AssetMetadata.java` | Versioned JSON loading and validated sprite/font frame rectangles. |
| `src/castle/infrastructure/android/TerrainArt.java` | Terrain PNG baking from the simulation's actual region polygons. |
| `src/castle/domain/UpgradeRules.java` | Shared upgrade limits, purchase eligibility and saved-state normalization. |

`assets/manifest.json` identifies every runtime atlas, the number font, campaign navigation and terrain layout files and fingerprint manifests. Atlases and fonts use the same JSON frame rectangle format. The renderer follows complete asset-relative image paths, so folder names do not encode engine behavior. No legacy configuration parser remains in the runtime.

## Combat and audio flow

An `Engine.March` retains its origin building kind, strength, destination, buffs and shared raid token. Field encounters combine survivors under a lightweight token. A siege tracks which tokens have already been announced, preventing duplicate battle-start sound when the same combined army arrives in several groups. Friendly reinforcement arrivals add strength without announcing an attack.

Engine spell and volley events enqueue `AudioCue` entries. `MatchAudioEvents` forwards the events through `GameAudio`; `AndroidGameAudio` waits for samples, owns and stops meteor/tornado streams, and pauses effects with the game. Background music is managed separately. Audio RNG is independent from simulation RNG.

Building combat uses the four combat-atlas cloud frames, with horizontal scale 0.70, vertical scale 0.90 and a center 16 units above the building ground anchor. Field encounters use their separate cloud frames. Counts are drawn above buildings and stack vertically during sieges.

## Validation and build

`tests/run-checks.sh` recursively compiles the domain and application classes without Android dependencies and runs all `*Check.java` checks except Android instrumentation. `ApplicationCheck` covers progression, match creation and event delivery; `ProfileUiCheck` verifies Android save migration. `StageCatalogCheck` verifies all campaign boundaries and checks that shared definitions cannot leak mutable state between games. Python checks validate every JSON atlas/font frame and world overlay, then verify deterministic terrain regeneration and overlay fingerprints.

`tests/run-ui-check.sh` builds instrumentation using the same SDK and signing key as `build.sh`. Gameplay, campaign information, terrain, spell audio and raid grouping each have focused UI checks. `scripts/android-env.sh` is the shared SDK/signing configuration. Generated build files remain under ignored `build/`; the retained key stays outside that disposable directory at `.local/signing/debug.jks`.
