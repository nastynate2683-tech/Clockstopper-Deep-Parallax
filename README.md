# Clockstopper Deep Parallax

A clean-room Android live-wallpaper project, kept completely separate from April.

## Design priorities
1. Smoothness first, depth second, visual impact third.
2. Phone tilt is the input: no automatic drift or idle animation.
3. Motion must be app-scoped, reversible, and have a clear off switch.
4. Preserve image proportions: fill the screen by cropping excess, never squeeze.
5. Target the Moto G Play (2024) first; keep GPU and memory budgets conservative.

## Current stage
This is the initial native Android live-wallpaper foundation. It establishes the app/service/build pipeline. The layered depth-map renderer, image import, per-layer parallax controls, glass/refraction shaders, and particle system are planned milestones—not represented as finished features yet.

## Build
Pushes to `main` run the Android debug build through GitHub Actions. Download the APK from the workflow run's Artifacts section.

## Planned milestones
- [ ] Buildable Android live-wallpaper shell
- [ ] User-selected image and depth-map import
- [ ] Multi-layer GPU displacement with edge-safe overscan
- [ ] Gyroscope smoothing, strength slider, and hard off switch
- [ ] Optional glass fragments, refraction, and restrained particles
- [ ] Device profiling and low-power mode

## License
Project code is intended to be MIT-licensed. Third-party models, assets, and libraries will be tracked with their own licenses and attribution.
