package dev.tc4.updater;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import com.google.gson.JsonParser;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.Manifest;
import java.util.zip.*;

public record ModMetadata(Path file, Map<String, String> mods, List<Dependency> dependencies,
                          Map<String, String> embeddedMods, boolean earlyService) {
    // Immersive Engineering ships a legitimate MANIFEST.MF above 1 MiB; keep a
    // bounded read while allowing large signed/classpath manifests.
    private static final int METADATA_LIMIT = 8 * 1024 * 1024;
    private static final List<String> EARLY_SERVICES = List.of(
            "cpw.mods.modlauncher.api.ITransformationService",
            "net.minecraftforge.forgespi.locating.IModLocator",
            "net.minecraftforge.forgespi.locating.IDependencyLocator",
            "net.minecraftforge.fml.loading.ImmediateWindowProvider");

    public record Dependency(String owner, String modid, boolean mandatory, String range, String side) {}

    @FunctionalInterface private interface Contents { byte[] read(String name, int limit) throws IOException; }

    public static ModMetadata read(Path file) throws IOException {
        try (ZipFile zip = new ZipFile(file.toFile())) {
            Contents contents = (name, limit) -> {
                ZipEntry entry = zip.getEntry(name);
                if (entry == null) return null;
                try (InputStream input = zip.getInputStream(entry)) { return FilesSupport.readBounded(input, limit); }
            };
            return readContents(file, contents, 0);
        } catch (RuntimeException error) {
            throw new IOException("Invalid mod metadata in " + file.getFileName(), error);
        }
    }

    private static ModMetadata readContents(Path file, Contents contents, int depth) throws IOException {
        Map<String, String> mods = new LinkedHashMap<>();
        List<Dependency> dependencies = new ArrayList<>();
        byte[] toml = contents.read("META-INF/mods.toml", METADATA_LIMIT);
        if (toml != null) {
            var config = new TomlParser().parse(new StringReader(new String(toml, StandardCharsets.UTF_8)));
            byte[] manifestBytes = contents.read("META-INF/MANIFEST.MF", METADATA_LIMIT);
            Manifest manifest = manifestBytes == null ? new Manifest() : new Manifest(new ByteArrayInputStream(manifestBytes));
            List<UnmodifiableConfig> entries = config.getOrElse("mods", List.of());
            for (var entry : entries) {
                String id = entry.get("modId");
                String version = entry.get("version");
                if ("${file.jarVersion}".equals(version)) version = manifest.getMainAttributes().getValue("Implementation-Version");
                if (id == null || !id.matches("[a-z][a-z0-9_]{1,63}") || version == null || version.contains("${"))
                    throw new IOException("Unresolved mod identity in " + file);
                if (mods.putIfAbsent(id, version) != null) throw new IOException("Duplicate mod ID " + id);
                List<UnmodifiableConfig> requirements = config.getOrElse(List.of("dependencies", id), List.of());
                for (var dependency : requirements) {
                    dependencies.add(new Dependency(id, dependency.get("modId"), dependency.getOrElse("mandatory", true),
                            dependency.getOrElse("versionRange", ""), dependency.getOrElse("side", "BOTH")));
                }
            }
        }
        boolean early = false;
        for (String service : EARLY_SERVICES) {
            if (contents.read("META-INF/services/" + service, METADATA_LIMIT) != null) early = true;
        }
        Map<String, String> embedded = new LinkedHashMap<>();
        byte[] jarjar = contents.read("META-INF/jarjar/metadata.json", METADATA_LIMIT);
        if (jarjar != null) {
            if (depth >= 4) throw new IOException("Nested JAR depth exceeds limit: " + file);
            var jars = JsonParser.parseString(new String(jarjar, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("jars");
            if (jars.size() > 128) throw new IOException("Too many embedded libraries: " + file);
            for (var element : jars) {
                String name = element.getAsJsonObject().get("path").getAsString();
                byte[] bytes = contents.read(name, 64 * 1024 * 1024);
                if (bytes == null) throw new IOException("Missing embedded library " + name);
                Map<String, byte[]> entries = new HashMap<>();
                int total = 0;
                try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
                    for (ZipEntry entry; (entry = zip.getNextEntry()) != null;) {
                        if (entry.getName().startsWith("META-INF/")) {
                            byte[] data = FilesSupport.readBounded(zip, 64 * 1024 * 1024);
                            total += data.length;
                            if (total > 64 * 1024 * 1024) throw new IOException("Embedded metadata exceeds limit");
                            entries.put(entry.getName(), data);
                        }
                    }
                }
                ModMetadata child = readContents(file, (key, limit) -> {
                    byte[] data = entries.get(key);
                    if (data != null && data.length > limit) throw new IOException("Embedded entry too large");
                    return data;
                }, depth + 1);
                embedded.putAll(child.mods);
                embedded.putAll(child.embeddedMods);
            }
        }
        return new ModMetadata(file, Map.copyOf(mods), List.copyOf(dependencies), Map.copyOf(embedded), early);
    }

    /** Unmanaged metadata is advisory (dependency presence only), never an update blocker. */
    public static ModMetadata scan(Path file, Set<String> managed) throws IOException {
        try { return read(file); }
        catch (IOException | RuntimeException error) {
            // A damaged managed JAR must not silently disappear from duplicate cleanup.
            String text = "";
            try (ZipFile zip = new ZipFile(file.toFile())) {
                ZipEntry entry = zip.getEntry("META-INF/mods.toml");
                if (entry != null) try (InputStream input = zip.getInputStream(entry)) {
                    text = new String(FilesSupport.readBounded(input, METADATA_LIMIT), StandardCharsets.UTF_8);
                }
            } catch (IOException ignored) { /* Unidentified files remain Forge's responsibility. */ }
            var ids = java.util.regex.Pattern.compile("modId\\s*=\\s*[\\\"']([a-z][a-z0-9_]*)[\\\"']").matcher(text);
            while (ids.find()) if (managed.contains(ids.group(1)))
                throw new IOException("Cannot inspect managed JAR " + file.getFileName(), error);
            return new ModMetadata(file, Map.of(), List.of(), Map.of(), false);
        }
    }
}
