package conciliummodern;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(ThaumicConcilium.ID)
public final class ThaumicConcilium {
    public static final String ID="thaumicconcilium";
    public ThaumicConcilium() {
        net.minecraftforge.common.crafting.CraftingHelper.register(FocusPatternIngredient.ID,FocusPatternIngredient.SERIALIZER);
        var bus=FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent event)->event.enqueueWork(()->{
            if(!ttmodern.TinkererConfig.KAMI.get())throw new IllegalStateException("Thaumic Concilium requires Thaumic Tinkerer's KAMI module (enableKami=true), as in the original 1.1.1 progression.");
        }));
        ConciliumWorldgen.register(bus);
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event)->event.enqueueWork(()->{
            PontifexDyeing.setup();
            Polishment.setup();
            ConciliumDyesIntegration.setup();
            ConciliumResearchPrerequisites.setup();
            thaumcraft.api.research.ResearchCategories.registerEnablement("thaumicconcilium:TCWITCHERY",WitcheryBridge::available);
            thaumcraft.api.research.ResearchCategories.registerEnablement("thaumicconcilium:TCWITCHERYBM",WitcheryBridge::alchemistAvailable);
            thaumcraft.api.research.ResearchCategories.registerEnablement("thaumicconcilium:TCDYES",ConciliumDyesIntegration::available);
            thaumcraft.api.research.ResearchCategories.registerEnablement("thaumicconcilium:TCBOTANIA",()->net.minecraftforge.fml.ModList.get().isLoaded("botania"));
            thaumcraft.api.research.ResearchCategories.registerEnablement("thaumicconcilium:TCAUTOMAGY",()->net.minecraftforge.fml.ModList.get().isLoaded("automagy"));
            thaumcraft.api.research.ResearchCategories.registerEnablement("thaumicconcilium:MATERIALPEELER",CrimsonContent::available);
        }));
        CrimsonContent.register(bus);
        ConciliumGems.init();
        ConciliumGolems.register(bus);
        RemainingMobs.register(bus);
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event)->event.enqueueWork(ConciliumGems::setup));
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event)->event.enqueueWork(CastingBracelet::setup));
        bus.addListener((net.minecraftforge.event.entity.EntityAttributeCreationEvent event)->event.put(ConciliumEntities.RED_MIND.get(),RedPoweredMind.attributes().build()));
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event)->event.enqueueWork(()->{
            thaumcraft.api.research.ResearchCategories.registerEnablement("thaumicconcilium:REDPOWEREDMIND",RedPoweredMindBlock::available);
            for(String research:java.util.List.of("TCHORIZONS","BRAINLITTERING"))thaumcraft.api.research.ResearchCategories.registerEnablement("thaumicconcilium:"+research,()->net.minecraftforge.fml.ModList.get().isLoaded("thaumichorizons"));
        }));
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event)->event.enqueueWork(()->
                thaumcraft.api.research.ResearchCategories.registerEnablement("thaumicconcilium:DRAINAGESYRINGE",DrainageSyringe::available)));
        bus.addListener((net.minecraftforge.event.entity.EntityAttributeCreationEvent event)->event.put(ConciliumEntities.PROTOLIMB.get(),Protolimb.attributes().build()));
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event)->event.enqueueWork(()->{
            for(String research:java.util.List.of("PROTOBODY","SOLIDVOID"))thaumcraft.api.research.ResearchCategories.registerEnablement("thaumicconcilium:"+research,
                    ()->net.minecraftforge.fml.ModList.get().isLoaded("taintedmagic")&&net.minecraftforge.fml.ModList.get().isLoaded("thaumichorizons"));
        }));
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event)->event.enqueueWork(()->
                thaumcraft.api.research.ResearchCategories.registerEnablement("thaumicconcilium:FLESHCRUCIBLE",()->net.minecraftforge.fml.ModList.get().isLoaded("taintedmagic"))));
        bus.addListener((net.minecraftforge.event.entity.EntityAttributeCreationEvent event)->event.put(ConciliumEntities.QUICKSILVER_ELEMENTAL.get(),QuicksilverElemental.attributes().build()));
        bus.addListener((net.minecraftforge.event.entity.EntityAttributeCreationEvent event)->event.put(ConciliumEntities.SMART_TAINT_SPIDER.get(),SmartTaintSpider.attributes().build()));
        bus.addListener((net.minecraftforge.event.entity.EntityAttributeCreationEvent event)->event.put(ConciliumEntities.BRIGHTEST.get(),BrightestOne.attributes().build()));
        ConciliumBlocks.register(bus);
        ConciliumItems.ITEMS.register(bus);
        ConciliumItems.TABS.register(bus);
        ConciliumEntities.TYPES.register(bus);
        ConciliumSounds.SOUNDS.register(bus);
        var monitorMenu=AstralMonitorMenu.TYPE;
        var lithographerMenu=LithographerMenu.TYPE;
        Lithography.init();
        RuneFlow.init();
        EtherealState.init();
        ManipulatorScroll.init();
        ShardSelection.init();
        ThaumaturgeMenu.MENUS.register(bus);
        ConciliumSpawns.CODECS.register(bus);
        bus.addListener(ConciliumSpawns::placements);
        bus.addListener((net.minecraftforge.event.entity.EntityAttributeCreationEvent event)->event.put(ConciliumEntities.NETHER_EXPLORER.get(),NetherExplorer.attributes().build()));
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON,ConciliumMobConfig.SPEC);
        bus.addListener((net.minecraftforge.event.entity.EntityAttributeCreationEvent event)->event.put(ConciliumEntities.MAD_THAUMATURGE.get(),MadThaumaturge.attributes().build()));
        bus.addListener((net.minecraftforge.event.entity.EntityAttributeCreationEvent event)->event.put(ConciliumEntities.THAUMATURGE.get(),Thaumaturge.attributes().build()));
        NetherMembraneRecipe.SERIALIZERS.register(bus);
        var polishment=PolishmentRecipe.TYPE;
        ChainedRiftRecipe.TYPES.register(bus);ChainedRiftRecipe.SERIALIZERS.register(bus);
    }
}
