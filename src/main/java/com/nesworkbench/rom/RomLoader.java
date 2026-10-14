package com.nesworkbench.rom;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** Reads a ROM file from disk for parsing. Only reads; never writes. */
public final class RomLoader {
    /** Larger than any valid iNES 1.0 file (16 + 512 + 255*16 KiB + 255*8 KiB), so bad inputs fail before being read. */
    public static final long MAX_ROM_BYTES = 16L + 512 + 255L * 16384 + 255L * 8192;
    private RomLoader() {}

    public static RomImage load(Path path) throws IOException, RomFormatException {
        Objects.requireNonNull(path, "path must not be null");
        if (!Files.isRegularFile(path)) throw new IOException("Not a readable file: " + path);
        long size = Files.size(path);
        if (size > MAX_ROM_BYTES) throw new RomFormatException("File is " + size + " bytes, larger than any valid iNES 1.0 ROM.");
        return RomImage.parse(Files.readAllBytes(path));
    }
}
