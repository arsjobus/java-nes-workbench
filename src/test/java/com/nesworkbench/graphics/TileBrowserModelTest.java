package com.nesworkbench.graphics;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class TileBrowserModelTest {
    private static List<int[][]> tiles(int n) { List<int[][]> l = new ArrayList<>(); for (int i = 0; i < n; i++) l.add(new int[8][8]); return l; }
    private static boolean hasSelectionColor(TileSheetRenderer.RenderedSheet s) {
        for (int c : s.argb()) if (c == TileSheetRenderer.SELECTION_COLOR) return true; return false;
    }

    @Test void splitsTilesIntoBankSizedPages() {
        TileBrowserModel m = new TileBrowserModel(tiles(1100));
        assertEquals(3, m.pageCount()); assertEquals(512, m.tilesOnPage());
        m.setPage(2); assertEquals(1024, m.firstTileOfPage()); assertEquals(76, m.tilesOnPage());
    }
    @Test void emptyModelHasNoPagesAndRendersNothing() {
        TileBrowserModel m = new TileBrowserModel(List.of());
        assertEquals(0, m.pageCount()); assertEquals(0, m.tilesOnPage());
        assertEquals(0, m.render().width()); assertEquals(-1, m.selectAt(0, 0));
        assertDoesNotThrow(() -> m.setPage(0)); assertThrows(IllegalArgumentException.class, () -> m.setPage(1));
    }
    @Test void selectAtReturnsAbsoluteIndexOnLaterPages() {
        TileBrowserModel m = new TileBrowserModel(tiles(1024)); m.setZoom(2); m.setPage(1);
        int cell = m.layout().cellSize();
        assertEquals(512 + 17, m.selectAt(cell, cell)); assertEquals(512 + 17, m.selectedTile());
    }
    @Test void clickOutsideKeepsExistingSelection() {
        TileBrowserModel m = new TileBrowserModel(tiles(20)); m.setZoom(1);
        assertEquals(1, m.selectAt(8, 0));
        assertEquals(-1, m.selectAt(500, 500)); assertEquals(-1, m.selectAt(-1, 0)); assertEquals(1, m.selectedTile());
    }
    @Test void changingPageClearsSelectionButSamePageDoesNot() {
        TileBrowserModel m = new TileBrowserModel(tiles(1024)); m.select(5);
        m.setPage(0); assertEquals(5, m.selectedTile());
        m.setPage(1); assertEquals(-1, m.selectedTile());
    }
    @Test void selectRejectsTilesOffThePage() {
        TileBrowserModel m = new TileBrowserModel(tiles(1024));
        assertThrows(IllegalArgumentException.class, () -> m.select(512)); assertThrows(IllegalArgumentException.class, () -> m.select(-1));
        m.setPage(1); assertDoesNotThrow(() -> m.select(1023)); assertThrows(IllegalArgumentException.class, () -> m.select(0));
    }
    @Test void zoomChangesLayoutAndKeepsSelection() {
        TileBrowserModel m = new TileBrowserModel(tiles(512)); m.select(3); m.setZoom(8);
        assertEquals(8, m.zoom()); assertEquals(1024, m.layout().widthPx()); assertEquals(3, m.selectedTile());
    }
    @Test void invalidZoomIsRejectedAndKeepsPreviousValue() {
        TileBrowserModel m = new TileBrowserModel(tiles(10)); m.setZoom(3);
        assertThrows(IllegalArgumentException.class, () -> m.setZoom(0)); assertThrows(IllegalArgumentException.class, () -> m.setZoom(9));
        assertEquals(3, m.zoom());
    }
    @Test void renderSizeMatchesLayoutAndHighlightsSelection() {
        TileBrowserModel m = new TileBrowserModel(tiles(512)); m.setZoom(2);
        TileSheetRenderer.RenderedSheet none = m.render();
        assertEquals(m.layout().widthPx(), none.width()); assertEquals(m.layout().heightPx(), none.height()); assertFalse(hasSelectionColor(none));
        m.select(40); assertTrue(hasSelectionColor(m.render()));
        m.clearSelection(); assertFalse(hasSelectionColor(m.render()));
    }
    @Test void gridOverlayToggleChangesRenderedOutput() {
        TileBrowserModel m = new TileBrowserModel(tiles(4)); m.setZoom(2);
        int[] off = m.render().argb(); m.setGridOverlay(true); assertTrue(m.gridOverlay());
        assertFalse(java.util.Arrays.equals(off, m.render().argb()));
    }
    @Test void tileListIsReadOnly() { assertThrows(UnsupportedOperationException.class, () -> new TileBrowserModel(tiles(1)).tiles().add(new int[8][8])); }
    @Test void rejectsNullTiles() { assertThrows(NullPointerException.class, () -> new TileBrowserModel(null)); }
}
