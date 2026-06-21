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
