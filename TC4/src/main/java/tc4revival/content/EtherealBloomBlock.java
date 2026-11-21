package tc4revival.content;
import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
public  class EtherealBloomBlock extends BushBlock implements EntityBlock {
    public EtherealBloomBlock(){super(Properties.of().noCollission().instabreak().sound(SoundType.GRASS).randomTicks().lightLevel(state->15));}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context){return box(1.6,0,1.6,14.4,12.8,14.4);}
    @Override public boolean canSurvive(BlockState state,LevelReader level,BlockPos pos){return level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP);}
    @Override public net.minecraftforge.common.PlantType getPlantType(BlockGetter level,BlockPos pos){return net.minecraftforge.common.PlantType.CAVE;}
    @Override public void randomTick(BlockState state,net.minecraft.server.level.ServerLevel level,BlockPos pos,net.minecraft.util.RandomSource random){if(!state.canSurvive(level,pos))level.destroyBlock(pos,true);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new EtherealBloomBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return type==TCArcane.ETHEREAL_BLOOM_ENTITY.get()?(world,pos,current,tile)->((EtherealBloomBlockEntity)tile).tick():null;}
    @Override public int getFlammability(BlockState state,BlockGetter level,BlockPos pos,Direction side){return 100;}
    @Override public int getFireSpreadSpeed(BlockState state,BlockGetter level,BlockPos pos,Direction side){return 60;}
}
