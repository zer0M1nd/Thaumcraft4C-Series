package tc4revival.content;

import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.IItemHandler;
import tc4revival.config.TCConfig;

/** Runtime transport decisions and game-tick retry exclusions, owned by one golem. */
public  class GolemTransportState {
    private record Timeout(ItemStack stack,long until) {}
    private final GolemWorkHost host;
    private final List<Timeout> timeouts=new ArrayList<>();
    public GolemTransportState(GolemWorkHost host) { this.host=host; }
    private ItemStack lastCargo=ItemStack.EMPTY;
    private net.minecraftforge.fluids.FluidStack lastFluid=net.minecraftforge.fluids.FluidStack.EMPTY;
    private tc4revival.aspect.Aspect lastEssentia;
    private int lastEssentiaAmount,stalledTicks;
    public void progress() { stalledTicks=0; }
    /** Progress is a completed operation or changed cargo, not merely walking in circles. */
    public void tickCargo() {
        if (!(host.mob().level() instanceof ServerLevel) || !host.workEnabled()) { stalledTicks=0; return; }
        var cargo=host.carried(); var fluid=host.fluid();
        if (!ItemStack.matches(cargo,lastCargo) || !net.minecraftforge.fluids.FluidStack.areFluidStackTagsEqual(fluid,lastFluid)
                || fluid.getFluid()!=lastFluid.getFluid() || fluid.getAmount()!=lastFluid.getAmount()
                || !Objects.equals(lastEssentia,host.essentia()) || lastEssentiaAmount!=host.essentiaAmount()) {
            lastCargo=cargo.copy();lastFluid=fluid.copy();lastEssentia=host.essentia();lastEssentiaAmount=host.essentiaAmount();stalledTicks=0;return;
        }
        if (!host.configuration().hasTask() || host.nearHomeAndIdle() && cargo.isEmpty() && fluid.isEmpty() && host.essentiaAmount()==0) { stalledTicks=0; return; }
        if (++stalledTicks>=2400 && host instanceof GolemEntity golem) {
            golem.recoverStalledTask(); stalledTicks=0;
        }
    }
    public boolean timedOut(ItemStack stack) {
        long now=host.mob().level().getGameTime();
        timeouts.removeIf(timeout->timeout.until()<=now);
        return timeouts.stream().anyMatch(timeout->GolemItemMatching.matches(timeout.stack(),stack,false,false,false));
    }
    private boolean reject(ItemStack stack) {
        if (!timedOut(stack)) timeouts.add(new Timeout(stack.copy(),host.mob().level().getGameTime()+Math.max(1,(TCConfig.SERVER.golemIgnoreMilliseconds()+49L)/50L)));
        return false;
    }
    /** Null means unfiltered extraction; an empty list means all configured candidates were rejected. */
    public List<ItemStack> needed() {
        var core=host.configuration().core();
        List<ItemStack> candidates;
        if (core==GolemCoreType.EMPTY || core==GolemCoreType.USE) {
            candidates=host.filters().itemsNeeded(host.configuration().upgrades(5)>0);
            if (candidates.isEmpty()) return null;
        } else if (core==GolemCoreType.SORTING) {
            if (!(host.mob().level() instanceof ServerLevel level)) return List.of();
            var inventory=GolemInventoryViews.single(level,host.homeInventory(),host.homeFacing());
            if (inventory==null) return List.of(); // Old missing-home null dereference could crash sorting AI.
            candidates=new ArrayList<>();
            for (int slot=0;slot<inventory.getSlots();slot++) {
                var stack=inventory.getStackInSlot(slot);
                if (!stack.isEmpty()) candidates.add(stack.copy());
            }
        } else return null;
        var result=new ArrayList<ItemStack>();
        for (var stack:candidates) if (!timedOut(stack) && findTarget(stack)) result.add(stack);
        return result;
    }
    public boolean validTarget(ItemStack stack) {
        return !timedOut(stack) && findTarget(stack);
    }
    public boolean findTarget(ItemStack stack) {
        if (!(host.mob().level() instanceof ServerLevel level)) return false;
        var core=host.configuration().core();
        if (core==GolemCoreType.USE) {
            for (byte color:host.filters().matchingColors(stack,host.configuration())) {
                for (var marker:host.markers().inDimension(level.dimension().location())) {
                    if (color!=-1 && marker.color()!=color) continue;
                    if (!level.hasChunkAt(marker.pos()) || level.isEmptyBlock(marker.pos())!=host.configuration().toggle(0)) continue;
                    var adjacent=marker.pos().relative(marker.face());
                    if (level.hasChunkAt(adjacent) && level.isEmptyBlock(adjacent)) return true;
                }
            }
        } else if (core==GolemCoreType.EMPTY || core==GolemCoreType.SORTING) {
            List<Byte> colors=host.filters().matchingColors(stack,host.configuration());
            double range=host.configuration().range();
            for (byte color:colors) {
                for (var pos:GolemMarkedInventories.withRoom(host,stack,color)) {
                    if (GolemInventoryViews.same(level,pos,host.homeInventory()) || host.mob().position().distanceToSqr(Vec3.atCenterOf(pos))>range*range) continue;
                    if (core==GolemCoreType.EMPTY) return true;
                    for (var face:host.markers().faces(pos,level.dimension().location(),(byte)-1))
                        if (!GolemItemTransfer.extract(GolemInventoryViews.single(level,pos,face),stack,stack.getCount(),host.configuration(),false).isEmpty()) return true;
                }
            }
            if (core==GolemCoreType.EMPTY) for (byte color:colors) {
                for (var marker:host.markers().inDimension(level.dimension().location()))
                    if ((color==-1 || marker.color()==color) && !marker.pos().equals(host.homeInventory()) && marker.pos().distSqr(host.home())<=range*range && !GolemInventoryViews.exists(level,marker.pos())) return true;
            }
        }
        return reject(stack);
    }
    public ItemStack first(IItemHandler inventory,boolean execute) {
        if (inventory==null || host.carrySpace()<=0) return ItemStack.EMPTY;
        for (int slot=0;slot<inventory.getSlots();slot++) {
            var stack=inventory.getStackInSlot(slot);
            if (stack.isEmpty() || timedOut(stack)) continue;
            var result=inventory.extractItem(slot,host.carrySpace(),!execute);
            if (!result.isEmpty()) return result;
        }
        return ItemStack.EMPTY;
    }
}
