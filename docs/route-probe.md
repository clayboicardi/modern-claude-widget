# Claude deep-link route probe

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
