package fmmodern;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(ForbiddenMagic.ID)
public final class ForbiddenMagic {
    public static final String ID = "forbidden_magic";
    public ForbiddenMagic() {
        RevivalFeatures.register();
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON,ForbiddenConfig.SPEC);
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(IntegrationPacks::add);
        IntegrationContent.register();
        Content.ITEMS.register(bus);
        Content.EFFECTS.register(bus);
        Content.BLOCKS.register(bus);
        Content.ENTITIES.register(bus);
        Content.MOBS.register(bus);Content.PROFESSIONS.register(bus);Content.POIS.register(bus);
        bus.addListener((net.minecraftforge.event.entity.EntityAttributeCreationEvent event)->event.put(Content.HERETIC.get(),net.minecraft.world.entity.npc.Villager.createAttributes().build()));
        Content.TABS.register(bus);
        DarkEnchantments.REGISTER.register(bus);
        MiningLoot.REGISTER.register(bus);
        MinecraftForge.EVENT_BUS.addListener(AspectInputPack::captureLoader);
        MinecraftForge.EVENT_BUS.addListener(AspectInputPack::worldReady);
        MinecraftForge.EVENT_BUS.register(new SinEvents());
        MinecraftForge.EVENT_BUS.register(new DarkWands());
        MinecraftForge.EVENT_BUS.register(new ConfigSync());
        bus.addListener(ConfigSync::setup);
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event)->event.enqueueWork(()->{net.minecraftforge.common.crafting.CraftingHelper.register(new RecipeConfigCondition.Serializer());ForbiddenConfig.setup();DarkWands.setup();IntegrationContent.setup();}));
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.AddReloadListenerEvent event)->event.addListener(new CageMobs()));
    }
}
