package automagy.content;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
/** Controller and passive tower segments have separate modern block identities. */
public final class ThaumostaticBlock extends BaseEntityBlock implements thaumcraft.api.wands.IWandable {
    private final boolean controller;
    public ThaumostaticBlock(boolean controller){super(Properties.of().strength(5,10).sound(SoundType.METAL).requiresCorrectToolForDrops());this.controller=controller;}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return controller?new ThaumostaticControllerBlockEntity(pos,state):new ThaumostaticPylonBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide||!controller?null:createTickerHelper(type,Content.THAUMOSTATIC_CONTROLLER_TILE.get(),ThaumostaticControllerBlockEntity::tick);}
    @Override public net.minecraft.world.InteractionResult onWandUseFirst(net.minecraft.world.item.ItemStack wand,net.minecraft.world.item.context.UseOnContext context){
        var player=context.getPlayer();if(!controller||player==null||!player.mayBuild()||context.getClickedFace().getAxis().isVertical())return net.minecraft.world.InteractionResult.PASS;
        if(player instanceof net.minecraft.server.level.ServerPlayer server&&context.getLevel().getBlockEntity(context.getClickedPos()) instanceof ThaumostaticControllerBlockEntity tile){
            if(tile.active()){
                tile.switchSuction(context.getClickedFace());
                server.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.automagy.tower_suction",tile.suctionType(context.getClickedFace()).displayName()),true);
                context.getLevel().playSound(null,context.getClickedPos(),net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,net.minecraft.sounds.SoundSource.BLOCKS,.5f,.9f+context.getLevel().random.nextFloat()*.2f);
            }else tile.activate(server,wand);
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block neighbor,BlockPos source,boolean moving){
        if(level.isClientSide)return;var tile=level.getBlockEntity(pos);
        if(tile instanceof ThaumostaticControllerBlockEntity owner)owner.requireValidation();
        else if(tile instanceof ThaumostaticPylonBlockEntity pylon)pylon.requireValidation();
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock())&&!level.isClientSide&&level.getBlockEntity(pos) instanceof ThaumostaticControllerBlockEntity tile)tile.deactivate();
        if(!state.is(next.getBlock())&&!level.isClientSide&&level.getBlockEntity(pos) instanceof ThaumostaticPylonBlockEntity pylon)pylon.requireValidation();
        super.onRemove(state,level,pos,next,moving);
    }
}
