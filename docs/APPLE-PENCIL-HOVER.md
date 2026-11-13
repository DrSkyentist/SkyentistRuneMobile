# SkyRune Apple Pencil hover (HOVER-1)

This branch derives from the **known-working iPad sharpness/bundled-login** branch `feature/ipad-sharpness-presets`, **not** the diagnostic tracing branch. It preserves the SideStore bundle identifier `dev.skyentist.skyrune.loginlab` so it updates the same existing app and keeps the user's clean RuneMobile profile.

## Scope
- On iPadOS 16.1+, attach `UIHoverGestureRecognizer` to the game canvas. On compatible iPads, Apple Pencil hover is delivered via native UIKit before the Pencil touches the display; iPad trackpad pointer hover may also be delivered.
- Map the hover location through the existing aspect-fit screen-to-game mapper (which supports the GPU present path and scaling).
- Dispatch position updates only through the existing `JVMLauncher.postMoveX:y:` -> `JavaBridge.onMove` -> **AWT MOUSE_MOVED** pipeline. On exit, dispatch `postClearHover`.
- Skip identical canvas pixel positions to reduce event queue churn. Ignore hover during open/closing right-click menus or item dragging.
- Never synthesize clicks, key presses, shifts, or automated actions. Pencil squeeze/double tap are **out of scope** and require separate UX/rules review.

## Test plan (requires physical Pencil-capable iPad)
1. Keep a copy of the previous working IPA. Install new IPA as a **SideStore update**, not a fresh install. Keep the app data and RuneMobile profile.
2. Enable JIT. With GPU and current plugins, hover Apple Pencil **above** a bank item without touching: expect the game/RuneLite tooltip to update without moving or clicking anything.
3. Hover over NPC/object/item, sweep across several targets and off the screen: expect tooltips to follow and clear when leaving.
4. Tap and drag with finger (and Pencil) normally: verify left taps, long-press context menu, camera movement, inventory dragging, pinching, UI panels still function.
5. Repeat after rotating orientation and after using iPad sharpness preset other than 100%.
6. If hover doesn't arrive, capture `Share Logs` and report device Pencil compatibility and hover setting. A compile success is not a hardware test.

## Future
- Implement optional Pencil squeeze and double tap as *separate* user-triggered controls only after verifying iPadOS hardware events and reviewing the client/plugin rules.
- Once validated, consider an upstream PR with hardware test results. Do not merge experimental code into `master` until verified.
