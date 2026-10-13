package com.nesworkbench.graphics;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Decodes NES 2bpp pattern data into 8x8 pixels with palette indices 0..3. */
public final class ChrTileDecoder {
    public static final int TILE_WIDTH = 8, TILE_HEIGHT = 8, BYTES_PER_TILE = 16;
    private ChrTileDecoder() {}

    /** Each tile contains eight low-plane bytes followed by eight high-plane bytes. */
    public static List<int[][]> decode(byte[] chrData) {
        Objects.requireNonNull(chrData, "chrData must not be null");
        if (chrData.length % BYTES_PER_TILE != 0)
            throw new IllegalArgumentException("CHR data length must be a multiple of 16 bytes.");
        List<int[][]> tiles = new ArrayList<>(chrData.length / BYTES_PER_TILE);
        for (int offset = 0; offset < chrData.length; offset += BYTES_PER_TILE) {
            int[][] pixels = new int[TILE_HEIGHT][TILE_WIDTH];
            for (int y = 0; y < TILE_HEIGHT; y++) {
                int low = chrData[offset + y] & 0xFF, high = chrData[offset + y + 8] & 0xFF;
                for (int x = 0; x < TILE_WIDTH; x++) {
                    int bit = 7 - x;
                    pixels[y][x] = ((low >>> bit) & 1) | (((high >>> bit) & 1) << 1);
                }
            }
            tiles.add(pixels);
        }
        return List.copyOf(tiles);
    }
}
