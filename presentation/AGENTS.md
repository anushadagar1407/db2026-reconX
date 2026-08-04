# Presentation Instructions

This file extends the repository-wide rules in `../AGENTS.md`; follow both. The `presentation/` package is the Reveal.js deck and presentation-only design system. It is separate from the main ReconX application in `frontend/` and embeds that application only through the guarded `LiveDemoFrame` boundary. Do not duplicate frontend product behavior in the deck.

## Development and Verification

Run native package commands from `presentation/`:

```sh
npm ci
npm run dev                 # Vite on http://localhost:4173
npm run lint
npm test
npm run build
npm run verify              # Runs lint, test, and build in sequence
npm run preview             # Production preview on http://localhost:4173
npm run export:slides       # Build, serve, and refresh every tracked slide PNG
npm run export:slides -- --slide 1
npm run export:pdf          # Build, serve, and refresh the delivery PDF
```

Presentation export requires the pinned Playwright Chromium build. Install it explicitly with `npx playwright install chromium`; the exporters never download browsers. Use `npm run export:slides -- --url http://localhost:4173` or `npm run export:pdf -- --url http://localhost:4173` to inspect an already-running HTTP(S) deck without building or starting the local preview. External URLs must not contain username/password userinfo. Run either command with `--help` for its complete CLI contract.

Run the main frontend separately from `frontend/` with `npm ci && npm run dev`; its native default is `http://localhost:5173`.

Run Docker commands from the repository root. The presentation service is opt-in:

```sh
docker build --target verification -t reconx-presentation:verify ./presentation
docker compose --profile presentation up -d --build
docker compose --profile presentation ps presentation
docker compose --profile presentation stop presentation
docker compose --profile presentation rm --force presentation
```

To point the container at another demo without rebuilding:

```sh
RECONX_DEMO_URL="https://demo.example.test" docker compose --profile presentation up -d --build presentation
```

Compose defaults `RECONX_DEMO_URL` to `http://localhost:5173`. The nginx entrypoint validates that it is an HTTP(S) URL and writes frozen runtime configuration to `/config.js`; invalid schemes stop the container. Native Vite serves `public/config.js` and does not read the shell variable. `src/config/runtime.ts` validates runtime configuration again and falls back to `http://localhost:5173`.

## File Map

- `src/main.tsx`: React entry point and global Reveal/design-system style imports.
- `src/App.tsx`: 1920x1080 Reveal deck configuration and setup-only scaffold slide; `src/App.test.tsx` covers mounting.
- `src/design-system/index.ts`: required public barrel for all presentation primitives and prop types.
- `src/design-system/tokens.css`: primitive and semantic color, type, spacing, safe-area, grid, shape, elevation, motion, focus, data, and status tokens.
- `src/design-system/system.css`: Reveal mappings and component/layout/print/reduced-motion styles.
- `src/design-system/SlideCanvas.tsx`: bounded slide shell with `paper`, `soft`, `ink`, and `blue` backgrounds; safe, compact, and edge insets.
- `src/design-system/Layout.tsx`: `Stack`, `Cluster`, `Grid`, and `Split` overflow-safe composition primitives.
- `src/design-system/contracts.ts`: typed gaps, alignment values, and the allowed technology registry keys.
- `src/design-system/AutoFit.tsx`: two-axis fitting, readable-scale floor, overflow signal, and ResizeObserver fallback.
- `src/design-system/Typography.tsx`: `SlideTitle`, `SectionHeading`, `BodyText` (`body`/`lede`), and metadata-only `SlideLabel`.
- `src/design-system/Callout.tsx`: the single semantic callout/surface primitive and status tones.
- `src/design-system/TechIcon.tsx`: centralized Devicon registry, labels, sizes, and accessibility behavior.
- `src/design-system/BrandMark.tsx`: local Deutsche Bank lockup/symbol component.
- `public/assets/deutsche-bank-logo.svg` and `public/assets/deutsche-bank-mark.svg`: official local brand artwork; the mark is also the favicon in `index.html`.
- `src/components/LiveDemoFrame.tsx`: explicit iframe activation, origin/source-validated readiness message, timeout, recovery, and sandbox policy.
- `src/config/runtime.ts`, `public/config.js`, and `docker-entrypoint.d/40-write-runtime-config.sh`: demo URL configuration path.
- `src/presentation.css`: setup-slide and live-demo-specific styles; reusable system styles belong under `src/design-system/`.
- `src/design-system/*.test.tsx`, `src/components/LiveDemoFrame.test.tsx`, and `src/config/runtime.test.ts`: co-located Vitest/Testing Library coverage.
- `scripts/presentation-export-runtime.mjs` and `scripts/presentation-export-runtime.test.mjs`: shared deterministic build/server/browser/wait runtime plus tested generation-level staged publication and rollback.
- `scripts/export-slides.mjs`: Playwright PNG CLI, Reveal navigation, per-slide capture, and overflow diagnostics.
- `scripts/slide-export-dom.mjs` and `scripts/slide-export-dom.test.mjs`: browser-safe measurement and adversarial tests for the real safe-content box and overflow signals.
- `scripts/slide-export-lib.mjs` and `scripts/slide-export-lib.test.mjs`: unit-tested argument, ordering, selection, manifest, cleanup, provenance, image, and exit-policy logic.
- `scripts/export-pdf.mjs`: Reveal print-mode PDF CLI and PDF.js validation integration.
- `scripts/pdf-export-lib.mjs` and `scripts/pdf-export-lib.test.mjs`: PDF arguments, paths, dimensions/page-count validation, and manifest provenance.
- `slide-renders/slide-NNN.png`: tracked canonical 1920x1080 renders in 1-based presentation order.
- `slide-renders/manifest.json`: tracked run metadata, per-slide provenance, dimensions/hashes, and overflow/AutoFit diagnostics.
- `pdf-export/reconx-presentation.pdf` and `pdf-export/manifest.json`: tracked delivery PDF and validated PDF provenance.
- `../.github/workflows/presentation-export.yml`: push/manual CI generation and commit-specific artifact upload.

