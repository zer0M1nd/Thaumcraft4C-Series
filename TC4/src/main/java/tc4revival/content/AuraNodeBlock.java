package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public  class AuraNodeBlock extends BaseEntityBlock implements net.minecraft.world.level.block.SimpleWaterloggedBlock {
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty WATERLOGGED=net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED;
    public AuraNodeBlock() {
        super(Properties.of().strength(2, 200).noCollission().noOcclusion().lightLevel(state -> 8).noLootTable());
        registerDefaultState(stateDefinition.any().setValue(WATERLOGGED,false));
    }
    @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block,BlockState> builder) { builder.add(WATERLOGGED); }
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return defaultBlockState().setValue(WATERLOGGED,context.getLevel().getFluidState(context.getClickedPos()).getType()==net.minecraft.world.level.material.Fluids.WATER);
    }
    @Override public net.minecraft.world.level.material.FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED)?net.minecraft.world.level.material.Fluids.WATER.getSource(false):super.getFluidState(state);
    }
    @Override public BlockState updateShape(BlockState state,net.minecraft.core.Direction side,BlockState neighbor,net.minecraft.world.level.LevelAccessor level,BlockPos pos,BlockPos other) {
        if(state.getValue(WATERLOGGED))level.scheduleTick(pos,net.minecraft.world.level.material.Fluids.WATER,net.minecraft.world.level.material.Fluids.WATER.getTickDelay(level));
        return super.updateShape(state,side,neighbor,level,pos,other);
    }
    @Override public boolean canBeReplaced(BlockState state,net.minecraft.world.level.material.Fluid fluid) { return false; }
    @Override public net.minecraft.world.level.block.SoundType getSoundType(BlockState state,net.minecraft.world.level.LevelReader world,BlockPos pos,net.minecraft.world.entity.Entity entity) {
        return new net.minecraft.world.level.block.SoundType(1,1,TCSounds.CRAFT_FAIL.get(),net.minecraft.sounds.SoundEvents.EMPTY,net.minecraft.sounds.SoundEvents.EMPTY,net.minecraft.sounds.SoundEvents.EMPTY,net.minecraft.sounds.SoundEvents.EMPTY);
    }
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientBlockExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientBlockExtensions() {
            @Override public boolean addHitEffects(BlockState state,Level level,net.minecraft.world.phys.HitResult hit,net.minecraft.client.particle.ParticleEngine engine) {
                if(hit instanceof net.minecraft.world.phys.BlockHitResult target && level.random.nextBoolean()) tc4revival.client.ClientFocusEffects.infusedOreHit(level,target.getBlockPos(),ShardType.AIR);
                return true;
            }
            @Override public boolean addDestroyEffects(BlockState state,Level level,BlockPos pos,net.minecraft.client.particle.ParticleEngine engine) { return true; }
        });
    }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return box(4.8, 4.8, 4.8, 11.2, 11.2, 11.2); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new AuraNodeBlockEntity(pos, state); }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,net.minecraft.world.entity.LivingEntity placer,net.minecraft.world.item.ItemStack stack) {
        if(level instanceof net.minecraft.server.level.ServerLevel server && placer instanceof net.minecraft.world.entity.player.Player
                && stack.is(TCItems.AURA_NODE.get())) {
            if (!(level.getBlockEntity(pos) instanceof AuraNodeBlockEntity node) || !CreativeAuraNodeItem.restoreNode(stack,node))
                AuraNodeGeneration.createRandom(server,pos,server.random,false,false,false);
        }
        super.setPlacedBy(level,pos,state,placer,stack);
    }
    @Override public void playerWillDestroy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.player.Player player) {
        dropEssence(level,pos);
        if (!level.isClientSide) tc4revival.network.FocusEffectPacket.burst(level,pos.getCenter(),1);
        super.playerWillDestroy(level, pos, state, player);
    }
    public static void dropEssence(Level level,BlockPos pos) {
        if (!level.isClientSide)for(var stack:essenceDrops(level.getBlockEntity(pos)))popResource(level,pos,stack);
    }
    public static java.util.List<net.minecraft.world.item.ItemStack> essenceDrops(BlockEntity entity) {
        var drops=new java.util.ArrayList<net.minecraft.world.item.ItemStack>();
        if(entity instanceof AuraNodeBlockEntity node)for(var aspect:node.aspects().aspects()) {
            int amount=node.aspects().amount(aspect);if(amount<5)continue;
            for(int i=0;i<=amount/10;i++)drops.add(TCItems.WISP_ESSENCE.get().of(aspect));
        }
        return drops;
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, TCArcane.AURA_NODE_ENTITY.get(), (world, pos, block, node) -> node.tick());
    }
}
