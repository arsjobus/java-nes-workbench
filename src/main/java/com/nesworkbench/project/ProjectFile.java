package com.nesworkbench.project;

import com.nesworkbench.mapping.TileMapping;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Reads and writes versioned project files. The format is UTF-8 text and is documented in
 * {@code docs/PROJECT_FORMAT.md}. Parsing is strict: anything unexpected is rejected, never guessed.
 */
public final class ProjectFile {
    public static final String MAGIC = "NES-WORKBENCH-PROJECT";
    public static final int CURRENT_VERSION = 1;
    public static final String EXTENSION = ".nesproj";
    /** Generous bound (a 255-bank CHR mapping is under 1 MiB of text), so bad inputs fail before being read. */
    public static final long MAX_FILE_BYTES = 4L * 1024 * 1024;

    /** Most tiles an iNES 1.0 ROM can hold: 255 CHR banks of 8 KiB at 16 bytes per tile. */
    public static final int MAX_TILES = 255 * 8192 / 16;

    private static final Set<String> KEYS = Set.of("rom-name", "rom-sha256", "tile-count", "mapping");
    private ProjectFile() {}

    public static String format(ProjectData data) {
        Objects.requireNonNull(data, "data must not be null");
        if (data.romName().indexOf('\n') >= 0 || data.romName().indexOf('\r') >= 0)
            throw new IllegalArgumentException("ROM name must not contain line breaks.");
        if (!data.romSha256().matches("[0-9a-f]{64}")) throw new IllegalArgumentException("ROM SHA-256 must be 64 lowercase hex digits.");
        int[] entries = data.mapping().toArray();
        StringBuilder sb = new StringBuilder();
        sb.append(MAGIC).append(' ').append(CURRENT_VERSION).append('\n');
        sb.append("rom-name=").append(data.romName()).append('\n');
        sb.append("rom-sha256=").append(data.romSha256()).append('\n');
        sb.append("tile-count=").append(entries.length).append('\n');
        sb.append("mapping=");
        for (int i = 0; i < entries.length; i++) { if (i > 0) sb.append(','); sb.append(entries[i]); }
        return sb.append('\n').toString();
    }

    public static ProjectData parse(String text) throws ProjectFormatException {
        Objects.requireNonNull(text, "text must not be null");
        String[] lines = text.split("\r?\n", -1);
        if (lines.length == 0 || lines[0].isEmpty()) throw new ProjectFormatException("Project file is empty.");
        checkHeader(lines[0]);

        Map<String, String> values = new LinkedHashMap<>();
        for (int n = 1; n < lines.length; n++) {
            String line = lines[n];
            if (line.isEmpty() && n == lines.length - 1) break; // single trailing newline
            int eq = line.indexOf('=');
            if (eq < 0) throw new ProjectFormatException("Line " + (n + 1) + " is not a key=value pair.");
            String key = line.substring(0, eq);
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
        try {
            return new ProjectData(name, sha, TileMapping.of(entries));
        } catch (IllegalArgumentException ex) {
            throw new ProjectFormatException("Invalid mapping: " + ex.getMessage());
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

    private static void checkHeader(String line) throws ProjectFormatException {
        String[] parts = line.split(" ", -1);
        if (parts.length != 2 || !parts[0].equals(MAGIC)) throw new ProjectFormatException("Not a NES Workbench project file.");
        int version = parseInt("version", parts[1]);
        if (version < 1) throw new ProjectFormatException("Invalid project version " + version + ".");
        if (version > CURRENT_VERSION)
            throw new ProjectFormatException("Project version " + version + " was made by a newer workbench; this one reads up to version " + CURRENT_VERSION + ".");
    }

    private static int parseInt(String what, String s) throws ProjectFormatException {
        if (!s.matches("[0-9]{1,9}|-[0-9]{1,9}")) throw new ProjectFormatException("Invalid number for " + what + ": '" + s + "'.");
        return Integer.parseInt(s);
    }
}
