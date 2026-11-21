package thaumicomniverse.compat.create;

import java.util.*;
import com.simibubi.create.content.contraptions.*;
import com.simibubi.create.content.contraptions.bearing.*;
import com.simibubi.create.content.contraptions.piston.*;
import com.simibubi.create.content.contraptions.gantry.GantryContraptionEntity;
import com.simibubi.create.content.contraptions.elevator.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.phys.Vec3;
import thaumicomniverse.mixin.*;
import thaumicomniverse.compat.create.airship.*;
import com.simibubi.create.content.trains.entity.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** Copies native mounted cargo before detaching; actuators restore only their own hardware. */
public final class RelocationTransfer {
    private static final ThreadLocal<Boolean> REMOUNTING=ThreadLocal.withInitial(()->false);
    public static boolean remounting(){return REMOUNTING.get();}

    public static WandContraptionEntity detach(Player player,AbstractContraptionEntity source){
        if(!(source.level() instanceof ServerLevel level)||source.isRemoved()||source.getContraption()==null)return null;
        if(source instanceof CarriageContraptionEntity carriage)return detachTrain(player,carriage);
        if(source instanceof AirshipEntity ship)return detachAirship(player,ship);
        IControlContraption controller=source instanceof ControlledContraptionEntity c?((CreateFocusControllerAccess)c).omniverse$controller():null;
        if(!(source instanceof GantryContraptionEntity||source.getVehicle() instanceof AbstractMinecart
                ||controller instanceof MechanicalBearingBlockEntity||controller instanceof ClockworkBearingBlockEntity
                ||controller instanceof LinearActuatorBlockEntity))return null;
        if(controller!=null&&!controller.isAttachedTo((ControlledContraptionEntity)source))return null;
        if(controller instanceof net.minecraft.world.level.block.entity.BlockEntity tile&&!allowed(player,tile.getBlockPos()))return null;
        if(!allowed(player,source))return null;
        var next=WandContraptionEntity.copyForThrow(source);
        if(next==null)return null;
        Contraption hardware=null;
        if(controller instanceof LinearActuatorBlockEntity){
            var tag=source.getContraption().writeNBT(false).copy();
            for(var key:List.of("Actors","Passengers","SubContraptions","Superglue","CapturedMultiblocks","Interactors","items","fluids"))tag.remove(key);
            hardware=Contraption.fromNBT(level,tag,false);
            if(source.getContraption() instanceof PistonContraption)hardware.getBlocks().values().removeIf(info->!pistonHardware(info));
            else hardware.getBlocks().clear();
        }
        // A cancelled spawn leaves both the controller and its inventory untouched.
        if(!level.addFreshEntity(next))return null;
        if(controller instanceof MechanicalBearingBlockEntity bearing){
            ((CreateRelocationBearingAccess)bearing).omniverse$detach(null);bearing.disassemble();bearing.setChanged();
        }else if(controller instanceof ClockworkBearingBlockEntity clock){
            var access=(CreateRelocationClockAccess)clock;
            if(access.omniverse$hour()==source)access.omniverse$hour(null);
            if(access.omniverse$minute()==source)access.omniverse$minute(null);
            access.omniverse$running(access.omniverse$hour()!=null||access.omniverse$minute()!=null);
            clock.setChanged();clock.sendData();
        }else if(controller instanceof LinearActuatorBlockEntity linear){
            if(source.getContraption() instanceof ElevatorContraption elevator){
                var column=ElevatorColumn.get(level,elevator.getGlobalColumn());if(column!=null)column.setActive(false);
            }
            transferRiders(source,next,false);
            source.getContraption().stop(level);((CreateRelocationEntityAccess)source).omniverse$replaceWithHardware(hardware);
            // Let the native controller restore its own rods/rope. The copied payload
            // has already left, so this cannot place or unmount its inventory twice.
            linear.disassemble();linear.assembleNextTick=false;linear.needsContraption=false;
            if(!source.isRemoved())source.discard();linear.setChanged();linear.sendData();return next;
        }
        transferRiders(source,next);
        return next;
    }
    private static boolean allowed(Player player,AbstractContraptionEntity source){
        if(source==null||source.isRemoved()||source.level()!=player.level()||source.getContraption()==null)return false;
        return source.getContraption().getBlocks().keySet().stream()
                .allMatch(local->allowed(player,BlockPos.containing(source.toGlobalVector(local.getCenter(),1))))
                &&source.getPassengers().stream().filter(AbstractContraptionEntity.class::isInstance)
                    .map(AbstractContraptionEntity.class::cast).allMatch(child->allowed(player,child));
    }
    private static void transferRiders(AbstractContraptionEntity source,WandContraptionEntity next){
        transferRiders(source,next,true);
    }
    private static void transferRiders(AbstractContraptionEntity source,WandContraptionEntity next,boolean discard){
        var seats=new HashMap<>(source.getContraption().getSeatMapping());
        REMOUNTING.set(true);
        try{
            for(var rider:List.copyOf(source.getPassengers())){
                Integer seat=seats.get(rider.getUUID());
                if(seat!=null)next.addSittingPassenger(rider,seat);
                else rider.startRiding(next,true);
                rider.getPersistentData().remove("ContraptionDismountLocation");
            }
            if(source.getVehicle() instanceof AbstractMinecart cart)cart.setDeltaMovement(Vec3.ZERO);
            if(discard)source.discard();
        }finally{REMOUNTING.remove();}
    }
    static boolean pistonHardware(net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo info){
        return info.state().is(com.simibubi.create.AllBlocks.PISTON_EXTENSION_POLE.get())||info.state().is(com.simibubi.create.AllBlocks.MECHANICAL_PISTON_HEAD.get());
    }
    private static void savedPassenger(WandContraptionEntity next,int seat,CompoundTag tag){
        if(tag.hasUUID("Player")||tag.hasUUID("PlayerPassenger")){
            UUID id=tag.hasUUID("Player")?tag.getUUID("Player"):tag.getUUID("PlayerPassenger");
            if(next.getPassengers().stream().noneMatch(e->e.getUUID().equals(id)))
                RelocationPassengers.get(((ServerLevel)next.level()).getServer()).remember(next,id,seat);
        }else next.carryPassenger(seat,tag.contains("Entity")?tag.getCompound("Entity"):tag);
    }
    private static WandContraptionEntity detachAirship(Player player,AirshipEntity source){
        var state=source.state();if(state==null||!allowed(player,source))return null;
        var next=WandContraptionEntity.copyForThrow(source);if(next==null||!source.level().addFreshEntity(next))return null;
        if(!state.schedule.isEmpty()){
            var schedule=com.simibubi.create.content.trains.schedule.Schedule.fromTag(state.schedule);schedule.savedProgress=state.scheduleIndex;
            var item=new ItemStack(AirshipContent.SCHEDULE.get());item.getOrCreateTag().put("Schedule",schedule.write());next.carryItem(item);
        }
        var saved=state.passengers.copy();AirshipManager.retireForRelocation(source);
        transferRiders(source,next);
        for(String id:saved.getAllKeys()){var tag=saved.getCompound(id);savedPassenger(next,tag.getInt("Seat"),tag);}
        return next;
    }
    /** All loaded carriages leave together, so no invalid spacing or dangling train references remain. */
    private static WandContraptionEntity detachTrain(Player player,CarriageContraptionEntity selected){
        var carriage=selected.getCarriage();if(carriage==null||carriage.train==null)return null;
        var train=carriage.train;var replacements=new LinkedHashMap<CarriageContraptionEntity,WandContraptionEntity>();
        for(var part:train.carriages){
            var source=part.anyAvailableEntity();
            if(part.presentInMultipleDimensions()||!allowed(player,source))return null;
            var next=WandContraptionEntity.copyForThrow(source);if(next==null)return null;
            replacements.put(source,next);
        }
        if(!replacements.containsKey(selected))return null;
        var spawned=new ArrayList<WandContraptionEntity>();
        for(var next:replacements.values()){
            if(!player.level().addFreshEntity(next)){spawned.forEach(net.minecraft.world.entity.Entity::discard);return null;}
            spawned.add(next);
        }
        if(train.runtime.getSchedule()!=null&&!train.runtime.isAutoSchedule){
            var schedule=train.runtime.getSchedule();schedule.savedProgress=train.runtime.currentEntry;
            var stack=new ItemStack(com.simibubi.create.AllItems.SCHEDULE.get());stack.getOrCreateTag().put("Schedule",schedule.write());
            replacements.get(selected).carryItem(stack);
        }
        train.runtime.discardSchedule();
        var station=train.getCurrentStation();if(station!=null)station.cancelReservation(train);
        com.simibubi.create.Create.RAILWAYS.removeTrain(train.id);
        com.simibubi.create.Create.RAILWAYS.markTracksDirty();
        com.simibubi.create.AllPackets.getChannel().send(net.minecraftforge.network.PacketDistributor.ALL.noArg(),new TrainPacket(train,false));
        for(var entry:replacements.entrySet()){
            var source=entry.getKey();var next=entry.getValue();
            var saved=new HashMap<>(((CreateRelocationCarriageAccess)source.getCarriage()).omniverse$passengers());
            transferRiders(source,next);saved.forEach((seat,tag)->savedPassenger(next,seat,tag));
            if(source!=selected)next.throwFrom(player);
        }
        return replacements.get(selected);
    }
    private static boolean allowed(Player player,BlockPos pos){
        return player.level().hasChunkAt(pos)&&player.level().mayInteract(player,pos)
                &&player.mayUseItemAt(pos,Direction.UP,player.getMainHandItem());
    }
    private RelocationTransfer(){}
}
