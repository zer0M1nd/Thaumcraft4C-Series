package temodern;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import thaumcraft.api.config.RevivalConfig;
import thaumcraft.api.config.MigratingConfigSpec;
import thaumcraft.api.config.FeatureRecipes;
import net.minecraft.resources.ResourceLocation;

/** Added-feature profile. Original gameplay options retain their independent semantics. */
public final class RevivalFeatures {
    public static final RevivalConfig CONFIG;
    public static final ForgeConfigSpec SPEC;
    public static final RevivalConfig.Option CELLS_256K, EXTENDED_CELLS, WIRELESS_ARCANE, VIS_CHARGING;
    static {
        var builder = new ForgeConfigSpec.Builder();
        CONFIG = new RevivalConfig(builder, true, () -> tc4revival.config.TCConfig.SERVER.revival.profile());
        CELLS_256K = CONFIG.feature("storage.cells_256k", "256k 源质存储元件；关闭时旧元件只允许取出已有源质。", "256k essentia cells; disabled existing cells allow extraction only.", RevivalConfig.Defaults.BENEFIT);
        EXTENDED_CELLS = CONFIG.feature("storage.extended_cells", "1024k 及以上、量子和奇点存储；依赖 256k 开关，旧元件只允许取出。", "1024k+, quantum and singularity storage; requires 256k cells. Disabled existing cells allow extraction only.", RevivalConfig.Defaults.BENEFIT);
        WIRELESS_ARCANE = CONFIG.feature("crafting.wireless_arcane_terminal", "无线奥术终端；仍需原有奥术终端配方开关及 AE 网络。关闭时保留库存。", "Wireless arcane terminal; requires the legacy arcane-terminal crafting option and an AE network. Stored inventory is retained.", RevivalConfig.Defaults.BENEFIT);
        VIS_CHARGING = CONFIG.feature("crafting.vis_charging_card", "无线终端魔力充能卡；依赖无线奥术终端，关闭后已安装卡不充能。", "Wireless terminal vis charging card; requires the wireless arcane terminal. Installed cards stop charging when disabled.", RevivalConfig.Defaults.BENEFIT);
        SPEC = builder.build(); CONFIG.bind(SPEC);
    }
    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, new MigratingConfigSpec(SPEC)
                .fromCommon("thaumicenergistics-common.toml")
                .booleanOption("crafting.extended_cells", EXTENDED_CELLS.path()), "thaumicenergistics-revival-server.toml");
        FeatureRecipes.deferCondition(RecipeConfig.CONDITION);
        FeatureRecipes.register(ResourceLocation.parse("thaumicenergistics:cells_256k"),
                id -> id.getNamespace().equals(TE.ID) && id.getPath().contains("256k"), CELLS_256K);
        FeatureRecipes.register(ResourceLocation.parse("thaumicenergistics:wireless_arcane"),
                id -> id.getNamespace().equals(TE.ID) && id.getPath().equals("wireless_arcane_terminal"), RevivalFeatures::wireless);
        FeatureRecipes.register(ResourceLocation.parse("thaumicenergistics:vis_charging"),
                id -> id.getNamespace().equals(TE.ID) && id.getPath().equals("vis_charging_card"), RevivalFeatures::charging);
    }
    public static boolean extended() { return CELLS_256K.get() && EXTENDED_CELLS.get(); }
    public static boolean wireless() { return WIRELESS_ARCANE.get() && RecipeConfig.enabled("arcane_terminal"); }
    public static boolean charging() { return wireless() && VIS_CHARGING.get(); }
    public static boolean tierEnabled(String id) {
        return switch (id) {
            case "256k" -> CELLS_256K.get();
            case "1024k", "4096k", "16384k", "quantum", "singularity" -> extended();
            default -> true;
        };
    }
    private RevivalFeatures() {}
}
