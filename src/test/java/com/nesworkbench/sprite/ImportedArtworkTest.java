package com.nesworkbench.sprite;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ImportedArtworkTest {
    private static int[][] grid(int w, int h) { int[][] p = new int[h][w]; for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) p[y][x] = (x + 2 * y) % 4; return p; }

    @Test void exposesSizesPixelsAndTiles() {
        ImportedArtwork a = new ImportedArtwork(grid(16, 8));
        assertEquals(16, a.widthPixels()); assertEquals(8, a.heightPixels()); assertEquals(2, a.widthTiles()); assertEquals(1, a.heightTiles());
        assertEquals(grid(16, 8)[3][9], a.pixel(9, 3));
        assertEquals(2, a.toTiles().size());
        assertEquals(a.pixel(8 + 2, 5), a.tile(1, 0)[5][2]);
    }
    @Test void validatesShapeAndValues() {
        for (int[][] bad : new int[][][]{new int[0][], new int[8][0], grid(9, 8), grid(8, 9), grid(264, 8), grid(8, 264), {new int[8], null, new int[8], new int[8], new int[8], new int[8], new int[8], new int[8]}})
            assertThrows(IllegalArgumentException.class, () -> new ImportedArtwork(bad));
        int[][] ragged = grid(8, 8); ragged[4] = new int[7];
        assertThrows(IllegalArgumentException.class, () -> new ImportedArtwork(ragged));
        int[][] high = grid(8, 8); high[0][0] = 4;
        assertThrows(IllegalArgumentException.class, () -> new ImportedArtwork(high));
        int[][] neg = grid(8, 8); neg[0][0] = -1;
        assertThrows(IllegalArgumentException.class, () -> new ImportedArtwork(neg));
        assertThrows(NullPointerException.class, () -> new ImportedArtwork(null));
    }
    @Test void isImmutableThroughInputAndOutputArrays() {
        int[][] in = grid(8, 8); ImportedArtwork a = new ImportedArtwork(in);
        int before = a.pixel(1, 1);
        in[1][1] = (before + 1) % 4; a.toPixels()[1][1] = (before + 2) % 4; a.tile(0, 0)[1][1] = (before + 3) % 4; a.toTiles().get(0)[1][1] = (before + 1) % 4;
        assertEquals(before, a.pixel(1, 1));
    }
    @Test void boundsAreChecked() {
        ImportedArtwork a = new ImportedArtwork(grid(8, 8));
        assertThrows(IndexOutOfBoundsException.class, () -> a.pixel(8, 0)); assertThrows(IndexOutOfBoundsException.class, () -> a.pixel(0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> a.tile(1, 0)); assertThrows(IndexOutOfBoundsException.class, () -> a.tile(0, -1));
    }
    @Test void equalityIsByPixels() {
        assertEquals(new ImportedArtwork(grid(8, 8)), new ImportedArtwork(grid(8, 8)));
        int[][] other = grid(8, 8); other[7][7] = (other[7][7] + 1) % 4;
        assertNotEquals(new ImportedArtwork(grid(8, 8)), new ImportedArtwork(other));
    }
}
