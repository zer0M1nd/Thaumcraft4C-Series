package arcane_engineering;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.*;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid=ArcaneEngineering.ID)
public class BootsNetwork {
    private static final String VERSION="1";
    public static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(ArcaneEngineering.id("boots"),()->VERSION,VERSION::equals,VERSION::equals);
    private record Input(boolean thrust,boolean hover,int tick) {}
    private static final Map<UUID,Input> INPUTS=new HashMap<>();
    private static final Map<UUID,Integer> LAST_THRUST=new HashMap<>();
    public record Controls(boolean thrust,boolean hover) {
        static void encode(Controls value,FriendlyByteBuf buffer) { buffer.writeBoolean(value.thrust); buffer.writeBoolean(value.hover); }
        static Controls decode(FriendlyByteBuf buffer) { return new Controls(buffer.readBoolean(),buffer.readBoolean()); }
        static void handle(Controls value,Supplier<NetworkEvent.Context> supplier) {
            var context=supplier.get(); var player=context.getSender();
            context.enqueueWork(()->{ if(player!=null) acceptControls(player,value); }); context.setPacketHandled(true);
        }
    }
    public static void register() { CHANNEL.registerMessage(0,Controls.class,Controls::encode,Controls::decode,Controls::handle,Optional.of(NetworkDirection.PLAY_TO_SERVER)); }
    public static void acceptControls(ServerPlayer player,Controls value) {
        if(value.thrust && eligible(player)) INPUTS.put(player.getUUID(),new Input(true,value.hover,player.tickCount));
        else INPUTS.remove(player.getUUID());
    }
    private static boolean eligible(ServerPlayer player) {
        var boots=player.getItemBySlot(EquipmentSlot.FEET);
        return player.isAlive() && !player.isSpectator() && !player.isInWater() && !player.isPassenger()
            && !player.getAbilities().flying && boots.getItem() instanceof ImpulseBootsItem && ImpulseBootsItem.fuel(boots)>0;
    }
    /** Only actual server-authorized thrust, plus one second of coast, can bypass vanilla floating detection. */
    public static boolean hasFlightPermit(ServerPlayer player) {
        var last=LAST_THRUST.get(player.getUUID());
        if(last!=null && eligible(player) && player.tickCount-last>=0 && player.tickCount-last<=20) return true;
        LAST_THRUST.remove(player.getUUID()); return false;
    }
    private static void clear(UUID id) { INPUTS.remove(id); LAST_THRUST.remove(id); }
    public static double vertical(double speed,boolean hover) { return hover ? (speed>-.25 && speed<.2 ? 0 : speed<0 ? speed*.7 : speed) : speed<-.9 ? speed*.6 : speed+.1; }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        if(!eligible(player)) { clear(player.getUUID()); return; }
        hasFlightPermit(player); // Expire stale permissions even if no more input packets arrive.
        var input=INPUTS.get(player.getUUID());
        if(input==null) return;
        var boots=player.getItemBySlot(EquipmentSlot.FEET);
        if(player.tickCount-input.tick>10) { INPUTS.remove(player.getUUID()); return; }
        // Original consumes one mB with probability 1/2 each active tick. Payment is authoritative here.
        if(player.getRandom().nextBoolean() && !ImpulseBootsItem.consume(boots)) return;
        LAST_THRUST.put(player.getUUID(),player.tickCount);
        var velocity=player.getDeltaMovement(); double y=vertical(velocity.y,input.hover);
        player.setDeltaMovement(velocity.x,y,velocity.z); player.hurtMarked=true;
        if(y>-.6) player.fallDistance=0;
        if(player.tickCount%2==0) player.serverLevel().sendParticles(ParticleTypes.FLAME,player.getX(),player.getY()+.1,player.getZ(),2,.15,0,.15,.025);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { clear(event.getEntity().getUUID()); }
    @SubscribeEvent public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) { clear(event.getEntity().getUUID()); }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) { clear(event.getEntity().getUUID()); }
}
