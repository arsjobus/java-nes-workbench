package com.nesworkbench.sprite;

import com.nesworkbench.graphics.TileSheetPngExporter;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * PNG artwork for compositions. Export writes a 2-bit indexed PNG at 1x (one image pixel per NES pixel).
 * Import accepts any PNG, indexed, grayscale or truecolor, whose size matches the composition and whose every pixel is
 * fully opaque and exactly one of the four palette colors; palette indices are recovered from the colors, not from
 * the file's own palette order. No JavaFX dependency; no temporary files.
 */
public final class ArtworkPng {
    /** Larger files are refused before decoding. */
    public static final long MAX_PNG_BYTES = 4L * 1024 * 1024;
    private static final byte[] SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private ArtworkPng() {}

    /** Exports a composition as a PNG of {@code widthPixels x heightPixels}. The palette colors must be distinct so the result can be imported again. */
    public static byte[] export(SpriteComposition composition, List<int[][]> sourceTiles, int[] palette) throws IOException {
        Objects.requireNonNull(composition, "composition must not be null");
        checkPalette(palette);
        return TileSheetPngExporter.toPng(composition.toTiles(sourceTiles), composition.widthTiles(), palette);
    }

    public static ImportedArtwork read(Path path, int expectedWidthPx, int expectedHeightPx, int[] palette) throws IOException, ArtworkFormatException {
        Objects.requireNonNull(path, "path must not be null");
        if (!Files.isRegularFile(path)) throw new IOException("Not a readable file: " + path);
        long size = Files.size(path);
        if (size > MAX_PNG_BYTES) throw new ArtworkFormatException("PNG file is " + size + " bytes, larger than the " + MAX_PNG_BYTES + "-byte limit.");
        return read(Files.readAllBytes(path), expectedWidthPx, expectedHeightPx, palette);
    }

    public static ImportedArtwork read(byte[] png, int expectedWidthPx, int expectedHeightPx, int[] palette) throws ArtworkFormatException {
        Objects.requireNonNull(png, "png must not be null");
        checkPalette(palette);
        int max = SpriteComposition.MAX_DIMENSION * SpriteComposition.TILE_SIZE;
        if (expectedWidthPx < 8 || expectedHeightPx < 8 || expectedWidthPx > max || expectedHeightPx > max || expectedWidthPx % 8 != 0 || expectedHeightPx % 8 != 0)
            throw new IllegalArgumentException("Expected size must be a multiple of 8 pixels between 8 and " + max + ".");
        if (png.length > MAX_PNG_BYTES) throw new ArtworkFormatException("PNG file is " + png.length + " bytes, larger than the " + MAX_PNG_BYTES + "-byte limit.");
        if (!hasSignature(png)) throw new ArtworkFormatException("Not a PNG file (missing PNG signature).");

        BufferedImage image = decode(png, expectedWidthPx, expectedHeightPx);
        int w = image.getWidth(), h = image.getHeight();
        int[] rgb = new int[4];
        for (int i = 0; i < 4; i++) rgb[i] = palette[i] & 0xFFFFFF;

        int[][] pixels = new int[h][w];
        long offPalette = 0;
        int[] firstOff = null, firstTransparent = null;
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int argb = argbAt(image, x, y);
            int alpha = argb >>> 24;
            if (alpha != 0xFF) { if (firstTransparent == null) firstTransparent = new int[]{x, y, alpha}; continue; }
            int index = -1;
            for (int i = 0; i < 4; i++) if (rgb[i] == (argb & 0xFFFFFF)) { index = i; break; }
            if (index < 0) { offPalette++; if (firstOff == null) firstOff = new int[]{x, y, argb & 0xFFFFFF}; continue; }
            pixels[y][x] = index;
        }
        if (firstTransparent != null)
            throw new ArtworkFormatException("Pixel (" + firstTransparent[0] + ", " + firstTransparent[1] + ") is not fully opaque (alpha " + firstTransparent[2] + "); transparency is not supported.");
        if (firstOff != null)
            throw new ArtworkFormatException(offPalette + " pixel(s) use colors outside the 4-color palette; the first is at (" + firstOff[0] + ", " + firstOff[1]
                    + ") with color " + hex(firstOff[2]) + ". Allowed colors: " + hex(rgb[0]) + ", " + hex(rgb[1]) + ", " + hex(rgb[2]) + ", " + hex(rgb[3]) + ".");
        return new ImportedArtwork(pixels);
    }

    /** Checks the size from the PNG header before decoding any pixels, so a huge image cannot exhaust memory. */
    private static BufferedImage decode(byte[] png, int expectedW, int expectedH) throws ArtworkFormatException {
        Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("png");
        if (!readers.hasNext()) throw new ArtworkFormatException("No PNG reader is available on this system.");
        ImageReader reader = readers.next();
        try (ImageInputStream in = new MemoryCacheImageInputStream(new ByteArrayInputStream(png))) {
            reader.setInput(in, true, true);
            int w = reader.getWidth(0), h = reader.getHeight(0);
            if (w != expectedW || h != expectedH)
                throw new ArtworkFormatException("Image is " + w + "x" + h + " pixels, but the composition needs " + expectedW + "x" + expectedH
                        + " (" + expectedW / 8 + "x" + expectedH / 8 + " tiles of 8x8 pixels).");
            BufferedImage image = reader.read(0);
            if (image == null) throw new ArtworkFormatException("PNG could not be decoded.");
            return image;
        } catch (IOException | RuntimeException ex) {
            throw new ArtworkFormatException("PNG could not be decoded: " + (ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()));
        } finally {
            reader.dispose();
        }
    }

    /** Color of one pixel as ARGB. Grayscale samples are used as stored, because AWT's own gray conversion applies gamma. */
    private static int argbAt(BufferedImage image, int x, int y) {
        ColorModel cm = image.getColorModel();
        if (cm instanceof IndexColorModel icm) return icm.getRGB(image.getRaster().getSample(x, y, 0));
        if (cm.getColorSpace().getType() == ColorSpace.TYPE_GRAY) {
            int bits = cm.getComponentSize(0), maxSample = (1 << bits) - 1;
            int gray = (int) (((long) image.getRaster().getSample(x, y, 0) * 255 + maxSample / 2) / maxSample);
            int alpha = cm.hasAlpha() ? cm.getAlpha(image.getRaster().getDataElements(x, y, null)) : 0xFF;
            return (alpha << 24) | (gray << 16) | (gray << 8) | gray;
        }
        return image.getRGB(x, y);
    }

    private static void checkPalette(int[] palette) {
        if (palette == null || palette.length != 4) throw new IllegalArgumentException("Palette must contain exactly 4 colors.");
        for (int i = 0; i < 4; i++) for (int j = i + 1; j < 4; j++)
            if ((palette[i] & 0xFFFFFF) == (palette[j] & 0xFFFFFF))
                throw new IllegalArgumentException("Palette colors must be distinct so pixels can be told apart (entries " + i + " and " + j + " match).");
    }

    private static boolean hasSignature(byte[] png) {
        if (png.length < SIGNATURE.length) return false;
        for (int i = 0; i < SIGNATURE.length; i++) if (png[i] != SIGNATURE[i]) return false;
        return true;
    }

    private static String hex(int rgb) { return String.format("#%06X", rgb & 0xFFFFFF); }
}
