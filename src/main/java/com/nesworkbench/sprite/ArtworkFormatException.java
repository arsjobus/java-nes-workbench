package com.nesworkbench.sprite;

/** Thrown when an imported PNG is not valid artwork for a composition: wrong format, size, or colors. */
public final class ArtworkFormatException extends Exception {
    private static final long serialVersionUID = 1L;
    public ArtworkFormatException(String message) { super(message); }
}
