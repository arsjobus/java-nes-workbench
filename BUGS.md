# Bug Log

Every confirmed bug fix should add a regression test where practical.

## Known limitations / outstanding work
- [ ] UI supports opening, inspecting, exporting, visual tile mapping, and saving/loading projects. CHR bytes cannot be edited. Keyboard navigation beyond the undo/redo shortcuts is planned for v0.5.
- [ ] Tile colors are a fixed grayscale palette; real NES palette inspection is future work.
- [ ] The UI layer (`App`) has no automated tests. The v0.2 mapping and project controls compile and are wired to unit-tested classes, but have not yet been run interactively; verify on a machine with a display. Logic they depend on lives in JavaFX-free, unit-tested classes.
- [ ] Parser supports standard iNES 1.0 only. NES 2.0 is deliberately rejected.
- [ ] Mapper number is parsed, but mapper-specific interpretation is not implemented.
- [ ] ROM writing is not implemented. Tile mappings are visual only until v0.4, and PNG export stays in original CHR order.
- [ ] A project is bound to one exact ROM (by SHA-256); it cannot be applied to a patched or re-saved copy of that ROM.

## Reporting template
### [Short description]
- **Observed:**
- **Expected:**
- **Reproduction:**
- **Regression test:**
- **Status:** Open / In progress / Fixed
