# Gameplay media

The screenshots, GIF previews and MP4 source files show Castle Wars Rebuilt v0.16 running on a 960×640 Android emulator. The clips are silent Android screen recordings, encoded as H.264 inside MP4 containers.

| Clip | Scenario |
| --- | --- |
| `videos/deployment-and-combat.mp4` | Original stage 5, with its recovered `map2.png` background. Scripted troop orders use the normal recruitment and combat rules. |
| `videos/spell-showcase.mp4` | Wild stage 65, with the authored `map3.png` background. A temporary 2,000-crystal budget and level-5 reinforcement show grouped arrivals, meteor impacts and tornado damage. |

Both captures run for approximately 18 seconds; the exact durations are recorded in `manifest.json`. The README embeds looping GIF derivatives at 480×320 and 10 fps; the MP4 sources retain their original resolution. Posters are decoded from the recorded video. Screenshots are taken from the running app. The capture fixture checks video duration, file size and three decoded frames before accepting a recording.

## Convert to GIF

On macOS, use Swift with the system AVFoundation and ImageIO frameworks; no FFmpeg installation is required:

```sh
swift tools/video-to-gif.swift docs/media/videos/deployment-and-combat.mp4 docs/media/gifs/deployment-and-combat.gif
swift tools/video-to-gif.swift docs/media/videos/spell-showcase.mp4 docs/media/gifs/spell-showcase.gif
```

The converter samples the whole clip, preserves normal playback speed and sets endless looping. `manifest.json` records frame counts, dimensions, durations and checksums for the converted files.

## Reproduce source recordings

Build and install the app on an attached emulator, then run:

```sh
./tests/run-ui-check.sh ShowcaseUiCheck -e scenario battle -e stage 5
./tests/run-ui-check.sh ShowcaseUiCheck -e scenario spells -e stage 65
```

The fixture writes `showcase-<scenario>.mp4`, `showcase-<scenario>.png`, a `showcase-<scenario>-map.png` campaign preview and three frame PNGs to the app's external files directory. Retrieve the files with ADB into this folder. Player progress is restored after capture; the fixture is excluded from the game APK.

[Architecture](../architecture.md) explains the domain and platform boundaries. Build and signing instructions are in the [development guide](../development.md).
