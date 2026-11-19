package conciliummodern;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.shapes.*;

public class ConciliumCrucibleBlock extends BaseEntityBlock {
    final boolean flesh;
    private static final VoxelShape SHAPE=Shapes.or(box(0,0,0,16,5,16),box(0,0,0,2,16,16),box(14,0,0,16,16,16),box(2,0,0,14,16,2),box(2,0,14,14,16,16));
    public ConciliumCrucibleBlock(boolean flesh){super(Properties.of().strength(3,17).sound(SoundType.METAL).noOcclusion());this.flesh=flesh;}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return SHAPE;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new ConciliumCrucibleBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){
        return createTickerHelper(type,flesh?ConciliumBlocks.FLESH_CRUCIBLE_TILE.get():ConciliumBlocks.QUICKSILVER_CRUCIBLE_TILE.get(),(world,pos,block,tile)->tile.tick());
    }
    @Override public void entityInside(BlockState state,Level level,BlockPos pos,Entity entity){
        if(level.isClientSide || !entity.isAlive() || !(level.getBlockEntity(pos) instanceof ConciliumCrucibleBlockEntity tile))return;
        // The surrounding wall also invokes entityInside; only accept entities in the open basin.
        if(!entity.getBoundingBox().intersects(new net.minecraft.world.phys.AABB(pos).deflate(.125,0,.125)))return;
        if(flesh){
            if(level.hasNeighborSignal(pos))return;
            if(entity instanceof MadThaumaturge mad){tile.smelt(mad);return;}
            if(entity instanceof ServerPlayer player){tile.sip(player);return;}
        } else if(entity instanceof ItemEntity item){tile.smelt(item);return;}
        if(entity instanceof LivingEntity && entity.tickCount%10==0 && entity.hurt(level.damageSources().inFire(),1))
            level.playSound(null,pos,SoundEvents.FIRE_EXTINGUISH,SoundSource.BLOCKS,.4f,2+level.random.nextFloat()*.4f);
    }
    @Override public boolean hasAnalogOutputSignal(BlockState state){return true;}
    @Override public int getAnalogOutputSignal(BlockState state,Level level,BlockPos pos){return level.getBlockEntity(pos) instanceof ConciliumCrucibleBlockEntity tile?(int)(tile.amount()*14/100f)+(tile.amount()>0?1:0):0;}
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random){
        if(random.nextInt(10)==0 && level.getBlockEntity(pos) instanceof ConciliumCrucibleBlockEntity tile && tile.amount()>0)
            level.playLocalSound(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,ConciliumSounds.MELTED.get(),SoundSource.BLOCKS,1.1f+random.nextFloat()*.1f,1.2f+random.nextFloat()*.2f,false);
    }
}
