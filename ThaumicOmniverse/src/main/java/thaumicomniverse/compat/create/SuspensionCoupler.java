package thaumicomniverse.compat.create;
import net.minecraft.core.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.*;
public class SuspensionCoupler extends Block {
    public static final DirectionProperty FACING=DirectionProperty.create("facing",Direction.UP,Direction.DOWN);
    public SuspensionCoupler(){super(Properties.copy(Blocks.OAK_PLANKS).strength(3).noOcclusion());registerDefaultState(defaultBlockState().setValue(FACING,Direction.UP));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx){return defaultBlockState().setValue(FACING,ctx.getClickedFace()==Direction.DOWN?Direction.DOWN:Direction.UP);}
    @Override public VoxelShape getShape(BlockState s,net.minecraft.world.level.BlockGetter l,BlockPos p,CollisionContext c){return s.getValue(FACING)==Direction.UP?Block.box(0,0,0,16,12,16):Block.box(0,4,0,16,16,16);}
    public static BlockPos partner(Level level,BlockPos pos){
        var state=level.getBlockState(pos);
        if(!ArcaneContraptions.enabled()||!(state.getBlock() instanceof SuspensionCoupler)||state.getValue(FACING)!=Direction.UP)return null;
        int range=10;
        for(int depth=1;depth<3;depth++){
            var below=pos.below(depth);if(!level.hasChunkAt(below))break;
            var support=level.getBlockState(below);
            if(!(support.getBlock() instanceof SuspensionCoupler)||support.getValue(FACING)!=Direction.UP)break;
            range+=10;
        }
        for(int y=1;y<=range;y++){
            var target=pos.above(y);if(!level.hasChunkAt(target))return null;
            var other=level.getBlockState(target);
            if(other.getBlock() instanceof SuspensionCoupler&&other.getValue(FACING)==Direction.DOWN)return target;
            if(other.isCollisionShapeFullBlock(level,target))return null;
        }return null;
    }
}
