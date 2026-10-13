# Development Roadmap

Every item must include implementation, automated tests, and explicit completion criteria. A feature is not complete merely because it compiles.

## v0.1 — Inspect NES graphics

### 1. Project foundation
- [x] Create Maven project and JavaFX application shell
- [x] Add mandatory AI development/testing instructions
- [x] Add roadmap and bug log
- [x] Configure JUnit 5
- [ ] Verify clean build and tests on the target development machine

### 2. ROM reading and validation
- [x] Parse standard iNES 1.0 header fields
- [x] Locate trainer, PRG-ROM, and CHR-ROM data
- [x] Reject truncated files and NES 2.0 headers clearly
- [ ] Display parsed metadata in the UI

**Test goals:** header fields, trainer offsets, PRG/CHR boundaries, CHR-RAM cartridges, invalid magic, truncated files, short headers, NES 2.0 rejection, defensive copies.

### 3. CHR tile decoding
- [x] Decode 2bpp CHR data into indexed 8×8 pixel tiles
- [ ] Add JavaFX tile grid with indices and selection
- [ ] Add zoom and grid overlays
- [ ] Export decoded tile sheets to PNG

**Test goals:** known bitplane patterns, all four pixel values, multiple tiles, empty data, invalid length, deterministic output.

## v0.2 — Mapping and projects
- [ ] Select, swap, and reorder tile mapping entries
- [ ] Preserve original tile identities
- [ ] Add undo and redo
- [ ] Save and reload versioned project files
- [ ] Validate mapping indices and project files

**Test goals:** swaps, identity mapping, invalid indices, undo/redo sequences, save/load round trips, malformed files, version handling.

## v0.3 — Sprite composition and artwork
- [ ] Assemble larger sprite compositions from indexed tiles
- [ ] Save named compositions with source tile references
- [ ] Export PNG artwork
- [ ] Import edited PNGs with palette and format validation
- [ ] Compare imported graphics with source tiles

**Test goals:** layout boundaries, source-reference preservation, PNG dimensions, palette constraints, pixel round trips, invalid imports.

## v0.4 — Safe ROM rebuilding
- [ ] Keep mapping edits distinct from CHR byte edits
- [ ] Write modified output to a new file by default
- [ ] Preserve unrelated bytes wherever format permits
- [ ] Produce a byte-level difference report
- [ ] Reopen and validate rebuilt output

**Test goals:** source remains unchanged, unchanged-region preservation, expected changed offsets, output reparsing, malformed output rejection.

## v0.5 — Reliability and usability
- [ ] Recent projects and safe recovery
- [ ] Keyboard navigation and clear selection feedback
- [ ] Clear errors for unsupported ROM formats
- [ ] Regression fixtures and critical end-to-end tests
- [ ] User documentation and known limitations

## v1.0 — Stable mapper 0 workflow
- [ ] Complete and document open → inspect → map → export → rebuild → verify workflow
- [ ] Establish compatibility fixtures
- [ ] Document supported and unsupported formats
- [ ] Review file safety, test coverage, and error handling
- [ ] Verify clean build and full test suite

## Future expansion
- [ ] Add other mapper families when requirements are clear
- [ ] Expand palette inspection and batch exports
- [ ] Optional visual testing through the Rust NES emulator
- [ ] Evaluate interoperability with PixelForge and existing Python tools
