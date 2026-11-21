# Gameplay media

The screenshots and MP4 files show Castle Wars Rebuilt v0.16 running on a 960×640 Android emulator. The clips are silent Android screen recordings, encoded as H.264 inside MP4 containers.

| Clip | Scenario |
| --- | --- |
| `videos/deployment-and-combat.mp4` | Original stage 5. Scripted troop orders use the normal recruitment and combat rules. |
| `videos/spell-showcase.mp4` | Wild stage 65. A temporary 2,000-crystal budget and level-5 reinforcement show grouped arrivals, meteor impacts and tornado damage. |

Both captures are 18 seconds long. Posters are decoded from the recorded video. Screenshots are taken from the running app. The capture fixture checks video duration, file size and three decoded frames before accepting a recording.

## Reproduce

Build and install the app on an attached emulator, then run:

```sh
./tests/run-ui-check.sh ShowcaseUiCheck -e scenario battle
./tests/run-ui-check.sh ShowcaseUiCheck -e scenario spells
```

The fixture writes `showcase-<scenario>.mp4`, `showcase-<scenario>.png` and three frame PNGs to the app's external files directory. Retrieve the files with ADB into this folder. Player progress is restored after capture; the fixture is excluded from the game APK.

[Architecture](../architecture.md) explains the domain and platform boundaries. Build and signing instructions are in the [development guide](../development.md).
