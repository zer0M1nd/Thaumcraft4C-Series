package thaumcraft.api.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraftforge.common.ForgeConfigSpec;

/** Shared Forge-native profile semantics. Call bind after the enclosing spec has been built. */
public class RevivalConfig {
    public record Defaults(boolean everything, boolean easiest, boolean hardest, boolean vanilla) {
        public static final Defaults BENEFIT = new Defaults(true, true, false, false);
        public static final Defaults CHALLENGE = new Defaults(true, false, true, false);
        public static final Defaults CONVENIENCE = new Defaults(true, true, true, false);
        public static final Defaults RESEARCH = new Defaults(true, false, true, false);
        public boolean in(RevivalProfile profile) {
            return switch (profile) {
                case EVERYTHING, FOLLOW_CORE -> everything;
                case EASIEST -> easiest;
                case HARDEST -> hardest;
                case VANILLA -> vanilla;
            };
        }
        public String mapping() {
            return "Everything=" + everything + ", Easiest=" + easiest + ", Hardest=" + hardest + ", Vanilla=" + vanilla;
        }
    }

    private static final Map<ForgeConfigSpec, RevivalConfig> REGISTERED = new java.util.IdentityHashMap<>();
    /** Resolve the shared feature policy without reflecting into an addon's implementation. */
    public static synchronized RevivalConfig forSpec(Object candidate) {
        if (candidate instanceof MigratingConfigSpec migration) candidate = migration.delegate();
        return REGISTERED.get(candidate);
    }
    private final ForgeConfigSpec.Builder builder;
    private final Supplier<RevivalProfile> core;
    private final Map<String, Option> options = new LinkedHashMap<>();
    private ForgeConfigSpec spec;
    public final ForgeConfigSpec.EnumValue<RevivalProfile> global;

    public RevivalConfig(ForgeConfigSpec.Builder builder, boolean addon, Supplier<RevivalProfile> core) {
        this.builder = builder;
        this.core = core;
        var allowed = addon ? List.of(RevivalProfile.values()) : List.of(RevivalProfile.EVERYTHING,
                RevivalProfile.VANILLA, RevivalProfile.EASIEST, RevivalProfile.HARDEST);
        global = builder.comment(
                "重制版新增功能默认策略；不改变原版玩法配置、要素兼容或必要现代适配。逐项 TRUE/FALSE 优先于全局。",
                "Everything 完整设计；Vanilla 关闭新增玩法；Easiest 偏向便利；Hardest 偏向挑战。附属 FOLLOW_CORE 跟随本体。",
                "Profile for additions only; legacy gameplay, aspect compatibility and necessary modern adaptations are unaffected.",
                "Everything: full design; Vanilla: no additions; Easiest: benefits; Hardest: challenges. Addons default to FOLLOW_CORE.",
                "Explicit per-feature TRUE/FALSE overrides this profile. Restart the world/server after editing / 修改后重启世界或服务器。")
                .worldRestart().defineEnum("revival_features.global", addon ? RevivalProfile.FOLLOW_CORE : RevivalProfile.EVERYTHING, allowed);
    }

    /** Client preferences inherit the effective core profile without introducing a second global selector. */
    public RevivalConfig(ForgeConfigSpec.Builder builder, Supplier<RevivalProfile> core) {
        this.builder = builder; this.core = core; this.global = null;
    }

    public void bind(ForgeConfigSpec spec) {
        this.spec = java.util.Objects.requireNonNull(spec);
        synchronized (RevivalConfig.class) { REGISTERED.put(spec, this); }
    }
    public boolean loaded() { return spec != null && spec.isLoaded(); }
    public RevivalProfile profile() {
        if (global == null) return core.get().resolve(RevivalProfile.EVERYTHING);
        var selected = loaded() ? global.get() : global.getDefault();
        return selected.resolve(selected == RevivalProfile.FOLLOW_CORE ? core.get() : selected);
    }
    public Option feature(String path, String chinese, String english, Defaults defaults) {
        if (options.containsKey(path)) throw new IllegalArgumentException("Duplicate revival feature " + path);
        var value = builder.comment(chinese, english,
                "初始 DEFAULT；DEFAULT 跟随本模块全局模式；TRUE 启用；FALSE 关闭。必要依赖仍须满足。",
                "Initial DEFAULT; DEFAULT follows this module's profile; TRUE enables; FALSE disables. Required dependencies still apply.",
                "默认映射 / DEFAULT mapping: " + defaults.mapping(),
                "修改后重启世界或服务器 / Restart the world/server after editing.")
                .worldRestart().defineEnum("revival_features." + path, FeatureSetting.DEFAULT);
        var option = new Option(path, value, defaults);
        options.put(path, option);
        return option;
    }
    /** A dependent default keeps explicit TRUE/FALSE authoritative. */
    public Option feature(String path, String chinese, String english, BooleanSupplier defaultValue) {
        if (options.containsKey(path)) throw new IllegalArgumentException("Duplicate revival feature " + path);
        var value=builder.comment(chinese,english,
                "初始 DEFAULT；DEFAULT 跟随上述功能开关；TRUE 启用；FALSE 关闭。",
                "Initial DEFAULT; DEFAULT follows the feature described above; TRUE enables; FALSE disables.",
                "修改后重启客户端 / Restart the client after editing.")
                .worldRestart().defineEnum("revival_features."+path,FeatureSetting.DEFAULT);
        var option = new Option(path,value,new Defaults(true,true,true,true));
        option.defaultValue = java.util.Objects.requireNonNull(defaultValue);
        options.put(path,option);
        return option;
    }
    public Map<String, Option> options() { return java.util.Collections.unmodifiableMap(options); }
    public Option option(String key) {
        var option = options.get(key);
        if (option == null) throw new IllegalArgumentException("Unknown revival feature " + key);
        return option;
    }

    public class Option implements BooleanSupplier, Supplier<Boolean> {
        private BooleanSupplier defaultValue;
        private final String key;
        public final ForgeConfigSpec.EnumValue<FeatureSetting> setting;
        public final Defaults defaults;
        protected Option(String key, ForgeConfigSpec.EnumValue<FeatureSetting> setting, Defaults defaults) {
            this.key = key; this.setting = setting; this.defaults = defaults;
        }
        public String key() { return key; }
        public String path() { return "revival_features." + key; }
        public FeatureSetting configured() { return loaded() ? setting.get() : setting.getDefault(); }
        @Override public boolean getAsBoolean() { return configured().resolve(defaultValue == null ? defaults.in(profile()) : defaultValue.getAsBoolean()); }
        @Override public Boolean get() { return getAsBoolean(); }
        public void set(boolean enabled) { setting.set(enabled ? FeatureSetting.TRUE : FeatureSetting.FALSE); }
        public void set(FeatureSetting value) { setting.set(value); }
    }

    /** Source-compatible helper for older in-process tests; it is not a second configuration master switch. */
    public static class LegacyGroup implements BooleanSupplier {
        private final List<Option> options;
        public LegacyGroup(Option... options) { this.options = List.of(options); }
        public Boolean get() { return options.stream().anyMatch(Option::getAsBoolean); }
        @Override public boolean getAsBoolean() { return get(); }
        public void set(boolean value) { options.forEach(option -> option.set(value)); }
        public List<Option> options() { return options; }
    }
}
