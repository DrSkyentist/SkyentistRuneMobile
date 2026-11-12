# SkyRune: iPadOS 0.10.1 login baseline experiment

This branch is an isolated diagnostic build; it does **not** replace the `master` source or the working RuneMobile 0.9 install.

## What's different
- Based on the 0.10.1 source in `build/ipados-baseline`.
- Bundle ID: `dev.skyentist.skyrune.loginlab`, so this installs as a separate app with its own sandbox.
- Display name: **SkyRune Login Test**.
- The release compiler defines `SKYRUNE_FORCE_BUNDLED_JARS=1`. `RMGameUpdate.enabled` returns NO; both the cover update check and boot selection use the IPA's bundled client jars.
- The in-app Share Logs report's game-update status should say `off (SkyRune login test: bundled jars)`.

## Test steps
1. Preserve the working 0.9 app; don't uninstall it.
2. Download the GitHub Actions ZIP artifact, extract the IPA, install it via SideStore (subject to free-signing app limits).
3. Enable JIT for **SkyRune Login Test** using StikDebug.
4. With the account logged out on PC, try the OSRS login once and record whether it gets past `Connecting to server…`.
5. If it stalls, use **Share Logs** in the test app and preserve the report; review it for sensitive content before sharing.

## What this test can and cannot prove
- If it works: the tested 0.10.1 code *can* log in with bundled game files in a clean, separately installed app. This does not isolate remote updates by itself; a fresh sandbox and app signing differ too.
- If it fails: we can stop blaming the updater alone and inspect Java/native login-state handoff. It won't prove an exact root cause.
- This experiment is **not** a gameplay enhancement and introduces no automated inputs.

Do not merge into `master` until we've reproduced and understood the behavior.
