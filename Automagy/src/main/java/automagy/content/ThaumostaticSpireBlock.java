package automagy.content;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
public final class ThaumostaticSpireBlock extends BaseEntityBlock {
    public static final BooleanProperty TOP=BooleanProperty.create("top");
    public ThaumostaticSpireBlock(){super(Properties.of().strength(5,10).sound(SoundType.METAL).requiresCorrectToolForDrops());registerDefaultState(stateDefinition.any().setValue(TOP,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(TOP);}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return state.getValue(TOP)?null:new ThaumostaticSpireBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide||state.getValue(TOP)?null:createTickerHelper(type,Content.THAUMOSTATIC_SPIRE_TILE.get(),ThaumostaticSpireBlockEntity::tick);}
    @Override public void playerWillDestroy(Level level,BlockPos pos,BlockState state,net.minecraft.world.entity.player.Player player){
        // Removing the upper half in creative must not trigger a survival drop from the lower half.
        if(!level.isClientSide&&player.getAbilities().instabuild&&state.getValue(TOP)){
            var lower=pos.below();var below=level.getBlockState(lower);
            if(below.is(this)&&!below.getValue(TOP))level.removeBlock(lower,false);
        }
        super.playerWillDestroy(level,pos,state,player);
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock())&&!level.isClientSide){var other=state.getValue(TOP)?pos.below():pos.above();
            if(level.hasChunkAt(other)&&level.getBlockState(other).is(this)&&level.getBlockState(other).getValue(TOP)!=state.getValue(TOP)){
                if(state.getValue(TOP))level.destroyBlock(other,true);else level.removeBlock(other,false);
            }
        }super.onRemove(state,level,pos,next,moving);
    }
}
