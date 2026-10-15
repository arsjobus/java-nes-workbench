package com.nesworkbench.sprite;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SpriteCompositionTest {
    /** Tile i has every pixel equal to i % 4, except pixel (0,0) which is (i / 4) % 4: distinguishes tiles deterministically. */
    static List<int[][]> sourceTiles(int count) {
        List<int[][]> tiles = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int[][] t = new int[8][8];
            for (int[] row : t) java.util.Arrays.fill(row, i % 4);
            t[0][0] = (i / 4) % 4;
            tiles.add(t);
        }
        return tiles;
    }

    // ---- construction and size boundaries ----
    @Test void emptyCompositionHasOnlyEmptyCells() {
        SpriteComposition c = SpriteComposition.empty("hero", 2, 3);
        assertEquals(6, c.cellCount()); assertEquals(0, c.placedTileCount()); assertEquals(SpriteComposition.EMPTY, c.maxSourceTile());
        assertEquals(16, c.widthPixels()); assertEquals(24, c.heightPixels());
        for (int y = 0; y < 3; y++) for (int x = 0; x < 2; x++) assertEquals(SpriteComposition.EMPTY, c.tileAt(x, y));
    }
    @Test void sizeBoundariesAreInclusive() {
        assertDoesNotThrow(() -> SpriteComposition.empty("min", 1, 1));
        assertDoesNotThrow(() -> SpriteComposition.empty("max", 32, 32));
        assertEquals(256, SpriteComposition.empty("max", 32, 32).widthPixels());
        for (int[] bad : new int[][]{{0, 1}, {1, 0}, {33, 1}, {1, 33}, {-1, 4}, {4, -1}})
            assertThrows(IllegalArgumentException.class, () -> SpriteComposition.empty("bad", bad[0], bad[1]), bad[0] + "x" + bad[1]);
    }
    @Test void ofValidatesCellCountAndReferences() {
        assertEquals(5, SpriteComposition.of("a", 2, 1, new int[]{5, -1}).tileAt(0, 0));
        assertThrows(IllegalArgumentException.class, () -> SpriteComposition.of("a", 2, 2, new int[]{1, 2, 3}));
        assertThrows(IllegalArgumentException.class, () -> SpriteComposition.of("a", 2, 1, new int[]{1, 2, 3}));
        assertThrows(IllegalArgumentException.class, () -> SpriteComposition.of("a", 2, 1, new int[]{1, -2}));
        assertThrows(NullPointerException.class, () -> SpriteComposition.of("a", 1, 1, null));
    }
    @Test void cellsAreDefensivelyCopied() {
        int[] input = {1, 2};
        SpriteComposition c = SpriteComposition.of("a", 2, 1, input);
        input[0] = 99; c.cells()[1] = 99;
        assertEquals(1, c.tileAt(0, 0)); assertEquals(2, c.tileAt(1, 0));
    }

    // ---- names ----
    @Test void nameRules() {
        assertDoesNotThrow(() -> SpriteComposition.checkName("Boss — phase 2 (ゲーム)"));
        assertDoesNotThrow(() -> SpriteComposition.checkName("x".repeat(SpriteComposition.MAX_NAME_LENGTH)));
        for (String bad : new String[]{"", " lead", "trail ", "x".repeat(SpriteComposition.MAX_NAME_LENGTH + 1), "a|b", "a\nb", "a\rb", "a\tb", "a\u0000b", "bad\uD800"})
            assertThrows(IllegalArgumentException.class, () -> SpriteComposition.checkName(bad), "name: " + bad.replace("\n", "\\n"));
        assertThrows(NullPointerException.class, () -> SpriteComposition.checkName(null));
        assertThrows(IllegalArgumentException.class, () -> SpriteComposition.empty("a|b", 1, 1));
    }
    @Test void renameKeepsCellsAndValidates() {
        SpriteComposition c = SpriteComposition.of("a", 2, 1, new int[]{3, 4}).renamed("b");
        assertEquals("b", c.name()); assertEquals(4, c.tileAt(1, 0));
        assertThrows(IllegalArgumentException.class, () -> c.renamed("x|y"));
    }

    // ---- editing is immutable and bounds-checked ----
    @Test void withTileAndClearedReturnNewCompositions() {
        SpriteComposition a = SpriteComposition.empty("a", 2, 2);
        SpriteComposition b = a.withTile(1, 0, 7);
        assertEquals(SpriteComposition.EMPTY, a.tileAt(1, 0)); assertEquals(7, b.tileAt(1, 0)); assertEquals(1, b.placedTileCount()); assertEquals(7, b.maxSourceTile());
        assertSame(b, b.withTile(1, 0, 7)); assertSame(a, a.cleared(0, 0));
        assertEquals(a, b.cleared(1, 0));
    }
    @Test void outOfBoundsCellsAndNegativeSourcesAreRejected() {
        SpriteComposition c = SpriteComposition.empty("a", 2, 2);
        for (int[] cell : new int[][]{{-1, 0}, {0, -1}, {2, 0}, {0, 2}}) {
            assertThrows(IndexOutOfBoundsException.class, () -> c.tileAt(cell[0], cell[1]));
            assertThrows(IndexOutOfBoundsException.class, () -> c.withTile(cell[0], cell[1], 1));
            assertThrows(IndexOutOfBoundsException.class, () -> c.cleared(cell[0], cell[1]));
        }
        assertThrows(IllegalArgumentException.class, () -> c.withTile(0, 0, -1));
    }

    // ---- layout: row-major placement of source tiles ----
    @Test void toTilesAndToPixelsPlaceTilesRowMajorAndBlankEmptyCells() {
        List<int[][]> src = sourceTiles(8);
        SpriteComposition c = SpriteComposition.of("a", 2, 2, new int[]{1, SpriteComposition.EMPTY, 2, 3}); // (0,0)=1 (1,0)=empty (0,1)=2 (1,1)=3
        List<int[][]> tiles = c.toTiles(src);
        assertEquals(4, tiles.size());
        assertArrayEquals(src.get(1), tiles.get(0)); assertArrayEquals(new int[8][8], tiles.get(1));
        assertArrayEquals(src.get(2), tiles.get(2)); assertArrayEquals(src.get(3), tiles.get(3));
        int[][] px = c.toPixels(src);
        assertEquals(16, px.length); assertEquals(16, px[0].length);
        assertEquals(1, px[1][1]);            // tile 1 interior
        assertEquals(0, px[1][9]);            // empty cell
        assertEquals(2, px[9][1]);            // tile 2, second row
        assertEquals(3, px[15][15]);          // tile 3 bottom-right corner
        assertEquals(src.get(3)[0][0], px[8][8]); // tile 3 top-left special pixel
    }
    @Test void emptyCellsAreBlankEvenWhenTileZeroIsNotBlank() {
        int[][] solid = new int[8][8]; for (int[] row : solid) java.util.Arrays.fill(row, 3);
        SpriteComposition c = SpriteComposition.of("a", 2, 1, new int[]{0, SpriteComposition.EMPTY});
        int[][] px = c.toPixels(List.<int[][]>of(solid));
        assertEquals(3, px[4][4]); assertEquals(0, px[4][12]);
        assertArrayEquals(new int[8][8], c.toTiles(List.<int[][]>of(solid)).get(1));
    }
    @Test void fullSizeCompositionHasExactPixelBounds() {
        SpriteComposition c = SpriteComposition.empty("big", 32, 32).withTile(31, 31, 2);
        int[][] px = c.toPixels(sourceTiles(4));
        assertEquals(256, px.length); assertEquals(256, px[255].length);
        assertEquals(2, px[255][255]); assertEquals(0, px[255][247]);
    }
    @Test void sourceReferencesAreOriginalIdentitiesAndSourceTilesAreNotModified() {
        List<int[][]> src = sourceTiles(4);
        int[][] before = src.get(2); int[][] snapshot = new int[8][]; for (int y = 0; y < 8; y++) snapshot[y] = before[y].clone();
        SpriteComposition c = SpriteComposition.empty("a", 1, 1).withTile(0, 0, 2);
        c.toTiles(src).get(0)[3][3] = 3;   // mutating output must not reach the source
        c.toPixels(src)[3][3] = 3;
        assertArrayEquals(snapshot, src.get(2));
        assertEquals(2, c.tileAt(0, 0));
    }

    // ---- invalid sources ----
    @Test void referencesBeyondTheAvailableTilesAreRejected() {
        SpriteComposition c = SpriteComposition.empty("a", 1, 1).withTile(0, 0, 4);
        assertTrue(assertThrows(IllegalArgumentException.class, () -> c.toTiles(sourceTiles(4))).getMessage().contains("references tile 4"));
        assertDoesNotThrow(() -> c.toTiles(sourceTiles(5)));
    }
    @Test void malformedSourceTilesAreRejected() {
        SpriteComposition c = SpriteComposition.empty("a", 1, 1).withTile(0, 0, 0);
        assertThrows(IllegalArgumentException.class, () -> c.toTiles(List.<int[][]>of(new int[7][8])));
        assertThrows(IllegalArgumentException.class, () -> c.toTiles(List.<int[][]>of(new int[8][7])));
        int[][] bad = new int[8][8]; bad[2][2] = 4;
        assertThrows(IllegalArgumentException.class, () -> c.toTiles(List.<int[][]>of(bad)));
        assertThrows(NullPointerException.class, () -> c.toTiles(null));
    }

    @Test void equalityCoversNameSizeAndCells() {
        SpriteComposition a = SpriteComposition.of("a", 2, 1, new int[]{1, 2});
        assertEquals(a, SpriteComposition.of("a", 2, 1, new int[]{1, 2})); assertEquals(a.hashCode(), SpriteComposition.of("a", 2, 1, new int[]{1, 2}).hashCode());
        assertNotEquals(a, SpriteComposition.of("b", 2, 1, new int[]{1, 2})); assertNotEquals(a, SpriteComposition.of("a", 1, 2, new int[]{1, 2}));
        assertNotEquals(a, SpriteComposition.of("a", 2, 1, new int[]{1, 3}));
    }
}
