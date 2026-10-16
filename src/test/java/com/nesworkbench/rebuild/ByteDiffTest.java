package com.nesworkbench.rebuild;

import com.nesworkbench.rom.RomImage;
import com.nesworkbench.rom.RomSummary;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ByteDiffTest {
    private static RomSummary summary(byte[] rom) throws Exception { return RomSummary.of(RomImage.parse(rom)); }

    @Test void identicalArraysHaveNoRanges() {
        ByteDiff d = ByteDiff.compare(new byte[]{1, 2, 3}, new byte[]{1, 2, 3});
        assertTrue(d.identical()); assertEquals(0, d.changedBytes()); assertTrue(d.ranges().isEmpty());
    }
    @Test void emptyArraysAreIdentical() { assertTrue(ByteDiff.compare(new byte[0], new byte[0]).identical()); }
    @Test void singleByteChangeIsOneRangeOfLengthOne() {
        ByteDiff d = ByteDiff.compare(new byte[]{1, 2, 3}, new byte[]{1, 9, 3});
        assertEquals(List.of(new ByteDiff.Range(1, 1)), d.ranges()); assertEquals(1, d.changedBytes()); assertFalse(d.identical());
    }
    @Test void consecutiveChangesMergeAndSeparatedOnesDoNot() {
        ByteDiff d = ByteDiff.compare(new byte[]{0, 0, 0, 0, 0, 0}, new byte[]{1, 1, 0, 1, 0, 1});
        assertEquals(List.of(new ByteDiff.Range(0, 2), new ByteDiff.Range(3, 1), new ByteDiff.Range(5, 1)), d.ranges());
        assertEquals(4, d.changedBytes());
    }
    @Test void changeAtFirstAndLastByteIsFound() {
        ByteDiff d = ByteDiff.compare(new byte[]{1, 2, 3}, new byte[]{9, 2, 9});
        assertEquals(List.of(new ByteDiff.Range(0, 1), new ByteDiff.Range(2, 1)), d.ranges());
    }
    @Test void differentLengthsAreNotIdenticalEvenWithMatchingPrefix() {
        ByteDiff d = ByteDiff.compare(new byte[]{1, 2}, new byte[]{1, 2, 3});
        assertFalse(d.identical()); assertTrue(d.ranges().isEmpty()); assertEquals(2, d.originalLength()); assertEquals(3, d.rebuiltLength());
    }
    @Test void rejectsNull() {
        assertThrows(NullPointerException.class, () -> ByteDiff.compare(null, new byte[0]));
        assertThrows(NullPointerException.class, () -> ByteDiff.compare(new byte[0], null));
    }
    @Test void reportForIdenticalFilesSaysSo() throws Exception {
        byte[] rom = RebuildFixtures.make(1, 1, false, 0);
        String r = ByteDiff.compare(rom, rom.clone()).report(summary(rom));
        assertTrue(r.contains("The files are identical."), r); assertFalse(r.contains("Changed bytes"));
    }
    @Test void reportNamesRegionsOffsetsAndTilesForChrChange() throws Exception {
        byte[] rom = RebuildFixtures.make(1, 1, false, 0), out = rom.clone();
        int chr = RebuildFixtures.chrOffset(1, false);
        out[chr + 3 * 16 + 2] ^= 0x55;   // tile 3
        String r = ByteDiff.compare(rom, out).report(summary(rom));
        assertTrue(r.contains("Changed bytes: 1 in 1 range(s)."), r);
        assertTrue(r.contains(String.format("$%05X..$%05X (1 byte(s)) CHR-ROM, tile 3 ($3)", chr + 50, chr + 50)), r);
        assertTrue(r.contains("CHR tiles touched: 1"), r);
        assertTrue(r.contains("CHR-ROM 1;"), r);
    }
    @Test void reportSplitsARangeThatCrossesFromPrgIntoChr() throws Exception {
        byte[] rom = RebuildFixtures.make(1, 1, false, 0), out = rom.clone();
        int chr = RebuildFixtures.chrOffset(1, false);
        for (int i = chr - 2; i < chr + 18; i++) out[i] ^= 1;   // one run: 2 PRG bytes then 18 CHR bytes (tiles 0 and 1)
        String r = ByteDiff.compare(rom, out).report(summary(rom));
        assertTrue(r.contains("Changed bytes: 20 in 1 range(s)."), r);
        assertTrue(r.contains("PRG-ROM 2;") && r.contains("CHR-ROM 18;"), r);
        assertTrue(r.contains(String.format("$%05X..$%05X (2 byte(s)) PRG-ROM", chr - 2, chr - 1)), r);
        assertTrue(r.contains(String.format("$%05X..$%05X (18 byte(s)) CHR-ROM, tiles 0..1 ($0..$1)", chr, chr + 17)), r);
        assertTrue(r.contains("CHR tiles touched: 2"), r);
    }
    @Test void reportLabelsHeaderTrainerAndTrailingBytes() throws Exception {
        byte[] rom = RebuildFixtures.make(1, 1, true, 4), out = rom.clone();
        out[8] ^= 1; out[16] ^= 1; out[out.length - 1] ^= 1;
        String r = ByteDiff.compare(rom, out).report(summary(rom));
        assertTrue(r.contains("Header 1;") && r.contains("Trainer 1;") && r.contains("Trailing bytes 1;"), r);
        assertTrue(r.contains("$00008..$00008 (1 byte(s)) Header"), r);
        assertTrue(r.contains("$00010..$00010 (1 byte(s)) Trainer"), r);
        assertFalse(r.contains("CHR tiles touched"), r);
    }
    @Test void reportMentionsLengthDifference() throws Exception {
        byte[] rom = RebuildFixtures.make(1, 1, false, 0), longer = java.util.Arrays.copyOf(rom, rom.length + 2);
        String r = ByteDiff.compare(rom, longer).report(summary(rom));
        assertTrue(r.contains("length differs by 2"), r);
    }
    @Test void reportListsAtMostTheCapAndCountsTheRest() throws Exception {
        byte[] rom = RebuildFixtures.make(1, 1, false, 0), out = rom.clone();
        int chr = RebuildFixtures.chrOffset(1, false), n = ByteDiff.MAX_REPORT_RANGES + 5;
        for (int t = 0; t < n; t++) out[chr + t * 16] ^= 1;   // one separate range per tile
        String r = ByteDiff.compare(rom, out).report(summary(rom));
        assertTrue(r.contains("Changed bytes: " + n + " in " + n + " range(s)."), r);
        assertTrue(r.contains("... and 5 more range(s)."), r);
        assertEquals(ByteDiff.MAX_REPORT_RANGES, r.lines().filter(l -> l.startsWith("$")).count());
    }
}
