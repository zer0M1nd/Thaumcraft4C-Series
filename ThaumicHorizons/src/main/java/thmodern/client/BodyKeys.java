package thmodern.client;

import net.minecraft.client.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thmodern.Horizons;
import thmodern.vat.*;

@Mod.EventBusSubscriber(modid=Horizons.ID,value=Dist.CLIENT)
public final class BodyKeys {
    public static KeyMapping FINGERS;
    public static KeyMapping CLIMB;
    public static KeyMapping INVISIBLE;
    public static void receive(CompoundTag data){var player=Minecraft.getInstance().player;if(player!=null&&data!=null)player.getPersistentData().put(SelfInfusions.DATA,data.copy());}
    @SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent event){
        if(event.phase!=net.minecraftforge.event.TickEvent.Phase.END)return;var mc=Minecraft.getInstance();
        while(CLIMB.consumeClick())if(mc.player!=null&&mc.screen==null)BodyNetwork.CHANNEL.sendToServer(new BodyNetwork.Action(0));
        while(INVISIBLE.consumeClick())if(mc.player!=null&&mc.screen==null)BodyNetwork.CHANNEL.sendToServer(new BodyNetwork.Action(1));
        while(FINGERS.consumeClick())if(mc.player!=null&&mc.screen==null)BodyNetwork.CHANNEL.sendToServer(new BodyNetwork.Action(2));
    }
}
