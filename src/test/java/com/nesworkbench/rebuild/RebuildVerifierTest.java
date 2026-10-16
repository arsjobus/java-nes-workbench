package com.nesworkbench.rebuild;

import com.nesworkbench.rom.RomImage;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

class RebuildVerifierTest {
    private static final byte[] ROM = RebuildFixtures.make(1, 1, true, 5);
    private static final int CHR = RebuildFixtures.chrOffset(1, true);
    private static final ChrEdits EDITS = ChrEdits.empty().withTile(4, RebuildFixtures.tileData(8));

    private static RomImage src() throws Exception { return RomImage.parse(ROM.clone()); }
    private static byte[] good() throws Exception { return RomRebuilder.rebuild(src(), EDITS); }
    private static String failure(byte[] bad, ChrEdits edits) throws Exception {
        return assertThrows(RebuildException.class, () -> RebuildVerifier.verify(src(), edits, bad)).getMessage();
    }

    @Test void acceptsCorrectOutputWithAndWithoutEdits() throws Exception {
        assertDoesNotThrow(() -> RebuildVerifier.verify(src(), EDITS, good()));
        assertDoesNotThrow(() -> RebuildVerifier.verify(src(), ChrEdits.empty(), ROM.clone()));
    }
    @Test void rejectsTruncatedOutput() throws Exception {
        String m = failure(Arrays.copyOf(good(), 100), EDITS);
        assertTrue(m.contains("not a valid ROM"), m);
    }
    @Test void rejectsBadMagic() throws Exception { byte[] b = good(); b[0] = 'X'; assertTrue(failure(b, EDITS).contains("not a valid ROM")); }
    @Test void rejectsNes20Header() throws Exception { byte[] b = good(); b[7] = 0x08; assertTrue(failure(b, EDITS).contains("not a valid ROM")); }
    @Test void rejectsEmptyOutput() throws Exception { assertTrue(failure(new byte[0], EDITS).contains("not a valid ROM")); }
    @Test void rejectsOutputWithBytesAppended() throws Exception {
        String m = failure(Arrays.copyOf(good(), ROM.length + 1), EDITS);
        assertTrue(m.contains("expected " + ROM.length), m);
    }
    @Test void rejectsOutputWithTrailingBytesRemoved() throws Exception {
        assertTrue(failure(Arrays.copyOf(good(), ROM.length - 1), EDITS).contains("expected " + ROM.length));
    }
    @Test void rejectsChangedHeaderByte() throws Exception {
        byte[] b = good(); b[8] ^= 1;
        assertTrue(failure(b, EDITS).contains("$00008"));
    }
    @Test void rejectsChangedLayoutFlags() throws Exception {
        byte[] b = good(); b[6] &= ~0x04;   // drops the trainer flag, so the layout no longer matches
        assertNotNull(failure(b, EDITS));
    }
    @Test void rejectsChangedMapper() throws Exception {
        byte[] b = good(); b[6] |= 0x10;
        assertTrue(failure(b, EDITS).contains("different ROM layout"));
    }
    @Test void rejectsChangedTrainerPrgAndTrailingBytes() throws Exception {
        for (int offset : new int[]{16, 16 + 512, ROM.length - 1}) {
            byte[] b = good(); b[offset] ^= 0x80;
            assertTrue(failure(b, EDITS).contains(String.format("$%05X", offset)), "offset " + offset);
        }
    }
    @Test void rejectsUneditedTileThatChanged() throws Exception {
        byte[] b = good(); b[CHR + 9 * 16 + 3] ^= 1;
        String m = failure(b, EDITS);
        assertTrue(m.contains("Tile 9") && m.contains("not edited"), m);
    }
    @Test void rejectsEditThatWasNotApplied() throws Exception {
        String m = failure(ROM.clone(), EDITS);
        assertTrue(m.contains("Tile 4") && m.contains("does not contain its edit"), m);
    }
    @Test void rejectsEditedTileWithWrongBytes() throws Exception {
        byte[] b = good(); b[CHR + 4 * 16 + 15] ^= 1;
        assertTrue(failure(b, EDITS).contains("Tile 4"));
    }
    @Test void rejectsEditsBeyondTheRomsTiles() throws Exception {
        assertTrue(failure(ROM.clone(), ChrEdits.empty().withTile(512, new byte[16])).contains("512"));
    }
    @Test void rejectsNulls() throws Exception {
        assertThrows(NullPointerException.class, () -> RebuildVerifier.verify(null, EDITS, ROM));
        assertThrows(NullPointerException.class, () -> RebuildVerifier.verify(src(), null, ROM));
        assertThrows(NullPointerException.class, () -> RebuildVerifier.verify(src(), EDITS, null));
    }
}
