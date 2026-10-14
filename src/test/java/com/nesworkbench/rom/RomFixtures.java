package com.nesworkbench.rom;

/** Synthetic iNES fixtures. No copyrighted ROM data is used. */
final class RomFixtures {
    private RomFixtures() {}

    static byte[] make(int prgBanks, int chrBanks, int flags6, int flags7) {
        int length = 16 + ((flags6 & 4) != 0 ? 512 : 0) + prgBanks * 16384 + chrBanks * 8192;
        byte[] rom = new byte[length];
        rom[0] = 'N'; rom[1] = 'E'; rom[2] = 'S'; rom[3] = 0x1A;
        rom[4] = (byte) prgBanks; rom[5] = (byte) chrBanks; rom[6] = (byte) flags6; rom[7] = (byte) flags7;
        return rom;
    }
}
