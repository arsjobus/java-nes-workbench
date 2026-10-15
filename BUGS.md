# Bug Log

Every confirmed bug fix should add a regression test where practical.

## Known limitations / outstanding work
- [ ] UI supports opening, inspecting, exporting, visual tile mapping, and saving/loading projects. CHR bytes cannot be edited. Keyboard navigation beyond the undo/redo shortcuts is planned for v0.5.
- [ ] Tile colors are a fixed grayscale palette; real NES palette inspection is future work.
- [ ] The UI layer (`App`) has no automated tests in the repository. The v0.2 mapping and project controls and the v0.3 composition controls compile and are wired to unit-tested classes, but have not been run interactively; verify on a machine with a display. For v0.3, composition placement and clearing, enable/disable states, dirty tracking and reset on ROM load were exercised with a throwaway script under a virtual display (Xvfb, JavaFX 11, not the pom's 21.0.6). The New composition dialog, the file choosers for exporting and importing artwork, and the comparison dialog were not exercised. Logic they depend on lives in JavaFX-free, unit-tested classes.
- [ ] Parser supports standard iNES 1.0 only. NES 2.0 is deliberately rejected.
- [ ] Mapper number is parsed, but mapper-specific interpretation is not implemented.
- [ ] ROM writing is not implemented. Tile mappings are visual only until v0.4, and PNG export stays in original CHR order. Imported artwork is compared with the source tiles but cannot yet be applied to CHR data.
- [ ] Compositions: no undo/redo for composition edits, no tile flipping or per-tile palette attributes, no renaming or resizing in the UI (the model supports renaming), at most 32x32 tiles per composition and 256 compositions per project.
- [ ] Artwork import matches the fixed grayscale palette exactly (colors are not approximated) and rejects transparency. A PNG missing only its trailing end-of-file chunk may still import, because the image decoder accepts it once all pixel data is present; truncated pixel data is always rejected.
- [ ] A project is bound to one exact ROM (by SHA-256); it cannot be applied to a patched or re-saved copy of that ROM.

## Reporting template
### [Short description]
- **Observed:**
- **Expected:**
- **Reproduction:**
- **Regression test:**
- **Status:** Open / In progress / Fixed
