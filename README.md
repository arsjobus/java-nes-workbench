# NES Asset Workbench

A local-first Java desktop application for inspecting and eventually editing graphics stored in NES ROMs.

## Goals
- Inspect supported `.nes` ROM files without modifying the source.
- Decode NES 2bpp CHR tiles and display indexed 8×8 tiles.
- Add tile mapping, sprite composition, PNG export/import, and safe ROM rebuilding incrementally.
- Keep project data portable and documented.
- Require automated tests for every feature and bug fix.

## Current starter functionality
- JavaFX application shell.
- Parser for standard iNES 1.0 headers and PRG/CHR data layout.
- CHR 2bpp tile decoder.
- Unit tests for ROM parsing and tile decoding.

This is a starter project, not a complete ROM editor. Mapper-specific interpretation, NES 2.0 support, ROM writing, project files, and editing UI are not implemented. NES 2.0 is explicitly rejected rather than being misread as iNES 1.0.

## Requirements
- JDK 21
- Maven 3.9 or newer

## Build, test, run
```bash
mvn test
mvn clean package
mvn javafx:run
```

## Safety principles
- Never overwrite a source ROM by default.
- Keep visual tile mappings separate from actual CHR bytes.
- Validate lengths, offsets, and indices before accessing binary data.
- Keep core ROM and graphics logic independent of JavaFX where practical.
- Add deterministic tests for normal, boundary, invalid, and regression cases.

See `ROADMAP.md` for planned milestones and `AGENTS.md` for mandatory development practices.
