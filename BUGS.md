# Bug Log

Every confirmed bug fix should add a regression test where practical.

## Known limitations / outstanding work
- [ ] UI supports opening, inspecting, and exporting only; there is no tile mapping or editing yet. Keyboard navigation is planned for v0.5.
- [ ] Tile colors are a fixed grayscale palette; real NES palette inspection is future work.
- [ ] The UI layer (`App`) has no automated tests; it was smoke-tested manually under a virtual display. Logic it depends on lives in JavaFX-free, unit-tested classes.
- [ ] Parser supports standard iNES 1.0 only. NES 2.0 is deliberately rejected.
- [ ] Mapper number is parsed, but mapper-specific interpretation is not implemented.
- [ ] ROM writing and project-file persistence are not implemented.

## Reporting template
### [Short description]
- **Observed:**
- **Expected:**
- **Reproduction:**
- **Regression test:**
- **Status:** Open / In progress / Fixed
