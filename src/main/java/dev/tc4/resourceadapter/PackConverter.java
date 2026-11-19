package dev.tc4.resourceadapter;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

final class PackConverter {
    static final String MANIFEST = "thaumcraftresourceadapter-manifest.json";
    private static final int FORMAT = 2;
    static final int MAX_ENTRY = 32 * 1024 * 1024;
    private static final int MAX_JSON = 4 * 1024 * 1024;
    private static final long MAX_OUTPUT = 512L * 1024 * 1024;
    private static final Gson JSON = new Gson();
    private static final byte[] PNG_SIGNATURE = {(byte)137, 80, 78, 71, 13, 10, 26, 10};
    private PackConverter() {}

    record Plan(Map<String, String> files, Map<String, String> inputHashes, String fingerprint, int textures) {}

    /** Hash only resources that can affect this output, including animation, icon and source credits. */
    static Plan plan(LegacyPack source, ResourceMapping mapping) throws IOException {
        var matches = mapping.matches(source.entries.keySet(), source.format);
        if (matches.isEmpty()) return null;
        var files = new TreeMap<String, String>();
        for (var rule : matches) {
            files.put(rule.target(), rule.source());
            String sidecar = rule.source() + ".mcmeta";
            if (source.entries.containsKey(sidecar)) files.put(rule.target() + ".mcmeta", sidecar);
        }
        if (source.entries.containsKey("pack.png")) files.put("pack.png", "pack.png");
        for (String name : source.entries.keySet()) {
            if (LegacyPack.attribution(name)) files.put("upstream/" + name, name);
        }
        long total = 0;
        for (String name : files.values()) {
            long size = source.entries.get(name).size();
            if (size < 0 || size > MAX_ENTRY || (total += size) > MAX_OUTPUT) {
                throw new IOException("Converted pack exceeds its size limit at " + name);
            }
        }
        var hashes = new TreeMap<String, String>();
        for (String name : new TreeSet<>(files.values())) hashes.put(name, sha256(read(source, name)));
        var digest = digest();
        update(digest, Integer.toString(FORMAT));
        update(digest, mapping.fingerprint());
        update(digest, source.name);
        update(digest, Integer.toString(source.format));
        update(digest, JSON.toJson(source.description));
        files.forEach((target, input) -> { update(digest, target); update(digest, input); });
        hashes.forEach((name, hash) -> { update(digest, name); update(digest, hash); });
        return new Plan(files, hashes, HexFormat.of().formatHex(digest.digest()), matches.size());
    }

    static void write(Path output, LegacyPack source, Plan plan) throws IOException {
        var manifest = new JsonObject();
        manifest.addProperty("format", FORMAT);
        manifest.addProperty("source", source.name);
        manifest.addProperty("sourcePackFormat", source.format);
        manifest.addProperty("fingerprint", plan.fingerprint());
        manifest.addProperty("textures", plan.textures());
        var hashes = new JsonObject();
        long total = 0;
        try (var zip = new ZipOutputStream(Files.newOutputStream(output))) {
            for (var pair : plan.files().entrySet()) {
                String name = pair.getValue();
                byte[] bytes = read(source, name);
                if (!sha256(bytes).equals(plan.inputHashes().get(name))) {
                    throw new IOException("Source changed during conversion: " + name);
                }
                if (name.endsWith(".png")) {
                    if (bytes.length < 24 || !Arrays.equals(bytes, 0, 8, PNG_SIGNATURE, 0, 8)) {
                        throw new IOException("Invalid PNG: " + name);
                    }
                } else if (name.endsWith(".mcmeta")) {
                    bytes = normalizeMetadata(bytes);
                }
                total += bytes.length;
                if (total > MAX_OUTPUT) throw new IOException("Converted pack exceeds its size limit");
                put(zip, pair.getKey(), bytes, hashes);
            }
            var pack = new JsonObject();
            pack.addProperty("pack_format", 15);
            if (source.description == null || source.description.isJsonNull()) pack.addProperty("description", source.name);
            else pack.add("description", source.description);
            var metadata = new JsonObject();
            metadata.add("pack", pack);
            put(zip, "pack.mcmeta", utf8(JSON.toJson(metadata)), hashes);
            manifest.add("files", hashes);
            put(zip, MANIFEST, utf8(JSON.toJson(manifest)), null);
        }
    }

    private static byte[] read(LegacyPack pack, String name) throws IOException {
        return pack.entries.get(name).read(name.endsWith(".mcmeta") ? MAX_JSON : MAX_ENTRY);
    }

    private static byte[] normalizeMetadata(byte[] bytes) throws IOException {
        try {
            String text = new String(bytes, StandardCharsets.UTF_8);
            var json = JsonParser.parseString(text.startsWith("\uFEFF") ? text.substring(1) : text).getAsJsonObject();
            if (json.has("animation") && !json.get("animation").isJsonObject()) throw new IllegalArgumentException();
            return utf8(JSON.toJson(json));
        } catch (RuntimeException error) {
            throw new IOException("Invalid texture metadata", error);
        }
    }

    private static void put(ZipOutputStream zip, String name, byte[] bytes, JsonObject hashes) throws IOException {
        var entry = new ZipEntry(name);
        entry.setTime(0);
        zip.putNextEntry(entry);
        zip.write(bytes);
        zip.closeEntry();
        if (hashes != null) hashes.addProperty(name, sha256(bytes));
    }

    static boolean valid(Path path, Plan plan) {
        if (!Files.isRegularFile(path, java.nio.file.LinkOption.NOFOLLOW_LINKS)) return false;
        try (var zip = new ZipFile(path.toFile())) {
            var manifest = JsonParser.parseString(new String(read(zip, MANIFEST, MAX_JSON), StandardCharsets.UTF_8)).getAsJsonObject();
            if (manifest.get("format").getAsInt() != FORMAT || !manifest.get("fingerprint").getAsString().equals(plan.fingerprint())
                    || manifest.get("textures").getAsInt() != plan.textures()) return false;
            var hashes = manifest.getAsJsonObject("files");
            if (zip.size() != plan.files().size() + 2 || hashes.size() != plan.files().size() + 1
                    || !hashes.has("pack.mcmeta") || !hashes.keySet().containsAll(plan.files().keySet())) return false;
            long total = 0;
            for (var item : hashes.entrySet()) {
                byte[] bytes = read(zip, item.getKey(), MAX_ENTRY);
                String hash = sha256(bytes);
                String input = plan.files().get(item.getKey());
                total += bytes.length;
                if (total > MAX_OUTPUT + MAX_JSON || !hash.equals(item.getValue().getAsString())) return false;
                if (input != null && !input.endsWith(".mcmeta") && !hash.equals(plan.inputHashes().get(input))) return false;
            }
            return true;
        } catch (IOException | RuntimeException error) { return false; }
    }

    private static byte[] read(ZipFile zip, String name, int limit) throws IOException {
        var entry = zip.getEntry(name);
        if (entry == null) throw new IOException("Missing converted resource: " + name);
        return new LegacyPack.Entry(entry.getSize(), () -> zip.getInputStream(entry)).read(limit);
    }
    private static byte[] utf8(String text) { return text.getBytes(StandardCharsets.UTF_8); }
    private static void update(MessageDigest digest, String text) {
        digest.update(utf8(text));
        digest.update((byte)0);
    }
    private static MessageDigest digest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
    static String sha256(byte[] bytes) { return HexFormat.of().formatHex(digest().digest(bytes)); }
}
