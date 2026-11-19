package tbmodern;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(ThaumicBases.ID)
public final class ThaumicBases {
    public static final String ID = "thaumicbases";
    public ThaumicBases() {
        RevivalFeatures.register();
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.SERVER,Configuration.SPEC);
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON,Configuration.RECIPE_SPEC,"thaumicbases-recipes.toml");
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) -> event.enqueueWork(() -> {
            net.minecraftforge.common.crafting.CraftingHelper.register(RevolverIngredient.ID,RevolverIngredient.SERIALIZER);
            ResearchModules.initialize();
            Scanning.initialize();
            GolemCrops.initialize();
            thaumcraft.api.ThaumcraftApi.addWarpToItem(new net.minecraft.resources.ResourceLocation(ID,"bracelet_primal"),3);
            thaumcraft.api.ThaumcraftApi.addWarpToItem(new net.minecraft.resources.ResourceLocation(ID,"tobacco_eldritch"),1);
            thaumcraft.api.ThaumcraftApi.addWarpToItem(new net.minecraft.resources.ResourceLocation(ID,"node_focus_destruction"),1);
            thaumcraft.api.ThaumcraftApi.addWarpToItem(new net.minecraft.resources.ResourceLocation(ID,"node_focus_taint"),2);
        }));
        Content.BLOCKS.register(bus);
        Content.ITEMS.register(bus);
        Content.TABS.register(bus);
        Content.TILES.register(bus);
        Content.MENUS.register(bus);
        Content.ENTITIES.register(bus);
        Content.SOUNDS.register(bus);
        Enchantments.REGISTER.register(bus);
    }
}
