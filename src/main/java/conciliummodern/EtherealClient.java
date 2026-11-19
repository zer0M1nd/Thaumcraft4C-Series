package conciliummodern;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderBlockScreenEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=ThaumicConcilium.ID,value=Dist.CLIENT)
public final class EtherealClient {
    static void receive(boolean enabled,int flesh,int limbs,boolean robe){var player=Minecraft.getInstance().player;if(player==null)return;
        var tag=EtherealState.data(player);tag.putBoolean("enabled",enabled);tag.putInt("flesh",flesh);tag.putInt("limbs",limbs);tag.putBoolean("robe",robe);EtherealState.save(player,tag);
        if(!enabled)player.noPhysics=player.isSpectator();
    }
    @SubscribeEvent public static void overlay(RenderBlockScreenEffectEvent event){if(EtherealState.active(event.getPlayer()))event.setCanceled(true);}
    @SubscribeEvent public static void scroll(net.minecraftforge.client.event.InputEvent.MouseScrollingEvent event){
        var mc=Minecraft.getInstance();var player=mc.player;if(mc.screen!=null || player==null || !player.isUsingItem() || !player.isShiftKeyDown() || event.getScrollDelta()==0)return;
        if(player.getUseItem().getItem() instanceof tc4revival.content.CastingWandItem && tc4revival.content.WandFocusItem.attached(player.getUseItem()).getItem() instanceof EtherealManipulatorFocus){event.setCanceled(true);ManipulatorScroll.send(event.getScrollDelta()>0?1:-1);}
    }
    private EtherealClient(){}
}
