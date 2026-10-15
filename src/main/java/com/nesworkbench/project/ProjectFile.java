package com.nesworkbench.project;

import com.nesworkbench.mapping.TileMapping;
import com.nesworkbench.sprite.SpriteComposition;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Reads and writes versioned project files. The format is UTF-8 text and is documented in
 * {@code docs/PROJECT_FORMAT.md}. Parsing is strict: anything unexpected is rejected, never guessed.
 * Version 1 holds a tile mapping; version 2 adds repeatable {@code composition} lines. A project without
 * compositions is still written as version 1, byte-for-byte as before, so older programs can open it.
 */
public final class ProjectFile {
    public static final String MAGIC = "NES-WORKBENCH-PROJECT";
    public static final int CURRENT_VERSION = 2;
    public static final String EXTENSION = ".nesproj";
    /** Generous bound (a 255-bank CHR mapping is under 1 MiB of text), so bad inputs fail before being read. */
    public static final long MAX_FILE_BYTES = 4L * 1024 * 1024;

    /** Most tiles an iNES 1.0 ROM can hold: 255 CHR banks of 8 KiB at 16 bytes per tile. */
    public static final int MAX_TILES = 255 * 8192 / 16;

    private static final Set<String> KEYS = Set.of("rom-name", "rom-sha256", "tile-count", "mapping");
    /** Repeatable key, version 2 and later. Value: {@code name|<w>x<h>|<cell>,<cell>,...}, a cell being a tile index or -1 for empty. */
    private static final String COMPOSITION_KEY = "composition";
    private ProjectFile() {}

    public static String format(ProjectData data) {
        Objects.requireNonNull(data, "data must not be null");
        if (data.romName().indexOf('\n') >= 0 || data.romName().indexOf('\r') >= 0)
            throw new IllegalArgumentException("ROM name must not contain line breaks.");
        if (!data.romSha256().matches("[0-9a-f]{64}")) throw new IllegalArgumentException("ROM SHA-256 must be 64 lowercase hex digits.");
        int[] entries = data.mapping().toArray();
        for (SpriteComposition c : data.compositions())
            if (c.maxSourceTile() >= entries.length)
                throw new IllegalArgumentException("Composition '" + c.name() + "' references tile " + c.maxSourceTile() + ", outside the " + entries.length + " mapped tiles.");
        StringBuilder sb = new StringBuilder();
        sb.append(MAGIC).append(' ').append(data.compositions().isEmpty() ? 1 : CURRENT_VERSION).append('\n');
        sb.append("rom-name=").append(data.romName()).append('\n');
        sb.append("rom-sha256=").append(data.romSha256()).append('\n');
        sb.append("tile-count=").append(entries.length).append('\n');
        sb.append("mapping=");
        for (int i = 0; i < entries.length; i++) { if (i > 0) sb.append(','); sb.append(entries[i]); }
        sb.append('\n');
        for (SpriteComposition c : data.compositions()) {
            sb.append(COMPOSITION_KEY).append('=').append(c.name()).append('|').append(c.widthTiles()).append('x').append(c.heightTiles()).append('|');
            int[] cells = c.cells();
            for (int i = 0; i < cells.length; i++) { if (i > 0) sb.append(','); sb.append(cells[i]); }
            sb.append('\n');
        }
        return sb.toString();
    }

    public static ProjectData parse(String text) throws ProjectFormatException {
        Objects.requireNonNull(text, "text must not be null");
        String[] lines = text.split("\r?\n", -1);
        if (lines.length == 0 || lines[0].isEmpty()) throw new ProjectFormatException("Project file is empty.");
        int version = checkHeader(lines[0]);

        Map<String, String> values = new LinkedHashMap<>();
        List<String> compositionLines = new ArrayList<>();
        for (int n = 1; n < lines.length; n++) {
            String line = lines[n];
            if (line.isEmpty() && n == lines.length - 1) break; // single trailing newline
            int eq = line.indexOf('=');
            if (eq < 0) throw new ProjectFormatException("Line " + (n + 1) + " is not a key=value pair.");
            String key = line.substring(0, eq);
            if (version >= 2 && key.equals(COMPOSITION_KEY)) { compositionLines.add(line.substring(eq + 1)); continue; }
            if (!KEYS.contains(key)) throw new ProjectFormatException("Unknown key '" + key + "' on line " + (n + 1) + ".");
            if (values.put(key, line.substring(eq + 1)) != null) throw new ProjectFormatException("Duplicate key '" + key + "'.");
        }
        for (String key : KEYS) if (!values.containsKey(key)) throw new ProjectFormatException("Missing required key '" + key + "'.");

        String name = values.get("rom-name");
        String sha = values.get("rom-sha256");
        if (!sha.matches("[0-9a-f]{64}")) throw new ProjectFormatException("rom-sha256 must be 64 lowercase hex digits.");
        int count = parseInt("tile-count", values.get("tile-count"));
        if (count < 0) throw new ProjectFormatException("tile-count must not be negative.");
        if (count > MAX_TILES) throw new ProjectFormatException("tile-count " + count + " exceeds the " + MAX_TILES + " tiles an iNES 1.0 ROM can hold.");

        String list = values.get("mapping");
        int[] entries = new int[count];
        if (count == 0) {
            if (!list.isEmpty()) throw new ProjectFormatException("mapping must be empty when tile-count is 0.");
        } else {
            String[] parts = list.split(",", -1);
            if (parts.length != count) throw new ProjectFormatException("mapping has " + parts.length + " entries, but tile-count is " + count + ".");
            for (int i = 0; i < count; i++) entries[i] = parseInt("mapping entry " + i, parts[i]);
        }
        TileMapping mapping;
        try {
            mapping = TileMapping.of(entries);
        } catch (IllegalArgumentException ex) {
            throw new ProjectFormatException("Invalid mapping: " + ex.getMessage());
        }
        if (compositionLines.size() > ProjectData.MAX_COMPOSITIONS)
            throw new ProjectFormatException("Project has " + compositionLines.size() + " compositions; at most " + ProjectData.MAX_COMPOSITIONS + " are allowed.");
        List<SpriteComposition> compositions = new ArrayList<>(compositionLines.size());
        for (int i = 0; i < compositionLines.size(); i++) compositions.add(parseComposition(i + 1, compositionLines.get(i), count));
        try {
            return new ProjectData(name, sha, mapping, compositions);
        } catch (IllegalArgumentException ex) {
            throw new ProjectFormatException("Invalid compositions: " + ex.getMessage());
        }
    }

