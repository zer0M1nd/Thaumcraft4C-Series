package wtmodern;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraftforge.registries.DeferredRegister;

@Mod("warptheory")
public class WarpTheory {
    public WarpTheory(){
        RevivalFeatures.register();
        var bus=FMLJavaModLoadingContext.get().getModEventBus();
        Content.ITEMS.register(bus);Content.BLOCKS.register(bus);Content.TILES.register(bus);Content.ENTITIES.register(bus);
        Content.PARTICLES.register(bus);Content.TABS.register(bus);
        bus.addListener((EntityAttributeCreationEvent e)->e.put(Content.FRIEND.get(),Creeper.createAttributes().add(Attributes.MAX_HEALTH,100).build()));
        bus.addListener(WarpData::gather);
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent e)->e.enqueueWork(()->thaumcraft.api.ThaumcraftApi.addWarpToItem(Content.SOMETHING.getId(),1)));
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON,WarpConfig.SPEC);
        net.minecraftforge.common.crafting.CraftingHelper.register(new WarpRecipeCondition.Serializer());
        MinecraftForge.EVENT_BUS.register(WarpEvents.class);
        WarpNetwork.init();
    }
}
