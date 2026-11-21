# Castle Wars Rebuilt

A single-player Android strategy game about capturing castles, moving armies and choosing when to spend crystals on spells. Lead the red army against blue and yellow rivals through **150 sequential stages** across five campaigns.

**Current version: 0.16 · Android 6.0+ · Landscape**

![Armies raiding a crystal mine in the Original campaign](docs/media/screenshots/building-raid.png)

## The game

Every castle is a foothold. Send half its garrison to capture a neutral building, reinforce an ally or raid an enemy. Combine several armies for a stronger assault, or redirect a moving group when the battlefield changes.

Castles recruit troops, crystal mines fund spells, and archer towers shoot at passing enemies. Swamps slow armies; poison drains their strength. Angel and shield altars give temporary protection, making the route to a target as important as the size of the army.

- **Three rival armies:** team-colored buildings, flags and combat counts make the front line readable.
- **Animated battles:** troop formations scale with army strength; sieges resolve gradually with clouds, arrows and casualty effects.
- **Four spells:** bombard an area, create a tornado, disrupt enemy movement or summon reinforcements.
- **Persistent progression:** clear stages in order, earn 0.5 points per clear and spend 1 point per upgrade through five tiers.
- **Music and effects:** battle tracks, spell sounds and combat audio follow the action.

## Gameplay GIFs

Both previews use the updated map artwork on the same stages: **Original 5** and **Wild 65**. They loop through the full, roughly **18-second** captures at 480×320 and 10 fps. The spell showcase uses a scripted crystal budget and upgraded reinforcement to demonstrate the effects.

| Deployment and building combat | Spell showcase |
| --- | --- |
| ![Deployment and combat](docs/media/gifs/deployment-and-combat.gif) | ![Spell showcase](docs/media/gifs/spell-showcase.gif) |
| Send waves into a contested building. | Reinforcement, meteor impacts and tornado damage. |

## Five campaigns

| Campaign | Stages | What changes |
| --- | --- | --- |
| **Original** | 1–30 | Learn deployment, buildings and terrain. Enemy nations use troop tactics without casting spells. |
| **Frontier** | 31–60 | New layouts and curved hazard regions introduce enemy spell casting. |
| **Wild** | 61–90 | New terrain shapes, more demanding routes and stronger enemy spell effects. |
| **Citadel** | 91–120 | Thirty unique symmetric battlefields against much stronger enemies. Both armies start with matching castles, so the challenge comes from mines, towers and the central keeps. |
| **Zenith** | 121–150 | Three active factions on symmetric river and diamond terrain, with the strongest enemies yet. |

![Campaign selection with sequential stage progression](docs/media/screenshots/campaign-map.png)

![A tornado and active sieges in the Wild campaign](docs/media/screenshots/spell-showcase.png)

## How to play

1. Select a friendly castle and drag to another castle to send **50%** of its troops. Select several castles to deploy them together.
2. Capture neutral buildings and secure crystal income before committing to a long siege.
3. Redirect moving armies to another castle when you need to defend or change the attack.
4. Watch the terrain: swamp slows movement, while poison punishes time spent inside it.
5. Choose a spell, read its tooltip and select a target. Friendly reinforcements can enter an allied castle under attack; besieged castles cannot deploy troops.

| Spell | Use |
| --- | --- |
| **Meteor** | Fireballs damage enemies in an area, including nearby moving troops. |
| **Tornado** | A moving damage effect remains active for a limited duration. |
| **Mutiny** | Temporarily disrupts enemy troop movement. |
| **Reinforce** | Groups appear around the chosen castle, then march inward to reinforce or invade it. |

## Build and development

The project uses Java and Android Canvas. With a JDK, Android platform 33 and build-tools 33.0.1 installed:

```sh
./build.sh
```

The APK is written to `build/Castle-Wars-Rebuilt.apk`. See the [development guide](docs/development.md) for SDK configuration, signing, tests and terrain authoring.

## OOP and architecture

The code uses a layered OOP design with partial Clean Architecture boundaries: domain rules, application use cases, presentation and Android adapters. `Main.java` stays directly under `src/castle/` as the executable entry point; supporting classes are grouped by responsibility. `PlayerProfile` owns progression and upgrades, `Engine` owns match state, and `AiCommander` owns AI decisions. The view receives resource, save and audio interfaces instead of opening platform services itself. Explanatory source comments are kept to one or two lines.

- [Architecture, folder rationale and current limitations](docs/architecture.md)
- [Current code map](docs/code-map.md)
- [Media capture notes](docs/media/README.md)

To run the domain and asset checks:

```sh
./tests/run-checks.sh
```
