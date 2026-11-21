# Castle Wars Rebuilt

A separate Android app built around assets and mission data extracted from the supplied Castle Wars 1.2.2 IPA. Package game.castlewars.rebuilt, display name Castle Wars Rebuilt. It installs alongside the earlier remake.

## What was reconstructed

The app uses original title artwork, menu controls, campaign map, 30 campaign marker positions, upgrade icons and descriptions, tutorial pages, pause and results screens, unit and building sprite atlases, bitmap number font, spell sprites, scenery and sound files.

The original 30 missions use their extracted building coordinates, map dimensions, scenery records and terrain overlays. A second campaign adds 30 authored Frontier building layouts, a campaign-map continuation using the original artwork, and original terrain PNG overlays paired with their matching effect regions. Terrain effects use extracted poison and marsh polygons. Larger maps support pinch zoom and dragging empty ground to pan. Campaign stages unlock sequentially: stage 1 is available initially, and clearing a stage unlocks the next, up to stage 90. The Frontier map unlocks after stage 30, and the Wilds map after stage 60. Cleared stages remain replayable.

Select one or more red castles and tap a target to deploy half the garrison. Double-tap a red castle to select all. Drag from any selected red castle to deploy the whole selection. Tap moving red armies to select them; tap or drag to redirect them to another building mid-route. Empty ground cannot receive a deployment or redirection. Castles under siege cannot deploy. Select a spell and tap the target, or drag the spell icon onto the battlefield.

Recovered rules include base siege damage, troop counts, deployment fraction, terrain multipliers, poison ticks, and base spell values. Original sprites animate movement, siege combat, flags and spell effects. Menu and randomly selected battle music loop. Victory and defeat use their original sound files.

## What remains approximate

This is an Android reconstruction, not the original executable running on Android. The encrypted/decrypted IPA and ARM executable are not included in this source package.

AI deployment now follows recovered nation-turn timing, nearby target choice, strongest-source grouping and departure thresholds. Obstacle path routing remains approximate. Enemy spell decisions now use the reconstructed tactics described in Version 0.9. Arrow damage, recruitment interval, upgrade progression and several upgrade multipliers remain adaptations. The original eleven upgrade categories are restored. Every category has five upgrade tiers. Levels appear inside the purple counters. Rates remain adaptations where the original values are unresolved. Attack and defense modifiers use the recovered 20% scaling.

Reinforce currently adds its troop groups directly. Storm moves through the selected area. Its orbit and Meteor landing timing are reconstructed. Original pathfinding and precise altar collision shapes remain approximate. Multiple attacking teams are represented at a castle, with a simplified ownership handoff.

Neutral buildings do not recruit. In combat, recruitment and over-cap decay pause. Outside combat, overflow drops by 50 per recruitment tick.

## Build and checks

Requires JDK 11 or later, Android SDK Platform 33 and Build Tools 33.0.1. Set ANDROID_SDK_ROOT to the SDK directory and run ./build.sh. The script produces a signed development APK in build/Castle-Wars-Rebuilt.apk. Installation requires Android 6 or later.

Run simulation checks:

```sh
javac --release 8 -d build/check src/castle/Engine.java src/castle/Missions.java src/castle/Altars.java src/castle/Frontier.java src/castle/Wilds.java src/castle/UpgradeRules.java tests/EngineCheck.java tests/FrontierCheck.java
java -cp build/check castle.EngineCheck
```

Regenerate world.json using python3 import_world.py with the extracted reference data at ../castle-reference/assets. The generated mission records and world.json are already included, so regeneration is not required to build.

Verified on an Android 12 / API 31 arm64 emulator: installation, launch, menu, campaign, battle rendering, original troop counts, AI actions and pause controls. The simulation checks cover recovered damage formulas and actual terrain polygons across all 30 stages. This has not been tested on a physical Android device.


## Version 0.2

Fight sound is emitted once when a hostile marching group reaches a castle. Repeated combat ticks do not replay it. A later raider causes one additional sound. The decorative campaign header plate has been removed.

Troop appearance follows Troop::GetSoldierName from the ARMv7 executable: house type 11 uses soldier variant 1, crystal type 12 uses variant 2, and tower type 13 uses variant 3. Variant 3 has four frames per direction, while variants 1 and 2 have eight.

