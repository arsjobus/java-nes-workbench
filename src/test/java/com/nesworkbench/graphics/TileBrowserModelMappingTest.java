package com.nesworkbench.graphics;

import com.nesworkbench.mapping.TileMapping;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TileBrowserModelMappingTest {
    /** Tile i has a distinct, recognizable top-left pixel pattern: its index in base 4 across three pixels. */
    private static List<int[][]> tiles(int n) {
        List<int[][]> l = new ArrayList<>();
        for (int i = 0; i < n; i++) { int[][] t = new int[8][8]; t[0][0] = i % 4; t[0][1] = (i / 4) % 4; t[0][2] = (i / 16) % 4; l.add(t); }
        return l;
    }
    private static TileBrowserModel model(int n) { TileBrowserModel m = new TileBrowserModel(tiles(n)); m.setZoom(1); return m; }
    private static List<int[][]> reordered(int n, int... order) { List<int[][]> base = tiles(n), out = new ArrayList<>(); for (int i : order) out.add(base.get(i)); return out; }
    private static int[] identityOrder(int n) { int[] o = new int[n]; Arrays.setAll(o, i -> i); return o; }

    @Test void startsWithIdentityMappingAndNoHistory() {
        TileBrowserModel m = model(10);
        assertTrue(m.mapping().isIdentity()); assertFalse(m.canUndo()); assertFalse(m.canRedo());
        assertEquals(7, m.originalTileAt(7)); assertSame(m.tiles().get(7), m.displayedTile(7));
    }
    @Test void swapExchangesDisplayedTilesAndSelectionFollowsTheTile() {
        TileBrowserModel m = model(10); m.select(2);
        assertTrue(m.swapSelectedWith(7));
        assertEquals(7, m.originalTileAt(2)); assertEquals(2, m.originalTileAt(7)); assertEquals(7, m.selectedTile()); assertTrue(m.canUndo());
    }
    @Test void swapWithItselfChangesNothingAndRecordsNothing() { TileBrowserModel m = model(5); m.select(3); assertFalse(m.swapSelectedWith(3)); assertFalse(m.canUndo()); assertEquals(3, m.selectedTile()); }
    @Test void renderedSheetReflectsMappingAndMatchesAModelBuiltInThatOrder() {
        TileBrowserModel m = model(6); m.select(1); m.swapSelectedWith(4); m.clearSelection();
        TileBrowserModel expected = new TileBrowserModel(reordered(6, 0, 4, 2, 3, 1, 5)); expected.setZoom(1);
        assertArrayEquals(expected.render().argb(), m.render().argb());
        assertFalse(Arrays.equals(model(6).render().argb(), m.render().argb()));
    }
    @Test void moveSelectedByShiftsNeighboursAndFollowsTheTile() {
        TileBrowserModel m = model(5); m.select(0);
        assertTrue(m.moveSelectedBy(3)); assertArrayEquals(new int[]{1, 2, 3, 0, 4}, m.mapping().toArray()); assertEquals(3, m.selectedTile());
        assertTrue(m.moveSelectedBy(-2)); assertArrayEquals(new int[]{1, 0, 2, 3, 4}, m.mapping().toArray()); assertEquals(1, m.selectedTile());
    }
    @Test void moveBeyondEitherEndReturnsFalseAndChangesNothing() {
        TileBrowserModel m = model(5); m.select(0);
        assertFalse(m.moveSelectedBy(-1)); assertFalse(m.moveSelectedBy(Integer.MIN_VALUE)); assertFalse(m.canUndo());
        m.select(4); assertFalse(m.moveSelectedBy(1)); assertFalse(m.moveSelectedBy(Integer.MAX_VALUE)); assertTrue(m.mapping().isIdentity());
    }
    @Test void invalidTargetThrowsAndKeepsSelectionAndMapping() {
        TileBrowserModel m = model(5); m.select(2);
        assertThrows(IndexOutOfBoundsException.class, () -> m.swapSelectedWith(5)); assertThrows(IndexOutOfBoundsException.class, () -> m.moveSelectedTo(-1));
        assertEquals(2, m.selectedTile()); assertTrue(m.mapping().isIdentity()); assertFalse(m.canUndo());
    }
    @Test void editsRequireASelection() {
        TileBrowserModel m = model(5);
        assertThrows(IllegalStateException.class, () -> m.swapSelectedWith(1)); assertThrows(IllegalStateException.class, () -> m.moveSelectedTo(1)); assertThrows(IllegalStateException.class, () -> m.moveSelectedBy(1));
    }
    @Test void originalTileIdentitiesAndPixelsNeverChange() {
        TileBrowserModel m = model(8); List<int[][]> before = tiles(8);
        m.select(0); m.moveSelectedBy(5); m.swapSelectedWith(1); m.undo(); m.redo();
        for (int i = 0; i < 8; i++) assertArrayEquals(before.get(i), m.tiles().get(i)); // CHR-order list is untouched
        int[] sorted = m.mapping().toArray(); Arrays.sort(sorted); assertArrayEquals(identityOrder(8), sorted); // every tile still appears once
    }
    @Test void undoAndRedoRestoreMappingAndSelect() {
        TileBrowserModel m = model(6); m.select(1); m.moveSelectedTo(4); m.swapSelectedWith(0);
        m.undo(); assertArrayEquals(new int[]{0, 2, 3, 4, 1, 5}, m.mapping().toArray()); assertEquals(4, m.selectedTile());
        m.undo(); assertTrue(m.mapping().isIdentity()); assertEquals(1, m.selectedTile());
        m.redo(); assertArrayEquals(new int[]{0, 2, 3, 4, 1, 5}, m.mapping().toArray()); assertEquals(4, m.selectedTile());
        assertTrue(m.canRedo());
    }
    @Test void undoRedoOnEmptyHistoryThrow() { TileBrowserModel m = model(3); assertThrows(IllegalStateException.class, m::undo); assertThrows(IllegalStateException.class, m::redo); }
    @Test void movingAcrossAPageBoundaryShowsTheNewPageAndKeepsSelection() {
        TileBrowserModel m = model(1024); m.select(511);
        assertTrue(m.moveSelectedBy(1));
        assertEquals(512, m.selectedTile()); assertEquals(1, m.page());
        assertEquals(512, m.originalTileAt(511)); assertEquals(511, m.originalTileAt(512));
        m.undo(); assertEquals(511, m.selectedTile()); assertEquals(0, m.page());
    }
    @Test void swapAcrossPagesWorks() {
        TileBrowserModel m = model(1024); m.select(3); m.swapSelectedWith(900);
        assertEquals(900, m.originalTileAt(3)); assertEquals(3, m.originalTileAt(900)); assertEquals(900, m.selectedTile()); assertEquals(1, m.page());
    }
    @Test void replaceMappingAppliesItClearsHistoryAndSelection() {
        TileBrowserModel m = model(4); m.select(0); m.swapSelectedWith(1);
        TileMapping loaded = TileMapping.of(new int[]{3, 2, 1, 0}); m.replaceMapping(loaded);
        assertEquals(loaded, m.mapping()); assertFalse(m.canUndo()); assertEquals(-1, m.selectedTile());
        assertSame(m.tiles().get(3), m.displayedTile(0));
    }
    @Test void replaceMappingRejectsWrongSizeAndNullWithoutChangingState() {
        TileBrowserModel m = model(4); m.select(0); m.swapSelectedWith(1); TileMapping before = m.mapping();
        assertThrows(IllegalArgumentException.class, () -> m.replaceMapping(TileMapping.identity(3))); assertThrows(NullPointerException.class, () -> m.replaceMapping(null));
        assertEquals(before, m.mapping()); assertTrue(m.canUndo()); assertEquals(1, m.selectedTile());
    }
    @Test void emptyModelHasAnEmptyMappingAndCannotEdit() {
        TileBrowserModel m = new TileBrowserModel(List.of());
        assertEquals(0, m.mapping().size()); assertThrows(IllegalStateException.class, () -> m.swapSelectedWith(0)); assertEquals(0, m.render().width());
    }
    @Test void selectionStillWorksByDisplayPositionAfterMapping() {
        TileBrowserModel m = model(20); m.select(0); m.swapSelectedWith(1); m.clearSelection();
        assertEquals(1, m.selectAt(8, 0)); assertEquals(0, m.originalTileAt(1));
    }
}
