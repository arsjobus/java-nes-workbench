package com.nesworkbench.graphics;

import com.nesworkbench.mapping.MappingEdit;
import com.nesworkbench.mapping.MappingEditor;
import com.nesworkbench.mapping.TileMapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * UI-independent state for browsing CHR tiles: paging, zoom, grid overlay, selection, and a visual tile mapping.
 * Positions (selection, pages, render) are display slots; {@link #tiles()} always stays in original CHR order and
 * is never changed by mapping edits.
 */
public final class TileBrowserModel {
    public static final int TILES_PER_PAGE = 512; // one 8 KiB CHR bank
    private final List<int[][]> tiles;
    private int page, zoom = 4, selected = -1;
    private boolean gridOverlay;
    private final MappingEditor editor;

    public TileBrowserModel(List<int[][]> tiles) {
        this.tiles = Collections.unmodifiableList(Objects.requireNonNull(tiles, "tiles must not be null"));
        this.editor = new MappingEditor(this.tiles.size());
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

    // ---- visual tile mapping (v0.2) ----

    public TileMapping mapping() { return editor.mapping(); }
    public boolean canUndo() { return editor.canUndo(); }
    public boolean canRedo() { return editor.canRedo(); }
    /** The original CHR tile index shown at a display position. */
    public int originalTileAt(int position) { return editor.mapping().tileAt(position); }
    /** The pixels shown at a display position, after mapping. */
    public int[][] displayedTile(int position) { return tiles.get(originalTileAt(position)); }

    /** Swaps the selected position with another. The selection follows the selected tile to its new position. Returns false if nothing changed. */
    public boolean swapSelectedWith(int position) {
        requireSelection();
        boolean changed = editor.swap(selected, position); // throws before any change if position is invalid
        followTo(position);
        return changed;
    }

    /** Moves the selected entry to a position, shifting the entries between. The selection follows it. Returns false if nothing changed. */
    public boolean moveSelectedTo(int position) {
        requireSelection();
        boolean changed = editor.move(selected, position);
        followTo(position);
        return changed;
    }

    /** Moves the selected entry by an offset. Returns false, changing nothing, if that would leave the tile list. */
    public boolean moveSelectedBy(int delta) {
        requireSelection();
        long target = (long) selected + delta;
        if (target < 0 || target >= tiles.size()) return false;
        return moveSelectedTo((int) target);
    }

    /** Reverses the latest mapping edit and selects the position it affected. */
    public void undo() { followTo(editor.undo().focus()); }

    /** Reapplies the latest undone mapping edit and selects the position it affected. */
    public void redo() { followTo(editor.redo().focus()); }

    /** Replaces the mapping (for example from a project file), clearing undo history and the selection. */
    public void replaceMapping(TileMapping newMapping) {
        Objects.requireNonNull(newMapping, "newMapping must not be null");
        if (newMapping.size() != tiles.size())
            throw new IllegalArgumentException("Mapping has " + newMapping.size() + " entries, but there are " + tiles.size() + " tiles.");
        editor.reset(newMapping);
        selected = -1;
    }

    private void requireSelection() { if (selected < 0) throw new IllegalStateException("No tile is selected."); }

    /** Selects a position and shows its page without clearing the selection. */
    private void followTo(int position) { selected = position; page = position / TILES_PER_PAGE; }

    public TileSheetRenderer.RenderedSheet render() {
        int first = firstTileOfPage();
        List<int[][]> pageTiles = new ArrayList<>(tilesOnPage());
        for (int p = first; p < first + tilesOnPage(); p++) pageTiles.add(displayedTile(p));
        int local = selected >= first && selected < first + tilesOnPage() ? selected - first : -1;
        return TileSheetRenderer.render(pageTiles, layout(), TileSheetRenderer.GRAYSCALE, local, gridOverlay);
    }

    private TileGridLayout layoutFor(int z) { return new TileGridLayout(tilesOnPage(), TileGridLayout.DEFAULT_COLUMNS, z); }
}
