# NES Asset Workbench

A local-first Java desktop application for inspecting and eventually editing graphics stored in NES ROMs.

## Goals
- Inspect supported `.nes` ROM files without modifying the source.
- Decode NES 2bpp CHR tiles and display indexed 8×8 tiles.
- Add tile mapping, sprite composition, PNG export/import, and safe ROM rebuilding incrementally.
- Keep project data portable and documented.
- Require automated tests for every feature and bug fix.

## Current functionality (v0.3)

- Open a `.nes` ROM from the UI; the source file is only ever read.
- Parser for standard iNES 1.0 headers and PRG/CHR data layout, with clear errors for truncated files, bad magic, and NES 2.0.
- ROM metadata panel: mapper, PRG/CHR sizes, trainer, battery, mirroring, file size, trailing bytes.
- CHR 2bpp tile decoder and a tile grid browser: one 8 KiB CHR bank (512 tiles) per page, zoom 1x-8x, optional grid overlay, click-to-select with tile index and CHR/file offsets in the status bar.
- Export all decoded CHR tiles as a 2-bit indexed grayscale PNG at 1x (16 tiles per row). Existing files are never replaced without explicit confirmation.
- Visual tile mapping: select a tile, swap it with another (Ctrl/Cmd+click) or move it one position earlier or later, with undo and redo (Ctrl/Cmd+Z, Ctrl/Cmd+Y). Mapping only changes what is displayed; CHR bytes and original tile identities are never altered, and PNG export stays in original CHR order.
- Versioned project files (`.nesproj`) that store a tile mapping for one specific ROM (matched by SHA-256). See [`docs/PROJECT_FORMAT.md`](docs/PROJECT_FORMAT.md). Opening another ROM or project asks before discarding unsaved mapping changes.
- Sprite compositions: create a named grid of 1x1 to 32x32 tiles, select a tile in the sheet and click a composition cell to place it (right-click clears a cell). Cells store the *original* CHR tile index, so they are unaffected by tile mapping changes. Compositions are saved in the project file (format version 2, see [`docs/PROJECT_FORMAT.md`](docs/PROJECT_FORMAT.md)); projects without compositions are still written as version 1.
- Artwork export: write a composition as a 2-bit indexed PNG at 1x (8 pixels per tile). Empty cells are index 0.
- Artwork import and comparison: read an edited PNG back, validate it, and report which pixels and cells differ from the source tiles. The PNG must be a real PNG of exactly the composition's size, fully opaque, using only the four palette colors (indexed, grayscale, or truecolor files are all accepted; indices come from the colors, not the file's palette order). Importing only compares; it never changes the ROM, the mapping, or a composition.
- Unit tests for parsing, loading, summaries, tile decoding, layout, rendering, browsing state, PNG export, mapping, undo/redo, project files, compositions, artwork import/export, and comparison.

This is not yet a ROM editor. Mapper-specific interpretation, NES 2.0 support, ROM writing and CHR byte editing are not implemented, and edited artwork cannot yet be applied back to a ROM (planned for v0.4). CHR-RAM cartridges have no tile data to display. Tile colors use a fixed grayscale palette, not the game's real NES palette.

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
