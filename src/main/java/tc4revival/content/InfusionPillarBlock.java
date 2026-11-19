package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public  class InfusionPillarBlock extends BaseEntityBlock {
    private final boolean upper;
    public InfusionPillarBlock(boolean upper) { super(Properties.of().strength(3,15).requiresCorrectToolForDrops().noOcclusion()); this.upper=upper; }
    @Override public RenderShape getRenderShape(BlockState state) { return upper ? RenderShape.INVISIBLE : RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return upper ? null : new InfusionPillarBlockEntity(pos,state); }
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) { return Block.box(0,upper ? -16 : 0,0,16,upper ? -8 : 8,16); }
    @Override public VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) { return Shapes.block(); }
    @Override public boolean triggerEvent(BlockState state,Level level,BlockPos pos,int id,int data) {
        if (id!=1) return super.triggerEvent(state,level,pos,id,data);
        if (level.isClientSide) {
            tc4revival.client.ClientFocusEffects.blockSparkle(level,pos,11960575);
            level.levelEvent(2001,pos,Block.getId(Blocks.STONE_BRICKS.defaultBlockState()));
        }
        return true;
    }
}
