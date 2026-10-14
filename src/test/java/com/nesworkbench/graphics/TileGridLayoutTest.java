package com.nesworkbench.graphics;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TileGridLayoutTest {
    @Test void computesGeometryForFullBankAtSeveralZooms() {
        TileGridLayout l = new TileGridLayout(512, 16, 1);
        assertEquals(32, l.rows()); assertEquals(128, l.widthPx()); assertEquals(256, l.heightPx());
        TileGridLayout z4 = l.withZoom(4);
        assertEquals(512, z4.widthPx()); assertEquals(1024, z4.heightPx()); assertEquals(32, z4.cellSize());
        assertEquals(512, z4.tileCount()); assertEquals(16, z4.columns());
    }
    @Test void narrowsImageWhenFewerTilesThanColumns() {
        TileGridLayout l = new TileGridLayout(5, 16, 2);
        assertEquals(1, l.rows()); assertEquals(80, l.widthPx()); assertEquals(16, l.heightPx());
    }
    @Test void keepsFullWidthForPartialLastRow() {
        TileGridLayout l = new TileGridLayout(20, 16, 1);
        assertEquals(2, l.rows()); assertEquals(128, l.widthPx()); assertEquals(16, l.heightPx());
    }
    @Test void emptyLayoutHasNoArea() {
        TileGridLayout l = new TileGridLayout(0, 16, 4);
        assertEquals(0, l.widthPx()); assertEquals(0, l.heightPx()); assertEquals(0, l.rows()); assertEquals(-1, l.tileIndexAt(0, 0));
    }
    @Test void mapsPixelsToTilesAtCellBoundaries() {
        TileGridLayout l = new TileGridLayout(40, 16, 2); int cell = 16;
        assertEquals(0, l.tileIndexAt(0, 0)); assertEquals(0, l.tileIndexAt(cell - 1, cell - 1));
        assertEquals(1, l.tileIndexAt(cell, 0)); assertEquals(16, l.tileIndexAt(0, cell));
        assertEquals(39, l.tileIndexAt(7 * cell, 2 * cell + 5));
    }
    @Test void returnsMinusOneOutsideImageAndInEmptyTail() {
        TileGridLayout l = new TileGridLayout(20, 16, 1);
        assertEquals(-1, l.tileIndexAt(-1, 0)); assertEquals(-1, l.tileIndexAt(0, -1));
        assertEquals(-1, l.tileIndexAt(128, 0)); assertEquals(-1, l.tileIndexAt(0, 16));
        assertEquals(-1, l.tileIndexAt(4 * 8, 8)); // tile 20 does not exist
        assertEquals(19, l.tileIndexAt(3 * 8, 8));
    }
    @Test void cellOriginsRoundTripThroughTileIndexAt() {
        TileGridLayout l = new TileGridLayout(37, 16, 3);
        for (int i = 0; i < 37; i++) assertEquals(i, l.tileIndexAt(l.cellX(i), l.cellY(i)), "tile " + i);
    }
    @Test void cellOriginRejectsInvalidIndex() {
        TileGridLayout l = new TileGridLayout(4, 16, 1);
        assertThrows(IndexOutOfBoundsException.class, () -> l.cellX(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> l.cellY(4));
    }
    @Test void validatesConstructorArguments() {
        assertThrows(IllegalArgumentException.class, () -> new TileGridLayout(-1, 16, 1));
        assertThrows(IllegalArgumentException.class, () -> new TileGridLayout(1, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new TileGridLayout(1, 16, 0));
        assertThrows(IllegalArgumentException.class, () -> new TileGridLayout(1, 16, TileGridLayout.MAX_ZOOM + 1));
        assertDoesNotThrow(() -> new TileGridLayout(1, 16, TileGridLayout.MAX_ZOOM));
    }
    @Test void rejectsSheetsLargerThanPixelBudget() {
        assertThrows(IllegalArgumentException.class, () -> new TileGridLayout(1_000_000, 16, 8));
    }
    @Test void withZoomValidates() { assertThrows(IllegalArgumentException.class, () -> new TileGridLayout(1, 16, 1).withZoom(99)); }
}
