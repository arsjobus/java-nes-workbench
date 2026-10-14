package com.nesworkbench.graphics;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** UI-independent state for browsing CHR tiles: paging, zoom, grid overlay, and selection. */
public final class TileBrowserModel {
    public static final int TILES_PER_PAGE = 512; // one 8 KiB CHR bank
    private final List<int[][]> tiles;
    private int page, zoom = 4, selected = -1;
    private boolean gridOverlay;

    public TileBrowserModel(List<int[][]> tiles) {
        this.tiles = Collections.unmodifiableList(Objects.requireNonNull(tiles, "tiles must not be null"));
    }

    public List<int[][]> tiles() { return tiles; }
    public int tileCount() { return tiles.size(); }
    public int pageCount() { return (tiles.size() + TILES_PER_PAGE - 1) / TILES_PER_PAGE; }
    public int page() { return page; }
    public int zoom() { return zoom; }
    public boolean gridOverlay() { return gridOverlay; }
    /** Absolute tile index across all pages, or -1 when nothing is selected. */
    public int selectedTile() { return selected; }
    public int firstTileOfPage() { return page * TILES_PER_PAGE; }
    public int tilesOnPage() { return Math.min(TILES_PER_PAGE, tiles.size() - firstTileOfPage()); }

    /** Changing page clears the selection. */
    public void setPage(int newPage) {
        if (newPage < 0 || newPage >= Math.max(1, pageCount())) throw new IllegalArgumentException("Page " + newPage + " does not exist.");
        if (newPage != page) { page = newPage; selected = -1; }
    }

    public void setZoom(int newZoom) { layoutFor(newZoom); zoom = newZoom; }
    public void setGridOverlay(boolean on) { gridOverlay = on; }
    public void clearSelection() { selected = -1; }

    public void select(int absoluteIndex) {
        if (absoluteIndex < firstTileOfPage() || absoluteIndex >= firstTileOfPage() + tilesOnPage())
            throw new IllegalArgumentException("Tile " + absoluteIndex + " is not on the current page.");
        selected = absoluteIndex;
    }

    /** Selects the tile under an image pixel on the current page. Returns its absolute index, or -1 (selection unchanged) if none. */
    public int selectAt(int x, int y) {
        int local = layout().tileIndexAt(x, y);
        if (local < 0) return -1;
        selected = firstTileOfPage() + local;
        return selected;
    }

    public TileGridLayout layout() { return layoutFor(zoom); }

    public TileSheetRenderer.RenderedSheet render() {
        int first = firstTileOfPage();
        List<int[][]> pageTiles = tiles.subList(first, first + tilesOnPage());
        int local = selected >= first && selected < first + tilesOnPage() ? selected - first : -1;
        return TileSheetRenderer.render(pageTiles, layout(), TileSheetRenderer.GRAYSCALE, local, gridOverlay);
    }

    private TileGridLayout layoutFor(int z) { return new TileGridLayout(tilesOnPage(), TileGridLayout.DEFAULT_COLUMNS, z); }
}
