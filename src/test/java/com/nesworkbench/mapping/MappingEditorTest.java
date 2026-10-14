package com.nesworkbench.mapping;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MappingEditorTest {
    @Test void startsAsIdentityWithNoHistory() {
        MappingEditor e = new MappingEditor(4);
        assertTrue(e.mapping().isIdentity()); assertFalse(e.canUndo()); assertFalse(e.canRedo());
    }
    @Test void swapAndMoveChangeMappingAndRecordHistory() {
        MappingEditor e = new MappingEditor(5);
        assertTrue(e.swap(0, 1)); assertTrue(e.move(4, 0));
        assertArrayEquals(new int[]{4, 1, 0, 2, 3}, e.mapping().toArray()); assertEquals(2, e.undoDepth());
    }
    @Test void noOpEditsAreNotRecorded() {
        MappingEditor e = new MappingEditor(3);
        assertFalse(e.swap(1, 1)); assertFalse(e.move(2, 2)); assertFalse(e.canUndo());
    }
    @Test void invalidEditsThrowAndChangeNothing() {
        MappingEditor e = new MappingEditor(3); e.swap(0, 1);
        assertThrows(IndexOutOfBoundsException.class, () -> e.swap(0, 3)); assertThrows(IndexOutOfBoundsException.class, () -> e.move(-1, 0));
        assertArrayEquals(new int[]{1, 0, 2}, e.mapping().toArray()); assertEquals(1, e.undoDepth()); assertFalse(e.canRedo());
    }
    @Test void undoReversesLatestEditAndRedoReappliesIt() {
        MappingEditor e = new MappingEditor(4); e.swap(0, 3); e.move(1, 2);
        TileMapping afterBoth = e.mapping();
        e.undo(); assertArrayEquals(new int[]{3, 1, 2, 0}, e.mapping().toArray());
        e.undo(); assertTrue(e.mapping().isIdentity()); assertFalse(e.canUndo()); assertTrue(e.canRedo());
        e.redo(); assertArrayEquals(new int[]{3, 1, 2, 0}, e.mapping().toArray());
        e.redo(); assertEquals(afterBoth, e.mapping()); assertFalse(e.canRedo());
    }
    @Test void undoAndRedoReportTheEditForSelectionRestoring() {
        MappingEditor e = new MappingEditor(5); e.move(0, 3);
        assertEquals(0, e.undo().focus()); // the tile is back where it started
        assertEquals(3, e.redo().focus());
    }
    @Test void swapUndoSelectsWhereTheFirstTileReturnsAndRedoSelectsTheOtherPosition() {
        MappingEditor e = new MappingEditor(5); e.swap(4, 0);
        assertEquals(4, e.undo().focus()); assertEquals(0, e.redo().focus());
    }
    @Test void newEditClearsRedoHistory() {
        MappingEditor e = new MappingEditor(4); e.swap(0, 1); e.undo(); assertTrue(e.canRedo());
        e.swap(2, 3); assertFalse(e.canRedo());
    }
    @Test void undoAndRedoOnEmptyHistoryThrow() {
        MappingEditor e = new MappingEditor(2);
        assertThrows(IllegalStateException.class, e::undo); assertThrows(IllegalStateException.class, e::redo);
    }
    @Test void undoingEveryEditRestoresIdentityThenRedoingRestoresFinalState() {
        MappingEditor e = new MappingEditor(16); int[][] ops = {{0, 5}, {3, 9}, {15, 0}, {7, 8}, {2, 14}};
        for (int i = 0; i < ops.length; i++) { if (i % 2 == 0) e.swap(ops[i][0], ops[i][1]); else e.move(ops[i][0], ops[i][1]); }
        TileMapping finalState = e.mapping();
        while (e.canUndo()) e.undo(); assertTrue(e.mapping().isIdentity());
        while (e.canRedo()) e.redo(); assertEquals(finalState, e.mapping());
    }
    @Test void historyIsCappedAtMaximumAndDropsOldestEdits() {
        MappingEditor e = new MappingEditor(3);
        for (int i = 0; i < MappingEditor.MAX_HISTORY + 10; i++) e.swap(0, 1);
        assertEquals(MappingEditor.MAX_HISTORY, e.undoDepth());
        while (e.canUndo()) e.undo(); // 1010 swaps in total, 1000 undone: an even number remain, so identity again
        assertTrue(e.mapping().isIdentity());
    }
    @Test void resetReplacesMappingAndClearsHistory() {
        MappingEditor e = new MappingEditor(3); e.swap(0, 1); e.undo();
        TileMapping loaded = TileMapping.of(new int[]{2, 1, 0}); e.reset(loaded);
        assertEquals(loaded, e.mapping()); assertFalse(e.canUndo()); assertFalse(e.canRedo());
    }
    @Test void rejectsNullArguments() {
        assertThrows(NullPointerException.class, () -> new MappingEditor((TileMapping) null));
        assertThrows(NullPointerException.class, () -> new MappingEditor(1).reset(null));
    }
}