Tower arrows use the original arrow1, arrow2 and arrow3 atlas frames. The projectile travels toward its target over 0.2 seconds, as seen in Architecture::TowerAttack, and rotates to the flight direction.

A friendly castle under siege receives reinforcements when other friendly castles are selected and the besieged castle is tapped.

Nation::UseSkill indexes the table at ARMv7 address 0xa9650. In original icon order the costs are Meteor 230, Mutiny 200, Storm 300, Reinforce 450. Crystal energy starts at 200, multiplied by 1 + 0.25 per starting-energy upgrade level. The previous 100-energy ceiling was removed. The recovered resource timer adds 25 plus 40 per owned crystal formation every six seconds, scaled by the production upgrade.


## Version 0.3

- Selection uses the original red-glowing role atlas frames ending in _d.
- Building information expires after 2.2 seconds and clears on the next touch.
- Marches intercept opposing marches at their meeting point. The battle holds participants there, accepts arriving reinforcements, and releases the surviving side toward its destination. All parties take simultaneous one-second losses. Buffs affect the incoming-damage calculation.
- Troop numbers are tinted by team.
- Poison and slow terrain use original buffer_poison and buffer_spd animation frames. Terrain polygons from every stage are exercised in the simulation checks.
- Capacities verified against Architecture::OnHeartBeat are castles 1,000 / 2,000 / 3,000, archer towers 500 / 1,000 / 1,500, and crystal formations 1,000.
- The supplied IPA creates FIVE fireballs, not three. Skill_Meteor::CreateMeteor at ARMv7 0x2aa5c iterates indices 0–4. Each explosion calls Attack and applies base 40 damage in a 100-by-100 map-unit target area, increased by upgrades. Casts can target ground and affect enemy buildings, marching armies and combat participants.
- Storm can target ground, moves around the selected point and applies area damage to nearby buildings and armies every 0.4 seconds.
- Angel (Immunity) map property 100 grants 15 seconds of immunity. Shield (Defup) property 101 grants 30 seconds of defense with +0.5 in the incoming-damage denominator. Altars have a 30-second recovery. Effects use the original immunity_eff, defup_eff, buffer_immunity and buffer_def frames.
- Original altar positions are imported from stages 9, 10, 11, 15, 18, and 20–24.
- The original eleven-button upgrade layout is restored. Each upgrade stops at level 5, and the original purple counters hold level numbers. Saved levels above 5 are capped and excess levels are refunded as points once.
- Insufficient-resource and cooldown states use the original grey icon_skill*_i frames. Both tap and drag casting enforce availability.


## Version 0.4

Building artwork coordinates and troop ground coordinates are now imported separately. Architecture::SetMapObj at ARMv7 0x1342e–0x13450 subtracts the map object's pivot at +0x60/+0x64 from its artwork position. The previous build ignored that pivot.

For stage 5, the red castle's artwork position is (418, 107), but its troop ground point is (418, 121). The yellow castle's ground point is (56, 247). Using artwork coordinates caused the red-to-yellow route to miss the original slowdown polygon.

The actual ground route now gives the same 39 slowed simulation steps for red, blue, and yellow at equal speed upgrades. Separate tests confirm 50% movement for every team and for upgraded red troops. Confirmed visually in the emulator with red troops showing the original buffer_spd animation and a half-speed label.


## Version 0.5

Spell descriptions use current skill levels and show damage, duration, reinforcement count, cost, current availability and upgraded cooldown. Grey icons can be pressed for information, but cannot arm or cast a spell. Building specifications no longer appear on press.

Reinforce adds its full upgraded troop count to friendly castles, or creates/joins the player's attacking pool at a hostile or neutral castle. It neither changes ownership nor applies immediate combat damage.

Dragging a selected castle deploys all selected castles, each at 50%. Moving armies can be selected by tapping and redirected by tapping a destination or dragging a selected army. Empty-ground destinations keep armies on the field. Redirection retains their full strength, original troop variant, buffs and current position. Armies already locked in an encounter cannot be redirected.

