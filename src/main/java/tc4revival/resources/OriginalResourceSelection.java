package tc4revival.resources;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/** Selection is independent of Minecraft and desktop classes, and never combines archives. */
public  class OriginalResourceSelection {
    private OriginalResourceSelection() {}
    public record Scan(List<OriginalResources.Snapshot> usable, List<String> errors,
                       OriginalResources.Snapshot automatic) {}

    public static Scan scan(Path gameDirectory, Map<String, String> mapping) throws IOException {
        return scan(gameDirectory, mapping, java.util.Set.of(OriginalResources.REFERENCE_SHA256));
    }

    public static Scan scan(Path gameDirectory, Map<String,String> mapping, java.util.Set<String> accepted) throws IOException {
        var usable = new ArrayList<OriginalResources.Snapshot>();
        var errors = new ArrayList<String>();
        for (Path file : OriginalResources.candidates(gameDirectory.resolve("thaumcraft"))) {
            try {
                var candidate = OriginalResources.read(file, mapping);
                if (accepted.contains(candidate.sha256())) usable.add(candidate);
                else errors.add(file.getFileName() + ": SHA-256 mismatch");
            }
            catch (IOException failure) { errors.add(file.getFileName() + ": " + failure.getMessage()); }
        }
        var known = usable.stream().filter(OriginalResources.Snapshot::reference).toList();
        OriginalResources.Snapshot automatic = known.isEmpty() ? null : known.get(0);
        if (known.size() > 1) errors.add("发现多个相同的原版 JAR，按文件名顺序使用第一份。");
        if (automatic == null && usable.size() == 1) automatic = usable.get(0);
        if (automatic == null && !usable.isEmpty()) {
            var remembered = new Properties();
            Path settings = settings(gameDirectory);
            if (Files.isRegularFile(settings)) {
                try (var input = Files.newInputStream(settings)) { remembered.load(input); }
                catch (IOException ignored) { /* A damaged preference must not block selection. */ }
                automatic = usable.stream().filter(candidate ->
                        candidate.file().toString().equals(remembered.getProperty("file"))
                        && candidate.sha256().equals(remembered.getProperty("sha256"))).findFirst().orElse(null);
            }
            // All candidates have a declared digest. A stale selection falls back to filename order.
            if (automatic == null) automatic = usable.get(0);
        }
        return new Scan(List.copyOf(usable), List.copyOf(errors), automatic);
    }

    public static void remember(Path gameDirectory, OriginalResources.Snapshot selected) throws IOException {
        var properties = new Properties();
        properties.setProperty("file", selected.file().toString());
        properties.setProperty("sha256", selected.sha256());
        var file = settings(gameDirectory);
        Files.createDirectories(file.getParent());
        try (var output = Files.newOutputStream(file)) { properties.store(output, "Local original resource selection"); }
    }

    private static Path settings(Path gameDirectory) {
        return gameDirectory.resolve(".thaumcraft-cache").resolve("selection.properties");
    }
}
