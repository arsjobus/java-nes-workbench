package com.nesworkbench.sprite;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Pixel-level comparison of imported artwork against the source tiles a composition references.
 * Empty cells have no source tile and count as blank (index 0), so any other pixel drawn there is a change.
 * Comparing never modifies anything. No JavaFX dependency.
 */
public final class ArtworkComparison {
    public static final int MAX_REPORT_CELLS = 100;

    /** One composition cell: where it is, its source tile ({@link SpriteComposition#EMPTY} if none), and how many of its 64 pixels differ. */
    public record CellDiff(int cellX, int cellY, int sourceTile, int changedPixels) {
        public boolean changed() { return changedPixels > 0; }
    }

    private final String compositionName;
    private final int width, height;
    private final boolean[][] changed; // [y][x]
    private final List<CellDiff> cells;
    private final int totalChanged;

    private ArtworkComparison(String name, int width, int height, boolean[][] changed, List<CellDiff> cells, int totalChanged) {
        this.compositionName = name; this.width = width; this.height = height; this.changed = changed; this.cells = List.copyOf(cells); this.totalChanged = totalChanged;
    }

    public static ArtworkComparison compare(SpriteComposition composition, List<int[][]> sourceTiles, ImportedArtwork artwork) {
        Objects.requireNonNull(composition, "composition must not be null");
        Objects.requireNonNull(artwork, "artwork must not be null");
        if (artwork.widthPixels() != composition.widthPixels() || artwork.heightPixels() != composition.heightPixels())
            throw new IllegalArgumentException("Artwork is " + artwork.widthPixels() + "x" + artwork.heightPixels() + " pixels, but composition '"
                    + composition.name() + "' is " + composition.widthPixels() + "x" + composition.heightPixels() + ".");
        int[][] source = composition.toPixels(sourceTiles); // validates references
        int w = composition.widthPixels(), h = composition.heightPixels(), total = 0;
        boolean[][] changed = new boolean[h][w];
        List<CellDiff> cells = new ArrayList<>(composition.cellCount());
        for (int cy = 0; cy < composition.heightTiles(); cy++) for (int cx = 0; cx < composition.widthTiles(); cx++) {
            int n = 0;
            for (int y = cy * 8; y < cy * 8 + 8; y++) for (int x = cx * 8; x < cx * 8 + 8; x++)
                if (source[y][x] != artwork.pixel(x, y)) { changed[y][x] = true; n++; }
            total += n;
            cells.add(new CellDiff(cx, cy, composition.tileAt(cx, cy), n));
        }
        return new ArtworkComparison(composition.name(), w, h, changed, cells, total);
    }

    public int widthPixels() { return width; }
    public int heightPixels() { return height; }
    public int totalPixels() { return width * height; }
    public int changedPixels() { return totalChanged; }
    public boolean identical() { return totalChanged == 0; }
    /** All cells, row-major, including unchanged ones. */
    public List<CellDiff> cells() { return cells; }
    public int changedCellCount() { return (int) cells.stream().filter(CellDiff::changed).count(); }

    public boolean pixelChanged(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height)
            throw new IndexOutOfBoundsException("Pixel (" + x + ", " + y + ") is outside the " + width + "x" + height + " comparison.");
        return changed[y][x];
    }

    /** A plain-text summary: totals first, then the changed cells (at most {@value #MAX_REPORT_CELLS}). */
    public String report() {
        StringBuilder sb = new StringBuilder("Comparison for composition '").append(compositionName).append("'\n");
        if (identical()) return sb.append("Imported artwork is identical to the source tiles (").append(totalPixels()).append(" pixels).\n").toString();
        sb.append(String.format(Locale.ROOT, "%d of %d pixels differ (%.1f%%) in %d of %d cells.%n",
                totalChanged, totalPixels(), 100.0 * totalChanged / totalPixels(), changedCellCount(), cells.size()));
        int listed = 0, changedCells = changedCellCount();
        for (CellDiff c : cells) {
            if (!c.changed()) continue;
            if (listed == MAX_REPORT_CELLS) { sb.append("... and ").append(changedCells - listed).append(" more changed cells.\n"); break; }
            String what = c.sourceTile() == SpriteComposition.EMPTY ? "empty cell, " + c.changedPixels() + " pixel(s) drawn"
                    : String.format(Locale.ROOT, "source tile %d ($%X), %d pixel(s) changed", c.sourceTile(), c.sourceTile(), c.changedPixels());
            sb.append("Cell (").append(c.cellX()).append(", ").append(c.cellY()).append("): ").append(what).append('\n');
            listed++;
        }
        return sb.toString();
    }
}
