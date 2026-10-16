package com.nesworkbench.rebuild;

import com.nesworkbench.graphics.ChrTileEncoder;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeMap;

/**
 * An immutable set of CHR byte edits: replacement 16-byte data for original CHR tiles, keyed by <em>original</em>
 * tile index. This is deliberately unrelated to {@code TileMapping}: a mapping only changes what is displayed, while
 * these edits are the only thing that can change CHR bytes in a rebuilt ROM. No JavaFX dependency.
 */
public final class ChrEdits {
    public static final int BYTES_PER_TILE = 16;
    private static final ChrEdits EMPTY = new ChrEdits(new TreeMap<>());

    private final TreeMap<Integer, byte[]> tiles; // never exposed; values are never mutated

    private ChrEdits(TreeMap<Integer, byte[]> tiles) { this.tiles = tiles; }

    public static ChrEdits empty() { return EMPTY; }

    /** Builds edits from many tiles at once. Validates every entry and copies the data. */
    public static ChrEdits of(Map<Integer, byte[]> edits) {
        Objects.requireNonNull(edits, "edits must not be null");
        TreeMap<Integer, byte[]> copy = new TreeMap<>();
        for (Map.Entry<Integer, byte[]> e : edits.entrySet()) copy.put(checkIndex(e.getKey()), checkData(e.getValue()).clone());
        return copy.isEmpty() ? EMPTY : new ChrEdits(copy);
    }

    /** Returns edits that also replace {@code tileIndex} with {@code data} (replacing any earlier edit of that tile). */
    public ChrEdits withTile(int tileIndex, byte[] data) {
        checkIndex(tileIndex); checkData(data);
        TreeMap<Integer, byte[]> copy = new TreeMap<>(tiles);
        copy.put(tileIndex, data.clone());
        return new ChrEdits(copy);
    }

    /** Like {@link #withTile} but encodes 8x8 pixels with indices 0..3. */
    public ChrEdits withPixels(int tileIndex, int[][] pixels) { return withTile(tileIndex, ChrTileEncoder.encode(pixels)); }

    public boolean isEmpty() { return tiles.isEmpty(); }
    public int size() { return tiles.size(); }
    public boolean has(int tileIndex) { return tiles.containsKey(tileIndex); }
    /** Edited original tile indices, ascending. */
    public SortedSet<Integer> tileIndices() { return Collections.unmodifiableSortedSet(tiles.navigableKeySet()); }
    /** Highest edited tile index, or -1 when there are no edits. */
    public int maxTileIndex() { return tiles.isEmpty() ? -1 : tiles.lastKey(); }

    /** A copy of the replacement data for an edited tile. */
    public byte[] tile(int tileIndex) {
        byte[] data = tiles.get(tileIndex);
        if (data == null) throw new IllegalArgumentException("Tile " + tileIndex + " has no edit.");
        return data.clone();
    }

    private static int checkIndex(Integer index) {
        if (index == null || index < 0) throw new IllegalArgumentException("Tile index must not be negative, got " + index + ".");
        return index;
    }

    private static byte[] checkData(byte[] data) {
        Objects.requireNonNull(data, "data must not be null");
        if (data.length != BYTES_PER_TILE) throw new IllegalArgumentException("Tile data must be " + BYTES_PER_TILE + " bytes, got " + data.length + ".");
        return data;
    }

    @Override public boolean equals(Object o) {
        if (!(o instanceof ChrEdits other) || other.tiles.size() != tiles.size()) return false;
        for (Map.Entry<Integer, byte[]> e : tiles.entrySet()) if (!java.util.Arrays.equals(e.getValue(), other.tiles.get(e.getKey()))) return false;
        return true;
    }
    @Override public int hashCode() { int h = 0; for (Map.Entry<Integer, byte[]> e : tiles.entrySet()) h += e.getKey() * 31 + java.util.Arrays.hashCode(e.getValue()); return h; }
    @Override public String toString() { return "ChrEdits[" + tiles.size() + " tile(s)]"; }
}
