# Intent Security Audit

Audit of the app's components and Intent handling against Android Intent
security best practices (Intent redirection, component exposure, PendingIntent
mutability, broadcast protection, ContentProvider guarding).

- **Date:** 2026-08-31
- **Scope:** `app/src/main/AndroidManifest.xml`, merged release manifest
  (including all library-contributed components), and all Kotlin sources under
  `app/src/main/java`.
- **Result: PASS — no vulnerabilities found, no hardening changes required.**

## Component inventory (merged release manifest)

| Component | Source | Exported | Protection | Verdict |
|---|---|---|---|---|
| `MainActivity` | app | yes | Launcher entry point only (`MAIN`/`LAUNCHER` intent filter) | OK — must be exported; reads no Intent data (see below) |
| `androidx.appfunctions.service.PlatformAppFunctionService` | appfunctions-service | yes | `android.permission.BIND_APP_FUNCTION_SERVICE` (system-only) | OK — only the system can bind |
| `androidx.appfunctions.service.ExtensionAppFunctionService` | appfunctions-service | yes | `android.permission.BIND_APP_FUNCTION_SERVICE` (system-only) | OK — only the system can bind |
| Startup `InitializationProvider` | androidx.startup | no | `android:exported="false"` | OK — app-private |
| `ProfileInstallReceiver` | androidx.profileinstaller | yes | `android.permission.DUMP` (shell/system-only) | OK — standard baseline-profile tooling |

The manifest also declares the auto-generated
`DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, which protects any dynamically
registered receivers by default (none are registered).

## Intent-handling surface (app code)

| Check | Finding |
|---|---|
| Nested/forwarded Intents (`getParcelableExtra` → `startActivity`) | None — no Intent redirection surface |
| Intent extras read (`getIntent`, `get*Extra`) | None — `MainActivity.onCreate` never reads its Intent, so no untrusted payload is processed |
| `onNewIntent` handling | Not overridden anywhere (no `singleTop`/warm-boot path) |
| `PendingIntent` creation | None |
| Broadcasts (`sendBroadcast`, `registerReceiver`, sticky) | None |
| `ContentProvider` implementations or `contentResolver` queries | None |
| Services in app code | None — `TtsService` is a plain Kotlin interface/class wrapping `TextToSpeech`, not an Android `Service` |

## AppFunctions entry point

`InsultFunctions.getFrenchInsult` is the only externally invokable code path
(via the system-mediated AppFunctions services):

- Callers are gated by `BIND_APP_FUNCTION_SERVICE`, a signature|privileged
  permission held only by the system.
- The single input (`maxLevel`) is range-validated and rejected with
  `AppFunctionInvalidArgumentException` when out of range.
- Failures surface as typed AppFunction exceptions without leaking internals.

## Permissions

The app requests only `android.permission.INTERNET` (normal protection level).
No custom permissions are defined by the app itself, and none are needed: no
component exposes functionality to other apps.

## Re-audit triggers

Re-run this audit (skill: `android-intent-security`) if any of the following
are introduced:

- A deep link, share target, or any new `intent-filter` on an activity
- Reading Intent extras in `MainActivity` (including `onNewIntent`)
- Notifications or widgets (PendingIntent creation)
- A ContentProvider, BroadcastReceiver, or bound/exported Service in app code