Version 0.5 tested 29 imported polygons, but its importer omitted regions by associating data with the next object; those checks were incomplete. Version 0.6 corrects this (below). Terrain::SetMapObj drops the editor closing vertex; stage 26 has a slightly displaced closing point which is now also excluded. Small movement substeps reduce boundary skipping, edges count as inside, and terrain status also refreshes for stopped armies. The earlier classification of skipped records as decoration was incorrect; those bytes belonged to the next object. The original Terrain::OnUpdate applies poison once per terrain second; traversing a region entirely between poison ticks can avoid damage. Slowdown is immediate and halves the current movement speed.

Angel/shield carrying sequences now play once on pickup, then retain their final frame as a status indicator until the buff expires. Altar pickup effects were already one-shot.

### AI evidence and remaining differences

- Nation::AI (ARMv7 0x24cf0): schedules a randomized next turn, iterates owned castles, sorts the owned source list by troop strength, and can dispatch multiple strongest sources toward a common target. Sieged sources are skipped.
- GlobalVar::GetNpcAI (0x18f80): with stage progress p=stage/30, turn delay ranges from 10-5p to 15-5p seconds. The source-count parameter grows from 1 to integer(1+3p); Nation::AI samples with the upper endpoint excluded.
- Architecture::InitArchiOthers (0x14704) stores reachable buildings by distance. GetOtherArchi (0x14c20) chooses the first belonging to another nation. The reconstruction currently uses distance directly; original obstacle routing remains approximate.
- Architecture::AIToArchi (0x144d8): castles and crystals require at least 500 troops, towers require half their capacity, before deploying. The previous custom desirability score and terrain penalties have been removed.
- Architecture::AIToTroop (0x14a94): a garrison of at least 1000 can intercept hostile troops targeting it. The reconstruction sends to their current ground position; original moving-target tracking remains approximate.
- The IPA also attempts enemy skills. That enemy-skill decision path is not yet ported. Enemy nations now have independent seeded turn timers.

Simulation checks include reinforcement at friendly/neutral/enemy buildings, repeated siege reinforcement, group commands, redirection, upgraded tooltip values, all-team terrain behavior, AI source/target selection, and all 30 stages. Android instrumentation checks exercise the actual touch handlers for grey spells, building presses, multiple selected castles, and multiple moving armies. Run tests/run-ui-check.sh with an emulator running (default serial emulator-5556).


## Version 0.6 — complete terrain record import

The v0.5 importer read vertices after a marsh/poison name. TMapObj::Load (ARMv7 0xfa54) stores vertices BEFORE properties and names; bytes following a name can belong to the next object. This omitted the first effect region on many maps and could assign the wrong type to a later region (including stage 27).

read_mission.py now parses every field of every object, validates the declared object count and exact end of file, and keeps each object's properties and vertices together. The mission importer reads terrain types from properties 50/51 rather than assuming fixed offsets from a name. Building records are unchanged.

There are 51 effect regions, compared with the previous incomplete 29. Stage 5 has TWO slow triangles: upper (72,1), (442,3), (243,140); lower (241,173), (432,312), (53,308). The area between them remains clear. Both regions now halve speed for every team. Tests also follow the actual red-castle-to-upper-crystal route, which crosses the upper region.

Run python3 tests/import_check.py with the extracted IPA reference files available. This verifies per-stage region counts, exact stage-5 vertices, stage-27 types and agreement between complete records and generated mission data. Simulation checks test actual slow movement or poison damage for all three teams in all 51 regions. tests/run-ui-check.sh TerrainUiCheck checks both stage-5 effects in Android and saves a screenshot in the app's external files directory.


## Version 0.7 — movement, deployment and reinforcement rules

