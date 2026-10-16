package com.nesworkbench.rebuild;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/** Writes ROM bytes to a file that must not exist yet. There is deliberately no overwrite option. */
public final class RomWriter {
    private RomWriter() {}

    /**
     * Creates {@code output} with {@code bytes}. Refuses (leaving the existing file untouched) when {@code output} already
     * exists, and refuses with a {@link RebuildException} when it is the source ROM itself, however the path is spelled.
     * A write that fails part-way removes the partial file it created.
     */
    public static void writeNew(Path sourcePath, Path output, byte[] bytes) throws IOException, RebuildException {
        Objects.requireNonNull(sourcePath, "sourcePath must not be null");
        Objects.requireNonNull(output, "output must not be null");
        Objects.requireNonNull(bytes, "bytes must not be null");
        if (Files.exists(output, LinkOption.NOFOLLOW_LINKS)) {
            if (Files.exists(sourcePath) && Files.isSameFile(sourcePath, output))
                throw new RebuildException("Refusing to overwrite the source ROM. Choose a different output file.");
            throw new FileAlreadyExistsException(output.toString(), null, "Output file already exists; choose a new name.");
        }
        try {
            Files.write(output, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (IOException e) {
            try { Files.deleteIfExists(output); } catch (IOException ignored) { /* best effort */ }
            throw e;
        }
    }
}
