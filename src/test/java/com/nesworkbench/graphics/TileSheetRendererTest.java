package com.nesworkbench.graphics;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static com.nesworkbench.graphics.TileSheetRenderer.*;
import static org.junit.jupiter.api.Assertions.*;

class TileSheetRendererTest {
    private static final int[] P = {0xFF000001, 0xFF000002, 0xFF000003, 0xFF000004};

    /** Tile whose pixel (x, y) has value (x + y) % 4, so every value appears. */
    private static int[][] diagonalTile() {
        int[][] t = new int[8][8];
        for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++) t[y][x] = (x + y) % 4;
        return t;
    }
    private static List<int[][]> tiles(int n) { List<int[][]> l = new ArrayList<>(); for (int i = 0; i < n; i++) l.add(diagonalTile()); return l; }
    private static RenderedSheet render(List<int[][]> t, int cols, int zoom, int sel, boolean grid) {
        return TileSheetRenderer.render(t, new TileGridLayout(t.size(), cols, zoom), P, sel, grid);
    }

    @Test void mapsEveryPixelValueToItsPaletteColorAtZoomOne() {
        RenderedSheet s = render(tiles(1), 16, 1, -1, false);
        assertEquals(8, s.width()); assertEquals(8, s.height());
        for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++) assertEquals(P[(x + y) % 4], s.argb()[y * 8 + x]);
    }
    @Test void replicatesEachPixelIntoZoomSquare() {
        RenderedSheet s = render(tiles(1), 16, 3, -1, false);
        assertEquals(24, s.width());
        for (int y = 0; y < 24; y++) for (int x = 0; x < 24; x++) assertEquals(P[((x / 3) + (y / 3)) % 4], s.argb()[y * 24 + x]);
    }
    @Test void placesTilesRowMajorInColumns() {
        List<int[][]> t = tiles(3); t.get(1)[0][0] = 3; t.get(2)[0][0] = 2; // tile 0 pixel (0,0) is value 0
        RenderedSheet s = render(t, 2, 1, -1, false);
        assertEquals(16, s.width()); assertEquals(16, s.height());
        assertEquals(P[0], s.argb()[0]); assertEquals(P[3], s.argb()[8]); assertEquals(P[2], s.argb()[8 * 16]);
    }
    @Test void leavesUnusedTailOfLastRowUntouched() {
        RenderedSheet s = render(tiles(3), 2, 1, -1, false);
        for (int y = 8; y < 16; y++) for (int x = 8; x < 16; x++) assertEquals(0, s.argb()[y * 16 + x]);
    }
    @Test void gridOverlayColorsOnlyTopAndLeftEdgeOfEachCell() {
        RenderedSheet plain = render(tiles(2), 2, 2, -1, false), grid = render(tiles(2), 2, 2, -1, true);
        for (int y = 0; y < 16; y++) for (int x = 0; x < 32; x++) {
            boolean edge = (x % 16 == 0) || (y % 16 == 0);
            assertEquals(edge ? GRID_COLOR : plain.argb()[y * 32 + x], grid.argb()[y * 32 + x], x + "," + y);
        }
    }
    @Test void gridOverlayIsIgnoredAtZoomOne() {
        assertArrayEquals(render(tiles(2), 2, 1, -1, false).argb(), render(tiles(2), 2, 1, -1, true).argb());
    }
    @Test void selectionDrawsOneBorderPixelAtLowZoom() {
        RenderedSheet plain = render(tiles(2), 2, 1, -1, false), s = render(tiles(2), 2, 1, 1, false);
        assertEquals(SELECTION_COLOR, s.argb()[8]); assertEquals(SELECTION_COLOR, s.argb()[15]);
        assertEquals(SELECTION_COLOR, s.argb()[7 * 16 + 8]); assertEquals(SELECTION_COLOR, s.argb()[7 * 16 + 15]);
        assertEquals(plain.argb()[3 * 16 + 11], s.argb()[3 * 16 + 11]); // interior unchanged
        for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++) assertEquals(plain.argb()[y * 16 + x], s.argb()[y * 16 + x]); // other tile unchanged
    }
    @Test void selectionBorderIsTwoPixelsThickAtHighZoom() {
        RenderedSheet plain = render(tiles(1), 16, 4, -1, false), s = render(tiles(1), 16, 4, 0, false);
        int w = 32;
        assertEquals(SELECTION_COLOR, s.argb()[1 * w + 1]); assertEquals(SELECTION_COLOR, s.argb()[30 * w + 30]);
        assertEquals(plain.argb()[2 * w + 2], s.argb()[2 * w + 2]); assertEquals(plain.argb()[29 * w + 29], s.argb()[29 * w + 29]);
    }
    @Test void noSelectionColorWhenNothingSelected() {
        for (int c : render(tiles(4), 16, 2, -1, true).argb()) assertNotEquals(SELECTION_COLOR, c);
    }
    @Test void outputIsDeterministic() {
        assertArrayEquals(render(tiles(5), 4, 3, 2, true).argb(), render(tiles(5), 4, 3, 2, true).argb());
    }
    @Test void rendersEmptyInputAsEmptyImage() {
        RenderedSheet s = TileSheetRenderer.render(List.of(), new TileGridLayout(0, 16, 1), P, -1, false);
        assertEquals(0, s.width()); assertEquals(0, s.height()); assertEquals(0, s.argb().length);
    }
    @Test void rejectsTileCountThatDisagreesWithLayout() {
        assertThrows(IllegalArgumentException.class, () -> TileSheetRenderer.render(tiles(2), new TileGridLayout(3, 16, 1), P, -1, false));
    }
    @Test void rejectsInvalidPalettes() {
        TileGridLayout l = new TileGridLayout(1, 16, 1);
        assertThrows(IllegalArgumentException.class, () -> TileSheetRenderer.render(tiles(1), l, null, -1, false));
        assertThrows(IllegalArgumentException.class, () -> TileSheetRenderer.render(tiles(1), l, new int[3], -1, false));
        assertThrows(IllegalArgumentException.class, () -> TileSheetRenderer.render(tiles(1), l, new int[5], -1, false));
    }
    @Test void rejectsOutOfRangeSelection() {
        TileGridLayout l = new TileGridLayout(2, 16, 1);
        assertThrows(IllegalArgumentException.class, () -> TileSheetRenderer.render(tiles(2), l, P, -2, false));
        assertThrows(IllegalArgumentException.class, () -> TileSheetRenderer.render(tiles(2), l, P, 2, false));
    }
    @Test void rejectsNulls() {
        TileGridLayout l = new TileGridLayout(1, 16, 1);
        assertThrows(NullPointerException.class, () -> TileSheetRenderer.render(null, l, P, -1, false));
        assertThrows(NullPointerException.class, () -> TileSheetRenderer.render(tiles(1), null, P, -1, false));
    }
    @Test void rejectsMalformedTiles() {
        List<int[][]> badValue = tiles(1); badValue.get(0)[2][2] = 4;
        List<int[][]> negative = tiles(1); negative.get(0)[0][0] = -1;
        List<int[][]> wrongSize = new ArrayList<>(); wrongSize.add(new int[7][8]);
        List<int[][]> wrongRow = new ArrayList<>(); wrongRow.add(new int[8][7]);
        TileGridLayout l = new TileGridLayout(1, 16, 1);
        for (List<int[][]> bad : List.of(badValue, negative, wrongSize, wrongRow))
            assertThrows(IllegalArgumentException.class, () -> TileSheetRenderer.render(bad, l, P, -1, false));
    }
    @Test void paletteIsNotMutated() {
        int[] copy = Arrays.copyOf(P, 4); render(tiles(1), 16, 2, 0, true); assertArrayEquals(copy, P);
    }
}
