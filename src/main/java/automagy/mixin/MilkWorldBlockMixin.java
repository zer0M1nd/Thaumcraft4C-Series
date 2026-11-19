package automagy.mixin;

import automagy.content.SoupFluids;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Forge supplies milk and its bucket but deliberately leaves its world block unset.
 * Supply only that missing fallback; respect another mod's non-air implementation.
 */
@Mixin(ForgeFlowingFluid.class)
public class MilkWorldBlockMixin {
 @Inject(method={"createLegacyBlock(Lnet/minecraft/world/level/material/FluidState;)Lnet/minecraft/world/level/block/state/BlockState;","m_5804_(Lnet/minecraft/world/level/material/FluidState;)Lnet/minecraft/world/level/block/state/BlockState;"},at=@At("RETURN"),cancellable=true,remap=false)
 private void automagy$milkBlock(FluidState fluid,CallbackInfoReturnable<BlockState> result){
  if(result.getReturnValue().isAir()&&ForgeMod.MILK.isPresent()&&ForgeMod.MILK.get().isSame(fluid.getType())&&SoupFluids.MILK_BLOCK.isPresent()){
   int level=fluid.isSource()?0:8-fluid.getAmount()+(fluid.getValue(FlowingFluid.FALLING)?8:0);
   result.setReturnValue(SoupFluids.MILK_BLOCK.get().defaultBlockState().setValue(LiquidBlock.LEVEL,level));
  }
 }
}
