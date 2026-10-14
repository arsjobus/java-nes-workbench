package com.nesworkbench.graphics;

import java.util.List;
import java.util.Objects;

/** Renders decoded tiles to a packed ARGB pixel array. Pure computation; no JavaFX or AWT. */
public final class TileSheetRenderer {
    public static final int[] GRAYSCALE = {0xFF000000, 0xFF555555, 0xFFAAAAAA, 0xFFFFFFFF};
    public static final int GRID_COLOR = 0xFF2E7D32, SELECTION_COLOR = 0xFFFF9800;
    private TileSheetRenderer() {}

    /** Packed row-major ARGB pixels. The array is owned by the caller. */
    public record RenderedSheet(int width, int height, int[] argb) {}

    /**
     * @param selectedIndex tile to outline, or -1 for none
     * @param gridOverlay   draws 1px lines on each tile's top/left edge; ignored at zoom 1 where it would hide pixel data
     */
    public static RenderedSheet render(List<int[][]> tiles, TileGridLayout layout, int[] palette,
                                       int selectedIndex, boolean gridOverlay) {
        Objects.requireNonNull(tiles, "tiles must not be null");
        Objects.requireNonNull(layout, "layout must not be null");
        checkPalette(palette);
        if (tiles.size() != layout.tileCount())
            throw new IllegalArgumentException("Layout expects " + layout.tileCount() + " tiles but got " + tiles.size() + ".");
        if (selectedIndex < -1 || selectedIndex >= tiles.size())
            throw new IllegalArgumentException("selectedIndex " + selectedIndex + " is out of range.");

        int w = layout.widthPx(), h = layout.heightPx(), zoom = layout.zoom();
        int[] argb = new int[w * h];
        for (int i = 0; i < tiles.size(); i++) {
            int ox = layout.cellX(i), oy = layout.cellY(i);
            for (int ty = 0; ty < 8; ty++) for (int tx = 0; tx < 8; tx++) {
                int color = palette[paletteIndex(tiles.get(i), tx, ty)];
                for (int dy = 0; dy < zoom; dy++) {
                    int row = (oy + ty * zoom + dy) * w + ox + tx * zoom;
                    for (int dx = 0; dx < zoom; dx++) argb[row + dx] = color;
                }
            }
            if (gridOverlay && zoom >= 2) {
                int cell = layout.cellSize();
                for (int k = 0; k < cell; k++) { argb[oy * w + ox + k] = GRID_COLOR; argb[(oy + k) * w + ox] = GRID_COLOR; }
            }
        }
        if (selectedIndex >= 0) {
            int ox = layout.cellX(selectedIndex), oy = layout.cellY(selectedIndex), cell = layout.cellSize();
            int thickness = Math.min(zoom >= 4 ? 2 : 1, cell);
            for (int k = 0; k < cell; k++) for (int t = 0; t < thickness; t++) {
                argb[(oy + t) * w + ox + k] = SELECTION_COLOR;
                argb[(oy + cell - 1 - t) * w + ox + k] = SELECTION_COLOR;
                argb[(oy + k) * w + ox + t] = SELECTION_COLOR;
                argb[(oy + k) * w + ox + cell - 1 - t] = SELECTION_COLOR;
            }
        }
        return new RenderedSheet(w, h, argb);
    }

    static void checkPalette(int[] palette) {
        if (palette == null || palette.length != 4) throw new IllegalArgumentException("Palette must contain exactly 4 colors.");
    }

    /** Returns the validated palette index at (x, y) of an 8x8 tile. */
    static int paletteIndex(int[][] tile, int x, int y) {
        if (tile == null || tile.length != 8 || tile[y] == null || tile[y].length != 8)
            throw new IllegalArgumentException("Tile must be an 8x8 array.");
        int v = tile[y][x];
        if (v < 0 || v > 3) throw new IllegalArgumentException("Pixel value " + v + " is outside 0..3.");
        return v;
    }
}
