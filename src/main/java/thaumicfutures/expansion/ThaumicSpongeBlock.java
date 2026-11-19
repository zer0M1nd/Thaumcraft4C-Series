package thaumicfutures.expansion;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.content.FluxGasBlock;
import tc4revival.content.FluxGooBlock;

public final class ThaumicSpongeBlock extends Block {
    public ThaumicSpongeBlock(){super(Properties.copy(Blocks.SPONGE));}
    @Override public void onPlace(BlockState state,Level level,BlockPos pos,BlockState previous,boolean moving){
        if(level.isClientSide||previous.is(this)||!thaumicfutures.FutureFeatures.Group.RESTORATION.enabled())return;
        int removed=0;
        for(var at:BlockPos.betweenClosed(pos.offset(-3,-3,-3),pos.offset(3,3,3))){
            if(level.isOutsideBuildHeight(at)||!level.hasChunkAt(at))continue;
            var block=level.getBlockState(at).getBlock();
            // removeBlock preserves the fluid at the position, including TC's finite flux fluids.
            if((block instanceof FluxGasBlock||block instanceof FluxGooBlock)
                    &&level.setBlockAndUpdate(at,Blocks.AIR.defaultBlockState()))removed++;
        }
        if(removed>0){
            level.setBlockAndUpdate(pos,RestoredContent.WET_SPONGE.get().defaultBlockState());
            level.playSound(null,pos,net.minecraft.sounds.SoundEvents.GENERIC_SPLASH,net.minecraft.sounds.SoundSource.BLOCKS,1,1);
        }
    }
}
