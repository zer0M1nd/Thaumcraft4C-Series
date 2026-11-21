package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

/** Original bucket placement: ray includes sources, places beside the hit, and works in the Nether. */
public abstract class LegacyFluidBucketItem extends Item {
    protected LegacyFluidBucketItem() { super(new Properties().stacksTo(1)); }
    protected abstract net.minecraft.world.level.material.Fluid fluid();
    protected abstract net.minecraft.world.level.block.state.BlockState placedState();
    @Override public net.minecraftforge.common.capabilities.ICapabilityProvider initCapabilities(ItemStack stack,net.minecraft.nbt.CompoundTag tag) {
        return new net.minecraftforge.fluids.capability.templates.FluidHandlerItemStackSimple.SwapEmpty(stack,new ItemStack(Items.BUCKET),1000) {
            @Override public net.minecraftforge.fluids.FluidStack getFluid() {
                return container.is(LegacyFluidBucketItem.this)?new net.minecraftforge.fluids.FluidStack(fluid(),1000):net.minecraftforge.fluids.FluidStack.EMPTY;
            }
            @Override public boolean canFillFluidType(net.minecraftforge.fluids.FluidStack resource) { return false; }
        };
    }
    public boolean placeLiquid(Level level,BlockPos pos) {
        if (level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos)) return false;
        var state=level.getBlockState(pos);
        if (!state.isAir() && state.blocksMotion()) return false;
        if (level.isClientSide) return true;
        if (!state.isAir() && state.getFluidState().isEmpty()) level.destroyBlock(pos,true);
        return level.setBlock(pos,placedState(),3);
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        var stack=player.getItemInHand(hand); var hit=getPlayerPOVHitResult(level,player,ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType()!=HitResult.Type.BLOCK) return InteractionResultHolder.pass(stack);
        var target=hit.getBlockPos().relative(hit.getDirection());
        if (!level.mayInteract(player,target) || !player.mayUseItemAt(target,hit.getDirection(),stack) || !placeLiquid(level,target)) return InteractionResultHolder.fail(stack);
        return InteractionResultHolder.sidedSuccess(!level.isClientSide && !player.getAbilities().instabuild ? new ItemStack(Items.BUCKET) : stack,level.isClientSide);
    }
}
