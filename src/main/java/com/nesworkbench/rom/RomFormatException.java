package com.nesworkbench.rom;

/** Thrown when a ROM is malformed or uses a format not supported by this version. */
public final class RomFormatException extends Exception {
    public RomFormatException(String message) { super(message); }
}
