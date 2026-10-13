package com.nesworkbench.rom;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

class RomImageTest {
    private byte[] makeRom(int prgBanks, int chrBanks, int flags6, int flags7) {
        int length = 16 + ((flags6 & 4) != 0 ? 512 : 0) + prgBanks * 16384 + chrBanks * 8192;
        byte[] rom = new byte[length];
        rom[0]='N'; rom[1]='E'; rom[2]='S'; rom[3]=0x1A;
        rom[4]=(byte)prgBanks; rom[5]=(byte)chrBanks; rom[6]=(byte)flags6; rom[7]=(byte)flags7;
        return rom;
    }

    @Test void parsesNromStyleRomAndSizes() throws Exception {
        RomImage image = RomImage.parse(makeRom(1, 1, 0, 0));
        assertEquals(0, image.mapper()); assertEquals(16384, image.prgRomSize());
        assertEquals(8192, image.chrRomSize()); assertFalse(image.hasTrainer()); assertFalse(image.usesChrRam());
    }
    @Test void detectsMapperFromHeaderFlags() throws Exception {
        assertEquals(0x12, RomImage.parse(makeRom(1, 1, 0x20, 0x10)).mapper());
    }
    @Test void accountsForTrainerBeforePrgRom() throws Exception {
        byte[] rom = makeRom(1, 1, 4, 0); Arrays.fill(rom, 16, 528, (byte)0x5A); rom[528] = 0x33;
        RomImage image = RomImage.parse(rom);
        assertTrue(image.hasTrainer()); assertEquals(512, image.trainer().length); assertEquals((byte)0x33, image.prgRom()[0]);
    }
    @Test void identifiesChrRamWhenChrBankCountIsZero() throws Exception {
        RomImage image = RomImage.parse(makeRom(1, 0, 0, 0)); assertTrue(image.usesChrRam()); assertEquals(0, image.chrRomSize());
    }
    @Test void rejectsInvalidMagic() { byte[] rom=makeRom(1,1,0,0); rom[0]='X'; assertThrows(RomFormatException.class, () -> RomImage.parse(rom)); }
    @Test void rejectsTruncatedRom() { assertThrows(RomFormatException.class, () -> RomImage.parse(Arrays.copyOf(makeRom(1,1,0,0), 100))); }
    @Test void rejectsHeaderShorterThanSixteenBytes() { assertThrows(RomFormatException.class, () -> RomImage.parse(new byte[15])); }
    @Test void rejectsNes20HeaderExplicitly() { assertThrows(RomFormatException.class, () -> RomImage.parse(makeRom(1,1,0,8))); }
    @Test void returnsDefensiveCopies() throws Exception {
        RomImage image=RomImage.parse(makeRom(1,1,0,0)); byte[] copy=image.originalBytes(); copy[0]=0;
        assertEquals((byte)'N', image.originalBytes()[0]);
    }
    @Test void rejectsZeroPrgBanks() { assertThrows(RomFormatException.class, () -> RomImage.parse(makeRom(0,1,0,0))); }
}
