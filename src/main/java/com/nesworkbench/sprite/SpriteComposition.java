package com.nesworkbench.sprite;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * An immutable, named grid of source-tile references: cell (x, y) shows the <em>original</em> CHR tile
 * {@code tileAt(x, y)}, or {@link #EMPTY}. References are original tile identities, never display positions of a
 * {@link com.nesworkbench.mapping.TileMapping}, so reordering the visual mapping cannot change what a composition
 * contains. A composition never touches CHR bytes. No JavaFX dependency.
 */
public final class SpriteComposition {
    public static final int EMPTY = -1;
    /** Size limits, in tiles. 32 tiles is 256 pixels, the NES screen width. */
    public static final int MIN_DIMENSION = 1, MAX_DIMENSION = 32;
    public static final int MAX_NAME_LENGTH = 64;
    public static final int TILE_SIZE = 8;

    private final String name;
    private final int width, height;
    private final int[] cells; // row-major source tile indices, EMPTY for no tile

    private SpriteComposition(String name, int width, int height, int[] cells) {
        this.name = name; this.width = width; this.height = height; this.cells = cells;
    }

    /** A composition with every cell empty. */
    public static SpriteComposition empty(String name, int widthTiles, int heightTiles) {
        checkName(name); checkSize(widthTiles, heightTiles);
        int[] cells = new int[widthTiles * heightTiles];
        Arrays.fill(cells, EMPTY);
        return new SpriteComposition(name, widthTiles, heightTiles, cells);
    }

    /** Validates and copies row-major cell references ({@link #EMPTY} or a tile index of at least 0). */
    public static SpriteComposition of(String name, int widthTiles, int heightTiles, int[] cells) {
        checkName(name); checkSize(widthTiles, heightTiles);
        Objects.requireNonNull(cells, "cells must not be null");
        if (cells.length != widthTiles * heightTiles)
            throw new IllegalArgumentException("Expected " + widthTiles * heightTiles + " cells for " + widthTiles + "x" + heightTiles + " tiles, got " + cells.length + ".");
        for (int i = 0; i < cells.length; i++)
            if (cells[i] < EMPTY) throw new IllegalArgumentException("Cell " + i + " holds invalid tile reference " + cells[i] + ".");
        return new SpriteComposition(name, widthTiles, heightTiles, cells.clone());
    }

    /**
     * Names must be non-empty, at most {@value #MAX_NAME_LENGTH} characters, free of leading/trailing whitespace,
     * control characters and {@code |} (the project file field separator), and valid Unicode text.
     */
    public static void checkName(String name) {
        Objects.requireNonNull(name, "name must not be null");
        if (name.isEmpty()) throw new IllegalArgumentException("Composition name must not be empty.");
        if (!name.equals(name.strip())) throw new IllegalArgumentException("Composition name must not start or end with whitespace.");
        if (name.length() > MAX_NAME_LENGTH) throw new IllegalArgumentException("Composition name is longer than " + MAX_NAME_LENGTH + " characters.");
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (Character.isISOControl(ch)) throw new IllegalArgumentException("Composition name must not contain control characters.");
            if (ch == '|') throw new IllegalArgumentException("Composition name must not contain '|'.");
        }
        if (!name.equals(new String(name.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8)))
            throw new IllegalArgumentException("Composition name is not valid Unicode text.");
    }

    private static void checkSize(int w, int h) {
        if (w < MIN_DIMENSION || w > MAX_DIMENSION || h < MIN_DIMENSION || h > MAX_DIMENSION)
            throw new IllegalArgumentException("Composition size must be between " + MIN_DIMENSION + "x" + MIN_DIMENSION
                    + " and " + MAX_DIMENSION + "x" + MAX_DIMENSION + " tiles, got " + w + "x" + h + ".");
    }

    public String name() { return name; }
    public int widthTiles() { return width; }
    public int heightTiles() { return height; }
    public int widthPixels() { return width * TILE_SIZE; }
    public int heightPixels() { return height * TILE_SIZE; }
    public int cellCount() { return cells.length; }
    public int[] cells() { return cells.clone(); }

    /** The original tile shown in a cell, or {@link #EMPTY}. */
    public int tileAt(int x, int y) { return cells[indexOf(x, y)]; }

    public int placedTileCount() { int n = 0; for (int c : cells) if (c != EMPTY) n++; return n; }

    /** The highest source tile index referenced, or {@link #EMPTY} when no cell is filled. */
    public int maxSourceTile() { int max = EMPTY; for (int c : cells) max = Math.max(max, c); return max; }

    /** Returns a copy with a source tile placed in a cell; {@code this} if the cell already holds it. */
    public SpriteComposition withTile(int x, int y, int sourceTile) {
        if (sourceTile < 0) throw new IllegalArgumentException("Source tile index must not be negative.");
        return with(indexOf(x, y), sourceTile);
    }

    /** Returns a copy with a cell emptied; {@code this} if it already was. */
    public SpriteComposition cleared(int x, int y) { return with(indexOf(x, y), EMPTY); }

    public SpriteComposition renamed(String newName) { checkName(newName); return new SpriteComposition(newName, width, height, cells); }

    /**
     * The composed cells as 8x8 pixel tiles in row-major order, copied from {@code sourceTiles}; empty cells become
     * blank (index 0) tiles. Fails if a cell references a tile that does not exist.
     */
    public List<int[][]> toTiles(List<int[][]> sourceTiles) {
        Objects.requireNonNull(sourceTiles, "sourceTiles must not be null");
        if (maxSourceTile() >= sourceTiles.size())
            throw new IllegalArgumentException("Composition '" + name + "' references tile " + maxSourceTile() + ", but only " + sourceTiles.size() + " tiles exist.");
        List<int[][]> tiles = new ArrayList<>(cells.length);
        for (int c : cells) tiles.add(c == EMPTY ? new int[TILE_SIZE][TILE_SIZE] : copyTile(sourceTiles.get(c), c));
        return tiles;
    }

    /** The composed image as palette indices, {@code [heightPixels][widthPixels]}. */
    public int[][] toPixels(List<int[][]> sourceTiles) {
        List<int[][]> tiles = toTiles(sourceTiles);
        int[][] pixels = new int[heightPixels()][widthPixels()];
        for (int i = 0; i < tiles.size(); i++) {
            int ox = (i % width) * TILE_SIZE, oy = (i / width) * TILE_SIZE;
            for (int y = 0; y < TILE_SIZE; y++) System.arraycopy(tiles.get(i)[y], 0, pixels[oy + y], ox, TILE_SIZE);
        }
        return pixels;
    }

    private SpriteComposition with(int index, int value) {
        if (cells[index] == value) return this;
        int[] copy = cells.clone();
        copy[index] = value;
        return new SpriteComposition(name, width, height, copy);
    }

    private int indexOf(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height)
            throw new IndexOutOfBoundsException("Cell (" + x + ", " + y + ") is outside the " + width + "x" + height + " composition.");
        return y * width + x;
    }

    private static int[][] copyTile(int[][] tile, int index) {
        if (tile == null || tile.length != TILE_SIZE) throw new IllegalArgumentException("Source tile " + index + " is not an 8x8 array.");
        int[][] copy = new int[TILE_SIZE][];
        for (int y = 0; y < TILE_SIZE; y++) {
            if (tile[y] == null || tile[y].length != TILE_SIZE) throw new IllegalArgumentException("Source tile " + index + " is not an 8x8 array.");
            for (int v : tile[y]) if (v < 0 || v > 3) throw new IllegalArgumentException("Source tile " + index + " has pixel value " + v + " outside 0..3.");
            copy[y] = tile[y].clone();
        }
        return copy;
    }

    @Override public boolean equals(Object o) {
        return o instanceof SpriteComposition c && name.equals(c.name) && width == c.width && height == c.height && Arrays.equals(cells, c.cells);
    }
    @Override public int hashCode() { return Objects.hash(name, width, height) * 31 + Arrays.hashCode(cells); }
    @Override public String toString() { return "SpriteComposition[" + name + ", " + width + "x" + height + " tiles, " + placedTileCount() + " placed]"; }
}
