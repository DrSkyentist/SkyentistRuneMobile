# SkyRune iPad Sharpness experiment

Branch: `feature/ipad-sharpness-presets` (based on the **working** isolated 0.10.1 bundled-jars login experiment, not `master`).

## What it changes
The source game's `ViewController.canvasForScreen:` rasterizes at about 1,689×1,267 on a landscape 13-inch iPad Pro; then iPadOS displays that frame at a larger physical pixel resolution. This can make details look soft.

This branch adds a **sharpness multiplier for large screens only**: 100% (unchanged), 125%, 150%, and 200%, capped at native short-axis pixel count. The existing **Interface size** long-press sheet (long-press the app's plugin/settings control) now exposes both interface size and sharpness for iPads. Changes take effect on the next launch.

This increases the engine's total rendered pixels and can lower FPS, especially at 200%. It also shrinks the apparent RuneLite HUD; the current engine layout ties HUD sizes to canvas resolution. A future independent HUD-scale design requires more extensive work. No click automation or gameplay behavior changes.

## Test
1. Keep the original known-good build ZIP/IPA archived. The new build uses the *same* side-by-side bundle ID `dev.skyentist.skyrune.loginlab` because Apple's free provisioning limit prevents installing a third game app.
2. Install the new IPA as a SideStore update to **SkyRune Login Test**, without removing its app data.
3. Run the StikDebug JIT attach, launch the game and verify login before changing settings.
4. Long-press the plugin/settings button; choose `Sharper (125%)`. Restart the game and compare text edges and FPS.
5. Try 150% if performance allows. 200% is an optional stress test, not a recommendation.
6. Revert with `Original sharpness (100%)`. If the new app misbehaves, reinstall the known-good IPA.

**Important:** This branch has not yet been device-tested and should not be merged until verified.
