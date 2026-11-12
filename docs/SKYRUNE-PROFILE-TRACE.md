# SkyRune profile/plugin freeze tracer

A diagnostic branch built from `feature/ipad-sharpness-presets`, retaining the known-good bundled-jars login and the same side-by-side SkyRune bundle ID.

## Purpose
Instrument `ConfigProfileBridge.switchProfile` and `PluginManager` at **INFO** level so the **Release** build's persistent RuneLite `.runelite/logs/client.log` captures progress even when the Java client thread freezes. No profile IDs, tokens or account names are deliberately written by the tracing code.

Look for `[skyrune-profile-trace]` in the in-app **Share logs** export:

- `SWITCH requested`, `PREPARE begin/done`, `APPLY begin/done`: isolate network/config work from the client-thread switch.
- `PROFILE_CHANGED`, `DEFAULT_CONFIG begin/done`, `RECONCILE begin/done`: trace the plugin reconciliation stage.
- `CHANGE PluginName stopped -> running`, `START begin PluginName`, `START done PluginName`: identify a plugin that blocks during startup or event registration.
- `STOP begin/done PluginName`: identify shutdown issues.
- An exception before a completion log is recorded with an error marker.

If the final line is `START begin`, the stall is likely in that plugin's startup **or events invoked before completion**; it is not proof the plugin alone is defective. If `APPLY begin` has no `APPLY done`, the culprit may instead be configuration or event delivery; the exact last line matters.

## Procedure
1. Save a copy of the working IPA, and do **not** remove or reset your current SkyRune app data.
2. Install this IPA as a SideStore **update** to `SkyRune Login Test` (same bundle ID).
3. Enable JIT via StikDebug. Keep the default profile, open the game, then switch to the problematic RuneLite profile **once**.
4. If it hangs, force-close/relaunch the app, and on the initial cover tap **Share logs** (not Copy Diagnostics). Save the generated `RuneMobile-logs-*.txt`, check for sensitive data, then upload it.
5. Avoid switching profiles on desktop while isolating the issue. Do not uninstall plugins.

Release build has a persistent `client.log`; DEBUG is not required for this targeted trace. Share Logs can retrieve the previous run's client.log after restart.

## Notes
This build only adds instrumentation and preserves the working renderer and login changes. Logs add minor overhead and should not stay enabled permanently. It is **not** a gameplay automation feature.
