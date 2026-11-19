package thaumicfutures.devices;

import java.util.Optional;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import tc4revival.content.WandInteractable;

public class PatternCrafterBlock extends BaseEntityBlock implements WandInteractable {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public PatternCrafterBlock() { super(Properties.copy(Blocks.IRON_BLOCK).strength(2).noOcclusion());registerDefaultState(defaultBlockState().setValue(FACING,Direction.SOUTH)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite()); }
    public static boolean knob(BlockState state,BlockPos pos,BlockHitResult hit) {
        var local=hit.getLocation().subtract(Vec3.atLowerCornerOf(pos));
        if(local.y<.125-1e-4 || local.y>.375+1e-4)return false;
        var face=state.getValue(FACING);
        return switch(face) {
            case SOUTH->local.x>=.375 && local.x<=.625 && local.z>=.749;
            case NORTH->local.x>=.375 && local.x<=.625 && local.z<=.251;
            case EAST->local.z>=.375 && local.z<=.625 && local.x>=.749;
            case WEST->local.z>=.375 && local.z<=.625 && local.x<=.251;
            default->false;
        };
    }
    private InteractionResult interact(Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit,boolean wand) {
        if(player==null || !player.mayBuild() || !(level.getBlockEntity(pos) instanceof PatternCrafter tile))return InteractionResult.PASS;
        boolean knob=knob(level.getBlockState(pos),pos,hit);
        if(!knob && !wand)return InteractionResult.PASS;
        if(!level.isClientSide) {
            if(knob)tile.cyclePreset(player);else tile.setNext(player,hand);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) { return interact(level,pos,player,hand,hit,player.getItemInHand(hand).getItem() instanceof tc4revival.content.CastingWandItem); }
    @Override public InteractionResult onWandUseFirst(ItemStack wand,UseOnContext context) {
        return interact(context.getLevel(),context.getClickedPos(),context.getPlayer(),context.getHand(),new BlockHitResult(context.getClickLocation(),context.getClickedFace(),context.getClickedPos(),context.isInside()),true);
    }
    @Override public Optional<ItemStack> onWandUse(Level level,ItemStack wand,Player player) { return Optional.empty(); }
    @Override public void onWandUseTick(ItemStack wand,Player player,int remaining) {}
    @Override public void onWandRelease(ItemStack wand,Level level,Player player,int remaining) {}
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving) {
        if(!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof PatternCrafter tile)tile.dropPending();
        super.onRemove(state,level,pos,next,moving);
    }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new PatternCrafter(pos,state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) { return createTickerHelper(type,Devices.CRAFTER_TILE.get(),(l,p,s,t)->t.tick()); }
}
