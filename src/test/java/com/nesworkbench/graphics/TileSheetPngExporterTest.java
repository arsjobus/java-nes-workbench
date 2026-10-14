package com.nesworkbench.graphics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class TileSheetPngExporterTest {
    @TempDir Path dir;
    private static final int[] P = {0xFF101010, 0xFFFF0000, 0xFF00FF00, 0xFF0000FF};

    private static int[][] tile(int seed) {
        int[][] t = new int[8][8];
        for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++) t[y][x] = (x * 3 + y + seed) % 4;
        return t;
    }
    private static BufferedImage read(byte[] png) throws Exception { return ImageIO.read(new ByteArrayInputStream(png)); }

    @Test void roundTripsEveryPixelIndexAndPaletteColor() throws Exception {
        List<int[][]> tiles = new ArrayList<>(); for (int i = 0; i < 3; i++) tiles.add(tile(i));
        BufferedImage img = read(TileSheetPngExporter.toPng(tiles, 2, P));
        assertEquals(16, img.getWidth()); assertEquals(16, img.getHeight());
        for (int i = 0; i < 3; i++) for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++) {
            int px = (i % 2) * 8 + x, py = (i / 2) * 8 + y, idx = tiles.get(i)[y][x];
            assertEquals(idx, img.getRaster().getSample(px, py, 0), "index at " + px + "," + py);
            assertEquals(P[idx] & 0xFFFFFF, img.getRGB(px, py) & 0xFFFFFF, "color at " + px + "," + py);
        }
    }
    @Test void unusedCellsInLastRowStayIndexZero() throws Exception {
        BufferedImage img = read(TileSheetPngExporter.toPng(List.of(tile(1), tile(2), tile(3)), 2, P));
        for (int y = 8; y < 16; y++) for (int x = 8; x < 16; x++) assertEquals(0, img.getRaster().getSample(x, y, 0));
    }
    @Test void exportsDecodedChrFromKnownBitplanes() throws Exception {
        byte[] chr = new byte[16]; chr[0] = (byte) 0b11000000; chr[8] = (byte) 0b01000000; // pixels 1, 3, 0...
        BufferedImage img = read(TileSheetPngExporter.toPng(ChrTileDecoder.decode(chr), 16, P));
        assertEquals(1, img.getRaster().getSample(0, 0, 0)); assertEquals(3, img.getRaster().getSample(1, 0, 0));
        assertEquals(0, img.getRaster().getSample(2, 0, 0));
    }
    @Test void usesPngSignatureAndIsDeterministic() throws Exception {
        byte[] a = TileSheetPngExporter.toPng(List.<int[][]>of(tile(0)), 16, P), b = TileSheetPngExporter.toPng(List.<int[][]>of(tile(0)), 16, P);
        assertEquals((byte) 0x89, a[0]); assertEquals((byte) 'P', a[1]); assertEquals((byte) 'N', a[2]); assertEquals((byte) 'G', a[3]);
        assertArrayEquals(a, b);
    }
    @Test void rejectsInvalidInputs() {
        assertThrows(IllegalArgumentException.class, () -> TileSheetPngExporter.toPng(List.of(), 16, P));
        assertThrows(IllegalArgumentException.class, () -> TileSheetPngExporter.toPng(List.<int[][]>of(tile(0)), 0, P));
        assertThrows(IllegalArgumentException.class, () -> TileSheetPngExporter.toPng(List.<int[][]>of(tile(0)), 16, new int[2]));
        assertThrows(NullPointerException.class, () -> TileSheetPngExporter.toPng(null, 16, P));
        int[][] bad = tile(0); bad[0][0] = 9;
        assertThrows(IllegalArgumentException.class, () -> TileSheetPngExporter.toPng(List.<int[][]>of(bad), 16, P));
    }
    @Test void writesNewFile() throws Exception {
        Path f = dir.resolve("out.png"); byte[] png = TileSheetPngExporter.toPng(List.<int[][]>of(tile(0)), 16, P);
        TileSheetPngExporter.write(f, png, false); assertArrayEquals(png, Files.readAllBytes(f));
    }
    @Test void refusesToOverwriteByDefault() throws Exception {
        Path f = dir.resolve("existing.png"); Files.write(f, new byte[]{1, 2, 3});
        assertThrows(FileAlreadyExistsException.class, () -> TileSheetPngExporter.write(f, new byte[]{9}, false));
        assertArrayEquals(new byte[]{1, 2, 3}, Files.readAllBytes(f));
    }
    @Test void overwritesAndTruncatesWhenExplicitlyAllowed() throws Exception {
        Path f = dir.resolve("existing.png"); Files.write(f, new byte[]{1, 2, 3, 4, 5});
        TileSheetPngExporter.write(f, new byte[]{9}, true); assertArrayEquals(new byte[]{9}, Files.readAllBytes(f));
    }
    @Test void writeRejectsNulls() {
        assertThrows(NullPointerException.class, () -> TileSheetPngExporter.write(null, new byte[0], false));
        assertThrows(NullPointerException.class, () -> TileSheetPngExporter.write(dir.resolve("x"), null, false));
    }
}
