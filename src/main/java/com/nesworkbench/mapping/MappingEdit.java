package com.nesworkbench.mapping;

/** One reversible change to a {@link TileMapping}. Positions are display slots, never CHR byte offsets. */
public sealed interface MappingEdit {
    TileMapping apply(TileMapping mapping);
    MappingEdit inverse();
    /** Display position to select after this edit has been applied. */
    int focus();

    /** Exchanges two display positions. Undoing selects {@code a}, where the swapped-from tile returns. */
    record Swap(int a, int b) implements MappingEdit {
        @Override public TileMapping apply(TileMapping m) { return m.swapped(a, b); }
        @Override public MappingEdit inverse() { return new Swap(b, a); }
        @Override public int focus() { return b; }
    }

    /** Removes the entry at {@code from} and reinserts it at {@code to}, shifting the entries between. */
    record Move(int from, int to) implements MappingEdit {
        @Override public TileMapping apply(TileMapping m) { return m.moved(from, to); }
        @Override public MappingEdit inverse() { return new Move(to, from); }
        @Override public int focus() { return to; }
    }
}
