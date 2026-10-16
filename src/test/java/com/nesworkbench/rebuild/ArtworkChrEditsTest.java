package com.nesworkbench.rebuild;

import com.nesworkbench.graphics.ChrTileDecoder;
import com.nesworkbench.sprite.ImportedArtwork;
import com.nesworkbench.sprite.SpriteComposition;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ArtworkChrEditsTest {
    /** 4 source tiles with distinct, non-blank content. */
    private static List<int[][]> tiles() {
        byte[] chr = new byte[64];
        for (int t = 0; t < 4; t++) for (int i = 0; i < 16; i++) chr[t * 16 + i] = (byte) (t * 37 + i * 11 + 1);
        return ChrTileDecoder.decode(chr);
    }
    /** Artwork for a composition, starting as an exact render of it. */
    private static int[][] render(SpriteComposition c) { return c.toPixels(tiles()); }

    @Test void identicalArtworkProducesNoEdits() throws Exception {
        SpriteComposition c = SpriteComposition.of("a", 2, 1, new int[]{0, 1});
        assertTrue(ArtworkChrEdits.from(c, tiles(), new ImportedArtwork(render(c))).isEmpty());
    }
    @Test void onlyTheChangedTileBecomesAnEditAndMatchesTheEditedPixels() throws Exception {
        SpriteComposition c = SpriteComposition.of("a", 2, 1, new int[]{2, 3});
        int[][] px = render(c); px[0][8] = (px[0][8] + 1) % 4;   // first pixel of the second cell (tile 3)
        ChrEdits edits = ArtworkChrEdits.from(c, tiles(), new ImportedArtwork(px));
        assertEquals(java.util.List.of(3), java.util.List.copyOf(edits.tileIndices()));
        int[][] expected = new int[8][];
        for (int y = 0; y < 8; y++) expected[y] = Arrays.copyOfRange(px[y], 8, 16);   // the second cell's pixels
        assertArrayEquals(expected, ChrTileDecoder.decode(edits.tile(3)).get(0));
    }
    @Test void editsUseOriginalTileIndicesNotCellPositions() throws Exception {
        SpriteComposition c = SpriteComposition.of("a", 2, 1, new int[]{3, 0});   // tile 3 sits in cell 0
        int[][] px = render(c); px[2][1] = (px[2][1] + 2) % 4;
        ChrEdits edits = ArtworkChrEdits.from(c, tiles(), new ImportedArtwork(px));
        assertTrue(edits.has(3)); assertFalse(edits.has(0)); assertEquals(1, edits.size());
    }
    @Test void aTilePlacedTwiceWithMatchingArtworkIsEditedOnce() throws Exception {
        SpriteComposition c = SpriteComposition.of("a", 2, 1, new int[]{1, 1});
        int[][] px = render(c);
        for (int y = 0; y < 8; y++) { px[y][0] = (px[y][0] + 1) % 4; px[y][8] = px[y][0]; }   // same change in both cells
        ChrEdits edits = ArtworkChrEdits.from(c, tiles(), new ImportedArtwork(px));
        assertEquals(1, edits.size()); assertTrue(edits.has(1));
    }
    @Test void aTilePlacedTwiceWithDifferentArtworkIsRefused() throws Exception {
        SpriteComposition c = SpriteComposition.of("a", 2, 1, new int[]{1, 1});
        int[][] px = render(c); px[0][0] = (px[0][0] + 1) % 4;   // only the first copy changes
        RebuildException e = assertThrows(RebuildException.class, () -> ArtworkChrEdits.from(c, tiles(), new ImportedArtwork(px)));
        assertTrue(e.getMessage().contains("Tile 1"), e.getMessage());
    }
    @Test void pixelsDrawnInAnEmptyCellAreRefusedButBlankEmptyCellsAreFine() throws Exception {
        SpriteComposition c = SpriteComposition.of("a", 2, 1, new int[]{0, SpriteComposition.EMPTY});
        int[][] px = render(c);
        assertTrue(ArtworkChrEdits.from(c, tiles(), new ImportedArtwork(px)).isEmpty());
        px[4][12] = 2;
        assertTrue(assertThrows(RebuildException.class, () -> ArtworkChrEdits.from(c, tiles(), new ImportedArtwork(px))).getMessage().contains("(1, 0)"));
    }
    @Test void artworkOfTheWrongSizeIsRefused() {
        SpriteComposition c = SpriteComposition.of("a", 2, 1, new int[]{0, 1});
        assertThrows(RebuildException.class, () -> ArtworkChrEdits.from(c, tiles(), new ImportedArtwork(new int[8][8])));
    }
    @Test void compositionReferencingTilesTheRomLacksIsRefused() {
        SpriteComposition c = SpriteComposition.of("a", 1, 1, new int[]{4});
        assertThrows(RebuildException.class, () -> ArtworkChrEdits.from(c, tiles(), new ImportedArtwork(new int[8][8])));
    }
    @Test void rejectsNulls() {
        SpriteComposition c = SpriteComposition.of("a", 1, 1, new int[]{0});
        ImportedArtwork art = new ImportedArtwork(render(c));
        assertThrows(NullPointerException.class, () -> ArtworkChrEdits.from(null, tiles(), art));
        assertThrows(NullPointerException.class, () -> ArtworkChrEdits.from(c, null, art));
        assertThrows(NullPointerException.class, () -> ArtworkChrEdits.from(c, tiles(), null));
    }
    @Test void appliedEditsRoundTripThroughARebuiltRom() throws Exception {
        byte[] rom = RebuildFixtures.make(1, 1, false, 0);
        int chr = RebuildFixtures.chrOffset(1, false);
        com.nesworkbench.rom.RomImage img = com.nesworkbench.rom.RomImage.parse(rom);
        List<int[][]> src = ChrTileDecoder.decode(img.chrRom());
        SpriteComposition c = SpriteComposition.of("hero", 2, 2, new int[]{10, 11, 12, 13});
        int[][] px = c.toPixels(src);
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) if ((x + y) % 5 == 0) px[y][x] = (px[y][x] + 1) % 4;
        ChrEdits edits = ArtworkChrEdits.from(c, src, new ImportedArtwork(px));
        byte[] out = RomRebuilder.rebuild(img, edits);
        RebuildVerifier.verify(img, edits, out);
        List<int[][]> after = ChrDecode.of(out, chr);
        assertArrayEquals(px, c.toPixels(after));
        assertArrayEquals(src.get(14), after.get(14));   // an unreferenced tile is untouched
    }

    /** Test helper: decode the CHR region of a rebuilt ROM. */
    private static final class ChrDecode {
        static List<int[][]> of(byte[] rom, int chrOffset) { return ChrTileDecoder.decode(Arrays.copyOfRange(rom, chrOffset, chrOffset + 8192)); }
    }
}
