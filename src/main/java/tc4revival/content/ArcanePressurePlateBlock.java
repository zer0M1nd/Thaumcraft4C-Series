package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public  class ArcanePressurePlateBlock extends BaseEntityBlock {
    public static final BooleanProperty POWERED=BlockStateProperties.POWERED;
    public static final IntegerProperty MODE=IntegerProperty.create("mode",0,2);
    public ArcanePressurePlateBlock() {
        super(Properties.of().strength(2,999).sound(net.minecraft.world.level.block.SoundType.WOOD).noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(POWERED,false).setValue(MODE,0));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(POWERED,MODE); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new OwnedDeviceBlockEntity(pos,state); }
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) { return box(1,0,1,15,state.getValue(POWERED) ? .5 : 1,15); }
    @Override public VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) { return net.minecraft.world.phys.shapes.Shapes.empty(); }
    @Override public float getDestroyProgress(BlockState state,Player player,BlockGetter level,BlockPos pos) {
        return tc4revival.config.TCConfig.SERVER.wardedStone.get() ? 0 : super.getDestroyProgress(state,player,level,pos);
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity placer,ItemStack stack) {
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof OwnedDeviceBlockEntity entity) { entity.access().setOwner(player.getGameProfile().getName()); entity.changed(); }
    }
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof OwnedDeviceBlockEntity entity && entity.access().mayConfigure(player.getGameProfile().getName())) {
            int mode=(state.getValue(MODE)+1)%3;
            level.setBlock(pos,state.setValue(MODE,mode),2); entity.changed();
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(switch(mode) {
                case 0->"It will now trigger on everything.";
                case 1->"It will now trigger on everything except you.";
                default->"It will now trigger on just you.";
            }),false);
            level.playSound(null,pos,net.minecraft.sounds.SoundEvents.STONE_BUTTON_CLICK_ON,net.minecraft.sounds.SoundSource.BLOCKS,.1f,.9f);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void entityInside(BlockState state,Level level,BlockPos pos,Entity entity) {
        if (!level.isClientSide && !state.getValue(POWERED)) updateSignal(level,pos,state);
    }
    @Override public void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random) { if (state.getValue(POWERED)) updateSignal(level,pos,state); }
    private void updateSignal(Level level,BlockPos pos,BlockState state) {
        if (!(level.getBlockEntity(pos) instanceof OwnedDeviceBlockEntity entity)) return;
        boolean powered=false;
        for (var candidate:level.getEntities((Entity)null,new AABB(pos.getX()+.125,pos.getY(),pos.getZ()+.125,pos.getX()+.875,pos.getY()+.25,pos.getZ()+.875))) {
            String name=candidate instanceof Player player ? player.getGameProfile().getName() : null;
            if (entity.access().triggersPlate(state.getValue(MODE),name,candidate.isIgnoringBlockTriggers())) { powered=true; break; }
        }
        if (powered!=state.getValue(POWERED)) {
            level.setBlock(pos,state.setValue(POWERED,powered),2); notifySignal(level,pos);
            level.playSound(null,pos,net.minecraft.sounds.SoundEvents.STONE_BUTTON_CLICK_ON,net.minecraft.sounds.SoundSource.BLOCKS,.2f,powered ? .6f : .5f);
        }
        if (powered) level.scheduleTick(pos,this,20);
    }
    private void notifySignal(Level level,BlockPos pos) { level.updateNeighborsAt(pos,this); level.updateNeighborsAt(pos.below(),this); }
    @Override public boolean isSignalSource(BlockState state) { return true; }
    @Override public int getSignal(BlockState state,BlockGetter level,BlockPos pos,Direction face) { return state.getValue(POWERED) ? 15 : 0; }
    @Override public int getDirectSignal(BlockState state,BlockGetter level,BlockPos pos,Direction face) { return face==Direction.UP && state.getValue(POWERED) ? 15 : 0; }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving) {
        if (!state.is(next.getBlock()) && state.getValue(POWERED)) notifySignal(level,pos);
        super.onRemove(state,level,pos,next,moving);
    }
    @Override public boolean canEntityDestroy(BlockState state,BlockGetter level,BlockPos pos,Entity entity) { return false; }
    @Override public boolean dropFromExplosion(net.minecraft.world.level.Explosion explosion) { return false; }
    @Override public void onBlockExploded(BlockState state,Level level,BlockPos pos,net.minecraft.world.level.Explosion explosion) {}

}
