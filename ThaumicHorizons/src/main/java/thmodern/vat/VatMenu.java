package thmodern.vat;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import thmodern.Horizons;

public final class VatMenu extends AbstractContainerMenu {
    public final BlockPos pos;
    private final Container inventory;
    private final DataSlot mode=DataSlot.standalone(),progress=DataSlot.standalone(),allowed=DataSlot.standalone();
    private final DataSlot health=DataSlot.standalone(),maximumHealth=DataSlot.standalone(),creatureInfusions=DataSlot.standalone(),selfInfusions=DataSlot.standalone();
    // guivat.png row 225 uses the original self-infusion IDs 1..10, not recipe order.
    // These bits exist only in the open menu; saved bodies retain their named abilities.
    public static final java.util.List<String> SELF_ICONS=java.util.List.of("quicksilver_limbs","morphic_fingers","awake_blood","diamond_skin","silverwood_heart","photosynthetic_skin","amphibious","warped_tumor","spider_climb","chameleon_skin");
    private final Player viewer;
    public VatMenu(int id,Inventory player,FriendlyByteBuf data){this(id,player,data.readBlockPos(),new SimpleContainer(2));}
    public VatMenu(int id,Inventory player,BlockPos pos,Container inventory){
        super(Horizons.VAT_MENU.get(),id);this.pos=pos;this.inventory=inventory;viewer=player.player;
        addSlot(new Slot(inventory,0,63,32){@Override public boolean mayPlace(ItemStack stack){return allowed.get()!=0&&VatClone.input(stack);}});
        addSlot(new Slot(inventory,1,96,32){@Override public boolean mayPlace(ItemStack stack){return allowed.get()!=0&&stack.is(Horizons.NUTRIENTS.get());}});
        for(int y=0;y<3;y++)for(int x=0;x<9;x++)addSlot(new Slot(player,9+y*9+x,8+x*18,127+y*18));
        for(int x=0;x<9;x++)addSlot(new Slot(player,x,8+x*18,185));
        addDataSlot(mode);addDataSlot(progress);addDataSlot(allowed);addDataSlot(health);addDataSlot(maximumHealth);addDataSlot(creatureInfusions);addDataSlot(selfInfusions);refresh();
    }
    public int mode(){return mode.get();}public int progress(){return progress.get();}public boolean allowed(){return allowed.get()!=0;}
    public float health(){return health.get()/10f;}public float maximumHealth(){return maximumHealth.get()/10f;}
    public int creatureInfusions(){return creatureInfusions.get();}public int selfInfusions(){return selfInfusions.get();}
    private void refresh(){
        if(inventory instanceof VatEntity vat){
            mode.set(vat.mode());progress.set(vat.progress());var subject=vat.subject();
            health.set(Math.round(Math.min(3276,subject!=null?subject.getHealth():vat.effigyHealth())*10));
            maximumHealth.set(Math.round(Math.min(3276,subject!=null?subject.getMaxHealth():vat.hasEffigy()?20:0)*10));
            int creature=0,self=0;
            for(int i=0;i<CreatureInfusions.TYPES.size();i++)if(subject!=null&&CreatureInfusions.has(subject,CreatureInfusions.TYPES.get(i)))creature|=1<<i;
            for(int i=0;i<SELF_ICONS.size();i++)if(vat.selfInfusions().contains(SELF_ICONS.get(i)))self|=1<<i;
            creatureInfusions.set(creature);selfInfusions.set(self);
        }
        if(viewer instanceof ServerPlayer player)allowed.set(VatEntity.canUseSamples(player)?1:0);
    }
    @Override public void broadcastChanges(){refresh();super.broadcastChanges();}
    @Override public boolean stillValid(Player player){return player.level().hasChunkAt(pos)&&player.distanceToSqr(pos.getCenter())<=64&&player.level().getBlockEntity(pos) instanceof VatEntity;}
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(index<0||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;var stack=slot.getItem();var copy=stack.copy();
        if(index<2){if(!moveItemStackTo(stack,2,38,true))return ItemStack.EMPTY;}
        else {if(!allowed())return ItemStack.EMPTY;int target=VatClone.input(stack)?0:stack.is(Horizons.NUTRIENTS.get())?1:-1;if(target<0||!moveItemStackTo(stack,target,target+1,false))return ItemStack.EMPTY;}
        if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);return copy;
    }
}
