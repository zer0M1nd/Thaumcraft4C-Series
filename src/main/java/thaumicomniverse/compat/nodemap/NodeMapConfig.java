package thaumicomniverse.compat.nodemap;

import net.minecraftforge.common.ForgeConfigSpec;
import thaumcraft.api.config.FeatureSetting;
import thaumcraft.api.config.MigratingConfigSpec;

public final class NodeMapConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue SMOOTH_DISASSEMBLY;
    public static final MigratingConfigSpec MIGRATING;
    private static final ForgeConfigSpec.EnumValue<FeatureSetting> VALUE;
    public static final Preference SHOW = new Preference();
    static {
        var builder = new ForgeConfigSpec.Builder();
        VALUE = builder.comment("显示已扫描节点。DEFAULT：Everything/Easiest 开，Hardest/Vanilla 关；依赖服务端节点地图功能。初始 DEFAULT，可选 DEFAULT/TRUE/FALSE。界面切换立即生效，卸载区块仅预测恢复。",
                "Show scanned nodes. DEFAULT: on in Everything/Easiest, off in Hardest/Vanilla; requires the server node-map feature. Initial DEFAULT; values DEFAULT/TRUE/FALSE. UI changes apply immediately; unloaded regeneration is a forecast.")
                .translation("omniverse.node_map.enabled").defineEnum("revival_features.node_map.show", FeatureSetting.DEFAULT);
        SMOOTH_DISASSEMBLY = builder.comment("Keep disassembled Create structures visible until terrain meshes are uploaded. Client rendering only.")
                .define("rendering.smoothContraptionDisassembly", true);
        SPEC = builder.build();
        MIGRATING = new MigratingConfigSpec(SPEC).booleanOption("showScannedAuraNodes", "revival_features.node_map.show");
    }
    public static final class Preference {
        public boolean get() {
            boolean allowed=thaumicomniverse.RevivalFeatures.Group.NODE_MAP.enabled();
            return allowed && (SPEC.isLoaded()?VALUE.get():FeatureSetting.DEFAULT).resolve(allowed);
        }
        public void set(boolean enabled) { VALUE.set(enabled?FeatureSetting.TRUE:FeatureSetting.FALSE); }
    }
}
