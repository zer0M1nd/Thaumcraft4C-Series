package tc4revival.content;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
public  class ArcaneSpaBlock extends BaseEntityBlock {
    public ArcaneSpaBlock() { super(Properties.of().strength(3,25).sound(SoundType.STONE).noOcclusion()); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new ArcaneSpaBlockEntity(pos,state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) { return level.isClientSide?null:createTickerHelper(type,TCArcane.ARCANE_SPA_ENTITY.get(),(world,pos,current,spa)->spa.tick()); }
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        if(player.isShiftKeyDown()||!(level.getBlockEntity(pos) instanceof ArcaneSpaBlockEntity spa))return InteractionResult.PASS;
        var held=player.getItemInHand(hand);var offered=FluidStack.EMPTY;ItemStack empty=ItemStack.EMPTY;
        if(held.is(Items.POTION)&&PotionUtils.getPotion(held)==Potions.WATER) { offered=new FluidStack(net.minecraft.world.level.material.Fluids.WATER,1000);empty=new ItemStack(Items.GLASS_BOTTLE); }
        else {
            var handler=FluidUtil.getFluidHandler(held.copyWithCount(1)).resolve();
            if(handler.isPresent()) { offered=handler.get().drain(Integer.MAX_VALUE,FluidAction.SIMULATE);if(!offered.isEmpty()&&!level.isClientSide&&spa.tank().fill(offered,FluidAction.SIMULATE)>0) { offered=handler.get().drain(Integer.MAX_VALUE,FluidAction.EXECUTE);empty=handler.get().getContainer(); } }
        }
        if(!offered.isEmpty()) {
            if(!level.isClientSide&&spa.tank().fill(offered,FluidAction.EXECUTE)>0) {
                held.shrink(1);if(!empty.isEmpty()&&!player.getInventory().add(empty))player.drop(empty,false);
                player.containerMenu.broadcastChanges();level.playSound(null,pos,net.minecraft.sounds.SoundEvents.GENERIC_SWIM,net.minecraft.sounds.SoundSource.BLOCKS,.33f,1+(level.random.nextFloat()-level.random.nextFloat())*.3f);
            }
        } else if(player instanceof ServerPlayer server) net.minecraftforge.network.NetworkHooks.openScreen(server,spa,pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving) {
        if(!next.is(this)&&!level.isClientSide&&level.getBlockEntity(pos) instanceof ArcaneSpaBlockEntity spa) Containers.dropContents(level,pos,spa);
        super.onRemove(state,level,pos,next,moving);
    }
}
