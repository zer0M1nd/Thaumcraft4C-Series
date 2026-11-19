package conciliummodern;

import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Server-owned, persistent eye inscriptions. Only derived readings travel to the client. */
@Mod.EventBusSubscriber(modid=ThaumicConcilium.ID)
public final class Lithography {
    private static final String KEY="thaumicconcilium:lithography";
    private static final net.minecraftforge.network.simple.SimpleChannel NETWORK=net.minecraftforge.network.NetworkRegistry.newSimpleChannel(
            net.minecraft.resources.ResourceLocation.parse("thaumicconcilium:lithography"),()->"2","2"::equals,"2"::equals);
    private record Readings(boolean visible,int[] values){}
    public static void init(){NETWORK.messageBuilder(Readings.class,0,net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
            .encoder((packet,buffer)->{buffer.writeBoolean(packet.visible);for(int value:packet.values)buffer.writeByte(value);})
            .decoder(buffer->{boolean visible=buffer.readBoolean();int[] values=new int[20];for(int i=0;i<20;i++)values[i]=Math.max(-1,Math.min(i<16?10:15,buffer.readByte()));return new Readings(visible,values);})
            .consumerMainThread((packet,context)->{net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->LithographyOverlay.readings(packet.visible,packet.values));context.get().setPacketHandled(true);}).add();}
    private static CompoundTag data(Player player){return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getCompound(KEY);}
    private static void save(Player player,CompoundTag data){var persisted=player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);persisted.put(KEY,data);player.getPersistentData().put(Player.PERSISTED_NBT_TAG,persisted);}
    public static boolean inscribed(Player player){return data(player).getBoolean("enabled");}
    public static void inscribe(ServerPlayer player,java.util.List<String> sources){
        var typed=new java.util.ArrayList<CompoundTag>();for(String name:sources){var tag=new CompoundTag();tag.putString("player",name);typed.add(tag);}inscribeSources(player,typed);
    }
    public static void inscribeSources(ServerPlayer player,java.util.List<CompoundTag> sources){
        var data=data(player);data.putBoolean("enabled",true);var list=new ListTag();for(int i=0;i<4;i++)list.add(i<sources.size()?sources.get(i).copy():new CompoundTag());data.put("sources",list);save(player,data);send(player);
    }
    public static void toggle(ServerPlayer player){var data=data(player);data.putBoolean("hidden",!data.getBoolean("hidden"));save(player,data);send(player);}
    private static void send(ServerPlayer player){
        var data=data(player);boolean visible=data.getBoolean("enabled")&&!data.getBoolean("hidden");int[] values=new int[20];java.util.Arrays.fill(values,-1);
        if(visible){var sources=data.getList("sources",Tag.TAG_COMPOUND);var legacy=data.getList("sources",Tag.TAG_STRING);for(int i=0;i<4;i++){
            var source=i<sources.size()?sources.getCompound(i):new CompoundTag();if(i<legacy.size())source.putString("player",legacy.getString(i));
            System.arraycopy(MonitorSource.playerValues(player,source),0,values,i*4,4);values[16+i]=MonitorSource.power(player,source,true);
        }}
        NETWORK.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(()->player),new Readings(visible,values));
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event){if(event.phase==TickEvent.Phase.END && event.player instanceof ServerPlayer player && player.tickCount%10==0)send(player);}
    @SubscribeEvent public static void clone(PlayerEvent.Clone event){var original=data(event.getOriginal());if(!original.isEmpty())save(event.getEntity(),original.copy());}
    private Lithography(){}
}
