# Claude deep-link route probe

Two dated probes. The **June 2026** results come first and are kept as history; the
[November 2026 probe](#november-2026-probe) below is the current one. Two June results no
longer hold: `claude://new` stopped opening a chat, and `CLEAR_TASK` is no longer required.

**Device:** Pixel 10 Pro XL (`mustang`), Android **17 / API 37**.
**Claude app:** `com.anthropic.claude` — versionName **`1.260611.30`**, versionCode **`26061130`** (minSdk 32, targetSdk 36).
**Date:** 2026-06-21.
**Method:** per URI — `am force-stop`, `cmd package resolve-activity`, `am start -W` (package-pinned), capture the foreground activity, screenshot the actual destination. Resolution success ≠ correct destination, so every screen was eyeballed.

All candidates resolve through `com.anthropic.claude/.deeplink.DeepLinkActivity` and land on the single `.mainactivity.MainActivity` (Compose navigation inside), so the destination is determined by the **rendered screen**, not the activity name.

| URI | Resolves via | Launch | Actual destination screen |
|---|---|---|---|
| `claude://code` | DeepLinkActivity | ok (318 ms) | **Code / Remote Control list** — tabs All / Working / Ready for review, live sessions, "New session" button. ✅ **Code route.** |
| `claude://code/new` | DeepLinkActivity | ok (394 ms) | **Code task composer** — "Opus 4.8 (1M)", Default Cloud Environment, repo selector, "Describe what you want to build…". (Extension hook `NEW_CODE`.) |
| `claude://claude.ai/new` | DeepLinkActivity | ok (325 ms) | New chat composer ("Hello…", "Chat with Claude…", keyboard up). |
| `claude://new` | DeepLinkActivity | ok (275 ms) | **New conversation composer** — one tap, keyboard up. ✅ **Chat route.** |
| `https://claude.ai/new` | DeepLinkActivity (match `0x508000`) | ok (299 ms) | New chat composer (equivalent to the above). |

## Decisions

- **Code → `claude://code`** — confidence **DOCUMENTED**. Opens the Code / Remote Control list directly.
- **Chat → `claude://new`** — confidence **PROBED**. Opens a new conversation composer in one tap (keyboard up). Label stays **"Chat"** (Branch A); the honest "Open Claude" fallback label is **not** needed.
- `claude://claude.ai/new` and `https://claude.ai/new` are equivalent new-chat routes; `claude://new` is the shortest/cleanest, so it is the chosen primary.

## Extension notes (validated, not shipped)

- `claude://code/new` opens the Code **task** composer → the spec's `NEW_CODE` extension hook is real and works. A prefilled task `claude://code/new?q=<url-encoded>` is plausible; the `q=` encoder already exists in `LaunchUri`.

## Launch-flag probe — re-navigation when Claude is already running (2026-06-21)

Bug found in real use: with Claude already running (backgrounded on, say, the Code page), tapping a widget button just resumed the existing task on its current page — the deep link was ignored. Probed by force-starting Claude on `claude://code`, pressing Home, then firing `claude://new` with different intent flags:

| Flags (`am start -f`) | Result |
|---|---|
| `NEW_TASK` (`0x10000000`) | ❌ stayed on the Code page (bug) |
| `NEW_TASK \| CLEAR_TOP` (`0x14000000`) | ❌ still Code — Claude does not re-route a re-delivered intent into an existing task |
| `NEW_TASK \| CLEAR_TASK` (`0x10008000`) | ✅ new chat composer |

**Decision:** `ViewUri` launches use `FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK`. Claude only re-runs its deep-link routing on a fresh (cleared) task, and clearing also gives the launcher the right semantics — each button deterministically lands on its surface. Confirmed end-to-end via the widget on-device.

---

# November 2026 probe

**Device:** Pixel 10 Pro XL (`mustang`), Android **17 QPR2 / API 37.2**, build `CP4A.261105.040.A1`, security patch 2026-11-05.
**Launcher:** Kvaesitso **1.41.0** (`de.mm20.launcher2.release`, targetSdk 36), the default home app.
**Claude app:** `com.anthropic.claude` — versionName **`1.261123.19`**, versionCode **`26112319`** (minSdk 32, targetSdk 37).
**Date:** 2026-11-26.
**Method:** as in June (`resolve-activity`, `am force-stop`, `am start -W`, read the screen), then real taps on the widget inside Kvaesitso with logcat's `ActivityTaskManager: START u0 {…}` lines as evidence. Screens were identified from the accessibility tree and local screenshots (not kept in the repo).

## What changed since June

1. **Widget taps were blocked in Kvaesitso.** With v1.0 installed, tapping a button did nothing.
2. **`claude://new` no longer opens a new chat.** It lands on the Code session list.
3. **Claude resolves links through a new component**, `.deeplink.SecretLinksInClaudeTask` (June: `.deeplink.DeepLinkActivity`). Every candidate below resolves to it.
4. **`CLEAR_TASK` is no longer required** for a running Claude to follow a link, though it still works.

## Routes, cold start (`NEW_TASK | CLEAR_TASK`, after `am force-stop`)

| URI | Launch | Actual destination screen |
|---|---|---|
| `claude://code` | ok, COLD, 259 ms | **Code session list** — "Filter sessions", Devices, Sessions, "New session". ✅ **Code route.** |
| `claude://new` | ok, COLD, 217 ms | ❌ **Code session list**, not a chat. Same result when the previous screen was a chat (194 ms). |
| `claude://claude.ai/new` | ok, 116 ms | **New chat composer** — greeting, "Chat with Claude", keyboard up. ✅ **Chat route.** |
| `https://claude.ai/new` | ok, COLD, 222 ms | Composer with the keyboard up (same accessibility tree as the row above; screenshot not inspected). |
| `claude://code/new` | ok, COLD, 189 ms | Composer with the keyboard up (the Code task composer per Anthropic's docs; screenshot not inspected). |
| `https://claude.ai/code` | ok, COLD, 201 ms | Composer with the keyboard up (documented as the new-session composer; screenshot not inspected). |

## Routes, warm (Claude running on another screen, then Home)

| Starting screen | URI | Flags | Result |
|---|---|---|---|
| Code list | `claude://claude.ai/new` | `NEW_TASK` (`0x10000000`) | ✅ new chat composer |
| New chat | `claude://code` | `NEW_TASK` (`0x10000000`) | ✅ Code list |
| Code list | `claude://claude.ai/new` | `NEW_TASK \| CLEAR_TASK` (`0x10008000`) | ✅ new chat composer |

In June, `NEW_TASK` alone left Claude on its current screen. That is fixed on Claude's side.

## Widget taps inside Kvaesitso

**Before (v1.0, `actionRunCallback` → `startActivity` from the broadcast):**

```
Background activity launch blocked! … callingPackage: com.clayboicardi.claudewidget … callingUidProcState: RECEIVER …
START u0 {act=android.intent.action.VIEW dat=claude://code/... flg=0x10008000 pkg=com.anthropic.claude
  cmp=com.anthropic.claude/.deeplink.SecretLinksInClaudeTask} … (BAL_BLOCK) result code=102
```

Kvaesitso stayed in the foreground. The same v1.0 path was **allowed** from the Pixel launcher on an API 37.0 and an API 37.2 emulator (`BAL_ALLOW_TOKEN`), and **blocked** from Kvaesitso 1.41.0 on an API 37.2 emulator, so the difference is the launcher, not the phone. Why the Pixel launcher's taps carry the allowance and Kvaesitso's stock `AppWidgetHostView` taps do not was not established.

**After (v1.1.0, tap → `LaunchActivity` → the launch chain):**

```
START u0 {dat=glance-action:/... cmp=com.clayboicardi.claudewidget/.LaunchActivity (has extras)} …
  (realCallingUid=<Kvaesitso>) (BAL_ALLOW_VISIBLE_WINDOW [realCaller]) result code=0
START u0 {act=android.intent.action.VIEW dat=claude://code/... flg=0x10008000 pkg=com.anthropic.claude
  cmp=com.anthropic.claude/.deeplink.SecretLinksInClaudeTask} …
```

| Tap | Claude running | Claude force-stopped first |
|---|---|---|
| **Code** | ✅ Code session list | ✅ Code session list |
| **Chat** | ✅ new chat composer, keyboard up | ✅ new chat composer, keyboard up |
| **Ask Claude…** pill | ✅ new chat composer, keyboard up | not run |

## Widget taps on the stock Pixel launcher (emulator)

The phone runs Kvaesitso, so the stock launcher was checked on an emulator: API 37.0, Pixel
launcher (`com.google.android.apps.nexuslauncher`), Claude not installed, v1.1.0 built from
the branch head, 2026-11-26. Each of **Chat**, **Code** and the pill gave:

```
START u0 {dat=glance-action:/... cmp=com.clayboicardi.claudewidget/.LaunchActivity (has extras)} …
  (realCallingUid=<Pixel launcher>) (BAL_ALLOW_VISIBLE_WINDOW [realCaller]) result code=0
START u0 {act=android.intent.action.VIEW dat=claude://… flg=0x10008000 pkg=com.anthropic.claude} … result code=-91
START u0 {act=android.intent.action.VIEW dat=market://details/... flg=0x10000000} … result code=-91
START u0 {act=android.intent.action.VIEW dat=https://play.google.com/... cmp=com.android.chrome/…} …
  (BAL_ALLOW_VISIBLE_WINDOW) result code=0
```

So the new tap path is allowed from the stock launcher too, and the not-installed fallback
runs to its last step (`-91` = no activity found; this image has no Play Store app). On the
API 37.2 emulator, inside Kvaesitso, the same chain stopped one step earlier, at the Play
Store app.

## Decisions

- **Taps start an Activity** (`actionStartActivity<LaunchActivity>`), not a callback. `LaunchActivity` is non-exported, draws nothing, runs the same ordered chain and finishes.
- **Chat → `claude://claude.ai/new`** — confidence **PROBED** on Android; documented by Anthropic for Desktop. `claude://new` is retired.
- **Code → `claude://code`** — confidence **DOCUMENTED** (Anthropic's mobile deep-link article) and re-probed.
- **`NEW_TASK | CLEAR_TASK` stays.** It is the one flag combination verified on both app generations, and it gives each button the same result every time. Its cost: it resets whatever Claude was showing. Dropping `CLEAR_TASK` is listed as a proposal in the audit.

## Not verified here

- The widget on this phone under the stock Pixel launcher. The stock launcher was checked on an
  emulator, which has no Claude app, so "stock launcher + real Claude" is the one combination
  not seen in this probe.
- The pill after a force-stop of Claude (Chat, which uses the same link, was run).
- The Claude-launcher fallback (deep link fails to resolve but Claude is installed): unit-tested only.
- The build on the phone predates one `@SuppressLint` annotation on `LaunchActivity`; the runtime code is the same as commit `ba22f34`.
