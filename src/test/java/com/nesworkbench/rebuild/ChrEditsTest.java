package com.nesworkbench.rebuild;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ChrEditsTest {
    @Test void emptyHasNoEdits() {
        ChrEdits e = ChrEdits.empty();
        assertTrue(e.isEmpty()); assertEquals(0, e.size()); assertEquals(-1, e.maxTileIndex()); assertTrue(e.tileIndices().isEmpty());
    }
    @Test void withTileReturnsNewEditsAndLeavesOriginalUntouched() {
        ChrEdits base = ChrEdits.empty(), one = base.withTile(5, RebuildFixtures.tileData(1));
        assertTrue(base.isEmpty()); assertEquals(1, one.size()); assertTrue(one.has(5)); assertFalse(one.has(4));
    }
    @Test void laterEditOfSameTileReplacesEarlier() {
        ChrEdits e = ChrEdits.empty().withTile(2, RebuildFixtures.tileData(1)).withTile(2, RebuildFixtures.tileData(2));
        assertEquals(1, e.size()); assertArrayEquals(RebuildFixtures.tileData(2), e.tile(2));
    }
    @Test void tileIndicesAreAscendingAndMaxIsHighest() {
        ChrEdits e = ChrEdits.empty().withTile(9, RebuildFixtures.tileData(1)).withTile(3, RebuildFixtures.tileData(2)).withTile(7, RebuildFixtures.tileData(3));
        assertEquals(java.util.List.of(3, 7, 9), java.util.List.copyOf(e.tileIndices())); assertEquals(9, e.maxTileIndex());
    }
    @Test void copiesDataOnTheWayInAndOut() {
        byte[] data = RebuildFixtures.tileData(1), kept = data.clone();
        ChrEdits e = ChrEdits.empty().withTile(0, data);
        data[0] ^= 0xFF; assertArrayEquals(kept, e.tile(0));
        e.tile(0)[1] ^= 0xFF; assertArrayEquals(kept, e.tile(0));
    }
    @Test void tileIndicesViewCannotBeModified() {
        ChrEdits e = ChrEdits.empty().withTile(1, RebuildFixtures.tileData(1));
        assertThrows(UnsupportedOperationException.class, () -> e.tileIndices().clear());
        assertEquals(1, e.size());
    }
    @Test void withPixelsEncodesTheTile() {
        int[][] px = new int[8][8]; px[0][0] = 3; px[7][7] = 1;
        byte[] expected = new byte[16]; expected[0] = (byte) 0x80; expected[8] = (byte) 0x80; expected[7] = 0x01;
        assertArrayEquals(expected, ChrEdits.empty().withPixels(4, px).tile(4));
    }
    @Test void rejectsNegativeIndexAndWrongLengthAndNull() {
        assertThrows(IllegalArgumentException.class, () -> ChrEdits.empty().withTile(-1, new byte[16]));
        assertThrows(IllegalArgumentException.class, () -> ChrEdits.empty().withTile(0, new byte[15]));
        assertThrows(IllegalArgumentException.class, () -> ChrEdits.empty().withTile(0, new byte[17]));
        assertThrows(NullPointerException.class, () -> ChrEdits.empty().withTile(0, null));
    }
    @Test void tileOfUneditedIndexIsRejected() { assertThrows(IllegalArgumentException.class, () -> ChrEdits.empty().tile(0)); }
    @Test void ofBuildsFromMapAndValidatesEveryEntry() {
        Map<Integer, byte[]> m = new HashMap<>(); m.put(1, RebuildFixtures.tileData(1)); m.put(8, RebuildFixtures.tileData(2));
        ChrEdits e = ChrEdits.of(m);
        assertEquals(2, e.size()); m.clear(); assertEquals(2, e.size());
        Map<Integer, byte[]> bad = new HashMap<>(); bad.put(-3, new byte[16]);
        assertThrows(IllegalArgumentException.class, () -> ChrEdits.of(bad));
        Map<Integer, byte[]> shortData = new HashMap<>(); shortData.put(0, new byte[2]);
        assertThrows(IllegalArgumentException.class, () -> ChrEdits.of(shortData));
        assertTrue(ChrEdits.of(new HashMap<>()).isEmpty());
    }
    @Test void equalityDependsOnTilesAndData() {
        ChrEdits a = ChrEdits.empty().withTile(1, RebuildFixtures.tileData(1)), b = ChrEdits.empty().withTile(1, RebuildFixtures.tileData(1));
        assertEquals(a, b); assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, ChrEdits.empty().withTile(1, RebuildFixtures.tileData(2)));
        assertNotEquals(a, ChrEdits.empty().withTile(2, RebuildFixtures.tileData(1)));
        assertNotEquals(a, ChrEdits.empty());
    }
}
