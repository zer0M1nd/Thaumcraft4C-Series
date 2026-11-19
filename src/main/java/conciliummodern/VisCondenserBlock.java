package conciliummodern;

import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.shapes.*;

public final class VisCondenserBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE=Block.box(0,0,0,16,11.2,16),SUPPORT=Block.box(0,15,0,16,16,16);
    public VisCondenserBlock(){super(Properties.of().strength(.7f,1).sound(SoundType.METAL).lightLevel(state->7).noOcclusion());}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return SHAPE;}
    @Override public VoxelShape getBlockSupportShape(BlockState state,BlockGetter level,BlockPos pos){return SUPPORT;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new VisCondenserBlockEntity(pos,state);}
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,net.minecraft.util.RandomSource random){
        if(level.getBlockEntity(pos) instanceof VisCondenserBlockEntity tile && tile.cooldown()>0)
            for(int i=0;i<5;i++)level.addParticle(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(150f/255,0,0),3),pos.getX()+.5,pos.getY()+1,pos.getZ()+.5,0,0,0);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){
        return createTickerHelper(type,ConciliumBlocks.CONDENSER_TILE.get(),VisCondenserBlockEntity::tick);
    }
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block changed,BlockPos from,boolean moving){
        if(!level.isClientSide && level.getBlockEntity(pos) instanceof VisCondenserBlockEntity tile)tile.activate();
    }
}
