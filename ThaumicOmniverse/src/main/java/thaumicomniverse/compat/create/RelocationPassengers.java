package thaumicomniverse.compat.create;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerEvent;

/** Offline riders keep a destination even after their original train/ship ledger is retired. */
public final class RelocationPassengers extends SavedData {
    private final CompoundTag riders=new CompoundTag();
    public static RelocationPassengers get(MinecraftServer server){
        return server.overworld().getDataStorage().computeIfAbsent(RelocationPassengers::load,RelocationPassengers::new,"thaumicomniverse_relocated_riders");
    }
    private static RelocationPassengers load(CompoundTag tag){var data=new RelocationPassengers();data.riders.merge(tag.getCompound("Riders"));return data;}
    @Override public CompoundTag save(CompoundTag tag){tag.put("Riders",riders.copy());return tag;}
    public void remember(WandContraptionEntity vehicle,UUID player,int seat){
        var tag=new CompoundTag();tag.putUUID("Vehicle",vehicle.getUUID());tag.putInt("Seat",seat);
        riders.put(player.toString(),tag);update(vehicle);setDirty();
    }
    public void update(WandContraptionEntity vehicle){
        for(String key:riders.getAllKeys()){
            var tag=riders.getCompound(key);if(!tag.hasUUID("Vehicle")||!tag.getUUID("Vehicle").equals(vehicle.getUUID()))continue;
            var seats=vehicle.getContraption().getSeats();int seat=tag.getInt("Seat");
            var position=seat>=0&&seat<seats.size()?vehicle.toGlobalVector(seats.get(seat).getCenter(),1):vehicle.position();
            tag.putString("Dimension",vehicle.level().dimension().location().toString());
            tag.putDouble("X",position.x);tag.putDouble("Y",position.y+.5);tag.putDouble("Z",position.z);setDirty();
        }
    }
    public static void login(PlayerEvent.PlayerLoggedInEvent event){
        if(!(event.getEntity() instanceof ServerPlayer player))return;
        var data=get(player.server);var tag=data.riders.getCompound(player.getStringUUID());
        if(!tag.hasUUID("Vehicle"))return;
        var dimension=ResourceLocation.tryParse(tag.getString("Dimension"));if(dimension==null)return;
        var level=player.server.getLevel(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,dimension));if(level==null)return;
        // A player who deliberately mounted a different vehicle wins over an old snapshot.
        if(player.getVehicle()==null){
            player.teleportTo(level,tag.getDouble("X"),tag.getDouble("Y"),tag.getDouble("Z"),player.getYRot(),player.getXRot());
            if(level.getEntity(tag.getUUID("Vehicle")) instanceof WandContraptionEntity vehicle){
                int seat=tag.getInt("Seat");if(seat>=0&&seat<vehicle.getContraption().getSeats().size())vehicle.addSittingPassenger(player,seat);
            }
        }
        data.riders.remove(player.getStringUUID());data.setDirty();
    }
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event){
        if(!(event.getEntity() instanceof ServerPlayer player)||!(player.getVehicle() instanceof WandContraptionEntity vehicle))return;
        Integer seat=vehicle.getContraption().getSeatMapping().get(player.getUUID());
        if(seat!=null)get(player.server).remember(vehicle,player.getUUID(),seat);
    }
}
