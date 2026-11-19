package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public  class EldritchTrapBlock extends BaseEntityBlock {
    public EldritchTrapBlock() {
        super(Properties.of().strength(15,30).noOcclusion().isValidSpawn((state,world,pos,type)->false));
    }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,net.minecraft.util.RandomSource random) {
        BlockPos at=pos.offset(random.nextInt(2)-random.nextInt(2),random.nextInt(2)-random.nextInt(2),random.nextInt(2)-random.nextInt(2));
        if (level.isEmptyBlock(at)) tc4revival.client.EldritchWisps.rune(level,at.getX()+random.nextFloat(),at.getY()+random.nextFloat(),at.getZ()+random.nextFloat(),
                .5f+random.nextFloat()*.5f,random.nextFloat()*.3f,.9f+random.nextFloat()*.1f,16+random.nextInt(4));
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new EldritchTrapBlockEntity(pos,state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type,TCEldritch.TRAP_ENTITY.get(),(world,pos,block,trap)->trap.tick());
    }
    @Override public int getExpDrop(BlockState state,net.minecraft.world.level.LevelReader world,net.minecraft.util.RandomSource random,BlockPos pos,int fortune,int silkTouch) {
        return 1+random.nextInt(4);
    }
}
