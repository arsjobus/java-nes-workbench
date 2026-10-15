package com.nesworkbench.sprite;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ArtworkPngTest {
    private static final int[] P = {0xFF101010, 0xFFFF0000, 0xFF00FF00, 0xFF0000FF};
    private static final int[] GRAY = {0xFF000000, 0xFF555555, 0xFFAAAAAA, 0xFFFFFFFF};
    private final List<int[][]> source = SpriteCompositionTest.sourceTiles(16);

    private static byte[] png(BufferedImage img) throws Exception { ByteArrayOutputStream o = new ByteArrayOutputStream(); assertTrue(ImageIO.write(img, "png", o)); return o.toByteArray(); }
    private static ArtworkFormatException rejects(byte[] png, int w, int h, int[] palette) { return assertThrows(ArtworkFormatException.class, () -> ArtworkPng.read(png, w, h, palette)); }
    private static BufferedImage rgb(int w, int h, int type, int fillArgb) {
        BufferedImage img = new BufferedImage(w, h, type);
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) img.setRGB(x, y, fillArgb);
        return img;
    }

    // ---- export ----
    @Test void exportHasCompositionDimensionsAndIndexedPixels() throws Exception {
        SpriteComposition c = SpriteComposition.of("a", 3, 2, new int[]{1, 2, 3, 5, SpriteComposition.EMPTY, 7});
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(ArtworkPng.export(c, source, P)));
        assertEquals(24, img.getWidth()); assertEquals(16, img.getHeight());
        assertInstanceOf(IndexColorModel.class, img.getColorModel());
        int[][] expected = c.toPixels(source);
        for (int y = 0; y < 16; y++) for (int x = 0; x < 24; x++) {
            assertEquals(expected[y][x], img.getRaster().getSample(x, y, 0), "index at " + x + "," + y);
            assertEquals(P[expected[y][x]] & 0xFFFFFF, img.getRGB(x, y) & 0xFFFFFF);
        }
    }
    @Test void exportDimensionsAtLimits() throws Exception {
        BufferedImage one = ImageIO.read(new ByteArrayInputStream(ArtworkPng.export(SpriteComposition.empty("a", 1, 1), source, P)));
        assertEquals(8, one.getWidth()); assertEquals(8, one.getHeight());
        BufferedImage max = ImageIO.read(new ByteArrayInputStream(ArtworkPng.export(SpriteComposition.empty("m", 32, 32), source, P)));
        assertEquals(256, max.getWidth()); assertEquals(256, max.getHeight());
    }
    @Test void exportRejectsBadReferencesAndAmbiguousPalettes() {
        assertThrows(IllegalArgumentException.class, () -> ArtworkPng.export(SpriteComposition.empty("a", 1, 1).withTile(0, 0, 99), source, P));
        assertThrows(IllegalArgumentException.class, () -> ArtworkPng.export(SpriteComposition.empty("a", 1, 1), source, new int[]{1, 2, 3}));
        assertThrows(IllegalArgumentException.class, () -> ArtworkPng.export(SpriteComposition.empty("a", 1, 1), source, new int[]{0xFF000000, 0xFF000000, 0xFFAAAAAA, 0xFFFFFFFF}));
    }

    // ---- pixel round trips ----
    @Test void exportThenImportRoundTripsEveryPixel() throws Exception {
        SpriteComposition c = SpriteComposition.of("a", 4, 2, new int[]{0, 1, 2, 3, 4, 5, SpriteComposition.EMPTY, 15});
        for (int[] palette : new int[][]{P, GRAY}) {
            ImportedArtwork art = ArtworkPng.read(ArtworkPng.export(c, source, palette), 32, 16, palette);
            assertArrayEquals(c.toPixels(source), art.toPixels());
            assertEquals(4, art.widthTiles()); assertEquals(2, art.heightTiles());
        }
    }
    @Test void roundTripThroughAFile(@TempDir Path dir) throws Exception {
        SpriteComposition c = SpriteComposition.empty("a", 2, 2).withTile(0, 0, 9).withTile(1, 1, 6);
        Path f = dir.resolve("art.png"); Files.write(f, ArtworkPng.export(c, source, P));
        assertArrayEquals(c.toPixels(source), ArtworkPng.read(f, 16, 16, P).toPixels());
    }

    // ---- accepted formats ----
    @Test void acceptsTruecolorPngsWithMatchingColors() throws Exception {
        BufferedImage img = rgb(8, 8, BufferedImage.TYPE_INT_RGB, 0xFF101010);
        img.setRGB(3, 4, 0xFF00FF00); img.setRGB(7, 7, 0xFF0000FF);
        int[][] px = ArtworkPng.read(png(img), 8, 8, P).toPixels();
        assertEquals(0, px[0][0]); assertEquals(2, px[4][3]); assertEquals(3, px[7][7]);
    }
    @Test void acceptsArgbPngsThatAreFullyOpaque() throws Exception {
        BufferedImage img = rgb(8, 8, BufferedImage.TYPE_INT_ARGB, 0xFFFF0000);
        assertEquals(1, ArtworkPng.read(png(img), 8, 8, P).pixel(5, 5));
    }
    @Test void acceptsEightBitGrayscalePngsWithoutGammaDrift() throws Exception {
        BufferedImage img = new BufferedImage(8, 8, BufferedImage.TYPE_BYTE_GRAY);
        int[] levels = {0x00, 0x55, 0xAA, 0xFF};
        for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++) img.getRaster().setSample(x, y, 0, levels[(x + y) % 4]);
        ImportedArtwork art = ArtworkPng.read(png(img), 8, 8, GRAY);
        for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++) assertEquals((x + y) % 4, art.pixel(x, y), x + "," + y);
    }
    @Test void indexedPngWithReorderedPaletteMapsByColorNotByFileIndex() throws Exception {
        // File palette order is reversed relative to the workbench palette: file index 0 is blue (workbench index 3).
        byte[] r = {0, 0, (byte) 255, 0x10}, g = {0, (byte) 255, 0, 0x10}, b = {(byte) 255, 0, 0, 0x10};
        BufferedImage img = new BufferedImage(8, 8, BufferedImage.TYPE_BYTE_BINARY, new IndexColorModel(2, 4, r, g, b));
        img.getRaster().setSample(0, 0, 0, 0); img.getRaster().setSample(1, 0, 0, 1); img.getRaster().setSample(2, 0, 0, 2); img.getRaster().setSample(3, 0, 0, 3);
        for (int x = 4; x < 8; x++) img.getRaster().setSample(x, 0, 0, 3);
        ImportedArtwork art = ArtworkPng.read(png(img), 8, 8, P);
        assertArrayEquals(new int[]{3, 2, 1, 0, 0, 0, 0, 0}, art.toPixels()[0]);
    }
    @Test void indexedPngMayLeaveUnusedPaletteEntriesOffPalette() throws Exception {
        byte[] r = {0x10, (byte) 255, 1, 2, 3, 4, 5, 6}, g = {0x10, 0, 1, 2, 3, 4, 5, 6}, b = {0x10, 0, 1, 2, 3, 4, 5, 6};
        BufferedImage img = new BufferedImage(8, 8, BufferedImage.TYPE_BYTE_BINARY, new IndexColorModel(4, 8, r, g, b));
        for (int x = 0; x < 8; x++) img.getRaster().setSample(x, 0, 0, x % 2);
        assertEquals(1, ArtworkPng.read(png(img), 8, 8, P).pixel(1, 0));
    }

    // ---- palette constraints ----
    @Test void rejectsColorsOutsideThePaletteAndReportsTheFirst() throws Exception {
        BufferedImage img = rgb(8, 8, BufferedImage.TYPE_INT_RGB, 0xFF101010);
        img.setRGB(2, 1, 0xFF123456); img.setRGB(5, 6, 0xFFABCDEF);
        String m = rejects(png(img), 8, 8, P).getMessage();
        assertTrue(m.contains("2 pixel(s)") && m.contains("(2, 1)") && m.contains("#123456") && m.contains("#101010") && m.contains("#0000FF"), m);
    }
    @Test void rejectsNearMissColors() throws Exception {
        assertTrue(rejects(png(rgb(8, 8, BufferedImage.TYPE_INT_RGB, 0xFF101011)), 8, 8, P).getMessage().contains("outside the 4-color palette"));
    }
    @Test void rejectsTransparentPixels() throws Exception {
        BufferedImage img = rgb(8, 8, BufferedImage.TYPE_INT_ARGB, 0xFF101010);
        img.setRGB(6, 3, 0x80101010);
        String m = rejects(png(img), 8, 8, P).getMessage();
        assertTrue(m.contains("(6, 3)") && m.contains("alpha 128") && m.contains("transparency"), m);
        assertThrows(ArtworkFormatException.class, () -> ArtworkPng.read(png(rgb(8, 8, BufferedImage.TYPE_INT_ARGB, 0x00101010)), 8, 8, P));
    }
    @Test void rejectsAmbiguousOrMalformedPalettes() throws Exception {
        byte[] ok = png(rgb(8, 8, BufferedImage.TYPE_INT_RGB, 0xFF101010));
        assertThrows(IllegalArgumentException.class, () -> ArtworkPng.read(ok, 8, 8, new int[]{0xFF000000, 0xFF000000, 0xFF111111, 0xFF222222}));
        assertThrows(IllegalArgumentException.class, () -> ArtworkPng.read(ok, 8, 8, new int[]{1, 2, 3}));
        assertThrows(IllegalArgumentException.class, () -> ArtworkPng.read(ok, 8, 8, null));
    }

    // ---- size validation ----
    @Test void rejectsWrongDimensionsWithBothSizesInTheMessage() throws Exception {
        byte[] img16 = png(rgb(16, 8, BufferedImage.TYPE_INT_RGB, 0xFF101010));
        String m = rejects(img16, 8, 8, P).getMessage();
        assertTrue(m.contains("16x8") && m.contains("8x8"), m);
        rejects(img16, 16, 16, P); rejects(img16, 8, 16, P);
        assertDoesNotThrow(() -> ArtworkPng.read(img16, 16, 8, P));
    }
    @Test void rejectsOffByOneSizesThatAreNotWholeTiles() throws Exception {
        byte[] img = png(rgb(9, 8, BufferedImage.TYPE_INT_RGB, 0xFF101010));
        rejects(img, 8, 8, P);
        rejects(png(rgb(7, 8, BufferedImage.TYPE_INT_RGB, 0xFF101010)), 8, 8, P);
    }
    @Test void invalidExpectedSizesAreProgrammerErrors() throws Exception {
        byte[] img = png(rgb(8, 8, BufferedImage.TYPE_INT_RGB, 0xFF101010));
        for (int[] bad : new int[][]{{0, 8}, {8, 0}, {7, 8}, {8, 12}, {264, 8}, {8, 264}})
            assertThrows(IllegalArgumentException.class, () -> ArtworkPng.read(img, bad[0], bad[1], P), bad[0] + "x" + bad[1]);
    }
    @Test void acceptsTheLargestComposition() throws Exception {
        assertEquals(256, ArtworkPng.read(png(rgb(256, 256, BufferedImage.TYPE_INT_RGB, 0xFF101010)), 256, 256, P).widthPixels());
    }

    // ---- not a valid PNG ----
    @Test void rejectsNonPngDataBeforeDecoding() throws Exception {
        assertTrue(rejects(new byte[0], 8, 8, P).getMessage().contains("PNG"));
        rejects("hello".getBytes(), 8, 8, P);
        ByteArrayOutputStream jpg = new ByteArrayOutputStream(); ImageIO.write(rgb(8, 8, BufferedImage.TYPE_INT_RGB, 0xFF101010), "jpg", jpg);
        assertTrue(rejects(jpg.toByteArray(), 8, 8, P).getMessage().contains("Not a PNG"));
        ByteArrayOutputStream gif = new ByteArrayOutputStream(); ImageIO.write(rgb(8, 8, BufferedImage.TYPE_INT_RGB, 0xFF101010), "gif", gif);
        rejects(gif.toByteArray(), 8, 8, P);
    }
    @Test void rejectsPngsWhosePixelDataIsTruncated() throws Exception {
        // 64x64 of pseudo-random palette colors: the compressed pixel data is several KB, so cutting anywhere in the
        // first half must lose pixel data. A partly decoded image must never be returned.
        java.util.Random random = new java.util.Random(7);
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++) img.setRGB(x, y, P[random.nextInt(4)] & 0xFFFFFF);
        byte[] good = png(img);
        assertDoesNotThrow(() -> ArtworkPng.read(good, 64, 64, P));
        for (int cut = 0; cut < good.length / 2; cut += 37)
            rejects(java.util.Arrays.copyOf(good, cut), 64, 64, P);
    }
    @Test void rejectsPngsWithADamagedHeader() throws Exception {
        byte[] good = png(rgb(8, 8, BufferedImage.TYPE_INT_RGB, 0xFF101010));
        rejects(java.util.Arrays.copyOf(good, 20), 8, 8, P);                    // cut inside IHDR
        byte[] noIhdr = good.clone(); noIhdr[12] = 'X'; noIhdr[13] = 'X';        // first chunk is no longer IHDR
        assertThrows(ArtworkFormatException.class, () -> ArtworkPng.read(noIhdr, 8, 8, P));
    }
    @Test void sizeIsCheckedFromTheHeaderBeforePixelsAreDecoded() throws Exception {
        // 20000x20000 would need ~1.6 GB as ARGB; the header size check must reject it first.
        byte[] png = png(rgb(8, 8, BufferedImage.TYPE_INT_RGB, 0xFF101010));
        java.nio.ByteBuffer.wrap(png).putInt(16, 20000).putInt(20, 20000); // IHDR width/height (CRC left stale; reader must still not allocate)
        String m = rejects(png, 8, 8, P).getMessage();
        assertTrue(m.contains("20000x20000") || m.contains("decoded"), m);
    }
    @Test void oversizedFilesAreRefusedBeforeReading(@TempDir Path dir) throws Exception {
        Path big = dir.resolve("big.png"); Files.write(big, new byte[(int) ArtworkPng.MAX_PNG_BYTES + 1]);
        assertTrue(assertThrows(ArtworkFormatException.class, () -> ArtworkPng.read(big, 8, 8, P)).getMessage().contains("larger"));
        assertThrows(java.io.IOException.class, () -> ArtworkPng.read(dir.resolve("missing.png"), 8, 8, P));
        assertThrows(java.io.IOException.class, () -> ArtworkPng.read(dir, 8, 8, P));
    }
    @Test void nullArgumentsAreRejected() {
        assertThrows(NullPointerException.class, () -> ArtworkPng.read((byte[]) null, 8, 8, P));
        assertThrows(NullPointerException.class, () -> ArtworkPng.export(null, source, P));
    }
}