## Slide-Authoring Contract

- Import components and types through `src/design-system/index.ts` only, for example `import { SlideCanvas, SlideTitle, Stack } from './design-system'` from `src/`. Do not deep-import design-system internals.
- Every Reveal `<Slide>` must use `SlideCanvas`. `.slide-canvas__safe-area` is the padded frame and `.slide-canvas__safe-content` is the measurable content box (1696x904 for the default inset). Keep normal/critical content in its default safe inset; use `compact` only when justified. `edge` explicitly changes the measured box to the full 1920x1080 canvas and is only for intentional full-bleed media, not ordinary critical copy.
- Compose with `Stack`, `Cluster`, `Grid`, and `Split`. Use typed `gap`, `align`, `justify`, column, and ratio props instead of one-off flex/grid CSS. Available gaps are `none`, `2xs`, `xs`, `sm`, `md`, `lg`, `xl`, and `2xl`.
- Preserve the 1920x1080 authoring canvas and the 1696x904 default safe content region. Test all real content; Reveal scaling is not permission to overflow or use small type.
- Use `SlideTitle`, `SectionHeading`, `BodyText`, and `SlideLabel` according to hierarchy. `SlideLabel` is for concise metadata, not an eyebrow repeated on every slide.

### AutoFit

Use `AutoFit` only for bounded, difficult material such as a dense diagram, table, or code sample after the layout has already been simplified. Do not use it to cram extra narrative onto a slide.

- It measures both width and height with `ResizeObserver`; without that API it measures initially and on window resize.
- The default minimum scale is `0.72`. Caller values are clamped to the hard readability floor of `0.6` and the maximum of `1`; non-finite values (`NaN`/infinities) normalize to `0.72`.
- It exposes the applied scale through `data-scale`, reports failure through `data-overflow="true"` and `onOverflowChange`, and emits a development warning naming `debugName` when the required scale is below the allowed minimum.
- When overflow is reported, reduce copy, split the slide, simplify the visualization, or increase its allotted area. Never bypass the floor, override the transform, or force smaller text.

## Design Language

- Use semantic tokens from `tokens.css`; do not hard-code substitute colors, spacing, shadows, radii, timing, or type sizes in slide content.
- Design for a bright training-room projector: light `paper` is the default, ink must remain high contrast, and inverse `ink`/`blue` slides require deliberate use and tinted secondary text.
- Deutsche Bank blue is a signal color, not a decorative wash. Use it for hierarchy, rules, focus, and meaningful emphasis; do not spread it evenly across every element.
- Keep typography editorial and offline-safe: obvious title/section/body steps, restrained weights, balanced headings, readable body measure, and monospace only for actual code or data.
- Build rhythm with tight related groups and generous separation between ideas. Prefer asymmetric, left-aligned composition; do not center everything or use uniform padding everywhere.
- Use `SlideCanvas` background variants and `Callout` tones semantically. `Callout` is not a generic card; avoid nested surfaces and repeated card grids.
- Motion must communicate state, use the system timing/easing, and remain sparse. Preserve `prefers-reduced-motion` behavior. Print/PDF output must keep the 1920x1080 canvas, fitted content, and exact colors without transitions.
- Reject neon or gradient text, glassmorphism, glowing dark themes, teal-on-navy AI styling, floating blobs, decorative monospace, excessive rounding/shadows, generic icon-card grids, hero-metric templates, decorative charts, and thick one-sided accent borders.

## Icons and Brand

- Use only `TechIcon` and its typed `Technology` registry: React, TypeScript, JavaScript, Java, Spring, PostgreSQL, Apache Kafka, Docker, GitHub, GitHub Actions, Vite, nginx, Prometheus, and Grafana. Do not import `devicons-react` directly in slides, add ad hoc icon packages, or mix logo styles, labels, colors, or sizes.
- Non-decorative `TechIcon` instances provide the registry label automatically. Set `decorative` only when the same meaning is already present in adjacent text; decorative artwork must stay out of the accessibility tree.
- Use `BrandMark` with the local assets. Do not redraw, recolor, distort, crop, or change the proportions of the Deutsche Bank artwork. It is public domain for copyright purposes but remains a protected trademark; use it only in the authorized presentation context.

