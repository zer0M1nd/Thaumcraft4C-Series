package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;

/** Entity-side contract for task scheduling, carried goods and chest presentation. */
public interface GolemWorkHost {
    PathfinderMob mob();
    BlockPos home();
    Direction homeFacing();
    int workDelay();
    boolean workEnabled();
    GolemConfiguration configuration();
    GolemFilterInventory filters();
    GolemMarkers markers();
    GolemTransportState transport();
    tc4revival.aspect.Aspect essentia();
    int essentiaAmount();
    void essentia(tc4revival.aspect.Aspect aspect,int amount);
    net.minecraftforge.fluids.FluidStack fluid();
    void fluid(net.minecraftforge.fluids.FluidStack stack);
    boolean watchingFluid();
    void watchingFluid(boolean watching);
    ItemStack watched();
    void watched(ItemStack stack);
    default java.util.List<ItemStack> missingItems() { return GolemMissingItems.calculate(this); }
    double navigationSpeed();
    ItemStack carried();
    void carried(ItemStack stack);
    void action();
    void leftArm();
    void rightArm();
    String ownerName();
    void dart(net.minecraft.world.entity.LivingEntity target);
    void openInventory(BlockPos pos);
    void closeInventory(BlockPos pos);
    default BlockPos homeInventory() { return home().relative(homeFacing().getOpposite()); }
    default int carrySpace() {
        return carried().isEmpty()?configuration().carryLimit():Math.min(configuration().carryLimit()-carried().getCount(),carried().getMaxStackSize()-carried().getCount());
    }
    default boolean nearHomeAndIdle() {
        return workEnabled() && mob().getNavigation().isDone()
                && GolemWorkRange.reaches(this,home(),5);
    }
}
