package gadomancymodern.client;

import gadomancymodern.disguise.ArmorDisguise;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

/** Swap only client render inventory; protection and server equipment always remain the real armor. */
@Mod.EventBusSubscriber(modid="gadomancy",value=Dist.CLIENT)
public class DisguiseRendering {
    protected static final java.util.Map<Player,java.util.Deque<java.util.List<ItemStack>>> ORIGINAL=new java.util.WeakHashMap<>();
    public static void before(Player player){
        var armor=player.getInventory().armor;ORIGINAL.computeIfAbsent(player,p->new java.util.ArrayDeque<>()).push(java.util.List.copyOf(armor));
        for(int i=0;i<armor.size();i++)armor.set(i,ArmorDisguise.appearance(armor.get(i)));
    }
    public static void after(Player player){var saved=ORIGINAL.get(player);if(saved==null||saved.isEmpty())return;var armor=saved.pop();for(int i=0;i<armor.size();i++)player.getInventory().armor.set(i,armor.get(i));if(saved.isEmpty())ORIGINAL.remove(player);}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void before(net.minecraftforge.client.event.RenderLivingEvent.Pre<?,?> event){if(!event.isCanceled()&&event.getEntity() instanceof Player player)before(player);}
    @SubscribeEvent(priority=EventPriority.LOWEST,receiveCanceled=true) public static void after(net.minecraftforge.client.event.RenderLivingEvent.Post<?,?> event){if(event.getEntity() instanceof Player player)after(player);}
    @SubscribeEvent public static void finish(net.minecraftforge.event.TickEvent.RenderTickEvent event){if(event.phase==net.minecraftforge.event.TickEvent.Phase.END)for(var player:java.util.List.copyOf(ORIGINAL.keySet()))while(ORIGINAL.containsKey(player))after(player);}
}
