package tc4revival.content;

import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Ghost filter editing never transfers a sample into the player's inventory. */
public  class GolemMenu extends AbstractContainerMenu {
    private final GolemEntity golem;
    private final GolemCoreType core;
    private final int sampleCount,ghostSlots;
    private int page;
    private final Container samples;
    private boolean renderingFluidSlots;
    public GolemMenu(int id,Inventory inventory,net.minecraft.network.FriendlyByteBuf buffer) {
        this(TCArcane.GOLEM_MENU.get(),id,inventory,openingEntity(inventory,buffer));
    }
    private static GolemEntity openingEntity(Inventory inventory,net.minecraft.network.FriendlyByteBuf buffer) {
        var tracked=inventory.player.level().getEntity(buffer.readInt()); var snapshot=buffer.readNbt();
        var golem=tracked instanceof GolemEntity existing?existing:new GolemEntity(TCEntities.GOLEM.get(),inventory.player.level());
        if (snapshot!=null) golem.state().applyVisualData(snapshot);
        if (!(tracked instanceof GolemEntity)) golem.discard();
        return golem;
    }
    public GolemMenu(MenuType<GolemMenu> type,int id,Inventory inventory,GolemEntity golem) {
        super(type,id); this.golem=golem; core=golem.configuration().core(); sampleCount=golem.filters().size();
        samples=new SimpleContainer(Math.min(6,sampleCount)) {
            @Override public ItemStack getItem(int index) { return inventory.player.level().isClientSide?super.getItem(index):golem.filters().sample(page*6+index); }
            @Override public void setItem(int index,ItemStack stack) { if (inventory.player.level().isClientSide) super.setItem(index,stack); else golem.filters().sample(page*6+index,stack); }
            @Override public ItemStack removeItem(int index,int amount) { return ItemStack.EMPTY; }
            @Override public ItemStack removeItemNoUpdate(int index) { return ItemStack.EMPTY; }
        };
        golem.state().paused(true);
        if (core!=null && core.hasInventory) for (int index=0;index<Math.min(6,sampleCount);index++) {
            // TC4 fill added two slots at the same position, both pointing at the same sample.
            if (core==GolemCoreType.FILL) addSlot(new GhostSlot(index,256,false));
            addSlot(new GhostSlot(index,1,core==GolemCoreType.LIQUID));
        }
        ghostSlots=slots.size();
        for (int row=0;row<3;row++) for (int col=0;col<9;col++) addSlot(new Slot(inventory,9+row*9+col,8+col*18,84+row*18));
        for (int col=0;col<9;col++) addSlot(new Slot(inventory,col,8+col*18,142));
        addDataSlot(new DataSlot() { @Override public int get() { return page; } @Override public void set(int value) { page=Math.max(0,Math.min(maxPage(),value)); } });
    }
    public GolemEntity golem() { return golem; }
    public int page() { return page; }
    public int maxPage() { return Math.max(0,sampleCount/6-1); }
    public int sampleCount() { return sampleCount; }
    public ItemStack displayedSample(int index) { return samples.getItem(index); }
    public void renderingFluidSlots(boolean value) { renderingFluidSlots=value; }
    public boolean isFluidSampleSlot(Slot slot) { return slot instanceof GhostSlot ghost && ghost.fluid; }
    @Override public boolean stillValid(Player player) { return golem.isAlive() && player.distanceToSqr(golem)<=64 && golem.configuration().core()==core && golem.filters().size()==sampleCount; }
    @Override public void removed(Player player) { super.removed(player); golem.state().paused(false); }
    @Override public boolean clickMenuButton(Player player,int button) {
        if (!stillValid(player) || player.containerMenu!=this) return false;
        boolean changed=false;
        if (button==68) { golem.configuration().axisAligned(!golem.configuration().axisAligned()); golem.mob().getNavigation().stop(); golem.state().changed(); changed=true; }
        if (button==66 && page>0) { page--; changed=true; }
        if (button==67 && page<maxPage()) { page++; changed=true; }
        if (button>=50 && button<=57) {
            int index=button-50; golem.configuration().toggle(index,!golem.configuration().toggle(index)); golem.state().changed(); changed=true;
        }
        if (button>=0 && button<sampleCount) { golem.filters().cycleColor(button,-1); changed=true; }
        if (button>=sampleCount && button<sampleCount*2) { golem.filters().cycleColor(button-sampleCount,1); changed=true; }
        if (changed) { golem.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.get(),.2f,.8f); broadcastChanges(); }
        return changed;
    }
    @Override public ItemStack quickMoveStack(Player player,int index) { return ItemStack.EMPTY; } // The old ghost container suppressed player shift transfers.
    @Override public boolean canDragTo(Slot slot) { return !(slot instanceof GhostSlot) && super.canDragTo(slot); }
    @Override public boolean canTakeItemForPickAll(ItemStack stack,Slot slot) { return !(slot instanceof GhostSlot) && super.canTakeItemForPickAll(stack,slot); }
    @Override public void clicked(int index,int button,ClickType type,Player player) {
        if (!stillValid(player)) return;
        if (index<0 || index>=ghostSlots) {
            if (type!=ClickType.QUICK_MOVE) super.clicked(index,button,type,player);
            return;
        }
        var slot=slots.get(index); var sample=slot.getItem(); var cursor=getCarried();
        if (type==ClickType.QUICK_MOVE && !sample.isEmpty()) {
            if (button==0) slot.set(ItemStack.EMPTY);
            else if (button==1) { sample.setCount(Math.min(slot.getMaxStackSize(),sample.getCount()+16)); slot.set(sample); }
        } else if (type==ClickType.THROW && cursor.isEmpty() && !sample.isEmpty()) {
            sample.shrink(button==0?1:sample.getCount()); slot.set(sample);
        } else if (type==ClickType.CLONE && button==3 && !(player.getAbilities().instabuild && cursor.isEmpty())) slot.set(ItemStack.EMPTY);
        else if (type==ClickType.PICKUP && (button==0 || button==1)) {
            if (sample.isEmpty()) {
                if (!cursor.isEmpty() && slot.mayPlace(cursor)) { var copy=cursor.copy(); copy.setCount(Math.min(slot.getMaxStackSize(),button==0?copy.getCount():1)); slot.set(copy); }
            } else if (cursor.isEmpty()) {
                int count=sample.getCount()+(button==0?-1:1);
                if (count<=slot.getMaxStackSize()) { sample.setCount(count); slot.set(sample); }
            } else if (slot.mayPlace(cursor)) {
                if (ItemStack.isSameItemSameTags(sample,cursor)) {
                    int amount=Math.min(button==0?cursor.getCount():1,Math.min(slot.getMaxStackSize()-sample.getCount(),cursor.getMaxStackSize()-sample.getCount()));
                    sample.grow(amount); slot.set(sample);
                } else if (cursor.getCount()<=slot.getMaxStackSize()) slot.set(cursor.copy());
            }
        }
        broadcastChanges();
    }
    private  class GhostSlot extends Slot {
        private final int limit;
        private final boolean fluid;
        GhostSlot(int index,int limit,boolean fluid) { super(samples,index,100+index/2*28,16+index%2*31); this.limit=limit; this.fluid=fluid; }
        @Override public int getMaxStackSize() { return limit; }
        @Override public ItemStack getItem() { return fluid && renderingFluidSlots?ItemStack.EMPTY:super.getItem(); }
        @Override public boolean mayPickup(Player player) { return false; }
        @Override public boolean mayPlace(ItemStack stack) { return !fluid || net.minecraftforge.fluids.FluidUtil.getFluidHandler(stack.copy()).isPresent(); }
        @Override public void setChanged() { golem.state().changed(); }
    }
}
