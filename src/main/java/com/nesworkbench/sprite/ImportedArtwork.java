package com.nesworkbench.sprite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Validated palette-index pixels of an imported PNG: a whole number of 8x8 tiles, every value in 0..3.
 * Immutable; arrays are copied on the way in and out. No JavaFX dependency.
 */
public final class ImportedArtwork {
    private static final int T = SpriteComposition.TILE_SIZE;
    private final int[][] pixels; // [y][x]

    public ImportedArtwork(int[][] pixels) {
        Objects.requireNonNull(pixels, "pixels must not be null");
        int h = pixels.length, w = h == 0 ? 0 : pixels[0] == null ? -1 : pixels[0].length;
        int max = SpriteComposition.MAX_DIMENSION * T;
        if (w <= 0 || h == 0 || w % T != 0 || h % T != 0 || w > max || h > max)
            throw new IllegalArgumentException("Artwork must be a multiple of " + T + " pixels in each direction, between " + T + " and " + max + ".");
        int[][] copy = new int[h][];
        for (int y = 0; y < h; y++) {
            if (pixels[y] == null || pixels[y].length != w) throw new IllegalArgumentException("Artwork rows must all have the same length.");
            for (int v : pixels[y]) if (v < 0 || v > 3) throw new IllegalArgumentException("Pixel value " + v + " is outside 0..3.");
            copy[y] = pixels[y].clone();
        }
        this.pixels = copy;
    }

    public int widthPixels() { return pixels[0].length; }
    public int heightPixels() { return pixels.length; }
    public int widthTiles() { return widthPixels() / T; }
    public int heightTiles() { return heightPixels() / T; }

    public int pixel(int x, int y) {
        if (x < 0 || x >= widthPixels() || y < 0 || y >= heightPixels())
            throw new IndexOutOfBoundsException("Pixel (" + x + ", " + y + ") is outside the " + widthPixels() + "x" + heightPixels() + " artwork.");
        return pixels[y][x];
    }

    public int[][] toPixels() {
        int[][] copy = new int[pixels.length][];
        for (int y = 0; y < pixels.length; y++) copy[y] = pixels[y].clone();
        return copy;
    }

    /** The 8x8 block at a tile cell. */
    public int[][] tile(int tileX, int tileY) {
        if (tileX < 0 || tileX >= widthTiles() || tileY < 0 || tileY >= heightTiles())
            throw new IndexOutOfBoundsException("Tile cell (" + tileX + ", " + tileY + ") is outside the " + widthTiles() + "x" + heightTiles() + " artwork.");
        int[][] tile = new int[T][];
        for (int y = 0; y < T; y++) tile[y] = Arrays.copyOfRange(pixels[tileY * T + y], tileX * T, tileX * T + T);
        return tile;
    }

    /** All 8x8 blocks in row-major order, e.g. for rendering with a tile grid. */
    public List<int[][]> toTiles() {
        List<int[][]> tiles = new ArrayList<>(widthTiles() * heightTiles());
        for (int ty = 0; ty < heightTiles(); ty++) for (int tx = 0; tx < widthTiles(); tx++) tiles.add(tile(tx, ty));
        return tiles;
    }

    @Override public boolean equals(Object o) { return o instanceof ImportedArtwork a && Arrays.deepEquals(pixels, a.pixels); }
    @Override public int hashCode() { return Arrays.deepHashCode(pixels); }
    @Override public String toString() { return "ImportedArtwork[" + widthPixels() + "x" + heightPixels() + " pixels]"; }
}
