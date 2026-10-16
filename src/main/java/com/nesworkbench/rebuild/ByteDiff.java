package com.nesworkbench.rebuild;

import com.nesworkbench.rom.RomImage;
import com.nesworkbench.rom.RomSummary;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;

/**
 * Byte-level difference between an original file and a rebuilt one: the maximal runs of differing bytes over the
 * common length, plus any length difference. Comparing never modifies either array. No JavaFX dependency.
 */
public final class ByteDiff {
    public static final int MAX_REPORT_RANGES = 200;

    /** A run of {@code length} consecutive differing bytes starting at file offset {@code start}. */
    public record Range(int start, int length) { public int end() { return start + length; } }

    private final int originalLength, rebuiltLength, changedBytes;
    private final List<Range> ranges;

    private ByteDiff(int originalLength, int rebuiltLength, List<Range> ranges) {
        this.originalLength = originalLength; this.rebuiltLength = rebuiltLength; this.ranges = List.copyOf(ranges);
        int n = 0; for (Range r : ranges) n += r.length(); this.changedBytes = n;
    }

    public static ByteDiff compare(byte[] original, byte[] rebuilt) {
        Objects.requireNonNull(original, "original must not be null");
        Objects.requireNonNull(rebuilt, "rebuilt must not be null");
        int common = Math.min(original.length, rebuilt.length);
        List<Range> ranges = new ArrayList<>();
        int i = 0;
        while (i < common) {
            if (original[i] == rebuilt[i]) { i++; continue; }
            int start = i;
            while (i < common && original[i] != rebuilt[i]) i++;
            ranges.add(new Range(start, i - start));
        }
        return new ByteDiff(original.length, rebuilt.length, ranges);
    }

    public int originalLength() { return originalLength; }
    public int rebuiltLength() { return rebuiltLength; }
    /** Differing bytes within the common length (excludes bytes added or removed at the end). */
    public int changedBytes() { return changedBytes; }
    public List<Range> ranges() { return ranges; }
    public boolean identical() { return ranges.isEmpty() && originalLength == rebuiltLength; }

    private enum Region {
        HEADER("Header"), TRAINER("Trainer"), PRG("PRG-ROM"), CHR("CHR-ROM"), TRAILING("Trailing bytes");
        final String label; Region(String label) { this.label = label; }
    }

    /** Plain-text report: totals, a per-region summary, then each changed range (at most {@value #MAX_REPORT_RANGES}) split at region boundaries. */
    public String report(RomSummary layout) {
        Objects.requireNonNull(layout, "layout must not be null");
        StringBuilder sb = new StringBuilder("Byte-level difference report\n");
        sb.append("Original: ").append(originalLength).append(" bytes; rebuilt: ").append(rebuiltLength).append(" bytes");
        if (originalLength != rebuiltLength) sb.append(" (length differs by ").append(rebuiltLength - originalLength).append(')');
        sb.append('\n');
        if (identical()) return sb.append("The files are identical.\n").toString();
        sb.append("Changed bytes: ").append(changedBytes).append(" in ").append(ranges.size()).append(" range(s).\n");

        int[] bounds = regionStarts(layout);
        // Split every range at region boundaries so each piece lies in exactly one region.
        List<Range> pieces = new ArrayList<>(); List<Region> pieceRegion = new ArrayList<>();
        int[] regionBytes = new int[Region.values().length];
        BitSet chrTiles = new BitSet();
        for (Range r : ranges) {
            int pos = r.start();
            while (pos < r.end()) {
                Region region = regionOf(pos, bounds);
                int end = Math.min(r.end(), regionEnd(region, bounds, Math.max(originalLength, rebuiltLength)));
                pieces.add(new Range(pos, end - pos)); pieceRegion.add(region);
                regionBytes[region.ordinal()] += end - pos;
                if (region == Region.CHR) chrTiles.set((pos - bounds[Region.CHR.ordinal()]) / 16, (end - 1 - bounds[Region.CHR.ordinal()]) / 16 + 1);
                pos = end;
            }
        }
        sb.append("By region:");
        for (Region region : Region.values()) sb.append(' ').append(region.label).append(' ').append(regionBytes[region.ordinal()]).append(';');
        sb.append('\n');
        if (chrTiles.cardinality() > 0) sb.append("CHR tiles touched: ").append(chrTiles.cardinality()).append('\n');

        for (int i = 0; i < pieces.size(); i++) {
            if (i == MAX_REPORT_RANGES) { sb.append("... and ").append(pieces.size() - i).append(" more range(s).\n"); break; }
            Range p = pieces.get(i); Region region = pieceRegion.get(i);
            sb.append(String.format("$%05X..$%05X (%d byte(s)) %s", p.start(), p.end() - 1, p.length(), region.label));
            if (region == Region.CHR) {
                int first = (p.start() - bounds[Region.CHR.ordinal()]) / 16, last = (p.end() - 1 - bounds[Region.CHR.ordinal()]) / 16;
                sb.append(first == last ? String.format(", tile %d ($%X)", first, first) : String.format(", tiles %d..%d ($%X..$%X)", first, last, first, last));
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    /** Start offsets of header, trainer, PRG, CHR and trailing regions. */
    private static int[] regionStarts(RomSummary s) {
        int trainer = RomImage.HEADER_SIZE, prg = trainer + (s.hasTrainer() ? RomImage.TRAINER_SIZE : 0);
        return new int[] {0, trainer, prg, s.chrFileOffset(), s.chrFileOffset() + s.chrRomBytes()};
    }

    private static Region regionOf(int offset, int[] starts) {
        Region[] all = Region.values();
        for (int i = all.length - 1; i > 0; i--) if (offset >= starts[i]) return all[i];
        return Region.HEADER;
    }

    private static int regionEnd(Region region, int[] starts, int fileEnd) {
        int next = region.ordinal() + 1;
        return next < starts.length ? starts[next] : fileEnd;
    }
}
