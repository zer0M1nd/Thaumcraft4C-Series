package tc4revival.content;

import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public  class PechMenu extends AbstractContainerMenu {
    private final PechEntity pech;
    private final Player player;
    public final SimpleContainer contents=new SimpleContainer(5);
    private final ContainerData offerValue=new SimpleContainerData(1);
    public PechMenu(int id,Inventory inventory,net.minecraft.network.FriendlyByteBuf buffer) { this(id,inventory,inventory.player.level().getEntity(buffer.readInt()) instanceof PechEntity p ? p : null); }
    public PechMenu(int id,Inventory inventory,PechEntity pech) {
        super(TCArcane.PECH_MENU.get(),id); this.pech=pech; player=inventory.player; addDataSlots(offerValue);
        addSlot(new Slot(contents,0,36,29));
        for (int i=0;i<4;i++) addSlot(new Slot(contents,i+1,106+(i%2)*18,20+(i/2)*18) { @Override public boolean mayPlace(ItemStack stack) { return false; } });
        for (int row=0;row<3;row++) for (int col=0;col<9;col++) addSlot(new Slot(inventory,9+row*9+col,8+col*18,84+row*18));
        for (int col=0;col<9;col++) addSlot(new Slot(inventory,col,8+col*18,142));
    }
    public boolean ready() { if ((player.level().isClientSide ? offerValue.get(0) : PechEntity.value(contents.getItem(0)))<=0) return false; for (int i=1;i<5;i++) if (!contents.getItem(i).isEmpty()) return false; return true; }
    @Override public void broadcastChanges() { if (!player.level().isClientSide) offerValue.set(0,PechEntity.value(contents.getItem(0))); super.broadcastChanges(); }
    @Override public boolean stillValid(Player player) { return pech!=null && pech.isAlive() && pech.friendly() && player.distanceToSqr(pech)<=64 && (player.level().isClientSide || pech.customer==player); }
    @Override public boolean clickMenuButton(Player player,int button) {
        if (button!=0 || player!=this.player || player.level().isClientSide || player.containerMenu!=this || !stillValid(player) || !ready()) return false;
        var random=pech.getRandom(); int value=PechEntity.value(contents.getItem(0));
        if (random.nextInt(100)<=value/2) { pech.friendly(false); pech.playSound(TCSounds.PECH_TRADE.get(),.4f,1); }
        if (random.nextInt(5)==0) value+=random.nextInt(3); else if (random.nextBoolean()) value-=random.nextInt(3);
        for (int iteration=0;value>0 && iteration<128;iteration++) {
            int amount=Math.min(5,Math.max((value+1)/2,random.nextInt(value)+1)); value-=amount;
            var available=new java.util.ArrayList<Integer>(); for (int i=0;i<9;i++) if (!pech.pack.getItem(i).isEmpty()) available.add(i);
            ItemStack reward;
            if (amount==1 && random.nextBoolean() && !available.isEmpty()) reward=pech.pack.removeItem(available.get(random.nextInt(available.size())),1);
            else {
                reward=amount>=4 && random.nextBoolean() ? PechTrades.dungeon(random) : PechTrades.select(pech.profession(),amount,random);
                if (reward.isEmpty()) { value+=amount; continue; }
                reward.getItem().onCraftedBy(reward,player.level(),player);
            }
            mergeReward(reward);
            if (!reward.isEmpty()) net.minecraftforge.items.ItemHandlerHelper.giveItemToPlayer(player,reward);
        }
        contents.removeItem(0,1); broadcastChanges(); return true;
    }
    private void mergeReward(ItemStack reward) {
        for (int i=1;i<5 && !reward.isEmpty();i++) {
            var slot=contents.getItem(i);
            if (!slot.isEmpty() && ItemStack.isSameItemSameTags(slot,reward)) { int count=Math.min(reward.getCount(),slot.getMaxStackSize()-slot.getCount()); slot.grow(count); reward.shrink(count); }
        }
        for (int i=1;i<5 && !reward.isEmpty();i++) if (contents.getItem(i).isEmpty()) { contents.setItem(i,reward.split(reward.getMaxStackSize())); }
        contents.setChanged();
    }
    @Override public ItemStack quickMoveStack(Player player,int index) {
        if (index<0 || index>=slots.size()) return ItemStack.EMPTY; var slot=slots.get(index); var stack=slot.getItem(); if (stack.isEmpty()) return ItemStack.EMPTY;
        var original=stack.copy(); if (!(index<5 ? moveItemStackTo(stack,5,41,true) : moveItemStackTo(stack,0,1,true))) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged(); slot.onTake(player,stack); return original;
    }
    @Override public void removed(Player player) {
        super.removed(player); if (pech!=null && pech.customer==player) pech.customer=null;
        if (!player.level().isClientSide) for (int i=0;i<5;i++) { var stack=contents.removeItemNoUpdate(i); if (!stack.isEmpty()) { var drop=player.drop(stack,false); if (drop!=null) drop.getPersistentData().putBoolean("PechDrop",true); } }
    }
}
