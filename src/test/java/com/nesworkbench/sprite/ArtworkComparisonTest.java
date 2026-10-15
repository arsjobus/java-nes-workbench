package com.nesworkbench.sprite;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ArtworkComparisonTest {
    private final List<int[][]> source = SpriteCompositionTest.sourceTiles(16);
    private final SpriteComposition comp = SpriteComposition.of("hero", 2, 2, new int[]{1, SpriteComposition.EMPTY, 2, 3});

    private int[][] pixels() { return comp.toPixels(source); }

    @Test void identicalArtworkHasNoDifferences() {
        ArtworkComparison r = ArtworkComparison.compare(comp, source, new ImportedArtwork(pixels()));
        assertTrue(r.identical()); assertEquals(0, r.changedPixels()); assertEquals(0, r.changedCellCount()); assertEquals(256, r.totalPixels());
        assertEquals(4, r.cells().size());
        assertTrue(r.report().contains("identical"));
    }
    @Test void countsChangedPixelsPerCellAndMarksThem() {
        int[][] p = pixels();
        p[0][1] = (p[0][1] + 1) % 4;        // cell (0,0)
        p[7][7] = (p[7][7] + 1) % 4;        // cell (0,0), last pixel
        p[8][8] = (p[8][8] + 2) % 4;        // cell (1,1), first pixel
        ArtworkComparison r = ArtworkComparison.compare(comp, source, new ImportedArtwork(p));
        assertFalse(r.identical()); assertEquals(3, r.changedPixels()); assertEquals(2, r.changedCellCount());
        assertEquals(new ArtworkComparison.CellDiff(0, 0, 1, 2), r.cells().get(0));
        assertEquals(0, r.cells().get(1).changedPixels()); assertEquals(0, r.cells().get(2).changedPixels());
        assertEquals(new ArtworkComparison.CellDiff(1, 1, 3, 1), r.cells().get(3));
        assertTrue(r.pixelChanged(1, 0)); assertTrue(r.pixelChanged(7, 7)); assertTrue(r.pixelChanged(8, 8));
        assertFalse(r.pixelChanged(0, 0)); assertFalse(r.pixelChanged(9, 9));
    }
    @Test void everyPixelDifferentCountsAllOfThem() {
        int[][] p = pixels();
        for (int[] row : p) for (int x = 0; x < row.length; x++) row[x] = (row[x] + 1) % 4;
        ArtworkComparison r = ArtworkComparison.compare(comp, source, new ImportedArtwork(p));
        assertEquals(256, r.changedPixels()); assertEquals(4, r.changedCellCount());
        assertTrue(r.report().contains("256 of 256 pixels differ (100.0%) in 4 of 4 cells"), r.report());
    }
    @Test void emptyCellsCompareAgainstBlankPixels() {
        int[][] p = pixels();
        p[2][10] = 3; p[3][11] = 1;          // cell (1,0) is empty in the composition
        ArtworkComparison r = ArtworkComparison.compare(comp, source, new ImportedArtwork(p));
        assertEquals(new ArtworkComparison.CellDiff(1, 0, SpriteComposition.EMPTY, 2), r.cells().get(1));
        assertTrue(r.report().contains("Cell (1, 0): empty cell, 2 pixel(s) drawn"), r.report());
    }
    @Test void reportNamesSourceTilesInDecimalAndHex() {
        int[][] p = pixels(); p[8][0] = (p[8][0] + 1) % 4;   // cell (0,1) = tile 2
        String report = ArtworkComparison.compare(comp, source, new ImportedArtwork(p)).report();
        assertTrue(report.contains("'hero'") && report.contains("Cell (0, 1): source tile 2 ($2), 1 pixel(s) changed") && report.contains("1 of 256 pixels differ (0.4%) in 1 of 4 cells"), report);
    }
    @Test void reportListIsCappedForHugeCompositions() {
        SpriteComposition big = SpriteComposition.empty("big", 32, 32);
        int[][] p = new int[256][256];
        for (int ty = 0; ty < 32; ty++) for (int tx = 0; tx < 32; tx++) p[ty * 8][tx * 8] = 1;
        String report = ArtworkComparison.compare(big, source, new ImportedArtwork(p)).report();
        long cellLines = report.lines().filter(l -> l.startsWith("Cell (")).count();
        assertEquals(ArtworkComparison.MAX_REPORT_CELLS, cellLines);
        assertTrue(report.contains("... and " + (1024 - ArtworkComparison.MAX_REPORT_CELLS) + " more changed cells."), report);
    }
    @Test void mismatchedArtworkSizeIsRejected() {
        assertTrue(assertThrows(IllegalArgumentException.class, () -> ArtworkComparison.compare(comp, source, new ImportedArtwork(new int[8][8]))).getMessage().contains("16x16"));
    }
    @Test void missingSourceTilesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> ArtworkComparison.compare(comp, SpriteCompositionTest.sourceTiles(3), new ImportedArtwork(pixels())));
    }
    @Test void pixelChangedChecksBounds() {
        ArtworkComparison r = ArtworkComparison.compare(comp, source, new ImportedArtwork(pixels()));
        assertThrows(IndexOutOfBoundsException.class, () -> r.pixelChanged(16, 0)); assertThrows(IndexOutOfBoundsException.class, () -> r.pixelChanged(0, -1));
    }
    @Test void exportEditImportCompareEndToEnd() throws Exception {
        int[] palette = {0xFF000000, 0xFF555555, 0xFFAAAAAA, 0xFFFFFFFF};
        byte[] png = ArtworkPng.export(comp, source, palette);
        ImportedArtwork untouched = ArtworkPng.read(png, 16, 16, palette);
        assertTrue(ArtworkComparison.compare(comp, source, untouched).identical());
        int[][] edited = untouched.toPixels(); edited[15][15] = (edited[15][15] + 1) % 4;
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) img.setRGB(x, y, palette[edited[y][x]]);
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream(); javax.imageio.ImageIO.write(img, "png", out);
        ArtworkComparison r = ArtworkComparison.compare(comp, source, ArtworkPng.read(out.toByteArray(), 16, 16, palette));
        assertEquals(1, r.changedPixels()); assertTrue(r.pixelChanged(15, 15)); assertEquals(3, r.cells().get(3).sourceTile());
    }
}
