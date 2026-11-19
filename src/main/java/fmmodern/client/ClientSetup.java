package fmmodern.client;

import fmmodern.*;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid=ForbiddenMagic.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ClientSetup {
    @Mod.EventBusSubscriber(modid=ForbiddenMagic.ID,value=Dist.CLIENT)
    public static final class Disconnect {
        @SubscribeEvent public static void research(thaumcraft.api.research.ResearchTooltipEvent event){ResearchSourceLabels.decorate(event);}
        @SubscribeEvent public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event){ConfigSync.clear();}
    }
    @SubscribeEvent public static void models(net.minecraftforge.client.event.ModelEvent.RegisterAdditional event){event.register(new ResourceLocation(ForbiddenMagic.ID,"item/focus_blink_orn"));}
    @SubscribeEvent public static void renderers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event){if(IntegrationContent.botania())BotanyClient.flowers(event);event.registerBlockEntityRenderer(Content.CAGE_ENTITY.get(),CageRenderer::new);event.registerEntityRenderer(Content.HERETIC.get(),HereticRenderer::new);}
    @SubscribeEvent public static void colors(net.minecraftforge.client.event.RegisterColorHandlersEvent.Item event) {
        event.register((stack,tint)->tint!=1?0xffffff:switch(MorphTools.phase(stack)){case 1->0x0010cc;case 2->0xe5da00;default->0x980000;},Content.MORPH_PICK.get(),Content.MORPH_SPADE.get(),Content.MORPH_AXE.get(),Content.MORPH_SWORD.get());
    }
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(()->{if(IntegrationContent.botania())BotanyClient.setup();
            ItemProperties.register(Content.FORK.get(),ResourceLocation.parse("forbidden_magic:spork"),(stack,level,entity,seed)->fmmodern.ForbiddenConfig.value(ForbiddenConfig.SPORK)?1:0);});
        event.enqueueWork(()->ItemProperties.register(Content.MOB_CRYSTAL.get(),new ResourceLocation(ForbiddenMagic.ID,"filled"),(stack,level,entity,seed)->SpecialItems.MobCrystal.mob(stack)==null?0:1));
    }
}
