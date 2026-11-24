package thaumicomniverse;

@net.minecraftforge.fml.common.Mod(ThaumicOmniverse.MOD_ID)
public final class ThaumicOmniverse {
    public static final String MOD_ID = "thaumicomniverse";
    public static final net.minecraftforge.registries.DeferredRegister<net.minecraft.world.item.Item> ITEMS =
            net.minecraftforge.registries.DeferredRegister.create(net.minecraftforge.registries.ForgeRegistries.ITEMS, MOD_ID);
    public static final net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.Item> THAUMIUM_SHEET =
            ITEMS.register("thaumium_sheet", () -> new net.minecraft.world.item.Item(new net.minecraft.world.item.Item.Properties()));
    public static final net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.Item> VOID_SHEET =
            ITEMS.register("void_sheet", () -> new net.minecraft.world.item.Item(new net.minecraft.world.item.Item.Properties()));
    private static final net.minecraftforge.registries.DeferredRegister<net.minecraft.world.item.CreativeModeTab> TABS =
            net.minecraftforge.registries.DeferredRegister.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB, MOD_ID);
    public static final net.minecraftforge.registries.RegistryObject<thaumicomniverse.content.SoulCoreItem> SOUL_CORE = ITEMS.register("soul_core",thaumicomniverse.content.SoulCoreItem::new);
    public static final net.minecraftforge.registries.RegistryObject<thaumicomniverse.content.UniversalClusterItem> ORE_CLUSTER = ITEMS.register("ore_cluster",thaumicomniverse.content.UniversalClusterItem::new);
    public ThaumicOmniverse() {
        thaumcraft.api.resources.ExternalResourceConverters.register(net.minecraft.resources.ResourceLocation.parse("thaumicomniverse:brass_pipes"),thaumicomniverse.client.BrassTubeTextures::append);
        RevivalFeatures.register();
        var bus = net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus();
        FeatureLootCondition.register(bus);
        var mods = net.minecraftforge.fml.ModList.get();
        if (mods.isLoaded("tconstruct")) thaumicomniverse.compat.tinkers.ArcaneMaterials.register(bus);
        if (thaumicomniverse.compat.AvaritiaCompat.loaded()) thaumicomniverse.compat.AvaritiaCompat.register(ITEMS);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(thaumicomniverse.compat.AvaritiaCompat::missingMappings);
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON,
                thaumicomniverse.diagnostic.BookOpeningTrace.SPEC, "thaumicomniverse-diagnostics.toml");
        thaumicomniverse.compat.nodemap.NodeMapNetwork.register();
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.CLIENT,
                thaumicomniverse.compat.nodemap.NodeMapConfig.MIGRATING, "thaumicomniverse-client.toml");
        ITEMS.register("sinister_compass", () -> new thaumicomniverse.content.ObeliskCompassItem(false));
        ITEMS.register("primal_compass", () -> new thaumicomniverse.content.ObeliskCompassItem(true));
        if(mods.isLoaded("hostilenetworks"))
            ITEMS.register("outerlands_prediction",()->new net.minecraft.world.item.Item(new net.minecraft.world.item.Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
        if (mods.isLoaded("mysticalagriculture") || mods.isLoaded("productivebees"))
            ITEMS.register("primordial_fragment", () -> new net.minecraft.world.item.Item(new net.minecraft.world.item.Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC)));
        if (mods.isLoaded("farmersdelight")) thaumicomniverse.compat.FarmersDelightCompat.register(ITEMS);
        if (mods.isLoaded("mekanismtools")) thaumicomniverse.compat.MekanismToolsCompat.register(ITEMS, bus);
        if (mods.isLoaded("allthemodium")) thaumicomniverse.compat.AtmCompat.register(ITEMS, bus);
        if (mods.isLoaded("sophisticatedbackpacks")) thaumicomniverse.compat.backpack.BackpackCompat.register(ITEMS);
        if (mods.isLoaded("create")) thaumicomniverse.compat.create.CreateCompat.register(bus);
        if (mods.isLoaded("thermal_expansion")) thaumicomniverse.compat.thermal.ThermalContent.register(bus);
        if (mods.isLoaded("quark")) thaumicomniverse.expansion.ArcaneCrafter.register(bus);
        thaumicomniverse.compat.create.AuraContent.register(bus);
        if (mods.isLoaded("draconicevolution")) thaumicomniverse.compat.draconic.ArcaneModules.register(bus);
        if (mods.isLoaded("gtceu")) thaumicomniverse.compat.gregtech.ArcaneGT.register(bus);
        if (mods.isLoaded("railcraft")) thaumicomniverse.compat.railcraft.ArcaneRail.register(bus);
        thaumicomniverse.content.ModernAlchemy.register(bus);
        thaumicomniverse.alignment.AlignmentContent.register(bus);
        thaumicomniverse.alignment.AlignmentNetwork.register();
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.AddReloadListenerEvent e) -> e.addListener(new thaumicomniverse.alignment.AlignmentDefinitions()));
        thaumicomniverse.expansion.AncientForestRegistration.register(bus);
        ITEMS.register(bus);
        bus.addListener(BackpackAliases::register);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(BackpackAliases::missing);
        TABS.register("collaboration", () -> net.minecraft.world.item.CreativeModeTab.builder()
                .title(net.minecraft.network.chat.Component.translatable("itemGroup.thaumicomniverse"))
                .icon(() -> new net.minecraft.world.item.ItemStack(tc4revival.content.TCItems.THAUMONOMICON.get()))
                .displayItems((parameters, output) -> {
                    ITEMS.getEntries().stream().filter(entry->RevivalFeatures.itemEnabled(entry.getId().getPath())).forEach(entry -> output.accept(entry.get()));
                    if (thaumicomniverse.compat.AvaritiaCompat.loaded() && RevivalFeatures.Group.AVARITIA.enabled())
                        output.accept(thaumicomniverse.compat.AvaritiaCompat.wand());
                    if (net.minecraftforge.fml.ModList.get().isLoaded("gtceu") && RevivalFeatures.Group.GREGTECH.enabled())
                        thaumicomniverse.compat.gregtech.ArcaneMachines.displayItems(output);
                })
                .build());
        TABS.register(bus);
        thaumicomniverse.recipe.CollaborationRecipes.register(bus);
        bus.addListener(this::setup);
    }
    private void setup(net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            if(net.minecraftforge.fml.ModList.get().isLoaded("create"))thaumicomniverse.compat.create.SequenceAspects.setup();
            thaumicomniverse.diagnostic.BookOpeningTrace.setup();
            thaumcraft.api.worldgen.NaturalGenerationRules.registerOreProvider("thaumicomniverse:atm",0,
                context->thaumicomniverse.compat.AtmOreGeneration.applies(context.level()),thaumicomniverse.compat.AtmOreGeneration::generate);
            thaumcraft.api.worldgen.NaturalGenerationRules.registerNodeChance("thaumicomniverse:ancient",0,(context,current)->
                thaumicomniverse.expansion.AncientForest.isAncient(context.level().getBiome(new net.minecraft.core.BlockPos(context.chunk().getMiddleBlockX(),context.level().getSeaLevel(),context.chunk().getMiddleBlockZ())))?current*4:current);
            thaumicomniverse.compat.nodemap.NodeMapRecords.setup();
            if(net.minecraftforge.fml.ModList.get().isLoaded("create"))thaumicomniverse.compat.create.AuraDrops.setup();
            if(net.minecraftforge.fml.ModList.get().isLoaded("create")){
                thaumicomniverse.compat.create.CreateDevices.setup();
                thaumcraft.api.crafting.MagicRecipeProviders.registerCrucible("thaumicomniverse:assembly",0,thaumicomniverse.compat.create.AssemblyCrucibleRecipe::candidate);
                thaumcraft.api.crafting.MagicRecipeProviders.registerInfusion("thaumicomniverse:assembly",0,thaumicomniverse.compat.create.AssemblyInfusionRecipe::find);
            }
            thaumcraft.api.research.ResearchDataContributors.register("thaumicomniverse:research",0,thaumcraft.api.research.ResearchDataContributors.Phase.BEFORE_ADDITIONS,source->{
                var loaded=net.minecraftforge.fml.ModList.get();
                source.add("categories",thaumicomniverse.research.CollaborationResearch.apply("research_catalog",source.getAsJsonArray("categories"),loaded::isLoaded));
                source.add("pages",thaumicomniverse.research.CollaborationResearch.apply("research_pages",source.getAsJsonArray("pages"),loaded::isLoaded));
                source.add("recipes",thaumicomniverse.research.CollaborationResearch.apply("research_page_recipes",source.getAsJsonArray("recipes"),loaded::isLoaded));return source;
            });
            thaumcraft.api.wands.MagicOperationRules.register("thaumicomniverse:feature_gates",100,context->FeatureItemGates.disabled(context.wand())
                ?thaumcraft.api.wands.MagicOperationRules.Verdict.deny(null):thaumcraft.api.wands.MagicOperationRules.Verdict.pass());
            thaumcraft.api.wands.WandRechargePolicies.registerVeto(net.minecraft.resources.ResourceLocation.parse("thaumicomniverse:feature_gates"),FeatureItemGates::disabled);
            thaumicomniverse.compat.GolemHazards.setup();
            thaumicomniverse.compat.TabletVisCharging.setup();
            if (thaumicomniverse.compat.AvaritiaCompat.loaded()) thaumicomniverse.compat.AvaritiaCompat.setup();
            if (net.minecraftforge.fml.ModList.get().isLoaded("apotheosis")) thaumicomniverse.compat.ApotheosisEnchantments.setup();
            if (net.minecraftforge.fml.ModList.get().isLoaded("apotheosis")) thaumicomniverse.compat.WarpedGemDrops.setup();
            if (net.minecraftforge.fml.ModList.get().isLoaded("sophisticatedbackpacks")) thaumicomniverse.compat.backpack.BackpackCompat.setup();
        });
    }
}
