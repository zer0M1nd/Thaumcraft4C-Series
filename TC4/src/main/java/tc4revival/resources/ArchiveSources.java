package tc4revival.resources;

import com.google.gson.*;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Expected archive digests keyed by the publisher's URL, including browser-only variants. */
public record ArchiveSources(Map<String,String> hashes, List<String> manualPages) {
    public ArchiveSources {
        hashes = Collections.unmodifiableMap(new LinkedHashMap<>(hashes));
        manualPages = List.copyOf(manualPages);
    }

    private static final JsonObject CATALOG = catalog();
    private static JsonObject catalog() {
        try (var in = ArchiveSources.class.getResourceAsStream("/tc4revival/legacy-manual-pages.json")) {
            if (in == null) throw new IOException("Missing legacy manual page catalog");
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }

    ArchiveSources withCatalog(String primaryHash, String legacyPage) {
        var entries = CATALOG.getAsJsonArray(primaryHash);
        var pages = new LinkedHashSet<>(manualPages);
        var pins = new LinkedHashMap<>(hashes);
        if (entries != null && !entries.isEmpty()) {
            if (URI.create(legacyPage).getPath().endsWith(".jar") && !legacyPage.equals(CurseForgeDownload.HEARTWORK_FIX.toString()))
                pages.remove(legacyPage);
            for (var entry : entries) {
                var data = entry.getAsJsonObject();
                String page = data.get("url").getAsString();
                pages.add(page);
                put(pins, page, data.has("sha256") ? data.get("sha256").getAsString() : primaryHash);
            }
        }
        if (legacyPage.equals("https://wwamr.lanzoum.com/ivivH4909gfe")) {
            String github = "https://github.com/cdj68765/TC4AutoResearch/releases/tag/0.0.3";
            pages.add(github);
            pins.put(github, primaryHash);
        }
        return new ArchiveSources(pins, List.copyOf(pages));
    }

    static ArchiveSources legacy(String hash, String page, List<String> downloads) {
        var hashes = new LinkedHashMap<String,String>();
        for (String url : downloads) hashes.put(url, hash);
        hashes.putIfAbsent(page, hash);
        return new ArchiveSources(hashes, List.of(page));
    }

    static List<String> read(JsonObject declaration, String field, String fallbackHash, Map<String,String> hashes) throws IOException {
        var urls = new ArrayList<String>();
        if (declaration.has(field)) for (var value : declaration.getAsJsonArray(field)) {
            String url = value.isJsonObject() ? value.getAsJsonObject().get("url").getAsString() : value.getAsString();
            String hash = value.isJsonObject() && value.getAsJsonObject().has("sha256")
                    ? value.getAsJsonObject().get("sha256").getAsString() : fallbackHash;
            if (!hash.matches("[a-fA-F0-9]{64}")) throw new IOException("Invalid SHA-256 for " + url);
            put(hashes, url, hash.toLowerCase(Locale.ROOT));
            urls.add(url);
        }
        return urls;
    }

    private static void put(Map<String,String> hashes, String url, String digest) {
        var prior = hashes.putIfAbsent(url, digest);
        if (prior != null && !prior.equals(digest)) throw new IllegalArgumentException("Conflicting SHA-256 for " + url);
    }

    ArchiveSources merge(ArchiveSources other) {
        var combined = new LinkedHashMap<>(hashes);
        other.hashes.forEach((url, hash) -> put(combined, url, hash));
        var pages = new LinkedHashSet<>(manualPages);
        pages.addAll(other.manualPages);
        return new ArchiveSources(combined, List.copyOf(pages));
    }
}
