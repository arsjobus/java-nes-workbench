package com.nesworkbench.project;

import com.nesworkbench.mapping.TileMapping;
import com.nesworkbench.rom.RomImage;
import com.nesworkbench.sprite.SpriteComposition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Project format version 2: named compositions with source tile references. */
class ProjectCompositionsTest {
    private static final String SHA = "ab".repeat(32);
    private static final SpriteComposition HERO = SpriteComposition.of("Hero", 2, 2, new int[]{0, 1, -1, 2});
    private static ProjectData data(SpriteComposition... c) { return new ProjectData("game.nes", SHA, TileMapping.of(new int[]{2, 0, 1, 3}), List.of(c)); }
    private static String base(int version) { return "NES-WORKBENCH-PROJECT " + version + "\nrom-name=game.nes\nrom-sha256=" + SHA + "\ntile-count=4\nmapping=2,0,1,3\n"; }
    private static ProjectFormatException rejects(String text) { return assertThrows(ProjectFormatException.class, () -> ProjectFile.parse(text)); }
    private static String v2(String... compositionValues) { StringBuilder sb = new StringBuilder(base(2)); for (String v : compositionValues) sb.append("composition=").append(v).append('\n'); return sb.toString(); }

    // ---- writing ----
    @Test void projectsWithoutCompositionsAreStillWrittenAsVersion1() { assertEquals(base(1), ProjectFile.format(data())); }
    @Test void projectsWithCompositionsAreWrittenAsVersion2WithDocumentedLayout() {
        assertEquals(base(2) + "composition=Hero|2x2|0,1,-1,2\n", ProjectFile.format(data(HERO)));
    }
    @Test void formatRejectsCompositionsThatReferenceTilesOutsideTheMapping() {
        SpriteComposition bad = SpriteComposition.empty("Bad", 1, 1).withTile(0, 0, 4);
        assertTrue(assertThrows(IllegalArgumentException.class, () -> ProjectFile.format(data(bad))).getMessage().contains("references tile 4"));
    }

    // ---- round trips ----
    @Test void multipleCompositionsRoundTripInOrder() throws Exception {
        ProjectData d = data(HERO, SpriteComposition.empty("Empty", 1, 1), SpriteComposition.of("Wide", 3, 1, new int[]{3, 3, 3}));
        ProjectData back = ProjectFile.parse(ProjectFile.format(d));
        assertEquals(d, back);
        assertEquals(List.of("Hero", "Empty", "Wide"), back.compositions().stream().map(SpriteComposition::name).toList());
    }
    @Test void unicodeAndPunctuatedNamesRoundTripThroughAFile(@TempDir Path dir) throws Exception {
        ProjectData d = data(SpriteComposition.empty("ボス = phase 2, (a) — #1", 1, 1), SpriteComposition.empty("a b", 2, 1));
        Path f = dir.resolve("p.nesproj"); ProjectFile.write(f, d, false);
        assertEquals(d, ProjectFile.read(f));
    }
    @Test void largestCompositionRoundTrips() throws Exception {
        int[] cells = new int[1024]; for (int i = 0; i < cells.length; i++) cells[i] = i % 5 == 0 ? -1 : i % 4;
        ProjectData d = data(SpriteComposition.of("Max", 32, 32, cells));
        assertEquals(d, ProjectFile.parse(ProjectFile.format(d)));
    }
    @Test void maximumNumberOfCompositionsRoundTripsWithinTheFileSizeLimit(@TempDir Path dir) throws Exception {
        List<SpriteComposition> all = new ArrayList<>();
        int[] cells = new int[1024]; java.util.Arrays.fill(cells, 3);
        for (int i = 0; i < ProjectData.MAX_COMPOSITIONS; i++) all.add(SpriteComposition.of("c" + i, 32, 32, cells));
        ProjectData d = new ProjectData("game.nes", SHA, TileMapping.of(new int[]{2, 0, 1, 3}), all);
        Path f = dir.resolve("big.nesproj"); ProjectFile.write(f, d, false);
        assertTrue(Files.size(f) < ProjectFile.MAX_FILE_BYTES, "size " + Files.size(f));
        assertEquals(d, ProjectFile.read(f));
    }
    @Test void compositionReferencesAreOriginalTilesAndAreIndependentOfTheMapping() throws Exception {
        ProjectData identity = new ProjectData("g.nes", SHA, TileMapping.identity(4), List.of(HERO));
        ProjectData swapped = new ProjectData("g.nes", SHA, TileMapping.identity(4).swapped(0, 3), List.of(HERO));
        assertEquals(HERO, ProjectFile.parse(ProjectFile.format(identity)).compositions().get(0));
        assertEquals(HERO, ProjectFile.parse(ProjectFile.format(swapped)).compositions().get(0));
    }

    // ---- versions ----
    @Test void version1FilesStillLoadWithNoCompositions() throws Exception {
        ProjectData d = ProjectFile.parse(base(1));
        assertTrue(d.compositions().isEmpty()); assertEquals(TileMapping.of(new int[]{2, 0, 1, 3}), d.mapping());
    }
    @Test void version2FilesWithoutCompositionsAreValid() throws Exception { assertTrue(ProjectFile.parse(base(2)).compositions().isEmpty()); }
    @Test void compositionLinesAreNotAllowedInVersion1() {
        assertTrue(rejects(base(1) + "composition=Hero|1x1|0\n").getMessage().contains("Unknown key 'composition'"));
    }
    @Test void version3IsRejectedAsNewer() {
        String m = rejects(base(3)).getMessage(); assertTrue(m.contains("newer") && m.contains("version 3"));
    }
    @Test void compositionLinesMayAppearBeforeTheKeysThatDefineTheirLimits() throws Exception {
        String text = "NES-WORKBENCH-PROJECT 2\ncomposition=Hero|2x2|0,1,-1,2\nmapping=2,0,1,3\ntile-count=4\nrom-sha256=" + SHA + "\nrom-name=game.nes\n";
        assertEquals(data(HERO), ProjectFile.parse(text));
    }

