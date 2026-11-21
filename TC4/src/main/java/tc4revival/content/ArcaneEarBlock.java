package tc4revival.content;

import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;

public  class ArcaneEarBlock extends BaseEntityBlock {
    public static final BooleanProperty POWERED=BlockStateProperties.POWERED;
    public ArcaneEarBlock() { super(Properties.of().strength(2.5f,6).sound(SoundType.WOOD).noOcclusion()); registerDefaultState(stateDefinition.any().setValue(POWERED,false)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(POWERED); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new ArcaneEarBlockEntity(pos,state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) { return level.isClientSide ? null : createTickerHelper(type,TCArcane.ARCANE_EAR_ENTITY.get(),ArcaneEarBlockEntity::tick); }
    @Override public boolean isSignalSource(BlockState state) { return true; }
    @Override public int getSignal(BlockState state,BlockGetter level,BlockPos pos,Direction direction) { return state.getValue(POWERED)?15:0; }
    @Override public int getDirectSignal(BlockState state,BlockGetter level,BlockPos pos,Direction direction) { return getSignal(state,level,pos,direction); }
    @Override public boolean canConnectRedstone(BlockState state,BlockGetter level,BlockPos pos,Direction side) { return true; }
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block block,BlockPos source,boolean moving) {
        if (level.getBlockEntity(pos) instanceof ArcaneEarBlockEntity ear) ear.updateTone();
        super.neighborChanged(state,level,pos,block,source,moving);
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity player,ItemStack stack) {
        if (level.getBlockEntity(pos) instanceof ArcaneEarBlockEntity ear) ear.updateTone();
    }
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ArcaneEarBlockEntity ear) ear.changePitch();
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public boolean triggerEvent(BlockState state,Level level,BlockPos pos,int event,int note) {
        if (event>=-1 && event<NoteBlockInstrument.values().length) {
            if (level.isClientSide) {
                if (event>=0) {
                    var sound=NoteBlockInstrument.values()[event].getSoundEvent();
                    level.playLocalSound(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,sound.get(),SoundSource.BLOCKS,3,(float)Math.pow(2,(note-12)/12.0),false);
                }
                level.addParticle(ParticleTypes.NOTE,pos.getX()+.5,pos.getY()+1.2,pos.getZ()+.5,note/24.0,0,0);
            }
            return true;
        }
        return super.triggerEvent(state,level,pos,event,note);
    }
}