    private static SpriteComposition parseComposition(int number, String value, int tileCount) throws ProjectFormatException {
        String where = "Composition " + number;
        String[] fields = value.split("\\|", -1);
        if (fields.length != 3) throw new ProjectFormatException(where + " must have the form name|WxH|cells.");
        where += " ('" + fields[0] + "')";
        if (!fields[1].matches("[0-9]{1,2}x[0-9]{1,2}")) throw new ProjectFormatException(where + ": invalid size '" + fields[1] + "'.");
        int x = fields[1].indexOf('x');
        int w = Integer.parseInt(fields[1].substring(0, x)), h = Integer.parseInt(fields[1].substring(x + 1));
        if (w < SpriteComposition.MIN_DIMENSION || w > SpriteComposition.MAX_DIMENSION || h < SpriteComposition.MIN_DIMENSION || h > SpriteComposition.MAX_DIMENSION)
            throw new ProjectFormatException(where + ": size " + w + "x" + h + " is outside " + SpriteComposition.MIN_DIMENSION + ".." + SpriteComposition.MAX_DIMENSION + " tiles.");
        String[] parts = fields[2].split(",", -1);
        if (parts.length != w * h) throw new ProjectFormatException(where + " has " + parts.length + " cells, but " + w + "x" + h + " needs " + w * h + ".");
        int[] cells = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            cells[i] = parseInt(where + " cell " + i, parts[i]);
            if (cells[i] < SpriteComposition.EMPTY || cells[i] >= tileCount)
                throw new ProjectFormatException(where + " cell " + i + " references tile " + cells[i] + ", outside -1.." + (tileCount - 1) + ".");
        }
        try {
            return SpriteComposition.of(fields[0], w, h, cells);
        } catch (IllegalArgumentException ex) {
            throw new ProjectFormatException(where + ": " + ex.getMessage());
        }
    }

    public static ProjectData read(Path path) throws IOException, ProjectFormatException {
        Objects.requireNonNull(path, "path must not be null");
        if (!Files.isRegularFile(path)) throw new IOException("Not a readable file: " + path);
        long size = Files.size(path);
        if (size > MAX_FILE_BYTES) throw new ProjectFormatException("Project file is " + size + " bytes, larger than any valid project.");
        try {
            String text = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(Files.readAllBytes(path))).toString();
            return parse(text);
        } catch (CharacterCodingException ex) {
            throw new ProjectFormatException("Project file is not valid UTF-8 text.");
        }
    }

    /**
     * Writes a project file. Refuses to replace an existing file unless {@code overwrite} is true. A replacement is
     * written to a temporary sibling first, so a failed write cannot leave a half-written project behind.
     */
    public static void write(Path target, ProjectData data, boolean overwrite) throws IOException {
        Objects.requireNonNull(target, "target must not be null");
        byte[] bytes = format(data).getBytes(StandardCharsets.UTF_8);
        if (!overwrite) {
            if (Files.exists(target)) throw new FileAlreadyExistsException(target.toString());
            Files.write(target, bytes, java.nio.file.StandardOpenOption.CREATE_NEW, java.nio.file.StandardOpenOption.WRITE);
            return;
        }
        Path dir = target.toAbsolutePath().getParent();
        Path temp = Files.createTempFile(dir, ".nesproj-", ".tmp");
        try {
            Files.write(temp, bytes);
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private static int checkHeader(String line) throws ProjectFormatException {
        String[] parts = line.split(" ", -1);
        if (parts.length != 2 || !parts[0].equals(MAGIC)) throw new ProjectFormatException("Not a NES Workbench project file.");
        int version = parseInt("version", parts[1]);
        if (version < 1) throw new ProjectFormatException("Invalid project version " + version + ".");
        if (version > CURRENT_VERSION)
            throw new ProjectFormatException("Project version " + version + " was made by a newer workbench; this one reads up to version " + CURRENT_VERSION + ".");
        return version;
    }

    private static int parseInt(String what, String s) throws ProjectFormatException {
        if (!s.matches("[0-9]{1,9}|-[0-9]{1,9}")) throw new ProjectFormatException("Invalid number for " + what + ": '" + s + "'.");
        return Integer.parseInt(s);
    }
}
