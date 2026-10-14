package com.nesworkbench.rom;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class RomLoaderTest {
    @TempDir Path dir;

    @Test void loadsValidRomAndLeavesSourceUnchanged() throws Exception {
        byte[] bytes = RomFixtures.make(1, 1, 0, 0); bytes[16 + 16384] = 0x7E;
        Path file = dir.resolve("test.nes"); Files.write(file, bytes);
        RomImage image = RomLoader.load(file);
        assertEquals(8192, image.chrRomSize()); assertEquals((byte) 0x7E, image.chrRom()[0]);
        assertArrayEquals(bytes, Files.readAllBytes(file));
    }
    @Test void rejectsMissingFile() { assertThrows(IOException.class, () -> RomLoader.load(dir.resolve("missing.nes"))); }
    @Test void rejectsDirectory() { assertThrows(IOException.class, () -> RomLoader.load(dir)); }
    @Test void propagatesFormatErrorsForTruncatedFiles() throws Exception {
        Path file = dir.resolve("short.nes"); Files.write(file, java.util.Arrays.copyOf(RomFixtures.make(1, 1, 0, 0), 100));
        assertThrows(RomFormatException.class, () -> RomLoader.load(file));
    }
    @Test void propagatesNes20Rejection() throws Exception {
        Path file = dir.resolve("v2.nes"); Files.write(file, RomFixtures.make(1, 1, 0, 8));
        assertThrows(RomFormatException.class, () -> RomLoader.load(file));
    }
    @Test void rejectsFilesLargerThanAnyValidRomWithoutReadingThem() throws Exception {
        Path file = dir.resolve("huge.nes");
        try (RandomAccessFile f = new RandomAccessFile(file.toFile(), "rw")) { f.setLength(RomLoader.MAX_ROM_BYTES + 1); }
        assertThrows(RomFormatException.class, () -> RomLoader.load(file));
    }
    @Test void acceptsFileExactlyAtMaximumDeclaredSize() throws Exception {
        byte[] bytes = RomFixtures.make(255, 255, 4, 0); Path file = dir.resolve("max.nes"); Files.write(file, bytes);
        assertEquals(RomLoader.MAX_ROM_BYTES, bytes.length);
        assertEquals(255 * 8192, RomLoader.load(file).chrRomSize());
    }
    @Test void rejectsNullPath() { assertThrows(NullPointerException.class, () -> RomLoader.load(null)); }
}
