package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public  class BrainJarBlock extends BaseEntityBlock {
    public BrainJarBlock(){super(Properties.of().strength(.3f).noOcclusion().lightLevel(state->9).sound(new net.minecraftforge.common.util.ForgeSoundType(1,1,TCSounds.JAR,TCSounds.JAR,TCSounds.JAR,TCSounds.JAR,TCSounds.JAR)));}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public VoxelShape getShape(BlockState state,BlockGetter world,BlockPos pos,CollisionContext context){return box(3,0,3,13,12,13);}
    @Override public VoxelShape getCollisionShape(BlockState state,BlockGetter world,BlockPos pos,CollisionContext context){return getShape(state,world,pos,context);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new BrainJarBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return createTickerHelper(type,TCArcane.BRAIN_JAR_ENTITY.get(),(world,pos,block,jar)->jar.tick());}
    @Override public boolean hasAnalogOutputSignal(BlockState state){return true;}
    @Override public int getAnalogOutputSignal(BlockState state,Level level,BlockPos pos){return level.getBlockEntity(pos) instanceof BrainJarBlockEntity jar?jar.comparator():0;}
    @Override public float getEnchantPowerBonus(BlockState state,LevelReader level,BlockPos pos){return 2;}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!(level.getBlockEntity(pos) instanceof BrainJarBlockEntity jar))return InteractionResult.PASS;
        jar.release();if(level.isClientSide)level.playLocalSound(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,TCSounds.JAR.get(),net.minecraft.sounds.SoundSource.BLOCKS,.2f,1,false);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){if(!state.is(next.getBlock())&&level.getBlockEntity(pos) instanceof BrainJarBlockEntity jar)jar.broken();super.onRemove(state,level,pos,next,moving);}
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,net.minecraft.util.RandomSource random){if(level.getBlockEntity(pos) instanceof BrainJarBlockEntity jar&&jar.experience()>=BrainJarBlockEntity.CAPACITY)tc4revival.client.MirrorTransferParticles.brainFull(level,pos);}
}
