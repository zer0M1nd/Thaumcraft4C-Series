package automagy.content;
import net.minecraft.core.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.*;
/** The legacy aggregator used a normal BlockItem, so halves cannot combine into a double slab. */
public final class LocusAggregatorBlock extends Block {
 public static final EnumProperty<Half> HALF=BlockStateProperties.HALF;
 private static final VoxelShape LOWER=box(0,0,0,16,8,16),UPPER=box(0,8,0,16,16,16);
 public LocusAggregatorBlock(){super(Properties.of().strength(3,10).sound(SoundType.STONE).requiresCorrectToolForDrops());registerDefaultState(stateDefinition.any().setValue(HALF,Half.BOTTOM));}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(HALF);}
 @Override public BlockState getStateForPlacement(BlockPlaceContext context){boolean upper=context.getClickedFace()==Direction.DOWN||context.getClickedFace()!=Direction.UP&&context.getClickLocation().y-context.getClickedPos().getY()>.5;return defaultBlockState().setValue(HALF,upper?Half.TOP:Half.BOTTOM);}
 @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return state.getValue(HALF)==Half.TOP?UPPER:LOWER;}
}
