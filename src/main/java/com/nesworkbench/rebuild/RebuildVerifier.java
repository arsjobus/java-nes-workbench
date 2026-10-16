package com.nesworkbench.rebuild;

import com.nesworkbench.rom.RomFormatException;
import com.nesworkbench.rom.RomImage;
import com.nesworkbench.rom.RomSummary;

import java.util.Arrays;
import java.util.Objects;

/**
 * Validates rebuilt output against its source and the intended edits, independently of how it was produced: it must
 * parse as a ROM with the same layout, every byte outside CHR must match the source, and every CHR tile must be either
 * the source tile or the edit for it. No JavaFX dependency.
 */
public final class RebuildVerifier {
    private RebuildVerifier() {}

    /** Throws {@link RebuildException} describing the first problem found. */
    public static void verify(RomImage source, ChrEdits edits, byte[] rebuilt) throws RebuildException {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(edits, "edits must not be null");
        Objects.requireNonNull(rebuilt, "rebuilt must not be null");
        RomImage parsed;
        try { parsed = RomImage.parse(rebuilt); }
        catch (RomFormatException e) { throw new RebuildException("Rebuilt output is not a valid ROM: " + e.getMessage(), e); }

        byte[] original = source.originalBytes();
        if (rebuilt.length != original.length)
            throw new RebuildException("Rebuilt output is " + rebuilt.length + " bytes, expected " + original.length + ".");
        if (parsed.mapper() != source.mapper() || parsed.usesChrRam() != source.usesChrRam() || parsed.hasTrainer() != source.hasTrainer()
                || parsed.prgRomSize() != source.prgRomSize() || parsed.chrRomSize() != source.chrRomSize())
            throw new RebuildException("Rebuilt output has a different ROM layout than the source.");

        RomSummary layout = RomSummary.of(source);
        int chrStart = layout.chrFileOffset(), chrEnd = chrStart + layout.chrRomBytes();
        for (int i = 0; i < original.length; i++) {
            if (i >= chrStart && i < chrEnd) { i = chrEnd - 1; continue; }
            if (original[i] != rebuilt[i])
                throw new RebuildException(String.format("Byte at file offset $%05X changed outside CHR data (expected $%02X, found $%02X).", i, original[i] & 0xFF, rebuilt[i] & 0xFF));
        }
        int tiles = layout.chrRomBytes() / ChrEdits.BYTES_PER_TILE;
        if (!edits.isEmpty() && edits.maxTileIndex() >= tiles)
            throw new RebuildException("Edit targets tile " + edits.maxTileIndex() + ", but the ROM has " + tiles + " tiles.");
        for (int t = 0; t < tiles; t++) {
            int at = chrStart + t * ChrEdits.BYTES_PER_TILE;
            byte[] expected = edits.has(t) ? edits.tile(t) : Arrays.copyOfRange(original, at, at + ChrEdits.BYTES_PER_TILE);
            byte[] actual = Arrays.copyOfRange(rebuilt, at, at + ChrEdits.BYTES_PER_TILE);
            if (!Arrays.equals(expected, actual))
                throw new RebuildException(String.format("Tile %d (file offset $%05X) %s.", t, at, edits.has(t) ? "does not contain its edit" : "changed but was not edited"));
        }
    }
}
