package thaumicomniverse.compat.create.airship;

import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraftforge.event.entity.player.PlayerEvent;

/** The saved ledger owns seated passengers whenever their contraption projection is absent. */
public final class AirshipPassengers {
    private AirshipPassengers(){}
    private static CompoundTag record(AirshipEntity ship,Entity passenger){
        Integer seat=ship.getContraption().getSeatMapping().get(passenger.getUUID());
        if(seat==null)return null;
        var tag=new CompoundTag();tag.putInt("Seat",seat);
        if(passenger instanceof ServerPlayer)tag.putUUID("Player",passenger.getUUID());
        else tag.put("Entity",passenger.serializeNBT());
        return tag;
    }
    public static void snapshot(AirshipEntity ship,FlightState state){
        for(var passenger:ship.getPassengers()){
            var tag=record(ship,passenger);if(tag!=null)state.passengers.put(passenger.getStringUUID(),tag);
        }
    }
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event){
        if(!(event.getEntity() instanceof ServerPlayer player)||!(player.getVehicle() instanceof AirshipEntity ship))return;
        var state=ship.state();var tag=record(ship,player);if(state==null||tag==null)return;
        tag.putBoolean("Offline",true);state.passengers.put(player.getStringUUID(),tag);
        FlightData.get(player.server).setDirty();
    }
    /** Called before Create ejects riders. Nonplayers must not also be saved as loose chunk entities. */
    public static void suspend(AirshipEntity ship,FlightState state){
        snapshot(ship,state);ship.suspendingPassengers=true;
        for(var passenger:List.copyOf(ship.getPassengers())){
            passenger.stopRiding();
            if(!(passenger instanceof ServerPlayer))passenger.discard();
            passenger.getPersistentData().remove("ContraptionDismountLocation");
        }
    }
    public static boolean hasOnlinePassenger(ServerLevel level,FlightState state){
        for(String key:state.passengers.getAllKeys()){
            var tag=state.passengers.getCompound(key);
            if(tag.hasUUID("Player")){var player=level.getServer().getPlayerList().getPlayer(tag.getUUID("Player"));if(player!=null&&!player.hasDisconnected())return true;}
        }
        return false;
    }
    public static void restore(AirshipEntity ship,FlightState state){
        if(!ship.isAddedToWorld()||ship.isRemoved())return;
        var level=(ServerLevel)ship.level();
        for(String key:List.copyOf(state.passengers.getAllKeys())){
            var tag=state.passengers.getCompound(key);int seat=tag.getInt("Seat");
            if(seat<0||seat>=ship.getContraption().getSeats().size())continue;
            UUID id;try{id=UUID.fromString(key);}catch(IllegalArgumentException ex){continue;}
            if(ship.getPassengers().stream().anyMatch(p->p.getUUID().equals(id)))continue;
            Entity passenger;
            if(tag.hasUUID("Player")){
                var player=level.getServer().getPlayerList().getPlayer(id);if(player==null||player.hasDisconnected())continue;
                if(player.getVehicle()!=null&&player.getVehicle()!=ship){state.passengers.remove(key);continue;}
                player.teleportTo(level,ship.getX(),ship.getY()+1,ship.getZ(),player.getYRot(),player.getXRot());player.setPortalCooldown();passenger=player;
            }else{
                passenger=null;for(var dimension:level.getServer().getAllLevels()){passenger=dimension.getEntity(id);if(passenger!=null)break;}
                // A live UUID outside this vessel wins over a stale snapshot; never create a second copy.
                if(passenger!=null){state.passengers.remove(key);continue;}
                passenger=EntityType.loadEntityRecursive(tag.getCompound("Entity"),level,p->{p.moveTo(ship.position());return p;});
                if(passenger==null||!level.tryAddFreshEntityWithPassengers(passenger))continue;
            }
            ship.addSittingPassenger(passenger,seat);passenger.setPortalCooldown();tag.remove("Offline");
        }
    }
}