- Troop::GotoTarget, ARMv7 0x2fdb0–0x2fde2, calculates speed as 40 * terrainMultiplier * (1 + 0.1 * speedUpgrade). Origin type 13 (archer tower) multiplies by 1.2. Constants at 0x2ff68/0x2ff6c/0x2ff70 are 0.1, 40, 1.2. Replaced the previous 95/125 movement rates and incorrect crystal boost with base 40, archer 48; marsh still halves either rate. No new delay was added to the original one-second poison timer.
- Engine.send rejects a source with an active siege. Group orders skip such sources while allowing other selected buildings to deploy.
- Deployments and mid-route redirects require building destinations. Empty-ground orders preserve the current selection and destination. AI's approximate ground interception was removed to honor the building-only destination rule.
- Architecture::BeReinforce (0x143c0) adds incoming friendly troops and clamps to 1.5 times building capacity. This applies on friendly arrivals, including friendly reinforcement during siege and the reinforcement spell. Hostile arrivals and surviving conquest troops bypass this arrival clamp. The next friendly arrival enforces it. Existing gradual decay above normal capacity remains unchanged.
- Immunity::OnAction (0x28d44) installs a 15-second timer (0x41700000); Defup::OnAction (0x28d90) installs a 30-second timer (0x41f00000). These durations include marching and fighting and do not refresh when an army redirects or enters combat. Friendly building arrivals absorb the army and end its carried effect.
- Siege::AddUnit (0x297f8) and Battle::AddUnit (0x15a48) combine same-nation strength using the incoming Troop_Prop, replacing the older record. Siege arrivals already replaced the team's buff state; field encounters now also replace it with the incoming group's buff and remaining duration. Ordinary reinforcements remove the older angel/shield effect, while a buffed arrival replaces it without restarting its full timer.

Simulation regression checks cover every building's reinforcement cap, uncapped invasion/conquest, the next post-conquest friendly arrival, blocked siege deployment, building-only commands, original 40/48 speed, poison exposure during a crossing, exact 15/30 buff expiry, and latest-arrival buff replacement in field/siege combat. Android touch checks cover rejected ground redirection and valid group redirection to another building.


## Version 0.8 — sequential campaign and revised raid cap

Stages unlock in order. Locked campaign markers are dimmed and cannot be selected; startMission also checks the unlock rule. Defeat cannot advance through Next. Victory records completion before processing Next, unlocking exactly one subsequent stage. The final stage has no next stage. Existing saved completed progress is preserved.

Base speed is now 50 map units/second at the user's requested pace, retaining the 1.2x archer-origin multiplier (60 units/second), speed upgrades and 50% marsh slowdown.

Friendly troops arriving while their castle is under siege now bypass the 1.5x reinforcement cap, including troops from the reinforcement spell. Hostile siege pools and conquest survivors remain uncapped. Once the siege ends, the next friendly reinforcement arrival enforces the 1.5x limit. Existing gradual overflow decay outside combat remains unchanged.

Simulation checks cover repeated uncapped friendly siege arrivals, the reinforcement spell during siege, the first capped post-siege arrival, and 50/60 movement. Android touch checks cover locked marker selection, the Play guard, defeat blocking Next, victory unlocking the next stage, replay availability and the final-stage boundary.


## Version 0.9 — sixty stages and the Frontier campaign

The original campaign remains stages 1–30. The new Frontier campaign contains stages 31–60 and a separate, horizontally scrollable island map. Campaign tabs let players revisit either map; the second map can be previewed while locked. Clearing stage 30 unlocks stage 31; progression remains sequential through stage 60. Existing progress and upgrades are preserved. Defeat cannot advance, and stage 60 has no next stage.

### Authored levels

The thirty named layouts combine six authored battlefield structures with different faction placements, orientations, building variants, terrain plans, altar routes and deterministic scenery. Every new stage has distinct building data and a tactical brief. There are 59 new slow/poison regions and 25 altar placements. All start with the existing 500-troop rule. Later stages add three-nation conflicts and larger towers/castles. New terrain is rendered directly from its effect polygons so its visible extent agrees with its simulation geometry. Battlefield artwork, buildings, units, sounds and effects use the supplied original assets. The second campaign uses a separate native Canvas island overview.

The complete stage-by-stage design is assets/frontier-design.json. generate_frontier.py reproduces Frontier.java, the additional world records and the new campaign markers while preserving the first 30 worlds. If regenerating original world.json, run import_world.py before generate_frontier.py.

### AI spells: increased PRICE, base effects

Each enemy nation starts with 200 crystals and has its own wallet and cooldowns. Every six seconds an active nation earns 25 crystals plus 40 per owned crystal formation. AI spells cost exactly 1.5 times player base costs: Meteor 345, Mutiny 300, Storm 450, Reinforce 675. Player prices remain unchanged.

