package thmodern;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraft.resources.ResourceLocation;

/** Original gameplay options remain independent of Revival profiles. */
public final class LegacyConfig {
    public static ForgeConfigSpec.BooleanValue POCKET_PLANES,ALTERNATE_BELL;
    public static void define(ForgeConfigSpec.Builder builder) {
        ALTERNATE_BELL=builder.comment("替代傀儡铃铛研究和配方；原作者 1.1.9 初始 false，可选 true/false；已有现代移植配置迁移为开启，关闭不删除已有铃铛；重启世界。",
                "Alternate golemancer's bell research and recipe; original 1.1.9 default false, true/false; existing port configs migrate to enabled and existing bells remain; restart the world.")
                .define("alternateGolemBell",false);
        POCKET_PLANES=builder.comment("口袋位面内容；新世界初始 false，可选 true/false；已有现代移植存档自动保留开启。关闭后暂停新建、生成与进入，允许离开已有位面，保留世界与托管珍珠；重启世界。",
                "Pocket-plane content; new-world default false, true/false; existing port worlds migrate to enabled. Disabling pauses creation, generation and entry, but permits exit and preserves worlds and escrowed pearls. Restart the world.")
                .define("enablePocketPlane",false);
    }
    public static boolean pocket() { return thmodern.vat.WarpTumor.CONFIG.isLoaded()?POCKET_PLANES.get():POCKET_PLANES.getDefault(); }
    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER,
                new thaumcraft.api.config.MigratingConfigSpec(thmodern.vat.WarpTumor.CONFIG)
                        .existingDefault("alternateGolemBell",true,"warpedTumorValue")
                        .existingDefault("enablePocketPlane",true,"warpedTumorValue"),"thaumichorizons-server.toml");
        for(String key:java.util.List.of("pocketPlane","leviathan","planarClouds","planarKeystone"))
            thaumcraft.api.research.ResearchCategories.registerEnablement(key,LegacyConfig::pocket);
        thaumcraft.api.config.FeatureRecipes.register(ResourceLocation.parse("thaumichorizons:pocket_planes"),
                id->id.getNamespace().equals("thaumichorizons")&&id.getPath().startsWith("infusion/keystone"),LegacyConfig::pocket);
        thaumcraft.api.config.FeatureRecipes.register(ResourceLocation.parse("thaumichorizons:alternate_bell"),
                id->id.toString().equals("thaumichorizons:arcane/golem_bell"),()->ALTERNATE_BELL.get());
        thaumcraft.api.research.ResearchCategories.registerEnablement("alternateGolemBell",()->ALTERNATE_BELL.get());
    }
}
