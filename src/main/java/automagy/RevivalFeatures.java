package automagy;

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
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SPEC, "automagy-revival-server.toml");
        thaumcraft.api.config.FeatureRecipes.register(net.minecraft.resources.ResourceLocation.parse("automagy:dimension_lure"),id->id.getNamespace().equals("automagy")&&(id.getPath().startsWith("dimension_lure")||java.util.Set.of("nether_rune_blaze","nether_rune_zombie","nether_rune_ghast","nether_rune_skeleton","nether_rune_slime","nether_rune_bat","nether_rune_wisp").contains(id.getPath())),()->AutomagyConfig.DIMENSION_LURE.get());
        thaumcraft.api.research.ResearchCategories.registerEnablementFilter(net.minecraft.resources.ResourceLocation.parse("automagy:legacy_modules"),key->!java.util.Set.of("automagy:DIMENSIONLURE","automagy:NETHERRUNE_ZOMBIE","automagy:NETHERRUNE_GHAST","automagy:NETHERRUNE_SKELETON","automagy:NETHERRUNE_SLIME","automagy:NETHERRUNE_BAT","automagy:NETHERRUNE_WISP").contains(key)||AutomagyConfig.DIMENSION_LURE.get());
        thaumcraft.api.research.ResearchCategories.registerEnablement("automagy:ADVNODEJAR",()->AutomagyConfig.ADVANCED_JARRING.get());
        thaumcraft.api.research.ResearchCategories.registerRevealRequirement("automagy:ADVNODEJAR","NODEJAR");
        thaumcraft.api.research.ResearchCategories.registerEnablement("automagy:ENTROPICREFINING",()->AutomagyConfig.REFINING_SLIVERS.get()>0);
        thaumcraft.api.config.FeatureRecipes.register(net.minecraft.resources.ResourceLocation.parse("automagy:entropic_refining"),id->id.getNamespace().equals("automagy")&&id.getPath().startsWith("refining/"),()->AutomagyConfig.REFINING_SLIVERS.get()>0);
    }
    private RevivalFeatures() {}
}
