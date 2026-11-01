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
- Choose a wallpaper image through Android's document picker.
- Optionally choose a matching grayscale depth map (white = near, black = far).
- A subdivided bitmap mesh uses depth-map luminance to offset vertices as the phone tilts.
- Accelerometer input is smoothed; motion can be disabled and strength adjusted.
- If no depth map is selected, the wallpaper falls back to simple image-pan parallax.
- No global overlays, accessibility service, global setting changes, or automatic drift.

## Depth-map guidance
Use a grayscale image matching the composition of the wallpaper. Keep important foreground objects bright and background regions dark. The current mesh renderer is a lightweight approximation: it warps a single image surface and does not yet create true occlusion, independent cutouts, glass refraction, or particles. Depth maps with abrupt edges may show mesh stretching; use soft transitions.

## Build and install
GitHub Actions builds a debug APK on pushes to `main`. Open the latest successful run, download its `Clockstopper-Deep-Parallax-debug` artifact, extract the ZIP, and install `app-debug.apk` on Android. Android may ask permission to install apps from the browser or file manager used to open the APK.

## Next milestones
- [x] Native live-wallpaper shell
- [x] Image selection and saved URI permission
- [x] Tilt enable/disable and strength setting
- [x] Optional depth-map import
- [x] Depth-driven mesh displacement prototype
- [ ] Separate foreground/midground/background layers with occlusion
- [ ] Glass fragments, refraction and restrained particles
- [ ] Device profiling and low-power tuning

Project code is intended to be MIT-licensed. Third-party models, assets and libraries will be tracked with their own licenses and attribution.
