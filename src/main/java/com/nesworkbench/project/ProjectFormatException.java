package com.nesworkbench.project;

/** Thrown when a project file is malformed, from an unsupported version, or does not match the ROM. */
public final class ProjectFormatException extends Exception {
    private static final long serialVersionUID = 1L;
    public ProjectFormatException(String message) { super(message); }
}
