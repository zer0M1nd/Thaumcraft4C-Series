package thaumicfutures.expansion;
import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.shapes.*;
import tc4revival.content.AlembicBlockEntity;
import thaumcraft.api.aspects.EssentiaSmelter;

/** TC6 auxiliary pump: adds another alembic column beside any implementing alchemical furnace. */
public class SlurryPump extends BaseEntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public SlurryPump(){super(Properties.copy(Blocks.IRON_BLOCK).strength(2).noOcclusion());registerDefaultState(defaultBlockState().setValue(FACING,Direction.NORTH));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){
        var face=context.getClickedFace();
        if(!face.getAxis().isHorizontal())return null;
        var state=defaultBlockState().setValue(FACING,face.getOpposite());
        return canSurvive(state,context.getLevel(),context.getClickedPos())?state:null;
    }
    @Override public boolean canSurvive(BlockState state,LevelReader level,BlockPos pos){return level.getBlockEntity(pos.relative(state.getValue(FACING))) instanceof EssentiaSmelter;}
    @Override public BlockState updateShape(BlockState state,Direction side,BlockState other,LevelAccessor level,BlockPos pos,BlockPos otherPos){
        return side==state.getValue(FACING)&&!canSurvive(state,level,pos)?Blocks.AIR.defaultBlockState():super.updateShape(state,side,other,level,pos,otherPos);
    }
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return box(2,0,2,14,16,14);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new Tile(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){
        return level.isClientSide?null:createTickerHelper(type,RestoredContent.PUMP_ENTITY.get(),(world,pos,block,tile)->tile.tick());
    }
    public static class Tile extends BlockEntity {
        public Tile(BlockPos pos,BlockState state){super(RestoredContent.PUMP_ENTITY.get(),pos,state);}
        public void tick(){
            if(level==null||!thaumicfutures.FutureFeatures.Group.RESTORATION.enabled())return;
            var sourcePos=worldPosition.relative(getBlockState().getValue(FACING));
            if(!level.hasChunkAt(sourcePos)||!(level.getBlockEntity(sourcePos) instanceof EssentiaSmelter smelter)
                    ||level.getGameTime()%Math.max(1,smelter.exportInterval())!=0)return;
            for(int height=1;height<=smelter.alembicLimit();height++){
                var pos=worldPosition.above(height);
                if(!level.hasChunkAt(pos)||!(level.getBlockEntity(pos) instanceof AlembicBlockEntity alembic))break;
                var available=smelter.smeltedAspects();
                var aspect=alembic.amount()>0?alembic.aspect():alembic.filter();
                if(aspect==null)aspect=available.alphabetical().stream().filter(a->available.amount(a)>0).findFirst().orElse(null);
                if(aspect==null||alembic.amount()>=alembic.capacity()||smelter.extractSmelted(aspect,1,true)!=1)continue;
                if(alembic.addToContainer(aspect,1)==0){smelter.extractSmelted(aspect,1,false);return;}
            }
        }
    }
}
