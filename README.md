# Clockstopper Deep Parallax

A clean-room native Android live-wallpaper project, kept separate from April.

## Design priorities
- Smoothness first, depth second, visual impact third.
- Phone tilt is the input: no automatic drift or idle animation.
- Motion is app-scoped, reversible, and has a clear off switch.
- Preserve image proportions: fill by cropping excess, never squeeze.
- Target Moto G Play (2024) first; keep GPU and memory budgets conservative.

## Current prototype
- Native Kotlin Android live-wallpaper service.
- Choose an image through Android's document picker; persisted read permission is requested.
- Accelerometer-driven, smoothed image parallax while the wallpaper is visible.
- Motion enable/disable switch and strength slider.
- No global overlays, accessibility service, global setting changes, or background animation.

## Important limitations
This is an early 2D image-pan prototype with a subtle foreground frame layer. It is not yet a true depth-map renderer. Per-pixel depth displacement, separate foreground/background cutouts, glass refraction, and GPU particles remain future milestones.

## Build
GitHub Actions builds a debug APK on pushes to `main` and supports manual dispatch from the Actions tab. Download the APK from the workflow run's Artifacts section.

## Next milestones
- [x] Native live-wallpaper shell
- [x] Image selection and saved URI permission
- [x] Tilt enable/disable and strength setting
- [ ] Real depth-map import and shader displacement
- [ ] Independently controllable foreground/midground/background layers
- [ ] Glass fragments, refraction and restrained particles
- [ ] Device profiling and low-power mode

Project code is intended to be MIT-licensed. Third-party models, assets and libraries will be tracked with their own licenses and attribution.
