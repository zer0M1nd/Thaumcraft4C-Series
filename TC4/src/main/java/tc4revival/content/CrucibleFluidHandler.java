package tc4revival.content;

import java.util.Objects;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/** A water-only Forge capability backed by the same state used for boiling and crafting. */
public  class CrucibleFluidHandler implements IFluidHandler {
    private final CrucibleContents contents;
    private final Runnable changed;
    public CrucibleFluidHandler(CrucibleContents contents, Runnable changed) { this.contents = Objects.requireNonNull(contents); this.changed = Objects.requireNonNull(changed); }
    @Override public int getTanks() { return 1; }
    private static void check(int tank) { if (tank != 0) throw new IndexOutOfBoundsException("Crucible has only tank zero"); }
    @Override public FluidStack getFluidInTank(int tank) { check(tank); return contents.water() == 0 ? FluidStack.EMPTY : new FluidStack(Fluids.WATER, contents.water()); }
    @Override public int getTankCapacity(int tank) { check(tank); return CrucibleContents.WATER_CAPACITY; }
    @Override public boolean isFluidValid(int tank, FluidStack stack) { check(tank); return stack.getFluid() == Fluids.WATER; }
    @Override public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !isFluidValid(0, resource)) return 0;
        int accepted = contents.fill(resource.getAmount(), action.execute());
        if (accepted > 0 && action.execute()) changed.run(); return accepted;
    }
    @Override public FluidStack drain(FluidStack resource, FluidAction action) {
        return resource.isEmpty() || !isFluidValid(0, resource) ? FluidStack.EMPTY : drain(resource.getAmount(), action);
    }
    @Override public FluidStack drain(int maximum, FluidAction action) {
        int removed = contents.drain(maximum, action.execute());
        if (removed == 0) return FluidStack.EMPTY;
        if (action.execute()) changed.run(); return new FluidStack(Fluids.WATER, removed);
    }
}
