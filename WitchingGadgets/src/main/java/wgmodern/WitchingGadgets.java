package wgmodern;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.common.MinecraftForge;

@Mod("witchinggadgets")
public class WitchingGadgets {
    public WitchingGadgets(){
        RevivalFeatures.register();
        var bus=FMLJavaModLoadingContext.get().getModEventBus();
        net.minecraftforge.common.crafting.CraftingHelper.register(TagOrItemIngredient.ID, TagOrItemIngredient.SERIALIZER);
        if(net.minecraftforge.fml.ModList.get().isLoaded("cb_multipart")) MultipartCompat.register(bus);
        WGWorldgen.PIECES.register(bus);
        thaumcraft.api.worldgen.VillageAdditions.register(WGWorldgen::village);
        SmelteryCompat.register();WallMirror.register();PrimordialContent.register();BlastFurnace.init();Metallurgy.register();Terraformer.register();Metallurgy.init();net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON,WGConfig.SERVER);
        WGMagic.ENCHANTMENTS.register(bus);WGMagic.EFFECTS.register(bus);Content.ITEMS.register(bus);Content.BLOCKS.register(bus);Content.TILES.register(bus);Content.MENUS.register(bus);Content.RECIPES.register(bus);Content.SERIALIZERS.register(bus);Content.TABS.register(bus);
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event)->event.enqueueWork(()->{
            thaumcraft.api.research.ResearchPresentation.registerSearch("witchinggadgets:legacy_search",0,()->true,
                (category,node)->!WGConfig.LIMIT_BOOK_SEARCH.get()||node.research().category().equals(category));
            thaumcraft.api.worldgen.NaturalGenerationRules.registerSoil("witchinggadgets:ring",0,"thaumcraft:eldritch_ring",block->WGConfig.allowedGround(block,true));
            thaumcraft.api.worldgen.NaturalGenerationRules.registerSoil("witchinggadgets:hilltop",0,"thaumcraft:hilltop_stones",block->WGConfig.allowedGround(block,false));
            PrimordialEvents.setupProtection();
            thaumcraft.api.aspects.ItemAspectProviders.registerRuleTransform("witchinggadgets:tags",0,json->{TagAspectExpansion.expand(json);return json;});
            thaumcraft.api.equipment.BootsBehaviorProviders.registerMovementProvider("witchinggadgets:primordial",100,
                stack->stack.getItem() instanceof PrimordialGear.Armor,
                (context,current)->!context.flying()&&context.forward()&&!context.grounded()?(context.hovering()?.03f:.05f):current);
            thaumcraft.api.equipment.BootsBehaviorProviders.registerRepairPolicy("witchinggadgets:traveller",100,
                stack->stack.getItem() instanceof tc4revival.content.TravellerBootsItem,
                (boots,material)->WGConfig.BOOTS_REPAIR.get()&&material.is(net.minecraft.world.item.Items.LEATHER)
                    ?thaumcraft.api.equipment.BootsBehaviorProviders.RepairDecision.ACCEPT:thaumcraft.api.equipment.BootsBehaviorProviders.RepairDecision.REJECT);
        }));
        bus.addListener(WGData::gather);bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent e)->e.enqueueWork(CrystalCapsule::initDispenser));
        MinecraftForge.EVENT_BUS.register(BagEvents.class);
        MinecraftForge.EVENT_BUS.register(WGAccessories.class);MinecraftForge.EVENT_BUS.register(WGMagic.class);MinecraftForge.EVENT_BUS.register(WGLoadedChunks.class);
        thaumcraft.api.research.ResearchCategories.registerEnablement("witchinggadgets:CLOAK_RAVEN",()->net.minecraftforge.fml.ModList.get().isLoaded("twilightforest")); thaumcraft.api.research.ResearchCategories.registerEnablement("witchinggadgets:MIRRORPUMP",()->tc4revival.config.TCConfig.SERVER.allowMirrors.get()); WGAccessories.registerCrafting(); WGNetwork.init(); net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.CLIENT,WGConfig.CLIENT);
    }
}

