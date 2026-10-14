package com.nesworkbench.mapping;

import org.junit.jupiter.api.Test;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class TileMappingTest {
    private static TileMapping m(int... e) { return TileMapping.of(e); }

    @Test void identityMapsEveryPositionToItself() {
        TileMapping id = TileMapping.identity(5);
        assertEquals(5, id.size()); assertTrue(id.isIdentity());
        for (int i = 0; i < 5; i++) assertEquals(i, id.tileAt(i));
    }
    @Test void emptyMappingIsValidAndIdentity() {
        TileMapping empty = TileMapping.identity(0);
        assertEquals(0, empty.size()); assertTrue(empty.isIdentity()); assertEquals(empty, TileMapping.of(new int[0]));
        assertThrows(IndexOutOfBoundsException.class, () -> empty.tileAt(0));
    }
    @Test void negativeSizeIsRejected() { assertThrows(IllegalArgumentException.class, () -> TileMapping.identity(-1)); }

    @Test void ofAcceptsAnyPermutation() { assertArrayEquals(new int[]{2, 0, 1}, m(2, 0, 1).toArray()); assertFalse(m(2, 0, 1).isIdentity()); }
    @Test void ofRejectsDuplicates() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> m(0, 1, 1));
        assertTrue(ex.getMessage().contains("more than once"));
    }
    @Test void ofRejectsOutOfRangeAndNegativeEntries() {
        assertThrows(IllegalArgumentException.class, () -> m(0, 3, 1));
        assertThrows(IllegalArgumentException.class, () -> m(0, -1, 1));
        assertThrows(IllegalArgumentException.class, () -> m(1));
    }
    @Test void ofRejectsNull() { assertThrows(NullPointerException.class, () -> TileMapping.of(null)); }
    @Test void ofAndToArrayMakeDefensiveCopies() {
        int[] in = {1, 0}; TileMapping map = TileMapping.of(in); in[0] = 0;
        assertEquals(1, map.tileAt(0));
        map.toArray()[0] = 0; assertEquals(1, map.tileAt(0));
    }

    @Test void swapExchangesTwoPositionsAndLeavesOthers() {
        assertArrayEquals(new int[]{3, 1, 2, 0, 4}, TileMapping.identity(5).swapped(0, 3).toArray());
        assertArrayEquals(new int[]{3, 1, 2, 0, 4}, TileMapping.identity(5).swapped(3, 0).toArray());
    }
    @Test void swapIsImmutableAndSamePositionReturnsSameInstance() {
        TileMapping id = TileMapping.identity(3); TileMapping s = id.swapped(0, 2);
        assertTrue(id.isIdentity()); assertNotSame(id, s); assertSame(id, id.swapped(1, 1));
    }
    @Test void swapTwiceRestoresOriginal() { TileMapping id = TileMapping.identity(6); assertEquals(id, id.swapped(1, 4).swapped(1, 4)); }
    @Test void swapRejectsInvalidPositions() {
        TileMapping id = TileMapping.identity(3);
        assertThrows(IndexOutOfBoundsException.class, () -> id.swapped(-1, 0)); assertThrows(IndexOutOfBoundsException.class, () -> id.swapped(0, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> id.swapped(3, 3));
    }

    @Test void moveForwardShiftsIntermediateEntriesBack() { assertArrayEquals(new int[]{1, 2, 3, 0, 4}, TileMapping.identity(5).moved(0, 3).toArray()); }
    @Test void moveBackwardShiftsIntermediateEntriesForward() { assertArrayEquals(new int[]{0, 4, 1, 2, 3}, TileMapping.identity(5).moved(4, 1).toArray()); }
    @Test void moveToEitherEnd() {
        assertArrayEquals(new int[]{1, 2, 3, 0}, TileMapping.identity(4).moved(0, 3).toArray());
        assertArrayEquals(new int[]{3, 0, 1, 2}, TileMapping.identity(4).moved(3, 0).toArray());
    }
    @Test void moveByOnePositionEqualsSwapOfNeighbours() { assertEquals(TileMapping.identity(4).swapped(1, 2), TileMapping.identity(4).moved(1, 2)); }
    @Test void moveToSamePositionReturnsSameInstance() { TileMapping id = TileMapping.identity(3); assertSame(id, id.moved(2, 2)); }
    @Test void moveRejectsInvalidPositions() {
        TileMapping id = TileMapping.identity(3);
        assertThrows(IndexOutOfBoundsException.class, () -> id.moved(-1, 0)); assertThrows(IndexOutOfBoundsException.class, () -> id.moved(0, 3));
    }
    @Test void moveAndBackRestoresOriginal() { TileMapping id = TileMapping.identity(8); assertEquals(id, id.moved(2, 6).moved(6, 2)); }

    @Test void equalityIsByContent() {
        assertEquals(m(1, 0), m(1, 0)); assertEquals(m(1, 0).hashCode(), m(1, 0).hashCode()); assertNotEquals(m(1, 0), m(0, 1));
        assertNotEquals(TileMapping.identity(2), TileMapping.identity(3));
    }
    @Test void randomEditsAlwaysRemainAPermutationOfOriginalTiles() {
        Random random = new Random(12345); TileMapping map = TileMapping.identity(64);
        for (int i = 0; i < 2000; i++) map = random.nextBoolean() ? map.swapped(random.nextInt(64), random.nextInt(64)) : map.moved(random.nextInt(64), random.nextInt(64));
        int[] result = map.toArray();
        assertDoesNotThrow(() -> TileMapping.of(result)); // permutation: nothing lost, nothing duplicated
    }
}
