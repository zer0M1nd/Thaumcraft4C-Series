package emtmodern.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import tc4revival.content.AuraNodeBlock;

/** Author's BlockAiry-derived node: placement rolls normal aura; quality and type are fixed by its tile. */
public class PortableNodeBlock extends AuraNodeBlock {
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new PortableNodeEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return createTickerHelper(type,emtmodern.ElectroMagicTools.PORTABLE_NODE_BE.get(),(world,pos,block,node)->node.tick());}
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity placer,ItemStack stack){
        if(level instanceof net.minecraft.server.level.ServerLevel server)tc4revival.content.AuraNodeGeneration.createRandom(server,pos,server.random,false,false,false);
    }
    @Override public boolean canEntityDestroy(BlockState state,BlockGetter level,BlockPos pos,Entity entity){return false;}
    @Override public void onBlockExploded(BlockState state,Level level,BlockPos pos,Explosion explosion){}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!level.isClientSide&&player.isShiftKeyDown()&&player.getMainHandItem().isEmpty())level.removeBlock(pos,false);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
