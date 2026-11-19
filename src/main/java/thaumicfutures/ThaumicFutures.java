package thaumicfutures;
@net.minecraftforge.fml.common.Mod("thaumicfutures")
public final class ThaumicFutures {
    public ThaumicFutures(){
        var bus=net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus();
        var mods=net.minecraftforge.fml.ModList.get();
        FutureFeatures.register();FeatureLootCondition.register(bus);FutureRecipes.register(bus);
        thaumcraft.api.resources.ExternalResourceConverters.register(net.minecraft.resources.ResourceLocation.parse("thaumicfutures:restored"),thaumicfutures.expansion.RestoredResources::append);
        thaumcraft.api.resources.ExternalResourceConverters.register(net.minecraft.resources.ResourceLocation.parse("thaumicfutures:legacy_bees"),
                thaumicfutures.compat.bees.LegacyBeeResources::prepare);
        thaumicfutures.expansion.RestoredIntegration.register();
        thaumicfutures.expansion.RestoredContent.register(bus);
        thaumicfutures.foci.FutureFoci.register(bus);
        thaumicfutures.autocaster.Autocasters.register(bus);
        thaumicfutures.devices.Devices.register(bus);
        if(mods.isLoaded("exdeorum"))thaumicfutures.compat.exdeorum.ExDeorumCompat.register(LegacyContent.ITEMS,bus);
        if(mods.isLoaded("tconstruct"))thaumicfutures.compat.TinkersCompat.register(bus);
        if(mods.isLoaded("botania"))thaumicfutures.compat.BotaniaCompat.register(LegacyContent.ITEMS,bus);
        if(mods.isLoaded("evilcraft"))thaumicfutures.compat.EvilCraftCompat.register(LegacyContent.ITEMS);
        if(mods.isLoaded("forestry")&&mods.isLoaded("mobees")){
            thaumicfutures.compat.bees.MagicBeeProducts.register(LegacyContent.ITEMS);
            thaumicfutures.compat.bees.LegacyBeeHives.register(LegacyContent.ITEMS,bus);
            thaumicfutures.compat.bees.BeeAuraContent.register(LegacyContent.ITEMS,bus);
        }
        if(mods.isLoaded("modular_machinery_reborn"))thaumicfutures.compat.machinery.MachineryContent.register(LegacyContent.ITEMS,bus);
        if(mods.isLoaded("immersiveengineering"))thaumicfutures.compat.ImmersiveEngineeringCompat.register();
        if(mods.isLoaded("industrialforegoing"))thaumicfutures.compat.IndustrialForegoingCompat.register(bus);
        if(mods.isLoaded("railcraft")){
            LegacyContent.ITEMS.register("thaumium_crowbar",()->new thaumicfutures.compat.railcraft.ArcaneCrowbar(false));
            LegacyContent.ITEMS.register("void_crowbar",()->new thaumicfutures.compat.railcraft.ArcaneCrowbar(true));
        }
        if(mods.isLoaded("create"))LegacyContent.ITEMS.register("alchemical_brass_sheet",()->new net.minecraft.world.item.Item(new net.minecraft.world.item.Item.Properties()));
        if(mods.isLoaded("gtceu"))LegacyBrassMaterials.register(bus);
        LegacyContent.ITEMS.register(bus);
        var tabs=net.minecraftforge.registries.DeferredRegister.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB,"thaumicfutures");
        tabs.register("restoration",()->net.minecraft.world.item.CreativeModeTab.builder().title(net.minecraft.network.chat.Component.translatable("itemGroup.thaumicfutures"))
            .icon(()->new net.minecraft.world.item.ItemStack(LegacyContent.BRASS.get())).displayItems((p,out)->LegacyContent.ITEMS.getEntries().stream().filter(e->FutureFeatures.itemEnabled(e.getId().getPath())).forEach(e->out.accept(e.get()))).build());tabs.register(bus);
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event)->event.enqueueWork(()->{
            thaumcraft.api.research.ResearchDataContributors.register("thaumicfutures:research",0,thaumcraft.api.research.ResearchDataContributors.Phase.BEFORE_ADDITIONS,source->{
                var loaded=net.minecraftforge.fml.ModList.get();
                source.add("categories",thaumicfutures.FutureResearch.apply("research_catalog",source.getAsJsonArray("categories"),loaded::isLoaded));
                source.add("pages",thaumicfutures.FutureResearch.apply("research_pages",source.getAsJsonArray("pages"),loaded::isLoaded));
                source.add("recipes",thaumicfutures.FutureResearch.apply("research_page_recipes",source.getAsJsonArray("recipes"),loaded::isLoaded));return source;
            });
            thaumcraft.api.wands.MagicOperationRules.register("thaumicfutures:feature_gates",100,context->FeatureItemGates.disabled(context.wand())
                ?thaumcraft.api.wands.MagicOperationRules.Verdict.deny(null):thaumcraft.api.wands.MagicOperationRules.Verdict.pass());
            thaumcraft.api.wands.WandRechargePolicies.registerVeto(net.minecraft.resources.ResourceLocation.parse("thaumicfutures:feature_gates"),FeatureItemGates::disabled);
            thaumcraft.api.research.ScannerViews.registerHint("thaumicfutures:celestial",0,player->{
                if(!FutureFeatures.Group.RESTORATION.enabled())return java.util.Optional.empty();
                var target=thaumicfutures.expansion.CelestialNotes.target(player);if(target==null)return java.util.Optional.empty();
                return java.util.Optional.of(new tc4revival.network.LensPacket(player.level().dimension().location(),
                    net.minecraft.network.chat.Component.translatable("thaumicfutures.sky."+target),java.util.List.of(),net.minecraft.network.chat.Component.empty()));
            });
            if(mods.isLoaded("pamhc2crops"))thaumicfutures.compat.PamsCompat.setup();
            if(mods.isLoaded("immersiveengineering"))thaumicfutures.compat.ImmersiveEngineeringCompat.setup();
        }));
    }
}