AI spell level is always ZERO regardless of stage, player upgrades or enemy troop upgrades: five Meteor impacts at 40 damage each; Storm 50 damage every 0.4 seconds for 10 seconds; Mutiny stops troops for 5 seconds; Reinforce adds 100 troops. Enemy cooldowns use base durations. The AI cannot cast without enough crystals or while that spell is cooling down. Spell damage respects the caster's team, sparing its allies and affecting other nations, including the player. A colored target ring and team/spell announcement identify enemy casts.

The AI prioritizes reinforcing a threatened besieged castle, stopping a dangerous approaching army, Storm against a strong stationary target, and Meteor against clustered enemies. Spells are enabled for enemy nations in both campaigns, using earned resources. First-campaign deployment keeps the recovered turn/target rules.

Frontier deployment is tougher: turn delays shorten from roughly 4.8–6.4 seconds at stage 31 to 2.9–4.5 at stage 60, sources can deploy from 250 troops, and up to 2–4 castles coordinate each turn. Target scoring considers incoming armies, defending a threatened ally, avoiding excessive commitment, crystal production, staging castles and terrain exposure. NPC attack levels increase from 1 to 3 and defense from 0 to 2 over the second campaign. These are troop modifiers; they do NOT upgrade spells.

### Validation and limits

Run:

```
python3 tests/frontier_data_check.py
python3 tests/import_check.py
javac --release 8 -d build/check src/castle/{Engine,Missions,Altars,Frontier,Wilds,UpgradeRules}.java tests/EngineCheck.java tests/FrontierCheck.java
java -cp build/check castle.EngineCheck
java -cp build/check castle.FrontierCheck
tests/run-ui-check.sh
tests/run-ui-check.sh FrontierUiCheck
```

Checks cover 60 world records, unique authored stage data and names, bounds and building spacing, all new-stage simulations, seeded determinism, independent AI income/wallets/cooldowns, all four spell costs and base effects, player damage and allied immunity, tactical spell decisions, campaign tabs, locked markers, the stage 30-to-31 transition and final-stage limits. Screenshots verify the new campaign map and a base-strength paid enemy Meteor in Android. The thirty new stages have not all been balanced through human playtesting; difficulty can be tuned from play feedback. Original obstacle pathfinding remains approximate.

An additional automated play sweep used a player bot with tier-3 upgrades against all thirty Frontier stages: 23 wins, 7 losses, and no unresolved matches at 300 seconds (seed 2026 + stage). This checks that battles can reach results under active play, rather than establishing human difficulty balance. The optional sweep is tests/FrontierPlaytest.java.


## Version 1.0 — original terrain overlays and campaign continuation

Removed the flat Canvas slow/poison strips and the custom island/grid campaign overview. Frontier battles now use the original grass ground and terrain5.png-style PNG overlays from original stages 5–30. Each reused overlay is paired with the exact source stage's poison/slow polygons. Both the PNG placement and its effect coordinates share the same world scaling, including overlays from the smaller original maps. The Frontier now has 75 reused effect regions, retaining the thirty authored building layouts and altar placements. Terrain briefs describe the reused hazards. assets/frontier-design.json identifies each source stage and its actual transformed regions.

Both campaign pages now use the original campaign artwork and path-marker arrangement, with stages 31–60 retaining their global numbering. The overview is fitted between the navigation tabs and bottom controls so markers do not hide under buttons. Campaign switching remembers the selected stage and centres it in view; app startup opens the next unlocked stage (or stage 60 after the campaign is complete). Saved completion and upgrades continue to use the same preferences.

The data regression verifies every reused PNG, its scaled placement, and every matching poison/slow polygon against its original source. Android checks cover original map reuse, stage selection memory, resuming at stage 46 rather than 31, the stage 30-to-31 unlock, and the stage-60 battle with an original PNG overlay. Earlier Version 0.9 Canvas terrain and balance notes describe the replaced implementation.


## Version 1.1 — new region shapes in stages 61–90

The third campaign, Wilds, adds stages 61–90 and unlocks after stage 60. Existing stages 1–60 and their terrain data are retained. The campaign tabs and remembered selection now support three maps, with progression ending at stage 90.

