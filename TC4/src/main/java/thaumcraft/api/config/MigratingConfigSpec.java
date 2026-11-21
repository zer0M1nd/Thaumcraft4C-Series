package thaumcraft.api.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.utils.UnmodifiableConfigWrapper;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ConfigFileTypeHandler;
import net.minecraftforge.fml.config.IConfigSpec;

/** Migrate before Forge removes unknown keys; registration, watching and server sync remain Forge-owned. */
public class MigratingConfigSpec extends UnmodifiableConfigWrapper<UnmodifiableConfig>
        implements IConfigSpec<MigratingConfigSpec> {
    private record Rule(String source, String target, Function<Object, Object> transform) {}
    private record ExistingDefault(String target, Object value, String marker) {}
    private final ForgeConfigSpec delegate;
    public ForgeConfigSpec delegate() { return delegate; }
    private final List<Rule> rules = new ArrayList<>();
    private final List<ExistingDefault> existingDefaults = new ArrayList<>();
    private Supplier<Path> previousFile;
    private String previousSibling;
    public MigratingConfigSpec(ForgeConfigSpec delegate) { super(delegate.getSpec()); this.delegate = delegate; }
    /** Import a former COMMON file only for an empty new world config; never alter the source file. */
    public MigratingConfigSpec fromCommon(String name) {
        return fromFile(() -> net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get().resolve(name));
    }
    public MigratingConfigSpec fromFile(Supplier<Path> path) { previousFile = path; return this; }
    /** Import only into an empty new file, from a sibling in the same config
     * directory. This also works for per-world serverconfig and defaultconfigs. */
    public MigratingConfigSpec fromSibling(String name) {
        if(name.isBlank()||!Path.of(name).getFileName().toString().equals(name)||name.equals(".")||name.equals(".."))
            throw new IllegalArgumentException("Expected a config file name");
        previousSibling=name;return this;
    }
    private Path importPath(CommentedConfig data){
        if(previousSibling!=null&&data instanceof CommentedFileConfig file)return file.getNioPath().resolveSibling(previousSibling);
        return previousFile==null?null:previousFile.get();
    }
    /** Preserve the pre-option behavior of existing files without changing defaults for new worlds. */
    public MigratingConfigSpec existingDefault(String target, Object value, String existingMarker) {
        existingDefaults.add(new ExistingDefault(target, value, existingMarker)); return this;
    }
    private boolean missingExistingDefault(CommentedConfig data, ExistingDefault rule) {
        return !data.contains(rule.target) && data.contains(rule.marker);
    }
    private boolean needsImport(CommentedConfig data) {
        var source=data==null?null:importPath(data);
        return data != null && data.isEmpty() && source!=null && Files.isRegularFile(source);
    }
    public MigratingConfigSpec move(String from, String to) { return map(from, to, Function.identity()); }
    public MigratingConfigSpec booleanOption(String from, String to) {
        return map(from, to, value -> value instanceof Boolean b ? (b ? "TRUE" : "FALSE")
                : value instanceof String s && (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("false"))
                ? s.toUpperCase(java.util.Locale.ROOT) : value);
    }
    public MigratingConfigSpec map(String from, String to, Function<Object, Object> transform) {
        if (from.equals(to)) throw new IllegalArgumentException("Migration source equals target");
        rules.add(new Rule(from, to, transform)); return this;
    }
    public boolean needsMigration(CommentedConfig data) {
        return data != null && (needsImport(data) || rules.stream().anyMatch(rule -> data.contains(rule.source))
                || existingDefaults.stream().anyMatch(rule -> missingExistingDefault(data, rule)));
    }
    public int migrate(CommentedConfig data) {
        if (!needsMigration(data)) return 0;
        if (data instanceof CommentedFileConfig file && !data.isEmpty()) ConfigFileTypeHandler.backUpConfig(file);
        int changed = 0;
        if (needsImport(data)) {
            try (var source = CommentedFileConfig.builder(importPath(data)).build()) {
                source.load(); data.putAll(source); changed++;
            }
        }
        for (var rule : existingDefaults) if (missingExistingDefault(data, rule)) {
            data.set(rule.target, rule.value); changed++;
        }
        for (var rule : rules) if (data.contains(rule.source) && !data.contains(rule.target)) {
            data.set(rule.target, rule.transform.apply(data.get(rule.source))); changed++;
        }
        for (var rule : rules) if (data.contains(rule.source)) { data.remove(rule.source); changed++; }
        return changed;
    }
    @Override public void acceptConfig(CommentedConfig data) { migrate(data); delegate.acceptConfig(data); }
    @Override public boolean isCorrecting() { return delegate.isCorrecting(); }
    @Override public boolean isCorrect(CommentedConfig data) { return !needsMigration(data) && delegate.isCorrect(data); }
    @Override public int correct(CommentedConfig data) { return migrate(data) + delegate.correct(data); }
    @Override public void afterReload() { delegate.afterReload(); }
}
