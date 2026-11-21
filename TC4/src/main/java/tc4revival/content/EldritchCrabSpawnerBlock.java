package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;

public  class EldritchCrabSpawnerBlock extends BaseEntityBlock {
    public EldritchCrabSpawnerBlock() { super(Properties.of().strength(15,30).noOcclusion().lightLevel(state->4).isValidSpawn((state,world,pos,type)->false)); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new EldritchCrabSpawnerBlockEntity(pos,state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
        return createTickerHelper(type,TCEldritch.CRAB_SPAWNER_ENTITY.get(),(world,pos,block,spawner)->spawner.tick());
    }
    @Override public int getExpDrop(BlockState state,net.minecraft.world.level.LevelReader world,net.minecraft.util.RandomSource random,BlockPos pos,int fortune,int silkTouch) { return 6+random.nextInt(5); }
}
