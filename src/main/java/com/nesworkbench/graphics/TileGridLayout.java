package com.nesworkbench.graphics;

/** Immutable geometry of a tile grid: where each 8x8 tile sits at a given zoom. No JavaFX dependency. */
public final class TileGridLayout {
    public static final int TILE_SIZE = 8, DEFAULT_COLUMNS = 16, MIN_ZOOM = 1, MAX_ZOOM = 8;
    /** Upper bound on rendered image area, to fail fast instead of exhausting memory. */
    public static final long MAX_IMAGE_PIXELS = 64_000_000L;

    private final int tileCount, columns, zoom;

    public TileGridLayout(int tileCount, int columns, int zoom) {
        if (tileCount < 0) throw new IllegalArgumentException("tileCount must not be negative.");
        if (columns < 1) throw new IllegalArgumentException("columns must be at least 1.");
        if (zoom < MIN_ZOOM || zoom > MAX_ZOOM)
            throw new IllegalArgumentException("zoom must be between " + MIN_ZOOM + " and " + MAX_ZOOM + ".");
        this.tileCount = tileCount; this.columns = columns; this.zoom = zoom;
        if ((long) widthPx() * heightPx() > MAX_IMAGE_PIXELS)
            throw new IllegalArgumentException("Tile sheet would exceed " + MAX_IMAGE_PIXELS + " pixels.");
    }

    public int tileCount() { return tileCount; }
    public int columns() { return columns; }
    public int zoom() { return zoom; }
    public int cellSize() { return TILE_SIZE * zoom; }
    public int rows() { return tileCount == 0 ? 0 : (tileCount + columns - 1) / columns; }
    public int widthPx() { return (int) Math.min((long) Math.min(columns, tileCount) * cellSize(), Integer.MAX_VALUE); }
    public int heightPx() { return (int) Math.min((long) rows() * cellSize(), Integer.MAX_VALUE); }

    public TileGridLayout withZoom(int newZoom) { return new TileGridLayout(tileCount, columns, newZoom); }

    /** Returns the tile index under the pixel, or -1 if outside the image or in the empty tail of the last row. */
    public int tileIndexAt(int x, int y) {
        if (x < 0 || y < 0 || x >= widthPx() || y >= heightPx()) return -1;
        int index = (y / cellSize()) * columns + (x / cellSize());
        return index < tileCount ? index : -1;
    }

    public int cellX(int index) { checkIndex(index); return (index % columns) * cellSize(); }
    public int cellY(int index) { checkIndex(index); return (index / columns) * cellSize(); }

    private void checkIndex(int index) {
        if (index < 0 || index >= tileCount) throw new IndexOutOfBoundsException("Tile index " + index + " outside 0.." + (tileCount - 1));
    }
}
