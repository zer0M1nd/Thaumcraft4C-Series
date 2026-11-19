package ttmodern;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

public final class ForcefieldBlock extends Block {
    public ForcefieldBlock() { super(Properties.of().strength(0).noLootTable().noOcclusion().isViewBlocking((s,w,p)->false).isSuffocating((s,w,p)->false)); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
    @Override public void onPlace(BlockState state,Level world,BlockPos pos,BlockState old,boolean moving) { world.scheduleTick(pos,this,62); }
    @Override public void tick(BlockState state,net.minecraft.server.level.ServerLevel world,BlockPos pos,net.minecraft.util.RandomSource random) { world.removeBlock(pos,false); }
}
