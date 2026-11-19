package tc4revival.resources;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;
import net.minecraftforge.fml.ModList;
import tc4revival.data.LegacyLanguageTranslations;

/** Explicit addon opt-in: only declared Chinese language entries leave the downloaded archive. */
public class LegacyLocalization {
    public static final String PACK_ID = "thaumcraft_legacy_translations";
    private static final int MAX_LANGUAGE_BYTES = 8 * 1024 * 1024;
    public record Mapping(String owner, String source, Map<String,String> keys, Map<String,String> pictures) {
        public Mapping { keys = Map.copyOf(keys); pictures = Map.copyOf(pictures); }
    }
    public record Prepared(Path directory, Set<String> allowed, int entries) {}
    private LegacyLocalization() {}

    public static List<Mapping> installed() throws IOException {
        var result = new ArrayList<Mapping>();
        for (var mod : ModList.get().getMods()) {
            if (!mod.getModId().equals("thaumcraft") && mod.getDependencies().stream()
                    .noneMatch(dep -> dep.getModId().equals("thaumcraft"))) continue;
            var path = mod.getOwningFile().getFile().findResource("data", mod.getModId(), "thaumcraft", "legacy_localization.json");
            if (!Files.isRegularFile(path)) continue;
            try (var reader = Files.newBufferedReader(path)) {
                result.addAll(decode(mod.getModId(), JsonParser.parseReader(reader).getAsJsonObject()));
            } catch (RuntimeException error) { throw new IOException("Invalid legacy localization declaration: " + path, error); }
        }
        result.sort(Comparator.comparing(Mapping::owner).thenComparing(Mapping::source));
        return List.copyOf(result);
    }

    public static List<Mapping> decode(String owner, JsonObject json) throws IOException {
        if (!owner.matches("[a-z0-9_]+") || json.get("format").getAsInt() != 1
                || !json.get("minecraft_version").getAsString().equals("1.7.10"))
            throw new IOException("Unsupported legacy localization declaration for " + owner);
        var result = new ArrayList<Mapping>();
        for (var element : json.getAsJsonArray("sources")) {
            var source = element.getAsJsonObject();
            String path = source.get("path").getAsString();
            if (!path.matches("assets/[a-z0-9_]+/lang/zh_[cC][nN]\\.lang"))
                throw new IOException("Only an explicit 1.7.10 Chinese language file is allowed: " + path);
            var keys = new TreeMap<String,String>();
            source.getAsJsonObject("keys").entrySet().forEach(e -> keys.put(e.getKey(), e.getValue().getAsString()));
            if (keys.keySet().stream().anyMatch(String::isBlank) || keys.values().stream().anyMatch(String::isBlank))
                throw new IOException("Empty legacy translation key");
            var pictures = new TreeMap<String,String>();
            if (source.has("pictures")) source.getAsJsonObject("pictures").entrySet()
                    .forEach(e -> pictures.put(e.getKey(), e.getValue().getAsString()));
            result.add(new Mapping(owner, path, keys, pictures));
        }
        return List.copyOf(result);
    }

    public static Map<String,String> parse(byte[] bytes) {
        var result = new LinkedHashMap<String,String>();
        new String(bytes, StandardCharsets.UTF_8).replace("\uFEFF", "").lines().forEach(line -> {
            int equals = line.indexOf('=');
            if (equals > 0 && !line.stripLeading().startsWith("#"))
                result.put(line.substring(0, equals).strip(), line.substring(equals + 1).strip());
        });
        return result;
    }

    /** Never extract the complete ZIP or copy assets from unrelated namespaces. */
    public static Map<String,Map<String,String>> convert(List<Path> archives, List<Mapping> mappings) throws IOException {
        var result = new TreeMap<String,Map<String,String>>();
        var seen = new HashSet<String>();
        for (var archive : archives) try (var zip = new ZipFile(archive.toFile())) {
            for (var mapping : mappings) {
                String identity = mapping.owner()+"/"+mapping.source().toLowerCase(Locale.ROOT);
                if (seen.contains(identity)) continue;
                var entry = zip.getEntry(mapping.source());
                if (entry == null) entry = zip.getEntry(mapping.source().replace("zh_CN.lang", "zh_cn.lang"));
                if (entry == null) entry = zip.getEntry(mapping.source().replace("zh_cn.lang", "zh_CN.lang"));
                if (entry == null || entry.isDirectory()) continue;
                // Match I18nUpdateMod's 1.7.10 converter: first archive containing a file wins.
                seen.add(identity);
                if (entry.getSize() > MAX_LANGUAGE_BYTES) throw new IOException("Oversized legacy language");
                byte[] bytes;
                try (var input = zip.getInputStream(entry)) { bytes = input.readNBytes(MAX_LANGUAGE_BYTES + 1); }
                if (bytes.length > MAX_LANGUAGE_BYTES) throw new IOException("Oversized legacy language");
                var old = parse(bytes);
                var target = result.computeIfAbsent(mapping.owner(), key -> new TreeMap<>());
                if (mapping.owner().equals("thaumcraft")) {
                    LegacyLanguageTranslations.append(old.entrySet().stream().map(e -> e.getKey()+"="+e.getValue()).toList(), true, target::put);
                }
                mapping.keys().forEach((modern, legacy) -> {
                    String value = old.get(legacy);
                    if (value != null && !value.isBlank()) target.put(modern,
                            tc4revival.research.ResearchText.remapPictures(value, mapping.pictures()));
                });
            }
        }
        return result;
    }

    public static Prepared prepare(Path game, List<Path> archives, List<Mapping> mappings) throws IOException {
        return prepare(game,convert(archives,mappings));
    }
    public static Prepared prepare(Path game, Map<String,Map<String,String>> converted) throws IOException {
        var gson = new GsonBuilder().disableHtmlEscaping().create();
        String content = gson.toJson(converted);
        Path root = game.resolve(".thaumcraft-cache/legacy-localization-" + OriginalResources.sha256(content.getBytes(StandardCharsets.UTF_8)));
        var output = new TreeMap<String,String>();
        int entries = 0;
        for (var owner : converted.entrySet()) if (!owner.getValue().isEmpty()) {
            output.put("assets/"+owner.getKey()+"/lang/zh_cn.json", gson.toJson(owner.getValue()));
            entries += owner.getValue().size();
        }
        output.put("pack.mcmeta", "{\"pack\":{\"pack_format\":15,\"description\":\"Thaumcraft 1.7.10 Chinese fallback (CFPA)\"}}");
        for (var entry : output.entrySet()) {
            Path file = root.resolve(entry.getKey());
            if (Files.isRegularFile(file) && Files.readString(file).equals(entry.getValue())) continue;
            Files.createDirectories(file.getParent());
            Path temp = Files.createTempFile(file.getParent(), ".translation-", ".tmp");
            try { Files.writeString(temp, entry.getValue()); Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING); }
            finally { Files.deleteIfExists(temp); }
        }
        return new Prepared(root, Set.copyOf(output.keySet()), entries);
    }
}
