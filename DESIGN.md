# Design notes & constraints

This widget is rendered with **Jetpack Glance**, which compiles to Android **RemoteViews** — *not* HTML/CSS, Compose UI, or a canvas. Designs must stay within Glance's capabilities or they can't be built. If you're mocking up a new look (e.g. in a visual design tool), design to these limits and hand back a **static mockup / screenshot + exact values** (hex colors, dp sizes). Treat the mockup as a *target*; the implementation approximates it in Glance.

## Canvas

- Home-screen widget. Sizes: **Compact ≈ 2×2 cells** (icons only) and **Standard ≈ 4×2 cells** (icons + text labels). Resizable between.
- Current structure: rounded **container** → an **"Ask Claude…" input pill** (opens a new chat) → a **row of two icon buttons** (Chat 💬, Code `</>`). The pill and each button are independent tap targets (**≥ 48dp**).

## ✅ Glance supports

- Layout: `Box`, `Column`, `Row`, `Spacer`; alignment (start/center/end); `fillMaxSize`/`fillMaxWidth`; fixed `size`/`height`/`width`; `defaultWeight` to distribute space.
- `padding` (per-side or symmetric).
- **Solid** background colors; **rounded corners** via `cornerRadius` (a single radius for all corners on most launchers).
- `Text` — system font, size (sp), color, `FontWeight`, `maxLines`.
- `Image` — vector or PNG drawables, with an optional tint color.
- Tap actions on any element.

## ⚠️ Glance does NOT do well (will be approximated or dropped)

- **CSS-style gradients** — no native gradient; can only be faked with a prebuilt image/9-patch background.
- **Custom fonts** — effectively system fonts only.
- **Drop shadows / blur / elevation / glassmorphism** — not available.
- **Per-corner radii** — generally one uniform radius only.
- **Absolute positioning / overlap / z-index**, opacity layering, and **animations/transitions** — not supported.

## Practical guidance for mockups

- Use **flat fills** and **simple rounded rectangles**. Keep it to text + icon glyphs + solid backgrounds.
- Provide **exact hex colors**, **corner radii (dp)**, **padding (dp)**, font sizes (sp), and the **icon style** you want (outline vs filled, stroke weight).
- Keep icons original (do not reproduce Anthropic's or any brand's marks).

## Current palette & geometry — source of truth: `app/src/main/res/values/`

The shipped look is the Clayboicardi-brand reskin: blue-black surfaces, purple→green
gradients, phosphor-green text/glyphs, CC logo in the pill. Exact values live in
[`colors.xml`](app/src/main/res/values/colors.xml) and
[`dimens.xml`](app/src/main/res/values/dimens.xml). Because Glance has no gradient modifier,
the gradients are **layer-list shape drawables** (`widget_shell_bg` / `widget_pill_bg` /
`widget_button_bg`) applied as `ImageProvider` backgrounds — edit the `grad_*` alpha bytes in
`colors.xml` to retune them.

| Token | Hex |
|---|---|
| Shell base | `#0D1019` |
| Pill base | `#161B27` |
| Button base | `#222A3A` |
| Text / glyphs (brand green) | `#08FF08` |
| Brand purple | `#47007D` |

Geometry: container radius 28dp · pill 22dp · buttons 16dp · padding 12dp · ~168dp tall.
