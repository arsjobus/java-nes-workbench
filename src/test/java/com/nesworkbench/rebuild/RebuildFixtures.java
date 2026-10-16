package com.nesworkbench.rebuild;

import java.util.Random;

/** Deterministic synthetic iNES fixtures with every byte non-trivial. No copyrighted ROM data is used. */
final class RebuildFixtures {
    static final int PRG_BANK = 16384, CHR_BANK = 8192;
    private RebuildFixtures() {}

    /** Random-filled ROM: header flags 6 = battery + vertical (+ trainer), then optional trailing bytes. */
    static byte[] make(int prgBanks, int chrBanks, boolean trainer, int trailing) {
        int length = 16 + (trainer ? 512 : 0) + prgBanks * PRG_BANK + chrBanks * CHR_BANK + trailing;
        byte[] rom = new byte[length];
        new Random(1234).nextBytes(rom);
        rom[0] = 'N'; rom[1] = 'E'; rom[2] = 'S'; rom[3] = 0x1A;
        rom[4] = (byte) prgBanks; rom[5] = (byte) chrBanks;
        rom[6] = (byte) (0x03 | (trainer ? 0x04 : 0)); rom[7] = 0;
        for (int i = 8; i < 16; i++) rom[i] = 0;
        return rom;
    }

    static int chrOffset(int prgBanks, boolean trainer) { return 16 + (trainer ? 512 : 0) + prgBanks * PRG_BANK; }

    static byte[] tileData(int seed) { byte[] d = new byte[16]; new Random(seed).nextBytes(d); return d; }
}
