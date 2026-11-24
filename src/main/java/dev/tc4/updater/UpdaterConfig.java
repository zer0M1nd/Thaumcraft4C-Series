package dev.tc4.updater;

import com.electronwill.nightconfig.toml.TomlParser;
import com.electronwill.nightconfig.toml.TomlWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public record UpdaterConfig(UpdateMode majorUpdates, UpdateMode stableUpdates, UpdateMode testingUpdates,
                            List<String> installOnce, String baseline, boolean thirdPartyOnly) {
    public static final String DEFAULT_BASELINE = "default";
    public UpdaterConfig { installOnce = List.copyOf(installOnce); }
    public UpdaterConfig(UpdateMode major, UpdateMode stable, UpdateMode testing, List<String> installOnce, String baseline) {
        this(major, stable, testing, installOnce, baseline, false);
    }
    public UpdaterConfig(UpdateMode major, UpdateMode stable, UpdateMode testing, List<String> installOnce) {
        this(major, stable, testing, installOnce, DEFAULT_BASELINE);
    }
    public UpdaterConfig(boolean unstable, boolean major) {
        this(major ? UpdateMode.YES : UpdateMode.NO, UpdateMode.YES, unstable ? UpdateMode.YES : UpdateMode.NO, List.of());
    }
    public UpdateMode mode(Version installed, Version candidate) {
        UpdateMode channel = candidate.stable() ? stableUpdates : testingUpdates;
        return candidate.parts().get(1).equals(installed.parts().get(1)) ? channel : UpdateMode.combine(channel, majorUpdates);
    }
    /** Requests are consumed while the instance lock is held, before recovery/scanning/network work. */
    public static UpdaterConfig read(Path game) throws IOException { return read(game,false); }
    static UpdaterConfig forLaunch(Path game) throws IOException { return read(game,true); }
    static String baselineName(Object value) throws IOException {
        if (!(value instanceof String name) || !name.matches("[a-z][a-z0-9_-]{0,47}")
                || name.matches("con|prn|aux|nul|com[1-9]|lpt[1-9]")) throw new IOException("Invalid release baseline: " + value);
        return name;
    }
    static Path stateDirectory(Path game, String baseline) {
        Path root = game.resolve(".thaumcraftupdater");
        return DEFAULT_BASELINE.equals(baseline) ? root : root.resolve("baseline-" + baseline);
    }
    boolean installAll() { return !DEFAULT_BASELINE.equals(baseline) && installOnce.contains("*"); }
    void finishRequests(Path game) throws IOException {
        Path pending = stateDirectory(game, baseline).resolve("install-pending.json");
        if (Files.exists(pending)) {
            var current = new TomlParser().parse(Files.readString(game.resolve("config/thaumcraftupdater.toml")));
            if (baseline.equals(current.getOrElse("baseline", DEFAULT_BASELINE))) Files.deleteIfExists(pending);
        }
    }
    private static UpdaterConfig read(Path game,boolean launch) throws IOException {
        Path file = game.resolve("config/thaumcraftupdater.toml");
        if (!Files.exists(file)) {
            Files.createDirectories(file.getParent());
            Files.writeString(file, "# 版本基线，省略时使用 default。\nbaseline = \"default\"\n# 取值：yes / no / ask；兼容旧值 是 / 否 / 询问。\n# 是否更新大版本（第二段 x 变化）；与通道同时适用，否优先。\nmajorUpdates = \"ask\"\n# 是否更新稳定版（三段版本）。\nstableUpdates = \"yes\"\n# 是否更新测试版（四段版本）。\ntestingUpdates = \"no\"\n\n# 一次性安装缺失的模组。命名基线可用 [\"*\"]，每次启动补齐该基线。\ninstallOnce = []\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        }
        FilesSupport.regularFile(file);
        try {
            var parsed = new TomlParser().parse(Files.readString(file, StandardCharsets.UTF_8));
            String baseline = baselineName(parsed.getOrElse("baseline", DEFAULT_BASELINE));
            Object thirdPartyOnly = parsed.getOrElse("thirdPartyOnly", false);
            if (!(thirdPartyOnly instanceof Boolean only)) throw new IOException("thirdPartyOnly must be true or false");
            Object setting = parsed.getOrElse("installOnce", List.of());
            Object requested = "*".equals(setting) ? List.of("*") : setting;
            boolean all = !DEFAULT_BASELINE.equals(baseline) && requested instanceof List<?> list && list.contains("*");
            List<String> retained = all ? List.of("*") : List.of();
            var journaled=new LinkedHashSet<String>();
            if (launch) {
                Path pending=stateDirectory(game,baseline).resolve("install-pending.json");
                Files.createDirectories(pending.getParent());
                if (Files.exists(pending,LinkOption.NOFOLLOW_LINKS)) {
                    FilesSupport.regularFile(pending);
                    try(var in=Files.newInputStream(pending)) {
                        for(var id:com.google.gson.JsonParser.parseString(new String(FilesSupport.readBounded(in,65536),StandardCharsets.UTF_8)).getAsJsonArray())
                            if(UpdateEngine.validModid(id.getAsString())) journaled.add(id.getAsString());
                    }
                }
                if(requested instanceof List<?> list) for(var entry:list)
                    if(entry instanceof String id && UpdateEngine.validModid(id)) journaled.add(id);
                if(!journaled.isEmpty()) FilesSupport.writeAtomic(pending,new com.google.gson.Gson().toJson(journaled).getBytes(StandardCharsets.UTF_8));
                if (all) journaled.add("*");
            }
            if (parsed.contains("installOnce") && !retained.equals(requested)) {
                parsed.set("installOnce", retained);
                FilesSupport.writeAtomic(file, new TomlWriter().writeToString(parsed).getBytes(StandardCharsets.UTF_8));
            }
            Set<String> allowed = Set.of("baseline", "thirdPartyOnly", "majorUpdates", "stableUpdates", "testingUpdates", "installOnce", "includeUnstable", "allowMajorUpdates");
            if (!allowed.containsAll(parsed.valueMap().keySet()) || !(requested instanceof List<?> values)
                    || values.stream().anyMatch(value -> !(value instanceof String))) throw new IOException("Invalid updater settings/installOnce list");
            boolean legacyTesting = legacyBoolean(parsed.get("includeUnstable"));
            boolean legacyMajor = legacyBoolean(parsed.get("allowMajorUpdates"));
            UpdateMode major = parsed.contains("majorUpdates") ? UpdateMode.parse(parsed.get("majorUpdates"))
                    : parsed.contains("allowMajorUpdates") ? (legacyMajor ? UpdateMode.YES : UpdateMode.NO) : UpdateMode.ASK;
            UpdateMode stable = parsed.contains("stableUpdates") ? UpdateMode.parse(parsed.get("stableUpdates")) : UpdateMode.YES;
            UpdateMode testing = parsed.contains("testingUpdates") ? UpdateMode.parse(parsed.get("testingUpdates")) : (legacyTesting ? UpdateMode.YES : UpdateMode.NO);
            boolean migrate = parsed.contains("includeUnstable") || parsed.contains("allowMajorUpdates")
                    || !parsed.contains("majorUpdates") || !parsed.contains("stableUpdates") || !parsed.contains("testingUpdates") || !parsed.contains("installOnce");
            if (migrate) {
                parsed.remove("includeUnstable"); parsed.remove("allowMajorUpdates");
                parsed.set("majorUpdates", major.configValue()); parsed.set("stableUpdates", stable.configValue()); parsed.set("testingUpdates", testing.configValue());
                parsed.set("installOnce", retained);
                parsed.setComment("majorUpdates", "是否更新 x 变化的大版本：yes / no / ask（兼容 是 / 否 / 询问）。与稳定/测试通道同时适用，no 优先。");
                parsed.setComment("stableUpdates", "是否更新三段稳定版：yes / no / ask（兼容 是 / 否 / 询问）。");
                parsed.setComment("testingUpdates", "是否更新四段测试版：yes / no / ask（兼容 是 / 否 / 询问）。");
                parsed.setComment("installOnce", "一次性安装缺失的系列 modid。命名基线可用 [\"*\"]，每次启动补齐该基线。");
                FilesSupport.writeAtomic(file, new TomlWriter().writeToString(parsed).getBytes(StandardCharsets.UTF_8));
            }
            return new UpdaterConfig(major, stable, testing, launch?List.copyOf(journaled):((List<?>) requested).stream()
                    .map(String.class::cast).filter(id -> all || !id.equals("*")).distinct().toList(), baseline, only);
        } catch (RuntimeException error) { throw new IOException("Invalid updater configuration: " + file, error); }
    }
    void restoreRequests(Path game, Collection<String> pending) throws IOException {
        if (pending.isEmpty()) return;
        Path file=game.resolve("config/thaumcraftupdater.toml");
        var config=new TomlParser().parse(Files.readString(file,StandardCharsets.UTF_8));
        if (!baseline.equals(config.getOrElse("baseline", DEFAULT_BASELINE))) return;
        Object existing = config.getOrElse("installOnce", List.of());
        if (!DEFAULT_BASELINE.equals(baseline) && ("*".equals(existing) || existing instanceof List<?> list && list.contains("*"))) return;
        var ids=new LinkedHashSet<String>();
        if (existing instanceof List<?> list) for (Object item : list) if (item instanceof String id) ids.add(id);
        pending.stream().filter(UpdateEngine::validModid).forEach(ids::add);
        config.set("installOnce",List.copyOf(ids));
        FilesSupport.writeAtomic(file,new TomlWriter().writeToString(config).getBytes(StandardCharsets.UTF_8));
    }
    private static boolean legacyBoolean(Object value) throws IOException {
        if (value == null) return false;
        if (value instanceof Boolean result) return result;
        throw new IOException("Legacy updater option must be true/false");
    }
}
