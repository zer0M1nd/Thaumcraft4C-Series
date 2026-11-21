package automagy.content;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class GolemTaskmasterBlock extends BaseEntityBlock {
    public GolemTaskmasterBlock(){super(Properties.of().strength(1.5f).noOcclusion());}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new GolemTaskmasterBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level,BlockState state,net.minecraft.world.level.block.entity.BlockEntityType<T> type){return level.isClientSide?null:createTickerHelper(type,Content.TASKMASTER_TILE.get(),GolemTaskmasterBlockEntity::tick);}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!(player.getItemInHand(hand).getItem() instanceof tc4revival.content.CastingWandItem))return InteractionResult.PASS;
        if(!level.isClientSide&&player.mayBuild()&&level.getBlockEntity(pos) instanceof GolemTaskmasterBlockEntity master){
            boolean linked=master.linkedCore()!=null||InventariumBlockEntity.nearest(level,pos,true).map(core->core.linkMemory(pos)).orElse(false);
            player.displayClientMessage(Component.translatable(linked?"message.automagy.scribe.linked":"message.automagy.scribe.no_core"),true);
        }return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock())&&level.getBlockEntity(pos) instanceof GolemTaskmasterBlockEntity master&&master.linkedCore()!=null&&level.hasChunkAt(master.linkedCore())&&level.getBlockEntity(master.linkedCore()) instanceof InventariumBlockEntity core)core.unlinkMemory(pos);
        super.onRemove(state,level,pos,next,moving);
    }
}