This campaign changes the SHAPES and arrangements of poison/slow regions, using the existing game artwork. It does not introduce a new texture style. Thirty unique transparent terrain PNGs in assets/Image/wilds contain 81 newly authored region silhouettes: curved marsh banks, crescents, winding channels, separated pools and broken rings. Each level also has a new building arrangement and tactical brief.

TerrainArt.java bakes existing material patches from terrain5.png (marsh) and terrain7.png (poison) through the new effect-region outlines with feathered shorelines. Each PNG uses the same 960×640 coordinates as its gameplay polygons. The terrain-manifest.json fingerprints the geometry used to bake each asset. The separate generated concept texture sheet is not used or included in the app.

Regenerate layouts with generate_wilds.py. To rebake the transparent PNGs, build/install the app, run tests/run-ui-check.sh TerrainBakeCheck, and pull the app's external files/wilds-terrain directory into assets/Image/wilds. Rebuild to package the results. The included PNGs are already baked, so this process is unnecessary for ordinary builds.

Validation includes thirty unique PNGs and region layouts, geometry fingerprints, transparent clear ground, visible region interiors, exact poison/slow behavior for all teams in all 81 regions, third-campaign simulations, stage 60-to-61 unlocking, the stage 90 boundary and an Android screenshot of winding channels. NPC spell prices remain 1.5× with base effects. Troop upgrades stay within level 5 and late-game AI timing has a 2.2-second minimum before its random delay.


Third-campaign checks:

```
python3 tests/wilds_data_check.py
javac --release 8 -d build/check src/castle/{Engine,Missions,Altars,Frontier,Wilds,UpgradeRules}.java tests/WildsCheck.java
java -cp build/check castle.WildsCheck
tests/run-ui-check.sh WildsUiCheck
```

An automated tier-5 player run resolved all thirty Wilds stages within 300 seconds: 29 wins and 1 loss. This is a simulation sanity check, not a substitute for human difficulty playtesting.


## Version 1.2 — restored Frontier layouts with the Wilds overlay process

Stages 31–60 now restore their original v0.9 authored poison/slow region arrangements: 59 regions across thirty levels. The replacement original-map geometry introduced in Version 1.0 is removed from this campaign. Its original castle layouts, names, altars and campaign progression are retained.

Those restored regions are baked with the SAME TerrainArt.java process as stages 61–90: existing marsh/poison artwork, new transparent PNG assets, feathered edges and geometry fingerprints. Assets are in assets/Image/frontier. Stages 1–30 and 61–90 retain their existing terrain data and artwork. The v0.9 region plans are preserved in archive/v09-frontier-region-plans.json.

Rebake with tests/run-ui-check.sh TerrainBakeCheck -e campaign frontier, pull external files/frontier-terrain into assets/Image/frontier, then rebuild. tests/frontier_data_check.py checks the restored outlines against the archived plans, their PNG fingerprints, and preservation of the other campaigns. The authored campaigns share the same ground rendering method.


## Version 0.13 — curved Frontier terrain

This release is labeled 0.13 (Android versionCode 13), as requested. Stages 31–60 retain their original region positions and tactical roles while replacing angular outlines with smooth, sampled boundaries. Narrow strips become bending channels; broad triangles, diamonds and patches become rounded banks. The original control points remain in the design metadata and archive for reference.

The curved boundary samples are shared by the gameplay regions and TerrainArt's transparent PNG bake. All thirty Frontier PNGs and geometry fingerprints have been regenerated. Existing artwork is retained. Original stages 1–30 and Wilds stages 61–90 keep their terrain data.

Checks validate the original footprints, dense curved boundaries, each baked PNG's geometry fingerprint, and correct slow/poison effects in all 59 curved regions for all three teams. Run tests/FrontierCurveCheck.java alongside the existing simulation and data checks.


## Version 0.14 — consistent campaign-map information

All ninety stages now use the same zero-padded stage-number/name heading and strategy-description layout. The original campaign receives thirty authored names and descriptions based on its actual castles, factions, terrain and altars. CampaignInfo.java holds this copy separately from the imported map geometry so rerunning the mission importer cannot overwrite it. Existing Frontier and Wilds names and descriptions are used through the same shared lookup. Locked stages show the same unlock-message format across campaigns.

