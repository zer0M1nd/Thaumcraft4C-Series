package thmodern.client;
import net.minecraft.client.*;
import net.minecraft.world.effect.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thmodern.Horizons;
import thmodern.lens.*;
@Mod.EventBusSubscriber(modid=Horizons.ID,value=Dist.CLIENT)
public final class LensClient {
    public static KeyMapping SELECT;
    private static final class LensVision extends MobEffectInstance {
        boolean owned=true;
        LensVision(){super(MobEffects.NIGHT_VISION,255,0,true,false,false);}
        @Override public boolean update(MobEffectInstance effect){if(!(effect instanceof LensVision))owned=false;return super.update(effect);}
    }
    @SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent event){
        if(event.phase!=net.minecraftforge.event.TickEvent.Phase.END)return;var mc=Minecraft.getInstance();
        while(SELECT.consumeClick())if(mc.player!=null&&mc.screen==null&&!LensEquipment.goggles(mc.player).isEmpty()){
            if(mc.player.isShiftKeyDown())LensNetwork.CHANNEL.sendToServer(new LensNetwork.Change(""));
            else mc.setScreen(new LensSelectionScreen());
        }
        if(mc.player==null)return;String key=LensEquipment.active(mc.player);boolean water=mc.player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER);
        boolean illuminate=mc.player.isAlive()&&(key.equals("fire")&&!water||key.equals("water")&&water);var effect=mc.player.getEffect(MobEffects.NIGHT_VISION);
        if(illuminate){if(effect==null||effect instanceof LensVision vision&&vision.owned&&effect.getDuration()<242)mc.player.addEffect(new LensVision());}
        else if(effect instanceof LensVision vision&&vision.owned)mc.player.removeEffect(MobEffects.NIGHT_VISION);
    }
    @SubscribeEvent public static void overlay(net.minecraftforge.client.event.RenderBlockScreenEffectEvent event){if(event.getOverlayType()==net.minecraftforge.client.event.RenderBlockScreenEffectEvent.OverlayType.WATER&&LensEquipment.active(event.getPlayer()).equals("water"))event.setCanceled(true);}
}
