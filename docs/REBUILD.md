# Safe ROM rebuilding (v0.4)

Rebuilding turns a loaded ROM plus a set of **CHR byte edits** into a new ROM file. The code lives in
`com.nesworkbench.rebuild` and has no JavaFX dependency.

## Rules

- **Mapping and CHR edits are separate.** A `TileMapping` only changes what is displayed. A `ChrEdits` object (replacement
  16-byte data keyed by *original* tile index) is the only input that can change CHR bytes. Rebuilding takes no mapping,
  so reordering tiles on screen never changes the output.
- **New file only.** `RomWriter` creates the output with create-new semantics. It refuses an existing file (left untouched)
  and refuses the source ROM however its path is spelled (including symbolic links). There is no overwrite option.
- **Unrelated bytes are preserved.** The output starts as a copy of the source file; only edited tiles are replaced. Header,
  trainer, PRG-ROM, unedited tiles and trailing bytes are identical. The output is always the same length as the source.
- **Edits are validated first.** Edits to a CHR-RAM ROM, or to a tile the ROM does not have, are refused before anything is written.
- **Output is reopened and validated.** The written file is read back from disk and `RebuildVerifier` checks, independently
  of the rebuilder, that it parses, has the same layout and length, matches the source byte-for-byte outside CHR data, and
  that every tile is either the source tile or exactly its edit. A failing file is deleted and the problem reported.
- **Difference report.** `ByteDiff` lists each run of changed bytes by file offset, split by region (header, trainer,
  PRG-ROM, CHR-ROM, trailing bytes) with the affected CHR tile indices. At most 200 ranges are listed; the total is always exact.

## Artwork to CHR edits

`ArtworkChrEdits.from(composition, sourceTiles, artwork)` converts validated imported artwork into edits. Only tiles whose
pixels changed become edits. It refuses, rather than guesses, when:

- a tile is placed in several cells and the artwork differs between those cells (one tile holds one image);
- pixels other than index 0 are drawn in an empty cell (there is no tile to write);
- the artwork size does not match the composition, or the composition references tiles the ROM lacks.

Composition cells hold original tile indices, so edits land at the original CHR offsets regardless of the tile mapping.

## Limitations

- Rebuilt ROMs are validated structurally, not by running them in an emulator.
- Edits affect CHR-ROM only. Mapper-specific bank layouts are not interpreted; tiles are addressed linearly through the CHR data.
- No overwrite option, no in-place editing, no editing of PRG-ROM or header fields.
