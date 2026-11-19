package tc4revival.content;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.AspectNbt;
import tc4revival.aspect.TCAspects;
import tc4revival.aspect.item.ItemStackAspects;

public  class AlchemyFurnaceBlockEntity extends BlockEntity implements WorldlyContainer,MenuProvider,thaumcraft.api.aspects.EssentiaSmelter,thaumcraft.api.devices.ExternalHeatReceiver {
    private final NonNullList<ItemStack> items=NonNullList.withSize(2,ItemStack.EMPTY);
    private AspectAmounts aspects=new AspectAmounts();
    private int burn,currentBurn,cook,smeltTime=100,ticks;
    private boolean speedBoost;
    private String customName="";
    private final EnumMap<Direction,LazyOptional<IItemHandlerModifiable>> automation=new EnumMap<>(Direction.class);
    public AlchemyFurnaceBlockEntity(BlockPos pos,BlockState state) { this(TCArcane.ALCHEMY_FURNACE_ENTITY.get(),pos,state); }
    protected AlchemyFurnaceBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type,BlockPos pos,BlockState state) { super(type,pos,state); initCaps(); }
    public int capacity(){return 50;}
    protected double smeltingSpeed(){return 1;}
    @Override public int alembicLimit(){return 5;}
    @Override public int exportInterval(){return speedBoost?20:40;}
    protected AspectAmounts smeltedYield(AspectAmounts input){return input;}
    @Override public AspectAmounts smeltedAspects(){return aspects();}
    @Override public int extractSmelted(Aspect aspect,int amount,boolean simulate){
        if(aspect==null||amount<=0)return 0;
        int taken=Math.min(amount,aspects.amount(aspect));
        if(taken>0&&!simulate){aspects.remove(aspect,taken);changed();}
        return taken;
    }
    @Override public int receiveHeat(int available,int cost,boolean powered,boolean simulate) {
        if(level==null || level.isClientSide || isRemoved())return 0;
        boolean ready=powered;
        if(!ready){var input=inputAspects(items.get(0));long total=input.total();ready=!input.isEmpty() && total>=0 && total<=capacity()-vis();}
        int heat=thaumcraft.api.devices.ExternalHeatReceiver.heatTicks(burn,available,cost,ready);
        if(heat>0 && !simulate){burn+=heat;currentBurn=Math.max(currentBurn,burn);changed();}
        return heat*Math.max(1,cost);
    }
    public int vis() { return (int)aspects.total(); }
    public boolean burning() { return burn>0; }
    public AspectAmounts aspects() { return aspects.copy(); }
    public final ContainerData data=new ContainerData() {
        @Override public int get(int index) { return switch(index) { case 0->cook; case 1->burn; case 2->currentBurn; case 3->vis(); case 4->smeltTime; default->0; }; }
        @Override public void set(int index,int value) {}
        @Override public int getCount() { return 5; }
    };
    public void tick() {
        if (level==null || level.isClientSide) return;
        ticks++; boolean wasBurning=burning(); if (burn>0) burn--;
        if (ticks%Math.max(1,exportInterval())==0 && !aspects.isEmpty()) distribute();
        if (burn==0 && canSmelt()) {
            currentBurn=burn=fuelTime(items.get(1));
            if (burn>0) {
                var fuel=items.get(1); speedBoost=fuel.is(TCItems.ALUMENTUM.get());
                var remainder=fuel.hasCraftingRemainingItem() ? fuel.getCraftingRemainingItem() : ItemStack.EMPTY;
                fuel.shrink(1); if (fuel.isEmpty()) items.set(1,remainder);
            }
        }
        if (burning() && canSmelt()) {
            if (++cook>=smeltTime) { cook=0; aspects.add(smeltedYield(inputAspects(items.get(0)))); items.get(0).shrink(1); changed(); }
        } else cook=0;
        if (wasBurning!=burning()) changed();
        if (burning() || wasBurning || cook>0) setChanged();
    }
    public static AspectAmounts inputAspects(ItemStack stack) { return thaumcraft.api.aspects.AspectProductionRules.item("thaumcraft:alchemy_furnace",stack,stack.isEmpty() ? new AspectAmounts() : ItemStackAspects.resolve(stack).orElseGet(AspectAmounts::new)); }
    public static int fuelTime(ItemStack stack) { return stack.isEmpty() ? 0 : ForgeHooks.getBurnTime(stack,net.minecraft.world.item.crafting.RecipeType.SMELTING); }
    private boolean canSmelt() {
        var input=inputAspects(items.get(0)); long amount=input.total();
        if (input.isEmpty() || amount<0 || amount>capacity()-vis()) return false;
        int bellows=BellowsBlockEntity.count(level,worldPosition,java.util.List.of(Direction.values()));
        smeltTime=Math.max(1,(int)(amount*10*(1-.125f*bellows)/smeltingSpeed())); return true;
    }
    private boolean take(Aspect aspect) {
        if (aspects.amount(aspect)<1) return false;
        aspects.reduce(aspect,1); if (aspects.amount(aspect)==0) aspects.remove(aspect); return true;
    }
    private void distribute() {
        var column=new ArrayList<AlembicBlockEntity>();
        for (int height=1;height<=alembicLimit();height++) {
            var pos=worldPosition.above(height); if (!level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof AlembicBlockEntity alembic)) break;
            column.add(alembic);
        }
        var exclude=new HashSet<Aspect>(); boolean moved=false;
        for (var alembic:column) if (alembic.aspect()!=null && alembic.amount()<alembic.capacity() && take(alembic.aspect())) {
            var selected=alembic.aspect(); alembic.addToContainer(selected,1); exclude.add(selected); moved=true;
        }
        for (var alembic:column) {
            if (alembic.aspect()!=null && alembic.amount()!=0) continue;
            Aspect selected=alembic.filter();
            if (selected==null) {
                var available=aspects.aspects().stream().filter(a->!exclude.contains(a) && aspects.amount(a)>0).toList();
                if (available.isEmpty()) continue; selected=available.get(level.random.nextInt(available.size()));
            }
            if (take(selected)) { alembic.addToContainer(selected,1); moved=true; break; }
        }
        if (moved) changed();
    }
    private void changed() {
        setChanged(); if (level!=null && !level.isClientSide) {
            var state=getBlockState(); var updated=state.setValue(AlchemyFurnaceBlock.LIT,burning()).setValue(AlchemyFurnaceBlock.CONTENTS,vis()>0);
            if (state!=updated) level.setBlock(worldPosition,updated,3); else level.sendBlockUpdated(worldPosition,state,state,2);
            level.updateNeighbourForOutputSignal(worldPosition,state.getBlock());
        }
    }
    @Override public int getContainerSize() { return 2; }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot,int quantity) { var removed=ContainerHelper.removeItem(items,slot,quantity); if (!removed.isEmpty()) changed(); return removed; }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items,slot); }
    @Override public void setItem(int slot,ItemStack stack) { items.set(slot,stack); if (stack.getCount()>64) stack.setCount(64); changed(); }
    @Override public void clearContent() { items.clear(); changed(); }
    @Override public boolean stillValid(Player player) { return level!=null && level.getBlockEntity(worldPosition)==this && player.distanceToSqr(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5)<=64; }
    @Override public boolean canPlaceItem(int slot,ItemStack stack) { return slot==0 ? !inputAspects(stack).isEmpty() : slot==1 && fuelTime(stack)>0; }
    @Override public int[] getSlotsForFace(Direction side) { return side==Direction.UP ? new int[0] : side==Direction.DOWN ? new int[]{1} : new int[]{0}; }
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction side) { return side!=Direction.UP && canPlaceItem(slot,stack); }
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction side) { return side!=Direction.DOWN || slot!=1 || stack.is(Items.BUCKET); }
    public void setCustomName(String name) { customName=name; setChanged(); }
    @Override public Component getDisplayName() { return customName.isEmpty() ? Component.translatable("container.alchemyfurnace") : Component.literal(customName); }
    @Override public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player) { return new AlchemyFurnaceMenu(id,inventory,this,data); }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); ContainerHelper.saveAllItems(tag,items); AspectNbt.write(tag,aspects);
        tag.putInt("BurnTime",burn); tag.putInt("CookTime",cook); tag.putBoolean("speedBoost",speedBoost); if (!customName.isEmpty()) tag.putString("CustomName",customName);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag); items.clear(); ContainerHelper.loadAllItems(tag,items); aspects=AspectNbt.read(tag,TCAspects.catalog()).amounts();
        for (var aspect:aspects.aspects()) if (aspects.amount(aspect)<=0) aspects.remove(aspect);
        burn=Math.max(0,tag.getInt("BurnTime")); cook=Math.max(0,tag.getInt("CookTime")); currentBurn=fuelTime(items.get(1));
        speedBoost=tag.getBoolean("speedBoost"); customName=tag.getString("CustomName");
    }
    private void initCaps() { for (var side:Direction.values()) automation.put(side,LazyOptional.of(()->new SidedInvWrapper(this,side))); }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> capability,Direction side) {
        if (capability==ForgeCapabilities.ITEM_HANDLER && side!=null && !isRemoved()) return automation.get(side).cast();
        return super.getCapability(capability,side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); automation.values().forEach(LazyOptional::invalidate); }
    @Override public void reviveCaps() { super.reviveCaps(); initCaps(); }
}
