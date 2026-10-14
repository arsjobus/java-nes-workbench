package com.nesworkbench.project;

import com.nesworkbench.mapping.TileMapping;
import com.nesworkbench.rom.RomImage;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * The persisted state of a project: which ROM it belongs to (by name and SHA-256) and the tile mapping.
 * The ROM itself is never stored or modified. No JavaFX dependency.
 */
public record ProjectData(String romName, String romSha256, TileMapping mapping) {
    public ProjectData {
        Objects.requireNonNull(romName, "romName must not be null");
        Objects.requireNonNull(romSha256, "romSha256 must not be null");
        Objects.requireNonNull(mapping, "mapping must not be null");
    }

    public static ProjectData forRom(String romName, RomImage rom, TileMapping mapping) {
        Objects.requireNonNull(rom, "rom must not be null");
        return new ProjectData(romName, sha256Hex(rom.originalBytes()), mapping);
    }

    /** Rejects a project that was made for a different ROM or whose mapping does not fit this ROM's CHR data. */
    public void verifyAgainst(RomImage rom) throws ProjectFormatException {
        Objects.requireNonNull(rom, "rom must not be null");
        if (rom.usesChrRam()) throw new ProjectFormatException("This ROM uses CHR-RAM and has no CHR tiles to map.");
        if (!romSha256.equals(sha256Hex(rom.originalBytes())))
            throw new ProjectFormatException("This project was saved for a different ROM (" + romName + "): the SHA-256 does not match.");
        int tiles = rom.chrRomSize() / 16;
        if (mapping.size() != tiles)
            throw new ProjectFormatException("Project maps " + mapping.size() + " tiles, but this ROM has " + tiles + ".");
    }

    public static String sha256Hex(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required on every Java platform.", e);
        }
    }
}
