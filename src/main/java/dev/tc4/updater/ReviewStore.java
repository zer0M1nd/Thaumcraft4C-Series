package dev.tc4.updater;

import com.google.gson.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Exact approvals and ignores persist independently of per-launch skip decisions. */
public final class ReviewStore {
    private static final Gson JSON = new Gson();
    public record Approval(String modid, String original, String oldHash, ReleaseApi.Release release, List<UpdateTransaction.Original> duplicates) {
        public Approval(String modid, String original, String oldHash, ReleaseApi.Release release) { this(modid, original, oldHash, release, List.of()); }
        public List<UpdateTransaction.Original> originals() {
            List<UpdateTransaction.Original> result = new ArrayList<>();
            result.add(new UpdateTransaction.Original(original, oldHash));
            if (duplicates != null) result.addAll(duplicates);
            return List.copyOf(result);
        }
    }
    private final Path root;
    private final Map<String, Approval> approvals = new LinkedHashMap<>();
    private final Map<String, Set<String>> ignored = new LinkedHashMap<>();

    public ReviewStore(Path game) throws IOException {
        this(game, UpdaterConfig.DEFAULT_BASELINE);
    }
    ReviewStore(Path game, String baseline) throws IOException {
        this(game, baseline, true);
    }
    ReviewStore(Path game, String baseline, boolean seriesEnabled) throws IOException {
        root = UpdaterConfig.stateDirectory(game, baseline);
        if (!seriesEnabled) return;
        Files.createDirectories(root);
        JsonElement saved = read(root.resolve("approved.json"));
        if (saved != null) for (JsonElement element : saved.getAsJsonArray()) {
            Approval approval = JSON.fromJson(element, Approval.class);
            validate(approval);
            if (approvals.putIfAbsent(approval.modid, approval) != null) throw new IOException("Duplicate approved modid");
        }
        saved = read(root.resolve("ignored-versions.json"));
        if (saved != null) for (var entry : saved.getAsJsonObject().entrySet()) {
            if (!UpdateEngine.validModid(entry.getKey()) || UpdateEngine.EXCLUDED.contains(entry.getKey())) continue;
            Set<String> versions = new LinkedHashSet<>();
            for (var version : entry.getValue().getAsJsonArray()) { Version.parse(version.getAsString()); versions.add(version.getAsString()); }
            ignored.put(entry.getKey(), versions);
        }
        // Reclaim verified downloads left behind by an interrupted atomic queue write/cleanup.
        Path directory = root.resolve("approved");
        if (Files.isSymbolicLink(directory)) throw new IOException("Unsafe approved directory");
        if (Files.isDirectory(directory)) try (var files = Files.list(directory)) {
            Set<String> referenced = new HashSet<>();
            approvals.values().forEach(value -> referenced.add(value.release.sha256()+".jar"));
            for (Path path : files.toList()) {
                String name = path.getFileName().toString();
                if (!referenced.contains(name) && name.matches("[a-f0-9]{64}\\.jar")
                        && Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS)
                        && FilesSupport.sha256(path).equals(name.substring(0,64))) Files.delete(path);
            }
        }
    }
    private static JsonElement read(Path path) throws IOException {
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) return null;
        FilesSupport.regularFile(path);
        try (var input = Files.newInputStream(path)) {
            return JsonParser.parseString(new String(FilesSupport.readBounded(input, 1024 * 1024), StandardCharsets.UTF_8));
        }
    }
    private static void validate(Approval value) throws IOException {
        if (value == null || !UpdateEngine.validModid(value.modid) || UpdateEngine.EXCLUDED.contains(value.modid)
                || value.release == null || !value.modid.equals(value.release.modid())) throw new IOException("Invalid approved modid");
        FilesSupport.localFilename(value.original); FilesSupport.filename(value.release.filename());
        Version.parse(value.release.version());
        if (value.oldHash == null || !value.oldHash.matches("[a-f0-9]{64}") || value.release.sha256() == null
                || !value.release.sha256().matches("[a-f0-9]{64}")) throw new IOException("Invalid approval hash");
        Set<String> filenames = new HashSet<>();
        if (value.originals().size() > 128) throw new IOException("Too many approved original files");
        for (var old : value.originals()) {
            FilesSupport.localFilename(old.filename());
            if (old.hash() == null || !old.hash().matches("[a-f0-9]{64}") || !filenames.add(old.filename().toLowerCase(Locale.ROOT)))
                throw new IOException("Invalid approved original files");
        }
    }
    public List<Approval> approvals() { return List.copyOf(approvals.values()); }
    public boolean ignored(String id, String version) { return ignored.getOrDefault(id, Set.of()).contains(version); }
    public Path staged(Approval approval) throws IOException {
        validate(approval);
        Path directory = root.resolve("approved");
        if (Files.isSymbolicLink(directory)) throw new IOException("Unsafe approved directory");
        return directory.resolve(approval.release.sha256() + ".jar");
    }
    public void remove(Collection<String> ids) throws IOException {
        List<Approval> removed = approvals.values().stream().filter(a -> ids.contains(a.modid)).toList();
        Map<String, Approval> updated = new LinkedHashMap<>(approvals); ids.forEach(updated::remove);
        FilesSupport.writeAtomic(root.resolve("approved.json"), JSON.toJson(updated.values()).getBytes(StandardCharsets.UTF_8));
        approvals.clear(); approvals.putAll(updated);
        for (Approval approval : removed) {
            Path path = staged(approval);
            if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) && FilesSupport.sha256(path).equals(approval.release.sha256())) Files.delete(path);
        }
    }
}
