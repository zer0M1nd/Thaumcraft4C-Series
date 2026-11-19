package tc4revival.content;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
public  class AlchemyCentrifugeBlock extends BaseEntityBlock {
    public AlchemyCentrifugeBlock(){super(Properties.of().strength(.5f,3).sound(SoundType.METAL).noOcclusion());}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context){return box(4,0,4,12,16,12);}
    @Override public net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context){return getShape(state,level,pos,context);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new AlchemyCentrifugeBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return createTickerHelper(type,TCArcane.ALCHEMY_CENTRIFUGE_ENTITY.get(),(world,pos,current,tile)->tile.tick());}
}
