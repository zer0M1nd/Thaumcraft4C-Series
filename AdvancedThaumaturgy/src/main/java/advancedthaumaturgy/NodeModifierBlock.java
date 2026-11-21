package advancedthaumaturgy;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
public class NodeModifierBlock extends BaseEntityBlock {
    public NodeModifierBlock(){super(Properties.of().strength(1).noOcclusion().requiresCorrectToolForDrops());}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new NodeModifierEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?null:createTickerHelper(type,AdvancedThaumaturgy.NODE_ENTITY.get(),(l,p,s,e)->e.tick());}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(player.getItemInHand(hand).is(tc4revival.content.TCItems.NODE_JAR.get())&&level.isEmptyBlock(pos.above()))return InteractionResult.PASS;
        if(!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,hit.getDirection(),player.getItemInHand(hand)))return InteractionResult.FAIL;
        if(player instanceof net.minecraft.server.level.ServerPlayer server&&level.getBlockEntity(pos) instanceof NodeModifierEntity entity)NetworkHooks.openScreen(server,entity,pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
