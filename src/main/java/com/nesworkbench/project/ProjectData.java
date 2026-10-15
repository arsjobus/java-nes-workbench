package com.nesworkbench.project;

import com.nesworkbench.mapping.TileMapping;
import com.nesworkbench.rom.RomImage;
import com.nesworkbench.sprite.SpriteComposition;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The persisted state of a project: which ROM it belongs to (by name and SHA-256), the tile mapping, and any named
 * sprite compositions (which reference original CHR tile indices). The ROM itself is never stored or modified.
 * No JavaFX dependency.
 */
public record ProjectData(String romName, String romSha256, TileMapping mapping, List<SpriteComposition> compositions) {
    public static final int MAX_COMPOSITIONS = 256;

    /** Compositions must have unique names and number at most {@value #MAX_COMPOSITIONS}. */
    public ProjectData {
        Objects.requireNonNull(romName, "romName must not be null");
        Objects.requireNonNull(romSha256, "romSha256 must not be null");
        Objects.requireNonNull(mapping, "mapping must not be null");
        Objects.requireNonNull(compositions, "compositions must not be null");
        compositions = List.copyOf(compositions);
        if (compositions.size() > MAX_COMPOSITIONS)
            throw new IllegalArgumentException("A project holds at most " + MAX_COMPOSITIONS + " compositions, got " + compositions.size() + ".");
        Set<String> names = new HashSet<>();
        for (SpriteComposition c : compositions)
            if (!names.add(c.name())) throw new IllegalArgumentException("Duplicate composition name '" + c.name() + "'.");
    }

    /** A project without compositions. */
    public ProjectData(String romName, String romSha256, TileMapping mapping) { this(romName, romSha256, mapping, List.of()); }

    public static ProjectData forRom(String romName, RomImage rom, TileMapping mapping) { return forRom(romName, rom, mapping, List.of()); }

    public static ProjectData forRom(String romName, RomImage rom, TileMapping mapping, List<SpriteComposition> compositions) {
        Objects.requireNonNull(rom, "rom must not be null");
        return new ProjectData(romName, sha256Hex(rom.originalBytes()), mapping, compositions);
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
        for (SpriteComposition c : compositions)
            if (c.maxSourceTile() >= tiles)
                throw new ProjectFormatException("Composition '" + c.name() + "' references tile " + c.maxSourceTile() + ", but this ROM has " + tiles + " tiles.");
    }

    public static String sha256Hex(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required on every Java platform.", e);
        }
    }
}