    // ---- malformed compositions ----
    @Test void rejectsWrongFieldCounts() {
        rejects(v2("Hero")); rejects(v2("Hero|1x1")); rejects(v2("Hero|1x1|0|extra")); rejects(v2("")); rejects(v2("a|b|1x1|0"));
    }
    @Test void rejectsBadSizes() {
        for (String size : new String[]{"", "x", "1", "1x", "x1", "0x1", "1x0", "33x1", "1x33", "-1x1", "1.5x1", " 1x1", "001x1", "100x1", "1X1", "1x1x1"})
            rejects(v2("Hero|" + size + "|0"));
    }
    @Test void rejectsCellCountMismatches() {
        rejects(v2("Hero|2x2|0,1,-1")); rejects(v2("Hero|2x2|0,1,-1,2,3")); rejects(v2("Hero|2x2|")); rejects(v2("Hero|1x1|0,"));
    }
    @Test void rejectsMalformedCells() {
        for (String cells : new String[]{"0,1,-1,a", "0,1,-1, 2", "0,,-1,2", "0,1,-1,2.0", "0,1,-1,+2", "0,1,-1,9999999999"}) rejects(v2("Hero|2x2|" + cells));
    }
    @Test void rejectsReferencesOutsideTheTileRange() {
        assertTrue(rejects(v2("Hero|2x2|0,1,-1,4")).getMessage().contains("references tile 4"));
        assertTrue(rejects(v2("Hero|2x2|0,1,-2,2")).getMessage().contains("outside"));
        assertDoesNotThrow(() -> ProjectFile.parse(v2("Hero|2x2|3,3,3,3")));
    }
    @Test void rejectsInvalidNames() {
        for (String name : new String[]{"", " Hero", "Hero ", "x".repeat(65)}) rejects(v2(name + "|1x1|0"));
    }
    @Test void rejectsDuplicateNames() {
        assertTrue(rejects(v2("Hero|1x1|0", "Other|1x1|1", "Hero|1x1|2")).getMessage().contains("Duplicate composition name 'Hero'"));
    }
    @Test void errorsIdentifyTheCompositionByNumberAndName() {
        String m = rejects(v2("Good|1x1|0", "Broken|1x1|9")).getMessage();
        assertTrue(m.contains("Composition 2") && m.contains("Broken"), m);
    }
    @Test void tooManyCompositionsAreRejected() {
        StringBuilder sb = new StringBuilder(base(2));
        for (int i = 0; i <= ProjectData.MAX_COMPOSITIONS; i++) sb.append("composition=c").append(i).append("|1x1|0\n");
        assertTrue(rejects(sb.toString()).getMessage().contains("at most " + ProjectData.MAX_COMPOSITIONS));
    }

    // ---- ProjectData ----
    @Test void projectDataRejectsDuplicateNamesAndTooManyCompositions() {
        assertThrows(IllegalArgumentException.class, () -> data(HERO, HERO));
        List<SpriteComposition> many = new ArrayList<>();
        for (int i = 0; i <= ProjectData.MAX_COMPOSITIONS; i++) many.add(SpriteComposition.empty("c" + i, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new ProjectData("g.nes", SHA, TileMapping.identity(1), many));
    }
    @Test void projectDataCopiesAndRejectsNulls() {
        List<SpriteComposition> list = new ArrayList<>(List.of(HERO));
        ProjectData d = new ProjectData("g.nes", SHA, TileMapping.identity(4), list);
        list.clear();
        assertEquals(1, d.compositions().size());
        assertThrows(UnsupportedOperationException.class, () -> d.compositions().add(HERO));
        assertThrows(NullPointerException.class, () -> new ProjectData("g.nes", SHA, TileMapping.identity(1), null));
        List<SpriteComposition> withNull = new ArrayList<>(); withNull.add(null);
        assertThrows(NullPointerException.class, () -> new ProjectData("g.nes", SHA, TileMapping.identity(1), withNull));
    }
    @Test void threeArgumentConstructorMeansNoCompositions() { assertEquals(data(), new ProjectData("game.nes", SHA, TileMapping.of(new int[]{2, 0, 1, 3}))); }

    // ---- matching a project to a ROM ----
    private static RomImage rom() throws Exception {
        byte[] b = new byte[16 + 16384 + 8192]; b[0] = 'N'; b[1] = 'E'; b[2] = 'S'; b[3] = 0x1A; b[4] = 1; b[5] = 1;
        for (int i = 16 + 16384; i < b.length; i++) b[i] = (byte) (i * 7);
        return RomImage.parse(b);
    }
    @Test void projectVerifiesWhenEveryReferenceExistsInTheRom() throws Exception {
        RomImage r = rom();
        SpriteComposition edge = SpriteComposition.empty("Edge", 1, 1).withTile(0, 0, 511);
        ProjectData d = ProjectData.forRom("g.nes", r, TileMapping.identity(512), List.of(HERO, edge));
        assertDoesNotThrow(() -> d.verifyAgainst(r));
    }
    @Test void projectRejectsCompositionsReferencingTilesTheRomLacks() throws Exception {
        RomImage r = rom();
        SpriteComposition past = SpriteComposition.empty("Past", 1, 1).withTile(0, 0, 512);
        ProjectData d = ProjectData.forRom("g.nes", r, TileMapping.identity(512), List.of(past));
        String m = assertThrows(ProjectFormatException.class, () -> d.verifyAgainst(r)).getMessage();
        assertTrue(m.contains("'Past'") && m.contains("512"), m);
    }
}
