# NES Asset Workbench

A local-first Java desktop application for inspecting and eventually editing graphics stored in NES ROMs.

## Goals
- Inspect supported `.nes` ROM files without modifying the source.
- Decode NES 2bpp CHR tiles and display indexed 8×8 tiles.
- Add tile mapping, sprite composition, PNG export/import, and safe ROM rebuilding incrementally.
- Keep project data portable and documented.
- Require automated tests for every feature and bug fix.

## Current functionality (v0.1)

- Open a `.nes` ROM from the UI; the source file is only ever read.
- Parser for standard iNES 1.0 headers and PRG/CHR data layout, with clear errors for truncated files, bad magic, and NES 2.0.
- ROM metadata panel: mapper, PRG/CHR sizes, trainer, battery, mirroring, file size, trailing bytes.
- CHR 2bpp tile decoder and a tile grid browser: one 8 KiB CHR bank (512 tiles) per page, zoom 1x-8x, optional grid overlay, click-to-select with tile index and CHR/file offsets in the status bar.
- Export all decoded CHR tiles as a 2-bit indexed grayscale PNG at 1x (16 tiles per row). Existing files are never replaced without explicit confirmation.
- Unit tests for parsing, loading, summaries, tile decoding, layout, rendering, browsing state, and PNG export.

This is not yet a ROM editor. Mapper-specific interpretation, NES 2.0 support, ROM writing, project files, and editing UI are not implemented. CHR-RAM cartridges have no tile data to display. Tile colors use a fixed grayscale palette, not the game's real NES palette.

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
