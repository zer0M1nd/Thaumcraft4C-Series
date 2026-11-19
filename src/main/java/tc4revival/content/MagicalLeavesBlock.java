package tc4revival.content;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Old four-edge leaf support and separate placed/pending-decay bits. */
public  class MagicalLeavesBlock extends Block implements net.minecraftforge.common.IForgeShearable, net.minecraft.world.level.block.SimpleWaterloggedBlock {
    public static final BooleanProperty PERSISTENT=BlockStateProperties.PERSISTENT;
    public static final BooleanProperty CHECK_DECAY=BooleanProperty.create("check_decay");
    public static final BooleanProperty WATERLOGGED=BlockStateProperties.WATERLOGGED;
    private final boolean silver;
    public MagicalLeavesBlock(boolean silver) {
        super(Properties.of().strength(.2f).sound(SoundType.GRASS).noOcclusion().randomTicks().lightLevel(state->silver ? 7 : 0)
                .isRedstoneConductor((state,level,pos)->false).isSuffocating((state,level,pos)->false).isViewBlocking((state,level,pos)->false));
        this.silver=silver; registerDefaultState(stateDefinition.any().setValue(PERSISTENT,false).setValue(CHECK_DECAY,false).setValue(WATERLOGGED,false));
    }
    public boolean silver() { return silver; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(PERSISTENT,CHECK_DECAY,WATERLOGGED); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(PERSISTENT,true)
            .setValue(WATERLOGGED,context.getLevel().getFluidState(context.getClickedPos()).getType()==net.minecraft.world.level.material.Fluids.WATER); }
    @Override public net.minecraft.world.level.material.FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED)?net.minecraft.world.level.material.Fluids.WATER.getSource(false):super.getFluidState(state);
    }
    @Override public BlockState updateShape(BlockState state,Direction side,BlockState neighbor,LevelAccessor level,BlockPos pos,BlockPos neighborPos) {
        if(state.getValue(WATERLOGGED))level.scheduleTick(pos,net.minecraft.world.level.material.Fluids.WATER,net.minecraft.world.level.material.Fluids.WATER.getTickDelay(level));
        return super.updateShape(state,side,neighbor,level,pos,neighborPos);
    }
    @Override public int getLightBlock(BlockState state,BlockGetter level,BlockPos pos) { return 1; }
    @Override public int getFlammability(BlockState state,BlockGetter level,BlockPos pos,Direction face) { return 60; }
    @Override public int getFireSpreadSpeed(BlockState state,BlockGetter level,BlockPos pos,Direction face) { return 30; }
    @Override public boolean isRandomlyTicking(BlockState state) { return state.getValue(CHECK_DECAY) && !state.getValue(PERSISTENT); }
    @Override public void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random) {
        if (!isRandomlyTicking(state) || !level.hasChunksAt(pos.offset(-5,-5,-5),pos.offset(5,5,5))) return;
        if (supported(level,pos)) level.setBlock(pos,state.setValue(CHECK_DECAY,false),3);
        else level.destroyBlock(pos,true);
    }
    public static boolean supported(BlockGetter level,BlockPos origin) {
        record Step(BlockPos pos,int distance) {}
        var todo=new ArrayDeque<Step>(); var seen=new HashSet<BlockPos>();
        todo.add(new Step(origin,0)); seen.add(origin);
        while (!todo.isEmpty()) {
            var current=todo.removeFirst();
            if (current.distance()==4) continue;
            for (var side:Direction.values()) {
                var pos=current.pos().relative(side); if (!seen.add(pos)) continue;
                var state=WardedBlock.queryOriginal(level,pos);
                if (state.is(BlockTags.LOGS)) return true;
                if (state.is(BlockTags.LEAVES)) todo.addLast(new Step(pos,current.distance()+1));
            }
        }
        return false;
    }
    public static void markDecay(Level level,BlockPos origin,int radius) {
        if (level.isClientSide || !level.hasChunksAt(origin.offset(-radius-1,-radius-1,-radius-1),origin.offset(radius+1,radius+1,radius+1))) return;
        for (var pos:BlockPos.betweenClosed(origin.offset(-radius,-radius,-radius),origin.offset(radius,radius,radius))) {
            var state=level.getBlockState(pos);
            if (state.getBlock() instanceof MagicalLeavesBlock && !state.getValue(CHECK_DECAY)) level.setBlock(pos,state.setValue(CHECK_DECAY,true),2);
        }
    }
    public static void removed(BlockState old,Level level,BlockPos pos,BlockState next) {
        if (level.isClientSide || old.getBlock()==next.getBlock()) return;
        var original=WardedBlock.unwrap(old,level,pos);
        if (original.is(BlockTags.LOGS)) markDecay(level,pos,4);
        else if (original.is(BlockTags.LEAVES)) markDecay(level,pos,1);
    }
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random) {
        if (level.isRainingAt(pos.above()) && !level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP) && random.nextInt(15)==1)
            level.addParticle(net.minecraft.core.particles.ParticleTypes.DRIPPING_WATER,pos.getX()+random.nextFloat(),pos.getY()-.05,pos.getZ()+random.nextFloat(),0,0,0);
        if (silver && random.nextInt(500)==0) tc4revival.client.ClientFocusEffects.silverwoodSparkle(level,pos);
    }
    @Override public List<ItemStack> onSheared(net.minecraft.world.entity.player.Player player,ItemStack tool,Level level,BlockPos pos,int fortune) { return List.of(new ItemStack(this)); }
}
