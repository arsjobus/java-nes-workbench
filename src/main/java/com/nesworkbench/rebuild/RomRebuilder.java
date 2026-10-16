package com.nesworkbench.rebuild;

import com.nesworkbench.rom.RomFormatException;
import com.nesworkbench.rom.RomImage;
import com.nesworkbench.rom.RomLoader;
import com.nesworkbench.rom.RomSummary;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Rebuilds a ROM with CHR byte edits applied. Only the edited 16-byte tiles change; the header, trainer, PRG-ROM,
 * other CHR tiles and any trailing bytes are copied verbatim. Visual tile mappings are not an input: they never reach
 * the output. The source is never written, and output goes to a new file. No JavaFX dependency.
 */
public final class RomRebuilder {
    private RomRebuilder() {}

    /** What a successful file rebuild produced: the new file, the byte-level differences, and the reopened, validated output. */
    public record Result(Path output, ByteDiff diff, RomImage reopened) {}

    /** Returns the rebuilt file contents. The source image is not modified. */
    public static byte[] rebuild(RomImage source, ChrEdits edits) throws RebuildException {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(edits, "edits must not be null");
        if (!edits.isEmpty()) {
            if (source.usesChrRam()) throw new RebuildException("This ROM uses CHR-RAM and has no CHR-ROM tiles to edit.");
            int tiles = source.chrRomSize() / ChrEdits.BYTES_PER_TILE;
            if (edits.maxTileIndex() >= tiles)
                throw new RebuildException("Edit targets tile " + edits.maxTileIndex() + ", but this ROM has " + tiles + " tiles.");
        }
        byte[] out = source.originalBytes(); // a private copy; the original stays untouched
        int chrOffset = RomSummary.of(source).chrFileOffset();
        for (int tile : edits.tileIndices())
            System.arraycopy(edits.tile(tile), 0, out, chrOffset + tile * ChrEdits.BYTES_PER_TILE, ChrEdits.BYTES_PER_TILE);
        return out;
    }

    /**
     * Rebuilds, writes to a brand-new file (never replacing anything, and never {@code sourcePath}), then reopens the
     * written file from disk and validates it. If validation fails, the file just written is removed and the problem reported.
     */
    public static Result rebuildToFile(RomImage source, Path sourcePath, ChrEdits edits, Path output)
            throws IOException, RebuildException {
        byte[] rebuilt = rebuild(source, edits);
        RomWriter.writeNew(sourcePath, output, rebuilt);
        RomImage reopened = reopenAndVerify(output, source, edits);
        return new Result(output, ByteDiff.compare(source.originalBytes(), reopened.originalBytes()), reopened);
    }

    /** Suggested name for a rebuilt copy: {@code game.nes} becomes {@code game-rebuilt.nes}. */
    public static String defaultOutputName(String sourceName) {
        Objects.requireNonNull(sourceName, "sourceName must not be null");
        int dot = sourceName.lastIndexOf('.');
        return dot > 0 ? sourceName.substring(0, dot) + "-rebuilt" + sourceName.substring(dot) : sourceName + "-rebuilt.nes";
    }

    /** Reopens a file this program just created and validates it; deletes that file when it is not valid. Only call on our own new output. */
    static RomImage reopenAndVerify(Path written, RomImage source, ChrEdits edits) throws IOException, RebuildException {
        try {
            RomImage reopened = RomLoader.load(written);
            RebuildVerifier.verify(source, edits, reopened.originalBytes());
            return reopened;
        } catch (RomFormatException e) {
            discard(written);
            throw new RebuildException("Rebuilt file is not a valid ROM: " + e.getMessage(), e);
        } catch (RebuildException | IOException | RuntimeException e) {
            discard(written);
            throw e;
        }
    }

    private static void discard(Path file) {
        try { Files.deleteIfExists(file); } catch (IOException ignored) { /* best effort; the original failure is what matters */ }
    }

}
