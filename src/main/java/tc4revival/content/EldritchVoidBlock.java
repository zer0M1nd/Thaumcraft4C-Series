package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Damaging shell; only exposed cells allocate a renderer block entity. */
public  class EldritchVoidBlock extends Block implements net.minecraft.world.level.block.EntityBlock {
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty EXPOSED=net.minecraft.world.level.block.state.properties.BooleanProperty.create("exposed");
    private static final VoxelShape COLLISION=Block.box(2,2,2,14,14,14);
    public EldritchVoidBlock() {
        super(Properties.of().strength(-1,6000000).sound(SoundType.WOOL).lightLevel(state->3).noLootTable());
        registerDefaultState(stateDefinition.any().setValue(EXPOSED,false));
    }
    @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block,BlockState> builder) { builder.add(EXPOSED); }
    @Override public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos,BlockState state) {
        return state.getValue(EXPOSED) ? new EldritchVoidBlockEntity(pos,state) : null;
    }
    private static boolean exposed(BlockGetter world,BlockPos pos) {
        for (var side:net.minecraft.core.Direction.values()) {
            BlockPos neighbor=pos.relative(side);
            if (!world.getBlockState(neighbor).isSolidRender(world,neighbor)) return true;
        }
        return false;
    }
    @Override public BlockState updateShape(BlockState state,net.minecraft.core.Direction side,BlockState neighbor,net.minecraft.world.level.LevelAccessor world,BlockPos pos,BlockPos neighborPos) {
        return state.setValue(EXPOSED,exposed(world,pos));
    }
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block source,BlockPos from,boolean moving) {
        boolean visible=exposed(level,pos);
        if (visible!=state.getValue(EXPOSED)) level.setBlock(pos,state.setValue(EXPOSED,visible),3);
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState replacement,boolean moving) {
        if (state.getValue(EXPOSED) && (!replacement.is(this) || !replacement.getValue(EXPOSED))) level.removeBlockEntity(pos);
        super.onRemove(state,level,pos,replacement,moving);
    }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientBlockExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientBlockExtensions() {
            // Creative picking supplies a player context; vanilla hit particles
            // query the same outline without one and would call bounds() on empty.
            @Override public boolean addHitEffects(BlockState state,Level level,net.minecraft.world.phys.HitResult hit,net.minecraft.client.particle.ParticleEngine engine) { return true; }
            @Override public boolean addDestroyEffects(BlockState state,Level level,BlockPos pos,net.minecraft.client.particle.ParticleEngine engine) { return true; }
        });
    }
    @Override public VoxelShape getShape(BlockState state,BlockGetter world,BlockPos pos,CollisionContext context) {
        return context instanceof net.minecraft.world.phys.shapes.EntityCollisionContext entity
                && entity.getEntity() instanceof Player player && player.isCreative() ? Shapes.block() : Shapes.empty();
    }
    @Override public VoxelShape getOcclusionShape(BlockState state,BlockGetter world,BlockPos pos) { return Shapes.block(); }
    @Override public VoxelShape getCollisionShape(BlockState state,BlockGetter world,BlockPos pos,CollisionContext context) { return COLLISION; }
    @Override public ItemStack getCloneItemStack(BlockGetter world,BlockPos pos,BlockState state) { return ItemStack.EMPTY; }
    @Override public void entityInside(BlockState state,Level level,BlockPos pos,Entity entity) {
        if (!level.isClientSide && entity.tickCount>20 && !(entity instanceof Player player && player.isCreative()))
            entity.hurt(level.damageSources().fellOutOfWorld(),8);
    }
}
