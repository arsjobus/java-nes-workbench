# Project file format (version 1)

Project files (`.nesproj`) store a visual tile mapping for one ROM. They never contain ROM or CHR data, and loading
one never modifies the ROM. The format is UTF-8 text with `\n` or `\r\n` line endings.

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

## Rules

- Keys appear once each; the four keys above are all required and no others are allowed. Order after the header is free; the writer always uses the order shown.
- `mapping` must contain exactly `tile-count` entries and be a permutation of `0..tile-count-1`: every original tile appears exactly once.
- Files over 4 MiB, invalid UTF-8, unknown keys, duplicate keys, malformed numbers, and bad hashes are rejected, never guessed at.
- A version newer than this program understands is rejected with a message saying so. Future versions that change the layout will increment the version number.
- Saving replaces an existing file through a temporary file in the same folder, so a failed save does not leave a half-written project.
