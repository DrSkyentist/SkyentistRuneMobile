# SkyRune: world-map gestures and keyboard destination (fix/map-and-text-input)

Base: `fix/pencil-input-ownership` @ `50610d49` (the Pencil ownership fix is untouched).
Also carries `ae864e54` from `fix/bank-scroll-touch-origin` (interface-scroll hit test from the landing point),
cherry-picked rather than merged so that branch's CI-only commit stays out.

Status: code written, contract checks pass, Java logic verified with a standalone harness. **Not yet built by
CI and not tested on the iPad** at the time of writing (the authoring session had no GitHub push access and no
Gradle dependency access). Treat every device-behaviour claim below as a hypothesis until tested.

## World map

### Confirmed at source level

| ID | Problem | Change |
|----|---------|--------|
| MAP-3 | `onCameraPan:` classified map pan vs Key/dropdown scroll from `locationInView`, which is already `kSceneDragSlop` (18pt) past the touch-down. Swipes near the Key list edge went to the wrong target. Android already classifies from the DOWN point (`e1`). | Classify from `RMCameraPanRecognizer.landing`. Chat scroll and interface scroll (bank) now use it too. |
| MAP-3 | With the map open, a drag that wasn't a pan or list scroll fell through to item drag / iface scroll / **camera rotation** behind the map. Android swallows these (`if (mapOpen) return true`). | Swallow until lift (`mapDragSwallowed`). |
| MAP-4 | Camera pan and pinch had no simultaneity rule, so a pan that began first (one finger moved 18pt before the second landed) blocked the pinch for the whole sequence: "it keeps panning, won't zoom". | Pinch gets the VC as delegate; `shouldRecognizeSimultaneously` allows pan+pinch **only while the map is open**. The pan drops its deltas while the pinch is live. The pinch's `shouldReceiveTouch` returns YES so its touch intake is unchanged. |
| MAP-1 | Map pinch used the camera's linear mapping (12 notches per unit scale). Each wheel notch over the map is a whole zoom level, so a 1.5x spread fired ~6 levels and banked notches kept firing after the fingers stopped. | Map-only path: geometric step (`kMapPinchStep = 1.30` relative scale per level), max one level per Changed event, surplus discarded after a step. Camera pinch unchanged. |
| MAP-2 | The wheel was posted at the pinch centroid. Over the Key list that scrolls the list; in the letterbox it fell back to a hard-coded (382,251). | Map pinch aims at the centroid only when it's over the map tiles (snapshot `map` rect, now parsed); otherwise the tile-area centre. |

### Checked and left alone
- `WorldMap` API has no zoom setter (only `getWorldMapZoom`), so wheel aggregation is the minimal grounded option.
- Map pan speed `kMapPanSpeed = 2.0` kept (user preference).
- Taps under 18pt on the map still click (that is how markers/buttons are used). Pans past 18pt cancel the arm as before.
- `applyMapZoom` display transform: input reads `self.screen` bounds space, unaffected.

### Tuning knob
`kMapPinchStep` in `mapPinchChanged:`. Lower = more sensitive. If one level per event still feels fast on the
120 Hz display, the next step would be a short per-level cooldown, not a different mechanism.

## Keyboard destination (KB-1)

### How focus is actually represented
- `MESLAYERMODE` (legacy `INPUT_TYPE`): bank search and GE item search are `11` (SEARCH), PM `6`, amounts `7/16/19/30`, RuneLite chatbox inputs negative. Set for the life of the prompt.
- World map search: does **not** set MESLAYERMODE; `WORLDMAP_SEARCHING == 1` while focused.
- Public chat: no flag; with no prompt open, keys go to the chat line.
- GE quantity/price: focused input-field widget in `GE_OFFERS`.

### Change
- `JavaBridge.getTextInputTarget()` (additive, shared contract acknowledged): snapshotted on the client thread beside `textInputKind`. "Bank Search" requires the SEARCH prompt to be live **and** the bank grid visible; an open bank alone is still Chat (tested). Map search requires the map to be open (a stale flag is ignored, tested).
- `textInputOpen` / `textInputKind` are unchanged, so auto-raise and number-pad behaviour are identical on both shells.
- iOS: optional JNI lookup (older jars return -1 and show the old generic wording). The keyboard accessory bar now shows "Typing to: Chat / Bank Search / Map Search / Amount / …" and the action reads Send / Done / Search / Enter. The Return key legend follows. **Every action still sends exactly one VK_ENTER**; no new forwarding path, so no duplicate characters or double Enter can come from this.

### Not changed
- Hardware keyboard path (still via the hidden field).
- Map search raise-on-tap and dismiss-on-commit (FIX-14).
- Paste.

## Tests
- `JavaBridgeTextInputTest`: 8 new cases (classifier + snapshot transitions: bank open without search, bank search, close back to chat, map search, stale map flag).
- Gesture behaviour has no automated coverage; Java tests do not prove UIKit behaviour.

## Device checklist (priority order)
1. Map: one-finger drag from many spots, including right next to the Key list. Should pan, never rotate the camera.
2. Map: drag a little, then add a second finger and pinch. Should zoom.
3. Map: pinch in/out slowly and quickly. One level per clear spread, no runaway after stopping.
4. Map: pinch with fingers over the Key list. Should zoom the map, not scroll the list.
5. Map: swipe the Key list and the surface dropdown starting near their edges.
6. Map search: tap, check the bar says Map Search, type, Search, keyboard drops. Close map with keyboard up.
7. Chat: open keyboard, bar says Chat, type, Send once. Hide without sending.
8. Bank: open bank with keyboard up (should still say Chat), tap Search (Bank Search), type, Done, then Chat again with no stale label.
9. Amount prompt: number pad + "Enter ⏎".
10. Regression: Pencil hover/tap/squeeze, finger taps, camera rotation off-map, bank scrolling.

## Open
- `kMapPinchStep` value is a reasoned starting point, not measured.
- Bank scrolling inconsistency: only the landing-point fix is included; not claimed solved.
- GE item search is labelled "Item Search" on the assumption it uses MESLAYERMODE 11 without the bank; unverified on device.
