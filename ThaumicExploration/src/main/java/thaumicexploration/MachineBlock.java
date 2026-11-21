package thaumicexploration;

import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.fluids.FluidUtil;
import thaumcraft.api.wands.IWandable;

public class MachineBlock extends BaseEntityBlock implements IWandable {
    public final String kind;
    public MachineBlock(String kind) { super(Properties.copy(Blocks.STONE).strength(kind.equals("replicator")?4:2).noOcclusion().pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)); this.kind=kind; }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {
        return switch(kind) { case "replicator","think_tank","crucible_souls" -> box(0,0,0,16,16,16); case "trash_jar" -> box(3,0,3,13,12,13); default -> box(3,0,3,13,16,13); };
    }
    @Override public InteractionResult onWandUseFirst(ItemStack wand,net.minecraft.world.item.context.UseOnContext context){
        if(!kind.equals("replicator")&&!kind.equals("soul_brazier"))return InteractionResult.PASS;
        if(!context.getLevel().isClientSide&&context.getLevel().getBlockEntity(context.getClickedPos()) instanceof MachineEntity machine){
            if(kind.equals("replicator"))machine.startReplication();else if(context.getPlayer()!=null)machine.activate(context.getPlayer());
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new MachineEntity(pos,state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
        return createTickerHelper(type,ExplorationMod.MACHINE.get(),(l,p,s,be)->be.tick());
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity placer,ItemStack stack) {
        if(!level.isClientSide&&level.getBlockEntity(pos) instanceof MachineEntity machine&&placer instanceof Player){machine.owner=placer.getUUID();machine.setChanged();}
    }
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        if(!(level.getBlockEntity(pos) instanceof MachineEntity machine))return InteractionResult.PASS;
        if(kind.endsWith("urn"))return FluidUtil.interactWithFluidHandler(player,hand,level,pos,Direction.UP)?InteractionResult.SUCCESS:InteractionResult.PASS;
        if(kind.equals("think_tank")) {
            if(player instanceof net.minecraft.server.level.ServerPlayer server)net.minecraftforge.network.NetworkHooks.openScreen(server,machine,pos);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if(kind.equals("replicator")) {
            if(level.isClientSide)return InteractionResult.SUCCESS;
            var held=player.getItemInHand(hand);
            if(held.getItem() instanceof tc4revival.content.CastingWandItem){machine.startReplication();return InteractionResult.SUCCESS;}
            if(held.isEmpty()) {
                var output=machine.items.extractItem(1,64,false);if(!output.isEmpty()&&!player.addItem(output))player.drop(output,false);
                if(player.isShiftKeyDown()&&!machine.crafting){machine.template=ItemStack.EMPTY;machine.sync();}
            } else if(!machine.crafting&&machine.items.getStackInSlot(1).isEmpty()&&MachineEntity.canReplicate(held)) {
                machine.template=held.copyWithCount(1); machine.sync();
            }
            return InteractionResult.SUCCESS;
        }
        if(kind.equals("soul_brazier")) { if(!level.isClientSide)machine.activate(player); return InteractionResult.sidedSuccess(level.isClientSide); }
        return InteractionResult.PASS;
    }
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block block,BlockPos from,boolean moving) {
        if(!level.isClientSide&&kind.equals("replicator")&&level.getBlockEntity(pos) instanceof MachineEntity machine) {
            boolean signal=level.hasNeighborSignal(pos); if(signal&&!machine.powered)machine.startReplication();machine.powered=signal;machine.setChanged();
        }
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState replacement,boolean moving) {
        if(!state.is(replacement.getBlock())&&!level.isClientSide&&level.getBlockEntity(pos) instanceof MachineEntity machine) {
            machine.deactivate(); for(int i=0;i<machine.items.getSlots();i++)popResource(level,pos,machine.items.extractItem(i,64,false));
        }
        super.onRemove(state,level,pos,replacement,moving);
    }
}
