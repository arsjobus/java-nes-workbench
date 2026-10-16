package com.nesworkbench.rebuild;

import com.nesworkbench.graphics.ChrTileDecoder;
import com.nesworkbench.project.ProjectData;
import com.nesworkbench.rom.RomFormatException;
import com.nesworkbench.rom.RomImage;
import com.nesworkbench.rom.RomLoader;
import com.nesworkbench.rom.RomSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

/** End-to-end: real files on disk, source -> rebuilt copy -> reopened and validated. */
class RomRebuilderFileTest {
    @TempDir Path dir;
    private static final byte[] ROM = RebuildFixtures.make(2, 1, false, 3);
    private static final int CHR = RebuildFixtures.chrOffset(2, false);

    private Path writeSource() throws Exception { Path p = dir.resolve("game.nes"); Files.write(p, ROM); return p; }

    @Test void rebuildsToANewFileAndTheSourceStaysByteForByteUnchanged() throws Exception {
        Path src = writeSource(), out = dir.resolve("game-rebuilt.nes");
        String before = ProjectData.sha256Hex(Files.readAllBytes(src));
        byte[] edit = RebuildFixtures.tileData(11);
        RomRebuilder.Result r = RomRebuilder.rebuildToFile(RomLoader.load(src), src, ChrEdits.empty().withTile(7, edit), out);
        assertEquals(before, ProjectData.sha256Hex(Files.readAllBytes(src)));
        assertArrayEquals(ROM, Files.readAllBytes(src));
        assertEquals(out, r.output()); assertTrue(Files.isRegularFile(out));
        assertArrayEquals(edit, Arrays.copyOfRange(Files.readAllBytes(out), CHR + 112, CHR + 128));
    }
    @Test void diffReportsExactlyTheEditedTileOffsets() throws Exception {
        Path src = writeSource(), out = dir.resolve("o.nes");
        byte[] edit = RebuildFixtures.tileData(12);
        for (int i = 0; i < 16; i++) if (edit[i] == ROM[CHR + 16 * 3 + i]) edit[i] ^= 0x01; // guarantee every byte differs
        RomRebuilder.Result r = RomRebuilder.rebuildToFile(RomLoader.load(src), src, ChrEdits.empty().withTile(3, edit), out);
        assertEquals(1, r.diff().ranges().size());
        assertEquals(new ByteDiff.Range(CHR + 48, 16), r.diff().ranges().get(0));
        assertEquals(16, r.diff().changedBytes());
        String report = r.diff().report(RomSummary.of(r.reopened()));
        assertTrue(report.contains("CHR-ROM, tile 3 ($3)"), report);
    }
    @Test void reopenedImageDecodesTheEditedPixels() throws Exception {
        Path src = writeSource(), out = dir.resolve("o.nes");
        int[][] px = new int[8][8]; for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++) px[y][x] = (x * 3 + y) % 4;
        RomRebuilder.Result r = RomRebuilder.rebuildToFile(RomLoader.load(src), src, ChrEdits.empty().withPixels(0, px), out);
        assertArrayEquals(px, ChrTileDecoder.decode(r.reopened().chrRom()).get(0));
        assertArrayEquals(px, ChrTileDecoder.decode(RomLoader.load(out).chrRom()).get(0));
    }
    @Test void noEditsGivesAnIdenticalCopyWithAnIdenticalDiff() throws Exception {
        Path src = writeSource(), out = dir.resolve("copy.nes");
        RomRebuilder.Result r = RomRebuilder.rebuildToFile(RomLoader.load(src), src, ChrEdits.empty(), out);
        assertTrue(r.diff().identical()); assertArrayEquals(ROM, Files.readAllBytes(out));
    }
    @Test void refusesToOverwriteAnExistingOutputAndLeavesItUntouched() throws Exception {
        Path src = writeSource(), out = dir.resolve("exists.nes"); Files.write(out, new byte[]{1, 2});
        assertThrows(FileAlreadyExistsException.class, () -> RomRebuilder.rebuildToFile(RomLoader.load(src), src, ChrEdits.empty(), out));
        assertArrayEquals(new byte[]{1, 2}, Files.readAllBytes(out));
    }
    @Test void refusesTheSourcePathAsOutput() throws Exception {
        Path src = writeSource();
        assertThrows(RebuildException.class, () -> RomRebuilder.rebuildToFile(RomLoader.load(src), src, ChrEdits.empty().withTile(0, new byte[16]), src));
        assertArrayEquals(ROM, Files.readAllBytes(src));
    }
    @Test void invalidEditsFailBeforeAnythingIsWritten() throws Exception {
        Path src = writeSource(), out = dir.resolve("never.nes");
        assertThrows(RebuildException.class, () -> RomRebuilder.rebuildToFile(RomLoader.load(src), src, ChrEdits.empty().withTile(512, new byte[16]), out));
        assertFalse(Files.exists(out));
    }
    @Test void malformedOutputIsRejectedAndRemoved() throws Exception {
        Path src = writeSource(), bad = dir.resolve("bad.nes");
        Files.write(bad, Arrays.copyOf(ROM, 40));   // truncated: not a valid ROM
        RomImage source = RomLoader.load(src);
        RebuildException e = assertThrows(RebuildException.class, () -> RomRebuilder.reopenAndVerify(bad, source, ChrEdits.empty()));
        assertInstanceOf(RomFormatException.class, e.getCause());
        assertFalse(Files.exists(bad));
        assertArrayEquals(ROM, Files.readAllBytes(src));
    }
    @Test void wellFormedButWrongOutputIsRejectedAndRemoved() throws Exception {
        Path src = writeSource(), wrong = dir.resolve("wrong.nes");
        byte[] bytes = ROM.clone(); bytes[16] ^= 1;   // PRG byte differs from the source
        Files.write(wrong, bytes);
        RomImage source = RomLoader.load(src);
        assertThrows(RebuildException.class, () -> RomRebuilder.reopenAndVerify(wrong, source, ChrEdits.empty()));
        assertFalse(Files.exists(wrong));
    }
    @Test void validOutputIsKeptWhenVerificationPasses() throws Exception {
        Path src = writeSource(), good = dir.resolve("good.nes"); Files.write(good, ROM);
        RomImage reopened = RomRebuilder.reopenAndVerify(good, RomLoader.load(src), ChrEdits.empty());
        assertTrue(Files.exists(good)); assertEquals(ROM.length, reopened.originalBytes().length);
    }
}
