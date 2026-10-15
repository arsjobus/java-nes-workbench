# Project file format (versions 1 and 2)

Project files (`.nesproj`) store a visual tile mapping and, from version 2, named sprite compositions for one ROM. They
never contain ROM or CHR data, and loading one never modifies the ROM. The format is UTF-8 text with `\n` or `\r\n`
line endings.

- **Version 1** holds the mapping only. It is what the program writes when a project has no compositions, so projects
  without compositions stay readable by older versions.
- **Version 2** adds repeatable `composition` lines. It is written only when there is at least one composition.
  Version 1 files are always accepted.

```
NES-WORKBENCH-PROJECT 1
rom-name=game.nes
rom-sha256=<64 lowercase hex digits>
tile-count=512
mapping=0,1,2,...,511
```

| Line | Meaning |
| --- | --- |
| Header | Literal `NES-WORKBENCH-PROJECT`, one space, then the format version. |
| `rom-name` | File name of the ROM, for display. Not used to find or verify the ROM. May not contain line breaks. |
| `rom-sha256` | SHA-256 of the complete ROM file. The project is refused for any other ROM. |
| `tile-count` | Number of CHR tiles, `0` to `130560`. Must match the ROM's CHR size / 16. |
| `mapping` | Comma-separated, no spaces. Entry *p* is the original tile index shown at display position *p*. |
| `composition` | Version 2 only; zero or more, in display order. `name\|WxH\|cells`, described below. |

### Composition lines

```
composition=Hero|2x2|12,13,-1,15
```

| Field | Meaning |
| --- | --- |
| `name` | 1 to 64 characters; no leading or trailing whitespace, no control characters, no `\|`. Unique within the file. |
| `WxH` | Width and height in tiles, each 1 to 32, written without leading zeros or signs. |
| `cells` | Comma-separated, no spaces, exactly W x H entries in row-major order (left to right, top to bottom). Each entry is an **original CHR tile index** (not a display position) from `0` to `tile-count-1`, or `-1` for an empty cell. |

Because cells hold original tile indices, changing the `mapping` never changes what a composition contains.
At most 256 compositions are allowed. A composition that references a tile the ROM does not have is rejected when the
project is applied to that ROM.

## Rules

- The four keys `rom-name`, `rom-sha256`, `tile-count` and `mapping` appear once each and are all required; no other keys are allowed except `composition` in version 2. Order after the header is free; the writer always uses the order shown, with `composition` lines last.
- `mapping` must contain exactly `tile-count` entries and be a permutation of `0..tile-count-1`: every original tile appears exactly once.
- Files over 4 MiB, invalid UTF-8, unknown keys, duplicate keys, malformed numbers, and bad hashes are rejected, never guessed at.
- A version newer than this program understands is rejected with a message saying so. Future versions that change the layout will increment the version number.
- Saving replaces an existing file through a temporary file in the same folder, so a failed save does not leave a half-written project.
