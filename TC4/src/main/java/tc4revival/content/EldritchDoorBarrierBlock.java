package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;

/** Invisible full collision behind the runic lock, without the warden field's effects. */
public  class EldritchDoorBarrierBlock extends Block {
    public EldritchDoorBarrierBlock() { super(Properties.of().strength(-1,Float.MAX_VALUE).noOcclusion().noLootTable()); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
    @Override public VoxelShape getShape(BlockState state,BlockGetter world,BlockPos pos,CollisionContext context) { return Shapes.empty(); }
    @Override public VoxelShape getCollisionShape(BlockState state,BlockGetter world,BlockPos pos,CollisionContext context) { return Shapes.block(); }
}
