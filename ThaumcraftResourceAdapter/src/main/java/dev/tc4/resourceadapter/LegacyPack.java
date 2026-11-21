package dev.tc4.resourceadapter;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipException;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;

/** Reads only one pack root, either at the top level or under one enclosing directory. */
final class LegacyPack implements Closeable {
    private static final int MAX_ENTRIES = 150_000;
    private static final int MAX_METADATA = 256 * 1024;
    final String name;
    final int format;
    final JsonElement description;
    final Map<String, Entry> entries = new HashMap<>();
    private final ZipFile zip;

    @FunctionalInterface
    interface Opener { InputStream open() throws IOException; }
    record Entry(long size, Opener opener) {
        byte[] read(int limit) throws IOException {
            if (size > limit) throw new IOException("Resource exceeds size limit (" + limit + " bytes)");
            try (var in = opener.open()) {
                byte[] bytes = in.readNBytes(limit + 1);
                if (bytes.length > limit) throw new IOException("Resource exceeds size limit (" + limit + " bytes)");
                return bytes;
            }
        }
    }

    private LegacyPack(Path path, int format, JsonElement description, ZipFile zip) {
        this.name = path.getFileName().toString();
        this.format = format;
        this.description = description;
        this.zip = zip;
    }

    static boolean candidate(Path path) {
        return Files.isDirectory(path, NOFOLLOW_LINKS) || Files.isRegularFile(path, NOFOLLOW_LINKS)
                && path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip");
    }

    /** Returns null for ordinary directories, modern packs and the previous adapter's generated ZIP. */
    static LegacyPack open(Path path) throws IOException {
        if (Files.isDirectory(path, NOFOLLOW_LINKS)) return directory(path);
        ZipFile zip = openZip(path);
        boolean keepOpen = false;
        try {
            ZipEntry metadata = zip.getEntry("pack.mcmeta");
            if (metadata == null) {
                if (zip.size() > MAX_ENTRIES) throw new IOException("Too many ZIP entries");
                var candidates = zip.stream().filter(e -> !e.isDirectory()
                        && e.getName().matches("[^/]+/pack\\.mcmeta") && !e.getName().startsWith("__MACOSX/")
                        && ResourceMapping.safe(e.getName())).limit(2).toList();
                if (candidates.size() != 1) return null;
                metadata = candidates.get(0);
            }
            var pack = metadata(path, zipped(zip, metadata), zip);
            if (pack == null) return null;
            if (zip.size() > MAX_ENTRIES) throw new IOException("Too many ZIP entries");
            String root = metadata.getName().substring(0, metadata.getName().lastIndexOf('/') + 1);
            for (var it = zip.entries(); it.hasMoreElements();) {
                var entry = it.nextElement();
                if (!entry.isDirectory() && entry.getName().startsWith(root)) {
                    String relative = entry.getName().substring(root.length());
                    pack.index(relative, zipped(zip, entry));
                }
            }
            keepOpen = true;
            return pack;
        } finally {
            if (!keepOpen) zip.close();
        }
    }

    private static ZipFile openZip(Path path) throws IOException {
        try { return new ZipFile(path.toFile()); }
        catch (ZipException invalid) {
            // Older ZIP writers use the original ZIP character set for unflagged names.
            // The JDK otherwise rejects an entire archive for a non-UTF-8 wrapper or readme.
            if (invalid.getMessage() == null || !invalid.getMessage().contains("entry name")) throw invalid;
            return new ZipFile(path.toFile(), Charset.forName("IBM437"));
        }
    }

    private static LegacyPack directory(Path path) throws IOException {
        Path root = path;
        if (!Files.isRegularFile(root.resolve("pack.mcmeta"), NOFOLLOW_LINKS)) {
            try (var children = Files.list(path)) {
                var wrapped = children.filter(p -> Files.isDirectory(p, NOFOLLOW_LINKS)
                        && !p.getFileName().toString().equals("__MACOSX")
                        && Files.isRegularFile(p.resolve("pack.mcmeta"), NOFOLLOW_LINKS)).limit(2).toList();
                if (wrapped.size() != 1) return null;
                root = wrapped.get(0);
            }
        }
        var pack = metadata(path, file(root.resolve("pack.mcmeta")), null);
        if (pack == null) return null;
        final Path packRoot = root;
        try (var children = Files.list(root)) {
            for (var it = children.iterator(); it.hasNext();) {
                Path child = it.next();
                String name = child.getFileName().toString();
                if (Files.isRegularFile(child, NOFOLLOW_LINKS) && interesting(name.toLowerCase(Locale.ROOT))) {
                    pack.index(name, file(child));
                }
            }
        }
        Path assets = root.resolve("assets");
        if (Files.isDirectory(assets, NOFOLLOW_LINKS)) {
            Files.walkFileTree(assets, new SimpleFileVisitor<>() {
                int visited;
                @Override public FileVisitResult visitFile(Path child, BasicFileAttributes attrs) throws IOException {
                    if (++visited > MAX_ENTRIES) throw new IOException("Too many files in resource pack");
                    if (attrs.isRegularFile()) {
                        String relative = packRoot.relativize(child).toString().replace(child.getFileSystem().getSeparator(), "/");
                        pack.index(relative, new Entry(attrs.size(), () -> Files.newInputStream(child, NOFOLLOW_LINKS)));
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        return pack;
    }

    private static Entry file(Path path) throws IOException {
        return new Entry(Files.size(path), () -> Files.newInputStream(path, NOFOLLOW_LINKS));
    }
    private static Entry zipped(ZipFile zip, ZipEntry entry) {
        return new Entry(entry.getSize(), () -> zip.getInputStream(entry));
    }

    private static LegacyPack metadata(Path path, Entry entry, ZipFile zip) throws IOException {
        try {
            String text = new String(entry.read(MAX_METADATA), StandardCharsets.UTF_8);
            JsonObject pack = JsonParser.parseString(text.startsWith("\uFEFF") ? text.substring(1) : text)
                    .getAsJsonObject().getAsJsonObject("pack");
            String format = pack.get("pack_format").getAsString();
            if (!format.matches("[123]")) return null;
            return new LegacyPack(path, Integer.parseInt(format), pack.get("description"), zip);
        } catch (RuntimeException e) {
            throw new IOException("Invalid pack.mcmeta", e);
        }
    }

    private void index(String relative, Entry entry) throws IOException {
        String key = relative.toLowerCase(Locale.ROOT);
        if (!ResourceMapping.safe(relative)) throw new IOException("Unsafe archive path: " + relative);
        if (interesting(key) && entries.putIfAbsent(key, entry) != null) {
            throw new IOException("Ambiguous resource path: " + relative);
        }
    }

    private static boolean interesting(String key) {
        if (key.startsWith("assets/")) return key.endsWith(".png") || key.endsWith(".png.mcmeta");
        return key.equals("pack.png") || attribution(key);
    }

    static boolean attribution(String path) {
        return path.indexOf('/') < 0 && path.matches("(?:licen[cs]e|credits|authors|copying|notice|readme)(?:\\.(?:md|txt))?");
    }

    @Override public void close() throws IOException {
        if (zip != null) zip.close();
    }
}
