# Changelog

## 1.10.0 — 2026-08-15

- Promote the conformance-validated 1.10 release candidate without additional
  runtime changes.

## 1.10.0-rc.2 — 2026-08-12

- Match iOS vertical alignment for text that fills its available height.
- Center bounded horizontal-grid items within their row tracks.
- Center bounded vertical-grid items within their column tracks.
- Refresh screenshot conformance baselines for the three remediated fixtures.

## 1.10.0-rc.1 — 2026-08-11

- Replace Accompanist Pager with Compose Foundation Pager.
- Replace ExoPlayer 2.19.1 with AndroidX Media3 1.10.1.
- Consolidate outer application chrome on Material 3 while retaining a narrow
  Material 2 compatibility boundary for protected authored controls.
- Add strict recursive parsing for `schemaVersion: "1.0.0"` descriptors with
  JSON-style paths in `ViewContentDescriptorException`.
- Align sizing, modifier order, grids, lists, included content, actions, and
  accessibility behavior with the shared ViewContent contract.
- Add synchronized unit, interaction, and 390 by 844 screenshot conformance
  coverage.

See [MIGRATION.md](MIGRATION.md) for Media3 and versioned-content guidance.
