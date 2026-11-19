package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public  class ThaumatoriumBlock extends BaseEntityBlock {
    public static final BooleanProperty UPPER=BooleanProperty.create("upper");
    public static final DirectionProperty FACING=BlockStateProperties.FACING;
    private static boolean forming;
    public ThaumatoriumBlock() {
        super(Properties.of().strength(3,17).sound(net.minecraft.world.level.block.SoundType.METAL).noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(UPPER,false).setValue(FACING,net.minecraft.core.Direction.NORTH));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(UPPER,FACING); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {
        return state.getValue(UPPER) ? box(0,-16,0,16,16,16) : box(0,0,0,16,32,16);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new ThaumatoriumBlockEntity(pos,state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
        return createTickerHelper(type,TCArcane.THAUMATORIUM_ENTITY.get(),(world,pos,block,entity)->entity.tick());
    }
    public static InteractionResult form(ItemStack stack,UseOnContext context) {
        var level=context.getLevel(); var pos=context.getClickedPos();
        if (!level.getBlockState(pos).is(TCArcane.ALCHEMICAL_CONSTRUCT.get()) || !(context.getPlayer() instanceof net.minecraft.server.level.ServerPlayer player)) return InteractionResult.PASS;
        if (!tc4revival.research.KnowledgeSavedData.get(player.getServer()).snapshot(player.getUUID())
                .map(knowledge->knowledge.researchMarkers().contains("THAUMATORIUM")).orElse(false)) return InteractionResult.PASS;
        if (!matches(level,pos)) { pos=pos.below(); if (!matches(level,pos)) return InteractionResult.PASS; }
        var lower=pos; var upper=pos.above();
        for (var target:java.util.List.of(lower,upper,lower.below()))
            if (!level.mayInteract(player,target) || !player.mayUseItemAt(target,context.getClickedFace(),stack)) return InteractionResult.FAIL;
        var cost=java.util.Map.of(tc4revival.aspect.TCAspects.catalog().require("ignis"),15,
                tc4revival.aspect.TCAspects.catalog().require("ordo"),30,tc4revival.aspect.TCAspects.catalog().require("aqua"),30);
        var wand=(CastingWandItem)stack.getItem(); if (!thaumcraft.api.wands.MagicOperationRules.payStructure("thaumcraft:alchemical_construct",wand,stack,player,cost,true,true,false)) return InteractionResult.PASS;
        var bottomState=level.getBlockState(lower); var topState=level.getBlockState(upper);
        var formed=TCArcane.THAUMATORIUM.get().defaultBlockState().setValue(FACING,context.getClickedFace());
        boolean committed=false; forming=true;
        try {
            if (!level.setBlock(lower,formed,18) || !level.getBlockState(lower).equals(formed)
                    || !(level.getBlockEntity(lower) instanceof ThaumatoriumBlockEntity core)) return InteractionResult.FAIL;
            var formedTop=formed.setValue(UPPER,true);
            if (!level.setBlock(upper,formedTop,18) || !level.getBlockState(upper).equals(formedTop)
                    || !(level.getBlockEntity(upper) instanceof ThaumatoriumBlockEntity top)
                    || !level.getBlockState(lower).equals(formed) || level.getBlockEntity(lower)!=core
                    || top.controller()!=core || !level.getBlockState(lower.below()).is(TCArcane.CRUCIBLE.get())) return InteractionResult.FAIL;
            if (!thaumcraft.api.wands.MagicOperationRules.payStructure("thaumcraft:alchemical_construct",wand,stack,player,cost,true,true,true)) return InteractionResult.FAIL;
            committed=true;
        } finally {
            try {
                if (!committed) {
                    level.setBlock(lower,bottomState,18); level.setBlock(upper,topState,18);
                    if (!level.getBlockState(lower).equals(bottomState) || !level.getBlockState(upper).equals(topState))
                        throw new IllegalStateException("Cannot restore thaumatorium at "+lower);
                }
            } finally { forming=false; }
        }
        level.updateNeighborsAt(lower,TCArcane.THAUMATORIUM.get()); level.updateNeighborsAt(upper,TCArcane.THAUMATORIUM.get());
        tc4revival.network.FocusEffectPacket.blockSparkle(player.serverLevel(),lower,-9999);
        tc4revival.network.FocusEffectPacket.blockSparkle(player.serverLevel(),upper,-9999);
        level.playSound(null,lower,TCSounds.WAND.get(),net.minecraft.sounds.SoundSource.BLOCKS,1,1); return InteractionResult.SUCCESS;
    }
    private static boolean matches(Level level,BlockPos pos) {
        return !level.isOutsideBuildHeight(pos.above()) && level.hasChunkAt(pos) && level.hasChunkAt(pos.below())
                && level.getBlockState(pos).is(TCArcane.ALCHEMICAL_CONSTRUCT.get()) && level.getBlockState(pos.above()).is(TCArcane.ALCHEMICAL_CONSTRUCT.get())
                && level.getBlockState(pos.below()).is(TCArcane.CRUCIBLE.get());
    }
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block from,BlockPos fromPos,boolean moving) {
        if (level.isClientSide || forming) return;
        boolean valid=state.getValue(UPPER) ? level.getBlockState(pos.below()).is(this) && !level.getBlockState(pos.below()).getValue(UPPER)
                : level.getBlockState(pos.above()).is(this) && level.getBlockState(pos.above()).getValue(UPPER) && level.getBlockState(pos.below()).is(TCArcane.CRUCIBLE.get());
        if (!valid) level.setBlock(pos,TCArcane.ALCHEMICAL_CONSTRUCT.get().defaultBlockState(),3);
        else if (level.getBlockEntity(pos) instanceof ThaumatoriumBlockEntity entity && entity.controller()!=null) {
            var core=entity.controller(); core.process().refreshLimit(core);
        }
    }
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (level.getBlockEntity(pos) instanceof ThaumatoriumBlockEntity entity && entity.controller()!=null) {
            if (player instanceof net.minecraft.server.level.ServerPlayer server) net.minecraftforge.network.NetworkHooks.openScreen(server,entity.controller(),entity.controller().getBlockPos());
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving) {
        if (!state.is(next.getBlock()) && !forming && !level.isClientSide && level.getBlockEntity(pos) instanceof ThaumatoriumBlockEntity entity)
            net.minecraft.world.Containers.dropContents(level,pos,entity);
        super.onRemove(state,level,pos,next,moving);
    }

}
