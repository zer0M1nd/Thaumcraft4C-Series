package gadomancymodern.claw;
import gadomancymodern.Gadomancy;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
public class InfusionClawBlock extends BaseEntityBlock {
    public InfusionClawBlock(){super(Properties.of().strength(3.5f).noOcclusion());}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new InfusionClawEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return createTickerHelper(type,Gadomancy.CLAW_BE.get(),InfusionClawEntity::tick);}
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block block,BlockPos from,boolean moving){if(!level.isClientSide&&level.getBlockEntity(pos) instanceof InfusionClawEntity claw)claw.powerChanged(level.hasNeighborSignal(pos));}
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,net.minecraft.world.entity.LivingEntity entity,net.minecraft.world.item.ItemStack stack){if(entity instanceof net.minecraft.world.entity.player.Player player&&level.getBlockEntity(pos) instanceof InfusionClawEntity claw)claw.claim(player);}
    @Override public net.minecraft.world.InteractionResult use(BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit){if(level.getBlockEntity(pos) instanceof InfusionClawEntity claw&&!level.isClientSide){if(!claw.hasOwner()){if(claw.claim(player))player.displayClientMessage(net.minecraft.network.chat.Component.translatable("gadomancy.info.InfusionClaw.owner"),true);}else if(player instanceof net.minecraft.server.level.ServerPlayer server)net.minecraftforge.network.NetworkHooks.openScreen(server,claw,pos);}return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);}
    @Override public boolean hasAnalogOutputSignal(BlockState state){return true;}
    @Override public int getAnalogOutputSignal(BlockState state,Level level,BlockPos pos){return level.getBlockEntity(pos) instanceof InfusionClawEntity claw?claw.comparator():0;}
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){if(!state.is(next.getBlock())&&level.getBlockEntity(pos) instanceof InfusionClawEntity claw){net.minecraft.world.Containers.dropItemStack(level,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,claw.getItem(0));claw.clearContent();}super.onRemove(state,level,pos,next,moving);}
    @Override public boolean triggerEvent(BlockState state,Level level,BlockPos pos,int id,int value){super.triggerEvent(state,level,pos,id,value);return level.getBlockEntity(pos) instanceof InfusionClawEntity claw&&claw.triggerEvent(id,value);}
}
