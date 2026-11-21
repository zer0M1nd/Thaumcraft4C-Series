package thaumicomniverse.compat.create.airship;

import java.util.*;
import java.util.regex.Pattern;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.trains.schedule.*;
import com.simibubi.create.content.trains.schedule.condition.*;
import com.simibubi.create.content.trains.schedule.destination.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumicomniverse.compat.create.ArcaneCreateContent;

/** Uses Create's actual schedule objects and conditions, with airship-specific cargo and station context. */
public final class AirshipSchedule {
    private static final Map<FlightState,Schedule> schedules=new WeakHashMap<>();
    private AirshipSchedule(){}
    public static boolean hasConductor(AirshipEntity entity){return entity.getContraption().getBlocks().values().stream().anyMatch(b->b.state().is(ArcaneCreateContent.BURNER.get())&&b.state().getValue(BlazeBurnerBlock.HEAT_LEVEL)!=BlazeBurnerBlock.HeatLevel.NONE);}
    public static boolean interact(AirshipEntity entity,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);if(!stack.is(AirshipContent.SCHEDULE.get())&&!stack.isEmpty())return false;
        if(entity.level().isClientSide)return true;var s=entity.state();if(s==null)return true;
        if(!hasConductor(entity)){player.displayClientMessage(Component.translatable("airship.error.conductor"),true);return true;}
        if(stack.is(AirshipContent.SCHEDULE.get())){
            var schedule=ScheduleItem.getSchedule(stack);
            if(schedule==null||schedule.entries.isEmpty()){player.displayClientMessage(Component.translatable("create.schedule.no_stops"),true);return true;}
            if(!s.schedule.isEmpty()){player.displayClientMessage(Component.translatable("create.schedule.remove_with_empty_hand"),true);return true;}
            if(schedule.entries.size()>256||schedule.entries.stream().anyMatch(e->!(e.instruction instanceof DestinationInstruction||e.instruction instanceof ChangeTitleInstruction||e.instruction instanceof ChangeThrottleInstruction))){
                player.displayClientMessage(Component.translatable("airship.error.schedule_instruction"),true);return true;
            }
            if(schedule.entries.stream().flatMap(e->e.conditions.stream()).flatMap(Collection::stream).anyMatch(c->!supported(c))){player.displayClientMessage(Component.translatable("airship.error.schedule_condition"),true);return true;}
            s.schedule=schedule.write();s.scheduleIndex=Math.max(0,Math.min(schedule.savedProgress,schedule.entries.size()-1));s.waitState=new CompoundTag();s.scheduleTitle="";s.automatic=true;s.paused=false;
            schedules.put(s,schedule);stack.shrink(1);player.displayClientMessage(Component.translatable("airship.schedule.applied"),true);
        }else if(!s.schedule.isEmpty()){
            if(player.isShiftKeyDown()){
                var schedule=Schedule.fromTag(s.schedule);schedule.savedProgress=s.scheduleIndex;var returned=new ItemStack(AirshipContent.SCHEDULE.get());returned.getOrCreateTag().put("Schedule",schedule.write());
                player.setItemInHand(hand,returned);s.schedule=new CompoundTag();s.waitState=new CompoundTag();s.automatic=false;s.paused=false;s.forward=false;schedules.remove(s);
            }else{s.paused=!s.paused;s.automatic=true;player.displayClientMessage(Component.translatable(s.paused?"airship.schedule.paused":"airship.schedule.resumed"),true);}
        }
        return true;
    }
    public static void tick(FlightData data,FlightState s,AirshipEntity entity){
        if(!s.automatic||s.paused||s.schedule.isEmpty())return;
        if(!hasConductor(entity)){s.stop("airship.error.conductor");s.paused=true;return;}
        var schedule=schedules.computeIfAbsent(s,k->Schedule.fromTag(k.schedule));
        if(s.scheduleIndex>=schedule.entries.size()){
            if(!schedule.cyclic){s.stop("airship.schedule.complete");s.paused=true;return;}s.scheduleIndex=0;
        }
        var entry=schedule.entries.get(s.scheduleIndex);
        if(entry.instruction instanceof ChangeThrottleInstruction throttle){s.throttle=Math.max(.05,Math.min(1,throttle.getData().getInt("Value")/100d));advance(s);return;}
        if(entry.instruction instanceof ChangeTitleInstruction title){s.scheduleTitle=title.getScheduleTitle();advance(s);return;}
        if(!(entry.instruction instanceof DestinationInstruction destination)){s.stop("airship.error.schedule_instruction");s.paused=true;return;}
        var graph=data.graph;
        UUID target=s.waitState.hasUUID("Destination")?s.waitState.getUUID("Destination"):null;
        if(target==null||graph.node(target)==null){
            String filter=destination.getFilter();String regex=String.join(".*",Arrays.stream(filter.split("\\*",-1)).map(Pattern::quote).toList());
            UUID from=FlightNavigation.approaching(graph,s);if(from==null){s.stop("airship.error.no_exit");return;}
            double best=Double.POSITIVE_INFINITY;
            for(var node:graph.nodes())if(node.station()&&node.name().matches(regex)){
                var path=graph.shortestPath(from,node.id(),s.id,s.portalExitNode);if(path.isEmpty()&&!from.equals(node.id()))continue;
                double distance=path.stream().mapToDouble(graph::stepLength).sum();
                if(distance<best){best=distance;target=node.id();}
            }
            if(target==null){s.stop("airship.schedule.no_destination");return;}s.waitState.putUUID("Destination",target);
        }
        if(target.equals(s.atNode)){
            s.forward=false;s.message="airship.schedule.waiting";
            if(conditions(entry,s,entity,graph.node(s.atNode)))advance(s);return;
        }
        FlightNavigation.prepareChoice(graph,s);UUID from=FlightNavigation.approaching(graph,s);if(from==null){s.stop("airship.error.no_exit");return;}
        if(!s.selectionLocked){
            if(target.equals(from)){s.dockSelected=true;s.selectedExit=null;}
            else{
                var path=graph.shortestPath(from,target,s.id,s.portalExitNode);
                if(path.isEmpty()){s.forward=false;s.message="airship.wait.signal";return;}
                s.dockSelected=path.get(0).equals(from)&&graph.portal(from);s.selectedExit=s.dockSelected?null:path.get(0);
            }
        }
        s.forward=true;s.reverse=false;s.dockHeld=true;
    }
    private static void advance(FlightState s){s.scheduleIndex++;s.waitState=new CompoundTag();s.forward=false;}
    private static boolean supported(ScheduleWaitCondition c){return c instanceof ScheduledDelay||c instanceof TimeOfDayCondition||c instanceof RedstoneLinkCondition||c instanceof StationPoweredCondition||c instanceof StationUnloadedCondition||c instanceof PlayerPassengerCondition||c instanceof ItemThresholdCondition||c instanceof FluidThresholdCondition||c instanceof IdleCargoCondition;}
    private static boolean conditions(ScheduleEntry entry,FlightState s,AirshipEntity entity,FlightGraph.Node station){
        if(entry.conditions.isEmpty())return true;
        for(int col=0;col<entry.conditions.size();col++){
            var column=entry.conditions.get(col);String key="Column"+col;var state=s.waitState.getCompound(key);int index=state.getInt("Progress");
            if(index>=column.size())return true;
            var context=state.getCompound("Context");
            if(condition(column.get(index),s,entity,station,context)){state.putInt("Progress",index+1);state.put("Context",new CompoundTag());}
            else state.put("Context",context);
            s.waitState.put(key,state);
        }return false;
    }
    private static boolean condition(ScheduleWaitCondition condition,FlightState s,AirshipEntity e,FlightGraph.Node station,CompoundTag context){
        var level=e.level();
        if(condition instanceof ScheduledDelay||condition instanceof TimeOfDayCondition||condition instanceof RedstoneLinkCondition)return condition.tickCompletion(level,null,context);
        if(condition instanceof StationPoweredCondition)return level.hasChunkAt(station.block())&&level.hasNeighborSignal(station.block());
        if(condition instanceof StationUnloadedCondition)return !level.isLoaded(station.block());
        if(condition instanceof PlayerPassengerCondition player){long count=e.getPassengers().stream().filter(p->p instanceof Player).count();return player.canOvershoot()?count>=player.getTarget():count==player.getTarget();}
        if(condition instanceof ItemThresholdCondition item){
            var filter=FilterItemStack.of(item.getItem(0));var items=e.getContraption().getStorage().getAllItems();int count=0;
            for(int slot=0;slot<items.getSlots();slot++){var stack=items.getStackInSlot(slot);if(filter.test(level,stack))count+=item.getMeasure()==1?(stack.getCount()==stack.getMaxStackSize()?1:0):stack.getCount();}
            return item.getOperator().test(count,item.getThreshold());
        }
        if(condition instanceof FluidThresholdCondition fluid){
            var filter=FilterItemStack.of(fluid.getItem(0));var fluids=e.getContraption().getStorage().getFluids();long count=0;
            if(fluids!=null)for(int tank=0;tank<fluids.getTanks();tank++){var stack=fluids.getFluidInTank(tank);if(filter.test(level,stack))count+=stack.getAmount();}
            return fluid.getOperator().test((int)Math.min(Integer.MAX_VALUE,count),(int)Math.min(Integer.MAX_VALUE,(long)fluid.getThreshold()*1000));
        }
        if(condition instanceof IdleCargoCondition idle){
            int hash=1;var items=e.getContraption().getStorage().getAllItems();
            for(int slot=0;slot<items.getSlots();slot++)hash=31*hash+items.getStackInSlot(slot).serializeNBT().hashCode();
            var fluids=e.getContraption().getStorage().getFluids();if(fluids!=null)for(int tank=0;tank<fluids.getTanks();tank++)hash=31*hash+fluids.getFluidInTank(tank).writeToNBT(new CompoundTag()).hashCode();
            int ticks=context.contains("Hash")&&hash==context.getInt("Hash")?context.getInt("Ticks")+1:0;context.putInt("Hash",hash);context.putInt("Ticks",ticks);return ticks>idle.totalWaitTicks();
        }
        s.message="airship.error.schedule_condition";return false;
    }
}
