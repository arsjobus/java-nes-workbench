package com.nesworkbench.rom;

import java.util.Arrays;
import java.util.Objects;

/** Immutable parsed view of a standard iNES 1.0 ROM. Does not write or modify ROM files. */
public final class RomImage {
    public static final int HEADER_SIZE = 16;
    public static final int PRG_BANK_SIZE = 16 * 1024;
    public static final int CHR_BANK_SIZE = 8 * 1024;
    public static final int TRAINER_SIZE = 512;

    private final byte[] originalBytes, header, trainer, prgRom, chrRom;
    private final int mapper;
    private final boolean hasTrainer, chrRam;

    private RomImage(byte[] originalBytes, byte[] header, byte[] trainer, byte[] prgRom,
                     byte[] chrRom, int mapper, boolean hasTrainer, boolean chrRam) {
        this.originalBytes = originalBytes.clone(); this.header = header.clone();
        this.trainer = trainer.clone(); this.prgRom = prgRom.clone(); this.chrRom = chrRom.clone();
        this.mapper = mapper; this.hasTrainer = hasTrainer; this.chrRam = chrRam;
    }

    public static RomImage parse(byte[] bytes) throws RomFormatException {
        Objects.requireNonNull(bytes, "bytes must not be null");
        if (bytes.length < HEADER_SIZE) throw new RomFormatException("ROM is shorter than the 16-byte iNES header.");
        if (bytes[0] != 'N' || bytes[1] != 'E' || bytes[2] != 'S' || bytes[3] != 0x1A)
            throw new RomFormatException("Invalid iNES magic signature.");

        int flags6 = u8(bytes[6]), flags7 = u8(bytes[7]);
        if ((flags7 & 0x0C) == 0x08) throw new RomFormatException("NES 2.0 headers are not supported yet.");
        int mapper = (flags6 >>> 4) | (flags7 & 0xF0);
        int prgBanks = u8(bytes[4]), chrBanks = u8(bytes[5]);
        if (prgBanks == 0) throw new RomFormatException("iNES 1.0 ROM must contain at least one PRG-ROM bank.");

        boolean hasTrainer = (flags6 & 0x04) != 0;
        boolean chrRam = chrBanks == 0;
        long trainerLength = hasTrainer ? TRAINER_SIZE : 0;
        long prgLength = (long) prgBanks * PRG_BANK_SIZE;
        long chrLength = (long) chrBanks * CHR_BANK_SIZE;
        long expectedLength = HEADER_SIZE + trainerLength + prgLength + chrLength;
        if (expectedLength > bytes.length) {
            throw new RomFormatException("ROM is truncated: header declares " + expectedLength
                    + " bytes, but file contains " + bytes.length + ".");
        }

        int offset = HEADER_SIZE;
        byte[] header = Arrays.copyOfRange(bytes, 0, HEADER_SIZE);
        byte[] trainer = new byte[(int) trainerLength];
        if (hasTrainer) { trainer = Arrays.copyOfRange(bytes, offset, offset + TRAINER_SIZE); offset += TRAINER_SIZE; }
        byte[] prg = Arrays.copyOfRange(bytes, offset, offset + (int) prgLength); offset += (int) prgLength;
        byte[] chr = Arrays.copyOfRange(bytes, offset, offset + (int) chrLength);
        return new RomImage(bytes, header, trainer, prg, chr, mapper, hasTrainer, chrRam);
    }

    private static int u8(byte value) { return value & 0xFF; }
    public int mapper() { return mapper; }
    public boolean hasTrainer() { return hasTrainer; }
    public boolean usesChrRam() { return chrRam; }
    public int prgRomSize() { return prgRom.length; }
    public int chrRomSize() { return chrRom.length; }
    public byte[] originalBytes() { return originalBytes.clone(); }
    public byte[] header() { return header.clone(); }
    public byte[] trainer() { return trainer.clone(); }
    public byte[] prgRom() { return prgRom.clone(); }
    public byte[] chrRom() { return chrRom.clone(); }
}
