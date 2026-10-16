package com.nesworkbench.rebuild;

import com.nesworkbench.rom.RomImage;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

class RomRebuilderTest {
    private static final byte[] ROM = RebuildFixtures.make(2, 2, true, 7);
    private static final int CHR = RebuildFixtures.chrOffset(2, true);

    private static RomImage image() throws Exception { return RomImage.parse(ROM.clone()); }

    @Test void noEditsProducesAnIdenticalCopy() throws Exception {
        byte[] out = RomRebuilder.rebuild(image(), ChrEdits.empty());
        assertArrayEquals(ROM, out); assertNotSame(ROM, out);
    }
    @Test void editedTileIsWrittenAtItsChrOffset() throws Exception {
        byte[] data = RebuildFixtures.tileData(99);
        byte[] out = RomRebuilder.rebuild(image(), ChrEdits.empty().withTile(10, data));
        assertArrayEquals(data, Arrays.copyOfRange(out, CHR + 160, CHR + 176));
    }
    @Test void onlyTheExpectedOffsetsChangeAndEverythingElseIsPreserved() throws Exception {
        ChrEdits edits = ChrEdits.empty().withTile(0, RebuildFixtures.tileData(1)).withTile(10, RebuildFixtures.tileData(2)).withTile(1023, RebuildFixtures.tileData(3));
        byte[] out = RomRebuilder.rebuild(image(), edits);
        assertEquals(ROM.length, out.length);
        for (int i = 0; i < ROM.length; i++) {
            boolean inEditedTile = i >= CHR && i < ROM.length - 7 && edits.has((i - CHR) / 16);
            if (!inEditedTile) assertEquals(ROM[i], out[i], "byte at offset " + i + " should be preserved");
        }
        for (int t : edits.tileIndices()) assertArrayEquals(edits.tile(t), Arrays.copyOfRange(out, CHR + t * 16, CHR + t * 16 + 16));
        assertTrue(ByteDiff.compare(ROM, out).ranges().stream().allMatch(r -> r.start() >= CHR && r.end() <= ROM.length - 7));
    }
    @Test void headerTrainerPrgAndTrailingBytesSurvive() throws Exception {
        byte[] out = RomRebuilder.rebuild(image(), ChrEdits.empty().withTile(5, RebuildFixtures.tileData(4)));
        assertArrayEquals(Arrays.copyOfRange(ROM, 0, CHR), Arrays.copyOfRange(out, 0, CHR));   // header + trainer + PRG
        assertArrayEquals(Arrays.copyOfRange(ROM, ROM.length - 7, ROM.length), Arrays.copyOfRange(out, ROM.length - 7, ROM.length));
    }
    @Test void editIdenticalToTheSourceTileChangesNothing() throws Exception {
        byte[] same = Arrays.copyOfRange(ROM, CHR + 32, CHR + 48);
        assertTrue(ByteDiff.compare(ROM, RomRebuilder.rebuild(image(), ChrEdits.empty().withTile(2, same))).identical());
    }
    @Test void sourceImageAndInputArraysAreNotModified() throws Exception {
        byte[] input = ROM.clone(); RomImage img = RomImage.parse(input);
        RomRebuilder.rebuild(img, ChrEdits.empty().withTile(0, RebuildFixtures.tileData(5)));
        assertArrayEquals(ROM, img.originalBytes()); assertArrayEquals(ROM, input);
    }
    @Test void lastTileIsEditableAndOneBeyondIsRejected() throws Exception {
        assertDoesNotThrow(() -> RomRebuilder.rebuild(image(), ChrEdits.empty().withTile(1023, new byte[16])));
        assertThrows(RebuildException.class, () -> RomRebuilder.rebuild(image(), ChrEdits.empty().withTile(1024, new byte[16])));
    }
    @Test void anyOutOfRangeEditRejectsTheWholeRebuild() {
        assertThrows(RebuildException.class, () -> RomRebuilder.rebuild(image(), ChrEdits.empty().withTile(0, new byte[16]).withTile(5000, new byte[16])));
    }
    @Test void chrRamRomRejectsEditsButAllowsAPlainCopy() throws Exception {
        byte[] ramRom = RebuildFixtures.make(1, 0, false, 0); RomImage img = RomImage.parse(ramRom);
        assertThrows(RebuildException.class, () -> RomRebuilder.rebuild(img, ChrEdits.empty().withTile(0, new byte[16])));
        assertArrayEquals(ramRom, RomRebuilder.rebuild(img, ChrEdits.empty()));
    }
    @Test void rebuiltOutputReparsesWithTheSameLayout() throws Exception {
        RomImage out = RomImage.parse(RomRebuilder.rebuild(image(), ChrEdits.empty().withTile(3, RebuildFixtures.tileData(6))));
        RomImage src = image();
        assertEquals(src.mapper(), out.mapper()); assertEquals(src.prgRomSize(), out.prgRomSize()); assertEquals(src.chrRomSize(), out.chrRomSize());
        assertArrayEquals(src.prgRom(), out.prgRom()); assertArrayEquals(src.trainer(), out.trainer()); assertArrayEquals(src.header(), out.header());
    }
    @Test void rejectsNulls() {
        assertThrows(NullPointerException.class, () -> RomRebuilder.rebuild(null, ChrEdits.empty()));
        assertThrows(NullPointerException.class, () -> RomRebuilder.rebuild(image(), null));
    }
    @Test void defaultOutputNameAddsRebuiltBeforeTheExtension() {
        assertEquals("game-rebuilt.nes", RomRebuilder.defaultOutputName("game.nes"));
        assertEquals("my.game-rebuilt.nes", RomRebuilder.defaultOutputName("my.game.nes"));
        assertEquals("game-rebuilt.nes", RomRebuilder.defaultOutputName("game"));
        assertEquals(".nes-rebuilt.nes", RomRebuilder.defaultOutputName(".nes"));
        assertThrows(NullPointerException.class, () -> RomRebuilder.defaultOutputName(null));
    }
}
