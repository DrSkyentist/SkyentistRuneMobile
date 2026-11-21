# Pencil input ownership

Baseline: `feature/input-visualizer` at `0b44d7435f8bdf011234444f51bea85d11ef9e3b`.
Work branch: `fix/pencil-input-ownership`.

## Problem and scope

The native hover handler previously called the unrestricted menu-pointer movement API. A tip tap moves the
engine pointer to the contact location, then waits for the engine to resolve that point before posting its
click. Hover returning after lift could move or exit that pointer while the tap was settling or awaiting
consumption. This is a code-level interference path; the November 20 recordings alone do not establish that
it explains every missed interaction.

Pencil contact also inherited finger long-press timing. This change keeps camera drags and finger thresholds,
but uses squeeze for Pencil context menus and allows a stationary Pencil contact to left-click on release
even when held longer than the finger long-press timeout. Pinch zoom accepts fingers only.

## Implementation

- Passive `onHover` / `endHover` events run on the client thread and retain only the latest requested hover.
  They cannot move or exit the pointer during a settling, armed or unconsumed tap, a held button, or an open menu.
- Deferred UI, right-click and login world-list move/press sequences also hold off passive hover through their
  completion. The login world's existing three-drain draw wait remains in place.
- Queued taps take precedence over hover. When the final tap completes, the latest live hover resumes; an
  ended hover is not replayed. Deferred cleanup cannot park the pointer belonging to a newer tap.
- Native contact, camera/scroll and pinch gestures suppress hover. Hover recognition cannot cancel touch
  delivery. Explicit menu-row tracking retains its original movement API.
- Repeated stationary hover updates refresh squeeze-location freshness before movement deduplication.
- Squeeze still opens on its began phase only. Release performs no action; a normal menu-row tap selects.

JNI lookups are optional so an older staged jar does not break startup. Such a jar gets no passive hover
instead of falling back to the interfering path. The test IPA rebuilds and bundles the matching Java code.
The shared bridge contract adds only `onHover(int,int)` and `endHover()`; no existing method changes signature.
Its baseline and generated API reference are refreshed. Android's existing callers continue to use their
existing APIs; runtime bridge censuses on both platforms remain pending.

## Verification

`JavaBridgePointerStreamTest` covers hover during armed and fast taps, click consumption, latest-point replay,
hover exit, cancellation, queued taps, explicit menu tracking, precise UI clicks, squeeze right-click ordering,
login world selection, and cleanup racing with a newer tap. Existing press, pointer, key and text suites run
before IPA packaging:

```sh
./gradlew :client:test --tests 'net.runelite.client.headless.JavaBridge*Test' --no-daemon --console=plain
```

Verified locally: all 50 tests across the five `JavaBridge*Test` suites passed, including 11 new pointer/hover
regressions. Java production and test compilation also passed. The run used Temurin JDK 21.0.13.1 and a temporary
Gradle init script to load the existing test-classpath Mockito jar with `-javaagent`; this workspace does not
support Mockito's default dynamic JVM attachment. No production build configuration was changed for that setup.

The shared-contract check and generated API-reference check pass. The GitHub workflow is configured to run the
input tests before the Apple SDK compile, but has not run for this patch. The iOS compile, runtime bridge
censuses and on-device acceptance checks remain pending. Java unit tests do not establish on-device interaction
quality.

## iPad acceptance checks (pending)

1. Hover over an object, tap, and move the Pencil toward another object immediately after lift. The first
   tap must act once at its contact point, then the hover should follow the new location.
2. Repeat quick taps, including inventory and chat controls. Check for missed, duplicated or displaced actions.
3. Rest the tip for longer than the finger long-press setting, then lift without dragging: one left click.
4. Hold the Pencil stationary in hover, squeeze once, release, then tap a menu row: one menu and one selection.
5. Rotate the camera with Pencil and finger; scroll the bank/chat; pinch with two fingers. Check that hover
   cannot move the pointer away from a scrolling interface while the gesture is active.
6. Verify finger long-press menus, login and the existing plugin profile. Test with the input visualizer both
   enabled and disabled. Mixed palm/Pencil contact needs device validation and is not claimed fixed here.

No chat plugins, profiles, networking, rendering defaults or bank layout changes are part of this patch.