Only campaign information and its display changed; original map data and game rules are retained.


### Version 0.14 AI spell policy

Campaign I (stages 1–30) disables enemy spell casting, including direct casting calls, while player spells stay available. Campaign II (31–60) charges enemy nations exactly 1.75× player base cost and uses level-0 spell effects. Campaign III (61–90) charges exactly 1.4× and uses level-2 spell effects. These are spell-upgrade levels; existing troop attack/defense progression is separate. Enemy global cooldowns stay at their base durations.

In engine order Meteor / Storm / Mutiny / Reinforce, enemy costs are 402.5 / 525 / 350 / 787.5 in Campaign II and 322 / 420 / 280 / 630 in Campaign III. Fractional crystal charges are kept exactly in the AI wallet. Player costs and player upgrade behavior are unchanged.

At level 2 the enemy's five Meteor impacts each deal 52 damage; Storm deals 65 per tick for 13 seconds; Mutiny lasts 6.5 seconds; Reinforce adds 280 troops. Each enemy nation continues to use its own earned crystals and cooldowns. CampaignInfoCheck exercises all campaign boundaries, both enemy teams, all four prices/effects, no-cast enforcement in every introductory stage, player-spell availability, and all ninety map headings/descriptions. CampaignInfoUiCheck verifies text fits and captures one map screen per campaign.


Current map/AI rule checks:

```
javac --release 8 -d build/check src/castle/{Engine,CampaignInfo,Missions,Altars,Frontier,Wilds,UpgradeRules}.java tests/CampaignInfoCheck.java
java -cp build/check castle.CampaignInfoCheck
tests/run-ui-check.sh CampaignInfoUiCheck
```

## Version 0.15 — original battle presentation

Army formations now show ceil(strength/60) animated soldiers, capped at 25 (1,500 displayed troop strength), following the requested density. This is an intentional density override: the original Troop::Init selects square formations of 1/4/9/16/25 sprites using thresholds 30/200/500/1000; Troop::BattleArray (0x30700) builds the formation. Strength and combat calculations continue to use the full army count. Group hit areas and selection rings cover the formation. March counts display for 1.8 seconds after deployment, fading over the final 0.65 seconds; combat counts remain visible and stack vertically.

Flags use original b/m/s frames (six frames for b/m, five for s); crystal flags use m. Architecture::SetMapObj (0x1341c) supplies the separate shadow offsets; Architecture::SetNation (0x13f94) supplies the flag anchors. These offsets are applied relative to the artwork pivot at the existing rendering scale, with the original vertical coordinate inverted. Castle counts sit above the roof/flag, with every hostile siege pool above the garrison count.

Sieges and field encounters use battle1–4 and battle_troop1–4 from skill.png. Casualties produce seeded soldier_die1/2 sprites with upward motion and fading, including area-spell damage. A separate effects RNG preserves game randomness; the number of simultaneous death effects is bounded. Mutiny plays skill_mutiny1–8 once, then fades, while its original gameplay stop timer continues. Angel/shield retain their existing single-play pickup animation and timed carrier status.

Each successful spell queues one sound: fireball.wav, twister.wav, hypnoses.wav or support.wav. Each meteor impact queues explosion.wav once. Sound events drain once during rendering; battle attack audio continues to trigger only on new hostile arrivals.

Skill_Reinforce::Attack (0x2b2c8) confirms group count 1+floor((level+1)/2), strength 100+20*level each, spawn radius 150. Total strengths at tiers 0–5: 100/240/280/480/540/800. Groups now appear around the selected castle and physically march to it. The original random angular placement is distributed evenly around a seeded rotation here to ensure groups surround the target. Friendly groups reinforce on arrival; hostile groups start/join raids. Existing arrival-cap and raid rules apply. The tooltip reports the current total, number of groups and per-group strength.

After victory/defeat, gameplay remains visible for one second. Then the original bg_victory/bg_defeat artwork appears with a tap prompt. A tap opens the existing summary dialog. Early taps cannot skip the delay, and the result sound begins with the splash.

