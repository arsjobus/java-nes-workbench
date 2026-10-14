package com.nesworkbench.rom;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class RomSummaryTest {
    private RomSummary summary(byte[] bytes) throws Exception { return RomSummary.of(RomImage.parse(bytes)); }

    @Test void summarizesBasicNromRom() throws Exception {
        RomSummary s = summary(RomFixtures.make(2, 1, 0, 0));
        assertEquals(0, s.mapper()); assertEquals(32768, s.prgRomBytes()); assertEquals(2, s.prgBanks());
        assertEquals(8192, s.chrRomBytes()); assertEquals(1, s.chrBanks()); assertEquals(512, s.chrTileCount());
        assertFalse(s.hasTrainer()); assertFalse(s.hasBattery()); assertFalse(s.usesChrRam());
        assertEquals(RomSummary.Mirroring.HORIZONTAL, s.mirroring());
        assertEquals(16 + 32768, s.chrFileOffset()); assertEquals(16 + 32768 + 8192, s.fileSize()); assertEquals(0, s.trailingBytes());
    }
    @Test void decodesMirroringBitsAndFourScreenOverridesVertical() throws Exception {
        assertEquals(RomSummary.Mirroring.VERTICAL, summary(RomFixtures.make(1, 1, 1, 0)).mirroring());
        assertEquals(RomSummary.Mirroring.FOUR_SCREEN, summary(RomFixtures.make(1, 1, 8, 0)).mirroring());
        assertEquals(RomSummary.Mirroring.FOUR_SCREEN, summary(RomFixtures.make(1, 1, 9, 0)).mirroring());
    }
    @Test void decodesBatteryFlag() throws Exception { assertTrue(summary(RomFixtures.make(1, 1, 2, 0)).hasBattery()); }
    @Test void chrFileOffsetAccountsForTrainer() throws Exception {
        RomSummary s = summary(RomFixtures.make(1, 1, 4, 0));
        assertTrue(s.hasTrainer()); assertEquals(16 + 512 + 16384, s.chrFileOffset());
    }
    @Test void reportsTrailingBytesBeyondDeclaredData() throws Exception {
        byte[] rom = Arrays.copyOf(RomFixtures.make(1, 1, 0, 0), 16 + 16384 + 8192 + 37);
        assertEquals(37, summary(rom).trailingBytes());
    }
    @Test void describesChrRamCartridge() throws Exception {
        RomSummary s = summary(RomFixtures.make(1, 0, 0, 0));
        assertTrue(s.usesChrRam()); assertEquals(0, s.chrTileCount());
        assertEquals("CHR-RAM (no CHR-ROM data)", s.fields().get("CHR"));
    }
    @Test void fieldsAreOrderedAndFormatted() throws Exception {
        Map<String, String> f = summary(RomFixtures.make(2, 1, 0x21, 0x10)).fields();
        assertEquals(java.util.List.of("Format", "Mapper", "PRG-ROM", "CHR", "Trainer", "Battery", "Mirroring", "File size", "Trailing bytes"),
                java.util.List.copyOf(f.keySet()));
        assertEquals("18", f.get("Mapper")); assertEquals("32768 bytes (2 x 16 KiB)", f.get("PRG-ROM"));
        assertEquals("8192 bytes (1 x 8 KiB, 512 tiles)", f.get("CHR")); assertEquals("Vertical", f.get("Mirroring"));
    }
    @Test void mapperZeroIsLabelledNrom() throws Exception { assertEquals("0 (NROM)", summary(RomFixtures.make(1, 1, 0, 0)).fields().get("Mapper")); }
    @Test void textContainsEveryField() throws Exception {
        RomSummary s = summary(RomFixtures.make(1, 1, 4, 0));
        String text = s.toText();
        s.fields().forEach((k, v) -> assertTrue(text.contains(k + ": " + v + "\n"), k));
    }
    @Test void describesTileOffsets() throws Exception {
        assertEquals("Tile 3 ($3) | CHR offset $0030 | file offset $04040", summary(RomFixtures.make(1, 1, 0, 0)).describeTile(3));
    }
    @Test void describeTileHonoursTrainerOffset() throws Exception {
        assertEquals("Tile 0 ($0) | CHR offset $0000 | file offset $04210", summary(RomFixtures.make(1, 1, 4, 0)).describeTile(0));
    }
    @Test void describeTileRejectsOutOfRangeIndices() throws Exception {
        RomSummary s = summary(RomFixtures.make(1, 1, 0, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> s.describeTile(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> s.describeTile(512));
        assertDoesNotThrow(() -> s.describeTile(511));
        RomSummary chrRam = summary(RomFixtures.make(1, 0, 0, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> chrRam.describeTile(0));
    }
    @Test void rejectsNullRom() { assertThrows(NullPointerException.class, () -> RomSummary.of(null)); }
}
