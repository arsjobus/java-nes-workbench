package com.nesworkbench.graphics;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Objects;

/** Exports decoded tiles as a 2-bit indexed PNG at 1x, so palette indices survive round trips. */
public final class TileSheetPngExporter {
    private TileSheetPngExporter() {}

    public static byte[] toPng(List<int[][]> tiles, int columns, int[] palette) throws IOException {
        Objects.requireNonNull(tiles, "tiles must not be null");
        TileSheetRenderer.checkPalette(palette);
        if (tiles.isEmpty()) throw new IllegalArgumentException("There are no tiles to export.");
        TileGridLayout layout = new TileGridLayout(tiles.size(), columns, 1);

        byte[] r = new byte[4], g = new byte[4], b = new byte[4];
        for (int i = 0; i < 4; i++) { r[i] = (byte) (palette[i] >> 16); g[i] = (byte) (palette[i] >> 8); b[i] = (byte) palette[i]; }
        BufferedImage image = new BufferedImage(layout.widthPx(), layout.heightPx(),
                BufferedImage.TYPE_BYTE_BINARY, new IndexColorModel(2, 4, r, g, b));
        for (int i = 0; i < tiles.size(); i++) {
            int ox = layout.cellX(i), oy = layout.cellY(i);
            for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++)
                image.getRaster().setSample(ox + x, oy + y, 0, TileSheetRenderer.paletteIndex(tiles.get(i), x, y));
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "png", out)) throw new IOException("No PNG writer available.");
        return out.toByteArray();
    }

    /** Writes PNG bytes to a file. Refuses to replace an existing file unless {@code overwrite} is true. */
    public static void write(Path target, byte[] png, boolean overwrite) throws IOException {
        Objects.requireNonNull(target, "target must not be null");
        Objects.requireNonNull(png, "png must not be null");
        OpenOption[] options = overwrite
                ? new OpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE}
                : new OpenOption[]{StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE};
        Files.write(target, png, options);
    }
}
