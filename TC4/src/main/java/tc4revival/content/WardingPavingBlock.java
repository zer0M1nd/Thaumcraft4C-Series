package tc4revival.content;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
public  class WardingPavingBlock extends BaseEntityBlock {
    public WardingPavingBlock() { super(Properties.of().strength(2,10).sound(SoundType.STONE).isValidSpawn((state,world,pos,type)->false)); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new WardingPavingBlockEntity(pos,state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) { return level.isClientSide?null:createTickerHelper(type,TCArcane.WARDING_PAVING_ENTITY.get(),(world,pos,current,tile)->tile.tick()); }
}
