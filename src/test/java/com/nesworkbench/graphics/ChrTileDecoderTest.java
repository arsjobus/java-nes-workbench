package com.nesworkbench.graphics;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ChrTileDecoderTest {
    @Test void decodesLowBitplaneBitsFromLeftToRight() {
        byte[] tile=new byte[16]; tile[0]=(byte)0b10000001; int[][] pixels=ChrTileDecoder.decode(tile).get(0);
        assertEquals(1,pixels[0][0]); assertEquals(0,pixels[0][1]); assertEquals(0,pixels[0][6]); assertEquals(1,pixels[0][7]);
    }
    @Test void combinesBothBitplanesIntoFourPixelValues() {
        byte[] tile=new byte[16]; tile[0]=(byte)0b11000000; tile[8]=(byte)0b01000000;
        int[][] pixels=ChrTileDecoder.decode(tile).get(0);
        assertEquals(1,pixels[0][0]); assertEquals(3,pixels[0][1]); assertEquals(0,pixels[0][2]);
    }
    @Test void decodesMultipleTiles() {
        byte[] data=new byte[32]; data[0]=(byte)0xFF; data[16]=(byte)0xFF;
        var tiles=ChrTileDecoder.decode(data); assertEquals(2,tiles.size());
        assertEquals(1,tiles.get(0)[0][0]); assertEquals(1,tiles.get(1)[0][0]);
    }
    @Test void returnsNoTilesForEmptyInput() { assertTrue(ChrTileDecoder.decode(new byte[0]).isEmpty()); }
    @Test void rejectsInputNotAlignedToWholeTiles() { assertThrows(IllegalArgumentException.class, () -> ChrTileDecoder.decode(new byte[17])); }
    @Test void rejectsNullInput() { assertThrows(NullPointerException.class, () -> ChrTileDecoder.decode(null)); }
    @Test void everyDecodedPixelIsInRangeZeroThroughThree() {
        byte[] data=new byte[16]; for(int i=0;i<data.length;i++) data[i]=(byte)(i*19);
        for(int[] row:ChrTileDecoder.decode(data).get(0)) for(int pixel:row) assertTrue(pixel>=0 && pixel<=3);
    }
}
