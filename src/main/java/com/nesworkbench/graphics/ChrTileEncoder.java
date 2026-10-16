package com.nesworkbench.graphics;

import java.util.Objects;

/** Encodes 8x8 pixels with palette indices 0..3 into NES 2bpp pattern data. The exact inverse of {@link ChrTileDecoder}. */
public final class ChrTileEncoder {
    private ChrTileEncoder() {}

    /** Returns 16 bytes: eight low-plane rows followed by eight high-plane rows, leftmost pixel in the top bit. */
    public static byte[] encode(int[][] pixels) {
        Objects.requireNonNull(pixels, "pixels must not be null");
        int size = ChrTileDecoder.TILE_HEIGHT;
        if (pixels.length != size) throw new IllegalArgumentException("A tile must have " + size + " rows, got " + pixels.length + ".");
        byte[] out = new byte[ChrTileDecoder.BYTES_PER_TILE];
        for (int y = 0; y < size; y++) {
            if (pixels[y] == null || pixels[y].length != ChrTileDecoder.TILE_WIDTH)
                throw new IllegalArgumentException("Row " + y + " must have " + ChrTileDecoder.TILE_WIDTH + " pixels.");
            int low = 0, high = 0;
            for (int x = 0; x < ChrTileDecoder.TILE_WIDTH; x++) {
                int v = pixels[y][x];
                if (v < 0 || v > 3) throw new IllegalArgumentException("Pixel value " + v + " at (" + x + ", " + y + ") is outside 0..3.");
                low = (low << 1) | (v & 1);
                high = (high << 1) | (v >>> 1);
            }
            out[y] = (byte) low; out[y + 8] = (byte) high;
        }
        return out;
    }
}
