package com.nesworkbench.rebuild;

/** Thrown when a ROM rebuild is refused or its output fails validation. */
public final class RebuildException extends Exception {
    public RebuildException(String message) { super(message); }
    public RebuildException(String message, Throwable cause) { super(message, cause); }
}