Validation: PresentationCheck covers formation thresholds/cap, all reinforcement tiers and destinations, physical arrival, Mutiny timing/fade, one-shot spell and impact cues, and casualty effects. Existing EngineCheck, FrontierCheck, WildsCheck and CampaignInfoCheck pass. PresentationUiCheck verifies original atlas frames, above-building counts and result transitions, and captures formations, multi-team siege, both splashes and the summary dialog.

The large/small archer shadow table has been corrected: b = (42.5, 71.78), m = (39.72, 62.97), s = (34, 62) in screen-coordinate offsets before rendering scale. Small-archer and crystal flag X anchors are 9 and 6.28. PresentationUiCheck also deploys 600 troops from each actual building kind and verifies distinct original soldier sets 1/2/3, captured in troop-origins-v015.png.

## Version 0.16 — complete combat audio and closer reinforcements

Reinforcement groups now spawn 100 units from the target (reduced from the recovered original 150), per the requested balance adjustment. Group counts, per-group strength, movement speed and arrival rules are unchanged.

The decrypted executable confirms these sound triggers: MainScene::PlayerToTarget uses randomized move%dAudio (move1/move2); Battle::OnLoad and Siege::OnLoad use randomized attack%dAudio (attack1/attack2); Architecture::TowerAttack uses arrowAudio. These variants are unrelated to troop source type. There is no separate move.wav in the extracted assets. Skill_Meteor::Explode stops fireballAudio and plays explosionAudio; MainScene/Nation::UseSkill identify twisterAudio, hypnosesAudio and supportAudio.

Every spawned fireball now owns a launch sound, stopped on its impact before the corresponding explosion sound. Tornado owns one looping twister stream for its active lifetime (continuous playback is the requested behavior; the recovered direct PlaySound call itself uses a non-looping flag). Its stream pauses with gameplay and stops on expiry, defeat/victory or leaving the battle. Tower volleys play arrow.wav once per shot. Deployment and new hostile raid waves use move1/move2 and attack1/attack2 chosen by an independent audio RNG. Combat ticks do not replay the battle-start sound.

SoundPool now waits for asynchronous sample loading rather than silently losing early requests. It allows 24 concurrent effects, tracks owned meteor/tornado streams, and stops stale effects across scene changes. Music and all sound effects play at 100% volume. Android AudioUiCheck verifies all ten game-effect samples load, SoundPool accepts five launch/five impact streams, the tornado loop starts once and survives pause/resume until expiry, and both RNG-selected movement/battle variants play. AudioCheck verifies event timing and tower volleys; the existing presentation, campaign, terrain and combat checks pass.

### v0.16 volume and radius adjustment

The release label remains 0.16. Music and every sound effect are set to 100% volume. Reinforcements spawn 100 units from the target. Sound event timing, RNG-selected movement/battle variants, group strengths and arrival rules are retained.

The original Audio/readme.txt explicitly says deployment uses move1/move2, battles use attack1/attack2, and both are selected randomly. PlayerToTarget and Battle/Siege::OnLoad confirm this with rand() and the move%dAudio/attack%dAudio formats. The rebuilt app uses RNG-selected variants, matching the latest request; arrow.wav remains the tower firing sound.

### v0.16 grouped raid audio and building-cloud adjustment

An army order shares a raid token across all deployed/redirected groups. Reinforcement spell groups share one token. Field-combat survivors adopt a shared token for the combined battle. Each siege announces a token once, so staggered groups from one combined raid do not stack battle-start sounds. Separate incoming hostile waves and new sieges can still announce themselves. Friendly reinforcement arrivals to an allied castle, including during a raid, add troops without emitting a battle-start sound. Lightweight tokens avoid retaining old encounter/unit graphs. The audio RNG is separate from combat randomness.

The original four building battle-cloud frames now render at horizontal scale 0.70 and vertical scale 0.90, centered 16 units above the building ground position (previously 0.65 and 2 units above). This extends the cloud upward while its lower edge stays near the ground. After visual review the width was narrowed by 22% from the initial enlarged preview, retaining the raised height. Field-combat clouds retain their separate rendering. RaidAudioCheck verifies combined orders, staggered field survivors, silent friendly reinforcements and distinct attacks; RaidUiCheck checks actual accepted SoundPool starts and captures all four raised cloud frames.
