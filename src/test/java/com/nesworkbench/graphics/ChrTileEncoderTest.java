package com.nesworkbench.graphics;

import org.junit.jupiter.api.Test;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class ChrTileEncoderTest {
    @Test void encodesKnownPatternIntoBothBitplanes() {
        int[][] px = new int[8][8];
        px[0][0] = 1; px[0][1] = 3; px[0][7] = 2; // low bits: 1,1,0..0 ; high bits: 0,1,0..1
        byte[] out = ChrTileEncoder.encode(px);
        assertEquals(16, out.length);
        assertEquals((byte) 0b11000000, out[0]);
        assertEquals((byte) 0b01000001, out[8]);
        for (int i = 1; i < 8; i++) { assertEquals(0, out[i]); assertEquals(0, out[i + 8]); }
    }
    @Test void roundTripsEveryPixelValueThroughTheDecoder() {
        int[][] px = new int[8][8];
        for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++) px[y][x] = (x + y) % 4;
        assertArrayEquals(px, ChrTileDecoder.decode(ChrTileEncoder.encode(px)).get(0));
    }
    @Test void decodeThenEncodeReproducesRandomChrBytes() {
        byte[] chr = new byte[16 * 64]; new Random(7).nextBytes(chr);
        var tiles = ChrTileDecoder.decode(chr);
        for (int t = 0; t < tiles.size(); t++)
            assertArrayEquals(java.util.Arrays.copyOfRange(chr, t * 16, t * 16 + 16), ChrTileEncoder.encode(tiles.get(t)), "tile " + t);
    }
    @Test void rejectsWrongRowCount() { assertThrows(IllegalArgumentException.class, () -> ChrTileEncoder.encode(new int[7][8])); }
    @Test void rejectsWrongRowWidth() { assertThrows(IllegalArgumentException.class, () -> ChrTileEncoder.encode(new int[8][9])); }
    @Test void rejectsNullRow() { int[][] px = new int[8][]; assertThrows(IllegalArgumentException.class, () -> ChrTileEncoder.encode(px)); }
    @Test void rejectsPixelsOutsideZeroThroughThree() {
        int[][] hi = new int[8][8]; hi[3][3] = 4;
        int[][] lo = new int[8][8]; lo[0][0] = -1;
        assertThrows(IllegalArgumentException.class, () -> ChrTileEncoder.encode(hi));
        assertThrows(IllegalArgumentException.class, () -> ChrTileEncoder.encode(lo));
    }
    @Test void rejectsNull() { assertThrows(NullPointerException.class, () -> ChrTileEncoder.encode(null)); }
}
