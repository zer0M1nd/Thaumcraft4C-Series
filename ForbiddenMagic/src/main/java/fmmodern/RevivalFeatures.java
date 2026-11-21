package fmmodern;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import thaumcraft.api.config.RevivalConfig;

/** Added-feature profile. Original gameplay options retain their independent semantics. */
public final class RevivalFeatures {
    public static final RevivalConfig CONFIG;
    public static final ForgeConfigSpec SPEC;
    static {
        var builder = new ForgeConfigSpec.Builder();
        CONFIG = new RevivalConfig(builder, true, () -> tc4revival.config.TCConfig.SERVER.revival.profile());
        SPEC = builder.build(); CONFIG.bind(SPEC);
    }
    public static void register() {
        thaumcraft.api.config.FeatureRecipes.deferCondition(net.minecraft.resources.ResourceLocation.parse("forbidden_magic:config"));
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SPEC, "forbidden_magic-revival-server.toml");
    }
    private RevivalFeatures() {}
}
