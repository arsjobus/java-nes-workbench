package com.nesworkbench.project;

import com.nesworkbench.mapping.TileMapping;
import com.nesworkbench.rom.RomFormatException;
import com.nesworkbench.rom.RomImage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ProjectFileTest {
    private static final String SHA = "ab".repeat(32);
    private static ProjectData data(int... mapping) { return new ProjectData("game.nes", SHA, TileMapping.of(mapping)); }
    private static String valid() { return "NES-WORKBENCH-PROJECT 1\nrom-name=game.nes\nrom-sha256=" + SHA + "\ntile-count=3\nmapping=2,0,1\n"; }
    private static ProjectFormatException rejects(String text) { return assertThrows(ProjectFormatException.class, () -> ProjectFile.parse(text)); }

    private static RomImage rom(int chrBanks, int seed) throws RomFormatException {
        byte[] b = new byte[16 + 16384 + chrBanks * 8192];
        b[0] = 'N'; b[1] = 'E'; b[2] = 'S'; b[3] = 0x1A; b[4] = 1; b[5] = (byte) chrBanks;
        for (int i = 16 + 16384; i < b.length; i++) b[i] = (byte) (i * 31 + seed);
        return RomImage.parse(b);
    }

    // ---- format / parse ----
    @Test void formatProducesDocumentedLayout() { assertEquals(valid(), ProjectFile.format(data(2, 0, 1))); }
    @Test void roundTripPreservesEverything() throws Exception {
        ProjectData original = data(2, 0, 1); assertEquals(original, ProjectFile.parse(ProjectFile.format(original)));
    }
    @Test void identityAndEmptyMappingsRoundTrip() throws Exception {
        ProjectData id = new ProjectData("a.nes", SHA, TileMapping.identity(512)); assertEquals(id, ProjectFile.parse(ProjectFile.format(id)));
        ProjectData empty = new ProjectData("a.nes", SHA, TileMapping.identity(0)); assertEquals(empty, ProjectFile.parse(ProjectFile.format(empty)));
    }
    @Test void largestMappingRoundTrips() throws Exception {
        ProjectData big = new ProjectData("big.nes", SHA, TileMapping.identity(ProjectFile.MAX_TILES).moved(0, ProjectFile.MAX_TILES - 1));
        assertEquals(big, ProjectFile.parse(ProjectFile.format(big)));
    }
    @Test void romNameWithEqualsSignsAndSpacesRoundTrips() throws Exception {
        ProjectData d = new ProjectData("my game = v2 (final).nes", SHA, TileMapping.identity(2)); assertEquals(d, ProjectFile.parse(ProjectFile.format(d)));
    }
    @Test void unicodeRomNameRoundTripsThroughFile(@TempDir Path dir) throws Exception {
        ProjectData d = new ProjectData("ゲーム.nes", SHA, TileMapping.identity(2)); Path f = dir.resolve("p.nesproj");
        ProjectFile.write(f, d, false); assertEquals(d, ProjectFile.read(f));
    }
    @Test void crlfLineEndingsAreAccepted() throws Exception { assertEquals(data(2, 0, 1), ProjectFile.parse(valid().replace("\n", "\r\n"))); }
    @Test void missingTrailingNewlineIsAccepted() throws Exception { assertEquals(data(2, 0, 1), ProjectFile.parse(valid().stripTrailing())); }
    @Test void formatRejectsRomNamesWithLineBreaks() {
        assertThrows(IllegalArgumentException.class, () -> ProjectFile.format(new ProjectData("a\nb.nes", SHA, TileMapping.identity(1))));
        assertThrows(IllegalArgumentException.class, () -> ProjectFile.format(new ProjectData("a\rb.nes", SHA, TileMapping.identity(1))));
    }
    @Test void formatRejectsMalformedHash() { assertThrows(IllegalArgumentException.class, () -> ProjectFile.format(new ProjectData("a.nes", "XYZ", TileMapping.identity(1)))); }

    // ---- malformed files ----
    @Test void rejectsEmptyAndWrongMagic() {
        assertTrue(rejects("").getMessage().contains("empty"));
        rejects("PNG 1\n"); rejects("nes-workbench-project 1\n"); rejects("NES-WORKBENCH-PROJECT\n"); rejects("NES-WORKBENCH-PROJECT 1 2\n");
    }
    @Test void rejectsBadVersions() {
        rejects(valid().replace("PROJECT 1", "PROJECT x")); rejects(valid().replace("PROJECT 1", "PROJECT 0")); rejects(valid().replace("PROJECT 1", "PROJECT -1"));
        rejects(valid().replace("PROJECT 1", "PROJECT 1.5")); rejects(valid().replace("PROJECT 1", "PROJECT 99999999999"));
    }
    @Test void newerVersionGetsAClearMessage() {
        String message = rejects(valid().replace("PROJECT 1", "PROJECT 3")).getMessage();
        assertTrue(message.contains("newer") && message.contains("version 3") && message.contains("up to version 2"));
    }
    @Test void rejectsMissingKeys() { for (String key : new String[]{"rom-name", "rom-sha256", "tile-count", "mapping"}) rejects(Arrays.stream(valid().split("\n")).filter(l -> !l.startsWith(key + "=")).reduce((a, b) -> a + "\n" + b).get()); }
    @Test void rejectsDuplicateAndUnknownKeys() { rejects(valid() + "mapping=2,0,1\n"); rejects(valid() + "extra=1\n"); }
    @Test void rejectsLinesWithoutEqualsAndBlankLinesInTheMiddle() { rejects(valid() + "garbage\n"); rejects(valid().replace("tile-count=3\n", "\ntile-count=3\n")); }
    @Test void rejectsBadHash() { rejects(valid().replace(SHA, "AB".repeat(32))); rejects(valid().replace(SHA, "ab".repeat(31))); rejects(valid().replace(SHA, "zz".repeat(32))); }
    @Test void rejectsBadTileCount() {
        for (String bad : new String[]{"x", "-1", "", "3.0", " 3", "130561", "99999999999"}) rejects(valid().replace("tile-count=3", "tile-count=" + bad));
    }
    @Test void hugeTileCountIsRejectedBeforeAllocating() { assertTrue(rejects(valid().replace("tile-count=3", "tile-count=999999999")).getMessage().contains("exceeds")); }
    @Test void rejectsMappingLengthMismatch() { rejects(valid().replace("2,0,1", "2,0")); rejects(valid().replace("2,0,1", "2,0,1,3")); rejects(valid().replace("2,0,1", "")); }
    @Test void rejectsNonPermutationMappings() {
        rejects(valid().replace("2,0,1", "2,2,1")); rejects(valid().replace("2,0,1", "0,1,3")); rejects(valid().replace("2,0,1", "0,1,-1"));
    }
    @Test void rejectsMalformedMappingEntries() { rejects(valid().replace("2,0,1", "2, 0,1")); rejects(valid().replace("2,0,1", "2,,1")); rejects(valid().replace("2,0,1", "2,0,a")); rejects(valid().replace("2,0,1", "2,0,1,")); }
    @Test void emptyTileCountRequiresEmptyMapping() { rejects(valid().replace("tile-count=3", "tile-count=0")); }

    // ---- files ----
    @Test void writeThenReadRoundTrip(@TempDir Path dir) throws Exception {
        Path f = dir.resolve("p.nesproj"); ProjectFile.write(f, data(2, 0, 1), false);
        assertEquals(valid(), Files.readString(f)); assertEquals(data(2, 0, 1), ProjectFile.read(f));
    }
    @Test void writeRefusesToReplaceUnlessOverwriteIsTrue(@TempDir Path dir) throws Exception {
        Path f = dir.resolve("p.nesproj"); Files.writeString(f, "keep me");
        assertThrows(FileAlreadyExistsException.class, () -> ProjectFile.write(f, data(2, 0, 1), false));
        assertEquals("keep me", Files.readString(f));
        ProjectFile.write(f, data(2, 0, 1), true); assertEquals(data(2, 0, 1), ProjectFile.read(f));
    }
    @Test void overwriteLeavesNoTemporaryFiles(@TempDir Path dir) throws Exception {
        Path f = dir.resolve("p.nesproj"); ProjectFile.write(f, data(2, 0, 1), true); ProjectFile.write(f, data(0, 1, 2), true);
        try (Stream<Path> files = Files.list(dir)) { assertEquals(1, files.count()); }
    }
    @Test void readRejectsMissingFilesDirectoriesAndOversizedFiles(@TempDir Path dir) throws Exception {
        assertThrows(IOException.class, () -> ProjectFile.read(dir.resolve("missing.nesproj"))); assertThrows(IOException.class, () -> ProjectFile.read(dir));
        Path big = dir.resolve("big.nesproj"); Files.write(big, new byte[(int) ProjectFile.MAX_FILE_BYTES + 1]);
        assertTrue(assertThrows(ProjectFormatException.class, () -> ProjectFile.read(big)).getMessage().contains("larger"));
    }
    @Test void readRejectsInvalidUtf8(@TempDir Path dir) throws Exception {
        Path f = dir.resolve("p.nesproj"); byte[] bytes = valid().getBytes(StandardCharsets.UTF_8); bytes[bytes.length - 2] = (byte) 0xFF; Files.write(f, bytes);
        assertThrows(ProjectFormatException.class, () -> ProjectFile.read(f));
    }

    // ---- matching a project to a ROM ----
    @Test void projectVerifiesAgainstItsOwnRom() throws Exception {
        RomImage r = rom(1, 0); ProjectData d = ProjectData.forRom("g.nes", r, TileMapping.identity(512).swapped(0, 511));
        assertDoesNotThrow(() -> d.verifyAgainst(r));
        assertEquals(ProjectData.sha256Hex(r.originalBytes()), d.romSha256());
    }
    @Test void projectRejectsADifferentRom() throws Exception {
        ProjectData d = ProjectData.forRom("g.nes", rom(1, 0), TileMapping.identity(512));
        assertTrue(assertThrows(ProjectFormatException.class, () -> d.verifyAgainst(rom(1, 1))).getMessage().contains("different ROM"));
    }
    @Test void projectRejectsMappingThatDoesNotFitTheRom() throws Exception {
        RomImage r = rom(1, 0); ProjectData d = new ProjectData("g.nes", ProjectData.sha256Hex(r.originalBytes()), TileMapping.identity(10));
        assertTrue(assertThrows(ProjectFormatException.class, () -> d.verifyAgainst(r)).getMessage().contains("512"));
    }
    @Test void projectRejectsChrRamRoms() throws Exception {
        RomImage r = rom(0, 0); ProjectData d = ProjectData.forRom("g.nes", r, TileMapping.identity(0));
        assertThrows(ProjectFormatException.class, () -> d.verifyAgainst(r));
    }
    @Test void sha256MatchesKnownVector() { assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", ProjectData.sha256Hex("abc".getBytes(StandardCharsets.US_ASCII))); }
    @Test void nullArgumentsAreRejected() {
        assertThrows(NullPointerException.class, () -> new ProjectData(null, SHA, TileMapping.identity(1)));
        assertThrows(NullPointerException.class, () -> ProjectFile.parse(null)); assertThrows(NullPointerException.class, () -> ProjectFile.format(null));
    }
}
