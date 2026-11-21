package tc4revival.content;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
public  class WardingBarrierBlockEntity extends BlockEntity {
    private int count;
    public WardingBarrierBlockEntity(BlockPos pos,BlockState state) { super(TCArcane.WARDING_BARRIER_ENTITY.get(),pos,state); }
    public void tick() {
        if(level==null||level.isClientSide)return;
        if(count==0)count=level.random.nextInt(100);
        if(++count%100==0&&!level.getBlockState(worldPosition.below()).is(TCArcane.WARDING_PAVING.get())&&!level.getBlockState(worldPosition.below(2)).is(TCArcane.WARDING_PAVING.get()))level.removeBlock(worldPosition,false);
    }
}
