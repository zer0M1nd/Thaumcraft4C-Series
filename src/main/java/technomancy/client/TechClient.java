package technomancy.client;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import technomancy.*;

@Mod.EventBusSubscriber(modid=Technomancy.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class TechClient {
    private static final Map<net.minecraft.world.item.Item,Integer> METAL_COLORS=new HashMap<>();
    @SubscribeEvent public static void setup(FMLClientSetupEvent event){event.enqueueWork(()->{
        MenuScreens.register(TechRegistry.MENU.get(),MachineScreen::new);
        ItemBlockRenderTypes.setRenderLayer(TechFluids.SOURCE.get(),RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(TechFluids.FLOWING.get(),RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(TechRegistry.GLASS.get(),RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(TechRegistry.GLASS_PANE.get(),RenderType.translucent());
    });}
    public static void openTome(){Minecraft.getInstance().setScreen(new RitualTomeScreen());}
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){event.registerBlockEntityRenderer(TechRegistry.MACHINE.get(),MachineRenderer::new);}
    @SubscribeEvent public static void reload(RegisterClientReloadListenersEvent event){event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)manager->{LegacyGeometry.clear();MachineEffects.clear();});}
    public static void power(CompoundTag data){if(data!=null&&Minecraft.getInstance().player!=null)Minecraft.getInstance().player.getPersistentData().put(Technomancy.ID,data.copy());}
    @SubscribeEvent public static void colors(RegisterColorHandlersEvent.Item event){
        for(int i=1;i<=6;i++)event.register((stack,index)->index==0?metalColor(OreProcessing.ingot(stack)):0xffffff,TechRegistry.item("purified_ore_"+i));
    }
    private static int metalColor(ItemStack ingot) {
        if(ingot.isEmpty())return 0xb8a6cf;
        return METAL_COLORS.computeIfAbsent(ingot.getItem(),item->{
            var model=Minecraft.getInstance().getItemRenderer().getModel(ingot,null,null,0);var sprite=model.getParticleIcon();
            long red=0,green=0,blue=0,n=0;
            for(int x=0;x<sprite.contents().width();x++)for(int y=0;y<sprite.contents().height();y++) {
                int pixel=sprite.getPixelRGBA(0,x,y);if((pixel>>>24)<128)continue;
                red+=pixel&255;green+=pixel>>>8&255;blue+=pixel>>>16&255;n++;
            }
            return n==0?0xffffff:((int)(red/n)<<16)|((int)(green/n)<<8)|(int)(blue/n);
        });
    }
    @SubscribeEvent public static void hud(RegisterGuiOverlaysEvent event){
        event.registerAboveAll("existence",(gui,graphics,partial,width,height)->{
            var mc=Minecraft.getInstance();if(mc.player==null||mc.options.hideGui||!TechConfig.HUD.get())return;
            var data=PlayerPower.data(mc.player);int x=16,y=75;
            graphics.blit(Technomancy.id("textures/models/hud.png"),x,y,0,0,32,32,256,256);
            int amount=Math.min(20,20*data.getInt("power")/Math.max(1,data.getInt("level")));
            graphics.blit(Technomancy.id("textures/models/exhud.png"),x,y+25-amount,0,0,32,amount,256,256);
            graphics.drawString(mc.font,Integer.toString(data.getInt("level")),x+36,y+12,0x95dff5);
        });
    }
    @SubscribeEvent public static void textureReload(TextureStitchEvent.Post event){METAL_COLORS.clear();}
    private TechClient(){}
}
