package dev.tc4.updater;

import com.electronwill.nightconfig.toml.TomlParser;
import com.electronwill.nightconfig.toml.TomlWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public record UpdaterConfig(UpdateMode majorUpdates, UpdateMode stableUpdates, UpdateMode testingUpdates, List<String> installOnce) {
    public UpdaterConfig { installOnce = List.copyOf(installOnce); }
    public UpdaterConfig(boolean unstable, boolean major) {
        this(major ? UpdateMode.YES : UpdateMode.NO, UpdateMode.YES, unstable ? UpdateMode.YES : UpdateMode.NO, List.of());
    }
    public UpdateMode mode(Version installed, Version candidate) {
        UpdateMode channel = candidate.stable() ? stableUpdates : testingUpdates;
        return candidate.parts().get(1).equals(installed.parts().get(1)) ? channel : UpdateMode.combine(channel, majorUpdates);
    }
    /** Requests are consumed while the instance lock is held, before recovery/scanning/network work. */
    public static UpdaterConfig read(Path game) throws IOException {
        Path file = game.resolve("config/thaumcraftupdater.toml");
        if (!Files.exists(file)) {
            Files.createDirectories(file.getParent());
            Files.writeString(file, "# 取值：yes / no / ask；兼容旧值 是 / 否 / 询问。\n# 是否更新大版本（第二段 x 变化）；与通道同时适用，否优先。\nmajorUpdates = \"ask\"\n# 是否更新稳定版（三段版本）。\nstableUpdates = \"yes\"\n# 是否更新测试版（四段版本）。\ntestingUpdates = \"no\"\n\n# 一次性安装缺失模组，仅最新稳定版；运行即清空。\ninstallOnce = []\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        }
        FilesSupport.regularFile(file);
        try {
            var parsed = new TomlParser().parse(Files.readString(file, StandardCharsets.UTF_8));
            Object requested = parsed.getOrElse("installOnce", List.of());
            if (parsed.contains("installOnce") && (!(requested instanceof List<?> values) || !values.isEmpty())) {
                parsed.set("installOnce", List.of());
                FilesSupport.writeAtomic(file, new TomlWriter().writeToString(parsed).getBytes(StandardCharsets.UTF_8));
            }
            Set<String> allowed = Set.of("majorUpdates", "stableUpdates", "testingUpdates", "installOnce", "includeUnstable", "allowMajorUpdates");
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
                parsed.set("installOnce", List.of());
                parsed.setComment("majorUpdates", "是否更新 x 变化的大版本：yes / no / ask（兼容 是 / 否 / 询问）。与稳定/测试通道同时适用，no 优先。");
                parsed.setComment("stableUpdates", "是否更新三段稳定版：yes / no / ask（兼容 是 / 否 / 询问）。");
                parsed.setComment("testingUpdates", "是否更新四段测试版：yes / no / ask（兼容 是 / 否 / 询问）。");
                parsed.setComment("installOnce", "一次性安装缺失的系列 modid，只选最新稳定版；运行即清空。");
                FilesSupport.writeAtomic(file, new TomlWriter().writeToString(parsed).getBytes(StandardCharsets.UTF_8));
            }
            return new UpdaterConfig(major, stable, testing, ((List<?>) requested).stream().map(String.class::cast).distinct().toList());
        } catch (RuntimeException error) { throw new IOException("Invalid updater configuration: " + file, error); }
    }
    private static boolean legacyBoolean(Object value) throws IOException {
        if (value == null) return false;
        if (value instanceof Boolean result) return result;
        throw new IOException("Legacy updater option must be true/false");
    }
}
