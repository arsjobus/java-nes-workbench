package com.nesworkbench.rebuild;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class RomWriterTest {
    @TempDir Path dir;

    @Test void createsANewFileWithExactlyTheGivenBytes() throws Exception {
        Path src = dir.resolve("a.nes"), out = dir.resolve("b.nes"); Files.write(src, new byte[]{1});
        byte[] bytes = {9, 8, 7};
        RomWriter.writeNew(src, out, bytes);
        assertArrayEquals(bytes, Files.readAllBytes(out)); assertArrayEquals(new byte[]{1}, Files.readAllBytes(src));
    }
    @Test void refusesToReplaceAnExistingFileAndLeavesItIntact() throws Exception {
        Path src = dir.resolve("a.nes"), out = dir.resolve("b.nes"); Files.write(src, new byte[]{1}); Files.write(out, new byte[]{5, 5});
        assertThrows(FileAlreadyExistsException.class, () -> RomWriter.writeNew(src, out, new byte[]{9}));
        assertArrayEquals(new byte[]{5, 5}, Files.readAllBytes(out));
    }
    @Test void refusesTheSourceRomItself() throws Exception {
        Path src = dir.resolve("a.nes"); Files.write(src, new byte[]{1, 2, 3});
        assertThrows(RebuildException.class, () -> RomWriter.writeNew(src, src, new byte[]{9}));
        assertArrayEquals(new byte[]{1, 2, 3}, Files.readAllBytes(src));
    }
    @Test void refusesTheSourceRomSpelledDifferently() throws Exception {
        Path src = dir.resolve("a.nes"); Files.write(src, new byte[]{1, 2, 3});
        Path alias = dir.resolve("sub").resolve("..").resolve("a.nes"); Files.createDirectory(dir.resolve("sub"));
        assertThrows(RebuildException.class, () -> RomWriter.writeNew(src, alias, new byte[]{9}));
        assertArrayEquals(new byte[]{1, 2, 3}, Files.readAllBytes(src));
    }
    @Test void refusesASymbolicLinkToTheSource() throws Exception {
        Path src = dir.resolve("a.nes"); Files.write(src, new byte[]{1, 2, 3});
        Path link = dir.resolve("link.nes");
        try { Files.createSymbolicLink(link, src); } catch (UnsupportedOperationException | IOException e) { return; /* platform without symlinks */ }
        assertThrows(RebuildException.class, () -> RomWriter.writeNew(src, link, new byte[]{9}));
        assertArrayEquals(new byte[]{1, 2, 3}, Files.readAllBytes(src));
    }
    @Test void missingFolderFailsWithoutCreatingAnything() throws Exception {
        Path src = dir.resolve("a.nes"); Files.write(src, new byte[]{1});
        Path out = dir.resolve("nope").resolve("b.nes");
        assertThrows(IOException.class, () -> RomWriter.writeNew(src, out, new byte[]{9}));
        assertFalse(Files.exists(dir.resolve("nope")));
    }
    @Test void worksWhenTheSourceFileNoLongerExists() throws Exception {
        Path out = dir.resolve("b.nes");
        RomWriter.writeNew(dir.resolve("gone.nes"), out, new byte[]{4});
        assertArrayEquals(new byte[]{4}, Files.readAllBytes(out));
    }
    @Test void rejectsNulls() {
        assertThrows(NullPointerException.class, () -> RomWriter.writeNew(null, dir.resolve("x"), new byte[0]));
        assertThrows(NullPointerException.class, () -> RomWriter.writeNew(dir.resolve("a"), null, new byte[0]));
        assertThrows(NullPointerException.class, () -> RomWriter.writeNew(dir.resolve("a"), dir.resolve("x"), null));
    }
}
