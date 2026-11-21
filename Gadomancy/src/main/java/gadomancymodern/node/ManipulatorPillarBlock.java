package gadomancymodern.node;
import gadomancymodern.Gadomancy;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
public class ManipulatorPillarBlock extends BaseEntityBlock {
    public static final IntegerProperty ORIENTATION=IntegerProperty.create("orientation",2,5);public final boolean top;
    public ManipulatorPillarBlock(boolean top){super(Properties.of().strength(3,25).noOcclusion().sound(SoundType.STONE));this.top=top;registerDefaultState(stateDefinition.any().setValue(ORIENTATION,2));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(ORIENTATION);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return top?null:new PillarEntity(pos,state);}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context){return top?box(2,0,2,14,12,14):box(1,0,1,15,16,15);}
    @Override public java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state,net.minecraft.world.level.storage.loot.LootParams.Builder params){return java.util.List.of(new net.minecraft.world.item.ItemStack(top?tc4revival.content.TCEldritch.STONE.get():tc4revival.content.TCEldritch.PEDESTAL_STONE.get()));}
    public static class PillarEntity extends BlockEntity{public PillarEntity(BlockPos pos,BlockState state){super(Gadomancy.PILLAR_BE.get(),pos,state);}@Override public net.minecraft.world.phys.AABB getRenderBoundingBox(){return new net.minecraft.world.phys.AABB(worldPosition).expandTowards(0,1,0);}}
}
