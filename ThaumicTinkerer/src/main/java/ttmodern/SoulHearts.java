package ttmodern;

import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.*;

@Mod.EventBusSubscriber(modid=Tinkerer.ID)
public final class SoulHearts {
    private static final String KEY="ThaumicTinkererSoulHearts";
    static final net.minecraftforge.network.simple.SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(net.minecraft.resources.ResourceLocation.parse(Tinkerer.ID+":main"),()->"4","4"::equals,"4"::equals);
    public record EnchanterAction(int menu,int action){}
    public static void enchanterAction(int menu,int action){CHANNEL.sendToServer(new EnchanterAction(menu,action));}
    public record Update(float amount){}
    public record ArmorToggle(){}
    public record ArmorState(boolean disabled){}
    public record BedrockOres(java.util.List<net.minecraft.resources.ResourceLocation> ores){}
    @SubscribeEvent public static void oreData(net.minecraftforge.event.OnDatapackSyncEvent event){
        var data=new BedrockOres(BedrockOreFeature.generatedOres(event.getPlayerList().getServer().registryAccess()));
        if(event.getPlayer()!=null)CHANNEL.send(PacketDistributor.PLAYER.with(event::getPlayer),data);
        else CHANNEL.send(PacketDistributor.ALL.noArg(),data);
    }
    public static void setup(){CHANNEL.messageBuilder(EnchanterAction.class,6,NetworkDirection.PLAY_TO_SERVER)
        .encoder((m,b)->{b.writeVarInt(m.menu);b.writeVarInt(m.action);}).decoder(b->new EnchanterAction(b.readVarInt(),b.readVarInt()))
        .consumerMainThread((m,c)->{var player=c.get().getSender();if(player!=null&&player.containerMenu.containerId==m.menu&&player.containerMenu instanceof EnchanterMenu menu)menu.clickMenuButton(player,m.action);}).add();
        CHANNEL.messageBuilder(BedrockOres.class,7,NetworkDirection.PLAY_TO_CLIENT)
            .encoder((m,b)->b.writeCollection(m.ores(),net.minecraft.network.FriendlyByteBuf::writeResourceLocation))
            .decoder(b->new BedrockOres(b.readList(net.minecraft.network.FriendlyByteBuf::readResourceLocation)))
            .consumerMainThread((m,c)->BedrockOreFeature.clientOres=java.util.Set.copyOf(m.ores())).add();
        CHANNEL.messageBuilder(Update.class,0,NetworkDirection.PLAY_TO_CLIENT)
        .encoder((packet,buffer)->buffer.writeFloat(packet.amount())).decoder(buffer->new Update(buffer.readFloat()))
        .consumerMainThread((packet,context)->net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->SoulHeartsHud.receive(packet.amount(),context.get().getNetworkManager()))).add();
        CHANNEL.messageBuilder(ArmorToggle.class,1,NetworkDirection.PLAY_TO_SERVER).encoder((packet,buffer)->{}).decoder(buffer->new ArmorToggle()).consumerMainThread((packet,context)->{
            var player=context.get().getSender();if(player==null)return;boolean wearing=false;for(var stack:player.getArmorSlots())wearing|=stack.getItem() instanceof AwakenedArmorItem;
            if(wearing){player.getPersistentData().putBoolean(AwakenedArmorEffects.DISABLED,!player.getPersistentData().getBoolean(AwakenedArmorEffects.DISABLED));sync(player);}
        }).add();
        CHANNEL.messageBuilder(ArmorState.class,2,NetworkDirection.PLAY_TO_CLIENT).encoder((packet,buffer)->buffer.writeBoolean(packet.disabled())).decoder(buffer->new ArmorState(buffer.readBoolean())).consumerMainThread((packet,context)->net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->AwakenedArmorClient.receive(packet.disabled(),context.get().getNetworkManager()))).add();
        CHANNEL.messageBuilder(ShadowbeamFocus.ShadowTrail.class,3,NetworkDirection.PLAY_TO_CLIENT).encoder(ShadowbeamFocus.ShadowTrail::encode).decoder(ShadowbeamFocus.ShadowTrail::decode).consumerMainThread((packet,context)->net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->KamiFocusClient.trail(packet))).add();
        CHANNEL.messageBuilder(SummoningSync.Snapshot.class,4,NetworkDirection.PLAY_TO_CLIENT).encoder(SummoningSync::encode).decoder(SummoningSync::decode).consumerMainThread((packet,context)->net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->SummoningClient.receive(packet,context.get().getNetworkManager()))).add();
        CHANNEL.messageBuilder(CropDropPreview.Snapshot.class,5,NetworkDirection.PLAY_TO_CLIENT).encoder(CropDropPreview::encode).decoder(CropDropPreview::decode).consumerMainThread((packet,context)->net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->SummoningClient.receiveCrops(packet,context.get().getNetworkManager()))).add();
    }
    public static float get(Player player){float value=player.getPersistentData().getFloat(KEY);return Float.isFinite(value)?Math.max(0,Math.min(20,value)):0;}
    static void set(Player player,float value){player.getPersistentData().putFloat(KEY,Float.isFinite(value)?Math.max(0,Math.min(20,value)):0);sync(player);}
    public static void add(Player player,float value){if(!player.level().isClientSide&&value>0)set(player,get(player)+value);}
    private static void sync(Player player){if(player instanceof ServerPlayer server&&!(server instanceof net.minecraftforge.common.util.FakePlayer)&&server.connection!=null){CHANNEL.send(PacketDistributor.PLAYER.with(()->server),new Update(get(player)));CHANNEL.send(PacketDistributor.PLAYER.with(()->server),new ArmorState(player.getPersistentData().getBoolean(AwakenedArmorEffects.DISABLED)));}}
    @SubscribeEvent(priority=EventPriority.HIGH) public static void damage(LivingHurtEvent event){
        if(event.isCanceled()||event.getAmount()<=0||!(event.getEntity() instanceof Player player)||player.level().isClientSide)return;
        float stored=get(player),used=Math.min(stored,event.getAmount());if(used>0){set(player,stored-used);event.setAmount(event.getAmount()-used);}
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event){sync(event.getEntity());}
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event){sync(event.getEntity());}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event){sync(event.getEntity());}
    private SoulHearts(){}
}
