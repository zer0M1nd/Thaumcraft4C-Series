package automagy.content;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class GolemInhibitorBlock extends BaseEntityBlock {
    public GolemInhibitorBlock(){super(Properties.of().strength(1.5f).noOcclusion());}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new GolemInhibitorBlockEntity(pos,state);}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?null:createTickerHelper(type,Content.INHIBITOR_TILE.get(),GolemInhibitorBlockEntity::tick);}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!(player.getItemInHand(hand).getItem() instanceof tc4revival.content.CastingWandItem))return InteractionResult.PASS;
        if(!level.isClientSide&&player.mayBuild()&&level.getBlockEntity(pos) instanceof GolemInhibitorBlockEntity inhibitor){inhibitor.toggle();player.displayClientMessage(Component.translatable(inhibitor.omitLinked?"message.automagy.inhibitor.exempt":"message.automagy.inhibitor.only_linked"),true);}
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
