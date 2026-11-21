package automagy.content;
import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
public final class RunedObsidianBlock extends Block {
 public static final IntegerProperty RUNE=IntegerProperty.create("rune",0,5);
 public RunedObsidianBlock(){super(Properties.of().strength(2.5f,2000).sound(SoundType.STONE));registerDefaultState(stateDefinition.any().setValue(RUNE,5));}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(RUNE);}
 @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context){var level=context.getLevel();var pos=context.getClickedPos();var choices=new java.util.ArrayList<Integer>(java.util.List.of(0,1,2,3,4,5));for(var dir:Direction.values()){var next=pos.relative(dir);if(!level.hasChunkAt(next))continue;var state=level.getBlockState(next);if(state.is(this))choices.remove(Integer.valueOf(state.getValue(RUNE)));}int rune=choices.isEmpty()?level.random.nextInt(6):choices.get(level.random.nextInt(choices.size()));return defaultBlockState().setValue(RUNE,rune);}
 @Override public boolean isFireSource(BlockState state,LevelReader level,BlockPos pos,Direction side){return true;}
}
