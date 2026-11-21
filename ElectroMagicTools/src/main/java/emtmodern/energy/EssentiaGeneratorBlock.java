package emtmodern.energy;
import emtmodern.ElectroMagicTools;
import net.minecraft.core.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;

public class EssentiaGeneratorBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ACTIVE=BlockStateProperties.LIT;
    public final String aspect;
    public EssentiaGeneratorBlock(String aspect){super(Properties.of().strength(4).sound(SoundType.METAL));this.aspect=aspect;registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(ACTIVE,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING,ACTIVE);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){return defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite());}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new EssentiaGeneratorBlockEntity(pos,state);}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?null:createTickerHelper(type,ElectroMagicTools.GENERATOR_BE.get(),EssentiaGeneratorBlockEntity::tick);}
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random){
        if(state.getValue(ACTIVE))emtmodern.client.GeneratorVisuals.smoke(pos,tc4revival.aspect.TCAspects.catalog().require(aspect).color());
    }
}
