package ttmodern;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class AspectAnalyzerEntity extends BlockEntity {
    public final net.minecraftforge.items.ItemStackHandler items=new net.minecraftforge.items.ItemStackHandler(1){@Override protected void onContentsChanged(int slot){setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}};
    private net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler> capability=net.minecraftforge.common.util.LazyOptional.of(()->items);
    public AspectAnalyzerEntity(BlockPos pos,BlockState state){super(Tinkerer.ASPECT_ANALYZER_ENTITY.get(),pos,state);}
    public java.util.Map<String,Integer> aspects(){var amounts=thaumcraft.api.ThaumcraftApiHelper.getObjectAspects(items.getStackInSlot(0));var result=new java.util.TreeMap<String,Integer>();for(var aspect:amounts.getAspects())result.put(aspect.getTag(),amounts.getAmount(aspect));return java.util.Collections.unmodifiableMap(result);}
    public boolean usable(net.minecraft.world.entity.player.Player player){return !isRemoved()&&player.level()==level&&level.getBlockEntity(worldPosition)==this&&player.distanceToSqr(worldPosition.getCenter())<=64&&level.mayInteract(player,worldPosition);}
    @Override public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> cap,Direction side){return !isRemoved()&&cap==net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER?capability.cast():super.getCapability(cap,side);}
    @Override public void invalidateCaps(){super.invalidateCaps();capability.invalidate();}
    @Override public void reviveCaps(){super.reviveCaps();capability=net.minecraftforge.common.util.LazyOptional.of(()->items);}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.put("Inventory",items.serializeNBT());}
    @Override public void load(CompoundTag tag){super.load(tag);var inventory=tag.getCompound("Inventory").copy();inventory.putInt("Size",1);items.deserializeNBT(inventory);}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
}
