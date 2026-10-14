package com.nesworkbench.mapping;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/** Holds the current {@link TileMapping} and its bounded undo/redo history. No JavaFX dependency. */
public final class MappingEditor {
    /** Oldest undo steps are discarded beyond this many, to bound memory. */
    public static final int MAX_HISTORY = 1000;

    private TileMapping current;
    private final Deque<MappingEdit> undoStack = new ArrayDeque<>(), redoStack = new ArrayDeque<>();

    public MappingEditor(int tileCount) { this(TileMapping.identity(tileCount)); }
    public MappingEditor(TileMapping initial) { current = Objects.requireNonNull(initial, "initial must not be null"); }

    public TileMapping mapping() { return current; }
    public boolean canUndo() { return !undoStack.isEmpty(); }
    public boolean canRedo() { return !redoStack.isEmpty(); }
    public int undoDepth() { return undoStack.size(); }
    public int redoDepth() { return redoStack.size(); }

    /** Swaps two positions. Returns false (and records nothing) when they are the same. Throws on invalid positions. */
    public boolean swap(int a, int b) { return perform(new MappingEdit.Swap(a, b)); }

    /** Moves one entry to a new position. Returns false (and records nothing) when it would not move. */
    public boolean move(int from, int to) { return perform(new MappingEdit.Move(from, to)); }

    /** Reverses the latest edit and returns the edit that was applied to do so. */
    public MappingEdit undo() {
        if (undoStack.isEmpty()) throw new IllegalStateException("Nothing to undo.");
        MappingEdit edit = undoStack.pop();
        MappingEdit reverse = edit.inverse();
        current = reverse.apply(current);
        redoStack.push(edit);
        return reverse;
    }

    /** Reapplies the latest undone edit and returns it. */
    public MappingEdit redo() {
        if (redoStack.isEmpty()) throw new IllegalStateException("Nothing to redo.");
        MappingEdit edit = redoStack.pop();
        current = edit.apply(current);
        undoStack.push(edit);
        return edit;
    }

    /** Replaces the mapping (for example when loading a project) and clears all history. */
    public void reset(TileMapping mapping) {
        current = Objects.requireNonNull(mapping, "mapping must not be null");
        undoStack.clear(); redoStack.clear();
    }

    private boolean perform(MappingEdit edit) {
        TileMapping next = edit.apply(current); // throws before any state changes if a position is invalid
        if (next == current) return false;
        current = next;
        undoStack.push(edit);
        if (undoStack.size() > MAX_HISTORY) undoStack.removeLast();
        redoStack.clear();
        return true;
    }
}