## Content Boundary

The current `App.tsx` slide is setup/design-system scaffolding, not the final deck. Do not add factual claims, screenshots, metrics, customer or team names, architecture assertions, or presentation narrative without task-specific source evidence. Preserve the explicit iframe activation, sandbox, origin/source checks, timeout, and fallback behavior unless a task specifically changes that security boundary.

## Testing and Visual Review

- Finish every presentation change with `npm run verify`; keep ESLint at zero warnings and add adversarial tests for boundaries, invalid input, overflow, accessibility, resize behavior, and recovery paths introduced by new utilities.
- For layout or visual changes, inspect a production build at 1920x1080 and at least one smaller viewport. Confirm no document/slide overflow and no browser console errors; do not leave screenshots or browser artifacts tracked.
- The export order is each horizontal Reveal slide followed by its nested vertical slides from top to bottom. Filenames use 1-based, zero-padded ordinals. The export fragment policy is `all-visible`: fragments are forced into a fully revealed QA state while controls, transitions, animations, and carets are suppressed. `LiveDemoFrame` remains in its idle state and is never activated.
- A non-zero exporter result means either invalid/tooling input or actionable slide diagnostics. Exit code `1` is written after the PNG and manifest when viewport, true safe-content, or unresolved `AutoFit` overflow is present; inspect the recorded diagnostics before changing content. Operational/usage failures use exit code `2`.
- Mandatory visual QA loop for slide work: run `npm run export:slides -- --slide <ordinal>`, read that entry in `slide-renders/manifest.json`, inspect the PNG for overlap, crowding, clipping, and weak rhythm, fix, and repeat. Run `npm run export:slides` for a full refresh before final review.
- A single-slide refresh is allowed only when the existing manifest and every retained PNG exactly match the current slide count and ordinal/horizontal/vertical structure. Missing/stale files, reordered/deleted slides, incomplete metadata, or retained overflow require `npm run export:slides`; the command fails before touching tracked artifacts.
- Do not rely only on Playwright DOM assertions, screenshots without manifest provenance, or manually named image files. The exporter owns filenames, ordering, hashes, and diagnostics.
- PDF is for release/delivery verification only. It uses Reveal `print-pdf` mode, print backgrounds, the exact 1469.04x826.08pt margin-adjusted page contract derived from the 1920x1080 canvas and `0.02` Reveal margin, and `pdfSeparateFragments: false`; PDF.js validates signature, bytes, page count, and dimensions within 0.05pt. Never substitute PDF review for the per-slide PNG loop.
- Both exporters generate complete candidate files and manifests in a hidden sibling generation directory, validate them, then publish via a directory swap. Handled publication failures restore the prior directory; candidate/backup directories are cleaned. A process or machine crash between filesystem renames cannot be fully transactional, so preserve any hidden sibling recovery directory for investigation rather than deleting tracked output manually.
- Refresh and stage `slide-renders/` and `pdf-export/` whenever slide visuals change. `sourceDirty` records whether source outside both render folders differed from `sourceCommit`; each PNG retains its own capture timestamp and provenance during partial refreshes. If an exact committed baseline is required, commit source changes first, rerun both full exports from that clean source revision, then stage the refreshed artifacts.
- `.github/workflows/presentation-export.yml` runs on every push to `develop` or `main` and by manual dispatch. CI performs `npm ci`, installs Chromium plus Linux dependencies, runs the full PNG and PDF exports, and uploads both directories as `presentation-exports-<commit-sha>` for 14 days. It never commits generated files. Export failures keep the job failed while the upload step still publishes any available diagnostics.
- Run `git diff --check` before handoff.
- Use `i-audit` for the final accessibility/performance/responsive check and `i-polish` for the final visual-detail pass on changed presentation surfaces.

## OpenCode Skill Routing

- `i-frontend-design`: establish or extend presentation visual context for a new slide surface.
- `i-extract`: add or consolidate reusable tokens, components, variants, or registry entries.
- `i-arrange`: correct slide composition, alignment, spacing, and hierarchy.
- `i-typeset`: adjust type scale, measure, weight, or readability.
- `i-adapt`: validate projection scaling, smaller viewports, print/PDF, and cross-device behavior.
- `i-harden`: handle overflow, long content, error states, accessibility, and resilience.
- `i-animate`: add purposeful state motion while preserving reduced-motion behavior.
- `i-audit`: run the technical quality review before finalizing visual work.
- `i-polish`: perform the last alignment, consistency, and micro-detail pass after behavior is correct.
- `i-normalize`: bring slide-specific styling back onto existing tokens and primitives when design drift appears.
