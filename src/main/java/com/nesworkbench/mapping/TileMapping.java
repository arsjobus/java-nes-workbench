package com.nesworkbench.mapping;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable visual tile mapping: display position {@code p} shows original CHR tile {@code tileAt(p)}.
 * The mapping is always a permutation of {@code 0..size-1}, so every original tile identity appears
 * exactly once and none can be lost or duplicated. It never touches CHR bytes. No JavaFX dependency.
 */
public final class TileMapping {
    private final int[] entries;

    private TileMapping(int[] entries) { this.entries = entries; }

    /** Every position shows its own tile. */
    public static TileMapping identity(int size) {
        if (size < 0) throw new IllegalArgumentException("size must not be negative.");
        int[] e = new int[size];
        for (int i = 0; i < size; i++) e[i] = i;
        return new TileMapping(e);
    }

    /** Validates that {@code entries} is a permutation of {@code 0..length-1} and copies it. */
    public static TileMapping of(int[] entries) {
        Objects.requireNonNull(entries, "entries must not be null");
        boolean[] seen = new boolean[entries.length];
        for (int p = 0; p < entries.length; p++) {
            int tile = entries[p];
            if (tile < 0 || tile >= entries.length)
                throw new IllegalArgumentException("Position " + p + " maps to tile " + tile + ", outside 0.." + (entries.length - 1) + ".");
            if (seen[tile]) throw new IllegalArgumentException("Tile " + tile + " appears more than once (second time at position " + p + ").");
            seen[tile] = true;
        }
        return new TileMapping(entries.clone());
    }

    public int size() { return entries.length; }

    /** The original tile index shown at a display position. */
    public int tileAt(int position) { check(position); return entries[position]; }

    public boolean isIdentity() {
        for (int i = 0; i < entries.length; i++) if (entries[i] != i) return false;
        return true;
    }

    public int[] toArray() { return entries.clone(); }

    /** Returns a mapping with two positions exchanged; {@code this} if they are the same position. */
    public TileMapping swapped(int a, int b) {
        check(a); check(b);
        if (a == b) return this;
        int[] copy = entries.clone();
        copy[a] = entries[b]; copy[b] = entries[a];
        return new TileMapping(copy);
    }

    /** Returns a mapping with the entry at {@code from} removed and reinserted at {@code to}; {@code this} if equal. */
    public TileMapping moved(int from, int to) {
        check(from); check(to);
        if (from == to) return this;
        int[] copy = entries.clone();
        if (from < to) System.arraycopy(entries, from + 1, copy, from, to - from);
        else System.arraycopy(entries, to, copy, to + 1, from - to);
        copy[to] = entries[from];
        return new TileMapping(copy);
    }

    private void check(int position) {
        if (position < 0 || position >= entries.length)
            throw new IndexOutOfBoundsException("Position " + position + " is outside 0.." + (entries.length - 1));
    }

    @Override public boolean equals(Object o) { return o instanceof TileMapping m && Arrays.equals(entries, m.entries); }
    @Override public int hashCode() { return Arrays.hashCode(entries); }
    @Override public String toString() { return "TileMapping[" + entries.length + " entries" + (isIdentity() ? ", identity" : "") + "]"; }
}
