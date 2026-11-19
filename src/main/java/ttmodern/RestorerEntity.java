package ttmodern;

import java.util.Map;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.*;
import tc4revival.aspect.*;
import tc4revival.content.EssentiaTransport;

/** One item, one essentia unit per ten ticks; inventory and source payment stay server-owned. */
public final class RestorerEntity extends BlockEntity implements thaumcraft.api.aspects.IEssentiaTransport,tc4revival.content.RevealedAspectContainer {
    private static final Map<String,Integer> REPAIR=Map.of("instrumentum",8,"fabrico",5,"ordo",3);
    private int ticks;
    private boolean repairing;
    public boolean repairing(){return repairing;}
    public final ItemStackHandler inventory=new ItemStackHandler(1) {
        @Override public boolean isItemValid(int slot,ItemStack stack){return repairable(stack);}
        @Override public int getSlotLimit(int slot){return 1;}
        @Override public ItemStack extractItem(int slot,int amount,boolean simulate){return repairable(getStackInSlot(slot))?ItemStack.EMPTY:super.extractItem(slot,amount,simulate);}
        @Override protected void onContentsChanged(int slot){changed();}
    };
    private LazyOptional<IItemHandler> capability=LazyOptional.of(()->inventory);
    public RestorerEntity(BlockPos pos,BlockState state){super(Tinkerer.RESTORER_ENTITY.get(),pos,state);}
    public static boolean repairable(ItemStack stack){return !stack.isEmpty() && stack.isDamageableItem() && stack.isDamaged() && (stack.getItem().isRepairable(stack)||TinkersRestoration.accepts(stack));}
    public ItemStack removeItem(){var stack=inventory.getStackInSlot(0);inventory.setStackInSlot(0,ItemStack.EMPTY);return stack;}
    public void insertItem(ItemStack stack){inventory.setStackInSlot(0,stack);}
    private Direction face(){return getBlockState().getValue(RestorerBlock.FACING);}
    private void changed(){setChanged();if(level!=null&&!level.isClientSide){level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);level.updateNeighborsAt(worldPosition,getBlockState().getBlock());}}
    public void tick() {
        if(++ticks%10!=0||level==null||level.isClientSide)return;
        boolean active=repairOnce();boolean different=repairing!=active;repairing=active;if(active||different)changed();
    }
    private boolean repairOnce(){
        if(!repairable(inventory.getStackInSlot(0)))return false;
        Direction side=face(),opposite=side.getOpposite();BlockPos source=worldPosition.relative(side);
        if(!level.hasChunkAt(source)||!(level.getBlockEntity(source) instanceof EssentiaTransport transport)
                || !transport.connectable(opposite)||!transport.canOutput(opposite)||transport.suction(opposite)>=128||transport.minimumSuction()>128)return false;
        Aspect aspect=transport.essentiaType(opposite);
        if(aspect==null||!REPAIR.containsKey(aspect.tag()))return false;
        var stack=inventory.getStackInSlot(0);var repaired=TinkersRestoration.repairedCopy(stack,REPAIR.get(aspect.tag()));if(repaired.isEmpty())return false;
        if(transport.takeEssentia(aspect,1,opposite)!=1)return false;
        stack.setTag(repaired.getTag()==null?null:repaired.getTag().copy());return true;
    }
    @Override public boolean connectable(Direction side){return side==face();}
    // Pull-only consumer: never advertise a nonexistent output or accept an unbounded push repair.
    @Override public boolean canInput(Direction side){return false;}
    @Override public boolean canOutput(Direction side){return false;}
    @Override public Aspect suctionType(Direction side){return side==face()?TCAspects.catalog().require("instrumentum"):null;}
    @Override public int suction(Direction side){return side==face()&&repairable(inventory.getStackInSlot(0))?128:0;}
    @Override public int minimumSuction(){return 0;}
    @Override public Aspect essentiaType(Direction side){return null;}
    @Override public int essentiaAmount(Direction side){return 0;}
    @Override public int takeEssentia(Aspect aspect,int amount,Direction side){return 0;}
    @Override public int addEssentia(Aspect aspect,int amount,Direction side){return 0;}
    @Override public AspectAmounts revealedAspects(){int damage=inventory.getStackInSlot(0).getDamageValue();return damage>0?new AspectAmounts().add(TCAspects.catalog().require("perditio"),damage):new AspectAmounts();}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.put("Inventory",inventory.serializeNBT());}
    @Override public void load(CompoundTag tag){super.load(tag);repairing=tag.getBoolean("Repairing");inventory.deserializeNBT(tag.getCompound("Inventory"));}
    @Override public CompoundTag getUpdateTag(){var tag=saveWithoutMetadata();tag.putBoolean("Repairing",repairing);return tag;}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,Direction side){return cap==ForgeCapabilities.ITEM_HANDLER?capability.cast():super.getCapability(cap,side);}
    @Override public void invalidateCaps(){super.invalidateCaps();capability.invalidate();}
    @Override public void reviveCaps(){super.reviveCaps();capability=LazyOptional.of(()->inventory);}
}
