package com.nesworkbench.rom;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Human-readable metadata derived from a parsed {@link RomImage}. No JavaFX dependency. */
public record RomSummary(int mapper, int prgRomBytes, int chrRomBytes, boolean usesChrRam, boolean hasTrainer,
                         boolean hasBattery, Mirroring mirroring, int chrFileOffset, int fileSize, int trailingBytes) {

    public enum Mirroring { HORIZONTAL, VERTICAL, FOUR_SCREEN }

    public static RomSummary of(RomImage rom) {
        Objects.requireNonNull(rom, "rom must not be null");
        byte[] header = rom.header();
        int flags6 = header[6] & 0xFF;
        Mirroring mirroring = (flags6 & 0x08) != 0 ? Mirroring.FOUR_SCREEN
                : (flags6 & 0x01) != 0 ? Mirroring.VERTICAL : Mirroring.HORIZONTAL;
        int chrOffset = RomImage.HEADER_SIZE + (rom.hasTrainer() ? RomImage.TRAINER_SIZE : 0) + rom.prgRomSize();
        int fileSize = rom.originalBytes().length;
        return new RomSummary(rom.mapper(), rom.prgRomSize(), rom.chrRomSize(), rom.usesChrRam(), rom.hasTrainer(),
                (flags6 & 0x02) != 0, mirroring, chrOffset, fileSize, fileSize - chrOffset - rom.chrRomSize());
    }

    public int prgBanks() { return prgRomBytes / RomImage.PRG_BANK_SIZE; }
    public int chrBanks() { return chrRomBytes / RomImage.CHR_BANK_SIZE; }
    public int chrTileCount() { return chrRomBytes / 16; }

    /** Ordered label/value pairs for display. */
    public Map<String, String> fields() {
        Map<String, String> f = new LinkedHashMap<>();
        f.put("Format", "iNES 1.0");
        f.put("Mapper", mapper == 0 ? "0 (NROM)" : Integer.toString(mapper));
        f.put("PRG-ROM", prgRomBytes + " bytes (" + prgBanks() + " x 16 KiB)");
        f.put("CHR", usesChrRam ? "CHR-RAM (no CHR-ROM data)"
                : chrRomBytes + " bytes (" + chrBanks() + " x 8 KiB, " + chrTileCount() + " tiles)");
        f.put("Trainer", hasTrainer ? "Present (" + RomImage.TRAINER_SIZE + " bytes)" : "None");
        f.put("Battery", hasBattery ? "Yes" : "No");
        f.put("Mirroring", switch (mirroring) {
            case HORIZONTAL -> "Horizontal"; case VERTICAL -> "Vertical"; case FOUR_SCREEN -> "Four-screen"; });
        f.put("File size", fileSize + " bytes");
        f.put("Trailing bytes", Integer.toString(trailingBytes));
        return f;
    }

    public String toText() {
        StringBuilder sb = new StringBuilder();
        fields().forEach((k, v) -> sb.append(k).append(": ").append(v).append('\n'));
        return sb.toString();
    }

    /** Describes a CHR tile's position within the CHR data and the source file. */
    public String describeTile(int index) {
        if (index < 0 || index >= chrTileCount())
            throw new IndexOutOfBoundsException("Tile index " + index + " is outside 0.." + (chrTileCount() - 1));
        int chrOffset = index * 16;
        return String.format("Tile %d ($%X) | CHR offset $%04X | file offset $%05X", index, index, chrOffset, chrFileOffset + chrOffset);
    }
}
