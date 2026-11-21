package thmodern.blood;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import thmodern.Horizons;

public final class BloodInfuserMenu extends AbstractContainerMenu {
    public static final List<String> ASPECTS=BloodInfuserEntity.FORMULA.aspects().stream().sorted().toList();
    public final BlockPos pos;
    private final Inventory playerInventory;
    private final Container inventory;
    private final DataSlot mode=DataSlot.standalone();
    private final List<DataSlot> selected=new ArrayList<>(),acquired=new ArrayList<>(),known=new ArrayList<>();
    public BloodInfuserMenu(int id,Inventory player,FriendlyByteBuf buffer){this(id,player,buffer.readBlockPos(),new SimpleContainer(10));}
    public BloodInfuserMenu(int id,Inventory player,BlockPos pos,Container inventory){
        super(Horizons.BLOOD_INFUSER_MENU.get(),id);this.pos=pos;this.inventory=inventory;playerInventory=player;
        addSlot(new Slot(inventory,0,16,37){@Override public boolean mayPlace(ItemStack stack){return BloodInfuserEntity.isBlood(stack);}});
        for(int x=0;x<3;x++)for(int y=0;y<3;y++)addSlot(new Slot(inventory,1+x*3+y,108+x*18,19+y*18){@Override public boolean mayPlace(ItemStack stack){return false;}});
        for(int y=0;y<3;y++)for(int x=0;x<9;x++)addSlot(new Slot(player,9+y*9+x,8+x*18,137+y*18));
        for(int x=0;x<9;x++)addSlot(new Slot(player,x,8+x*18,195));
        addDataSlot(mode);for(String aspect:ASPECTS){for(var list:List.of(selected,acquired,known)){var value=DataSlot.standalone();list.add(value);addDataSlot(value);}}
        refresh();
    }
    public int mode(){return mode.get();}
    public int selected(int index){return selected.get(index).get();}
    public int acquired(int index){return acquired.get(index).get();}
    public boolean known(int index){return known.get(index).get()!=0;}
    private void refresh(){
        if(!(inventory instanceof BloodInfuserEntity tile)||!(playerInventory.player instanceof ServerPlayer player))return;
        mode.set(tile.mode());
        for(int i=0;i<ASPECTS.size();i++){
            var key=ASPECTS.get(i);selected.get(i).set(tile.selection().getOrDefault(key,0));acquired.get(i).set(tile.acquired(key));
            known.get(i).set(thaumcraft.api.ThaumcraftApiHelper.hasDiscoveredAspect(player,thaumcraft.api.aspects.Aspect.of(key))?1:0);
        }
    }
    @Override public void broadcastChanges(){refresh();super.broadcastChanges();}
    @Override public boolean stillValid(Player player){return player.level().hasChunkAt(pos)&&player.distanceToSqr(pos.getCenter())<=64&&player.level().getBlockEntity(pos) instanceof BloodInfuserEntity;}
    @Override public boolean clickMenuButton(Player player,int button){
        if(!(player instanceof ServerPlayer)||!stillValid(player)||!(inventory instanceof BloodInfuserEntity tile))return false;
        if(button>=0&&button<=2){tile.mode(button);broadcastChanges();return true;}
        int index=button-3;boolean add=index<ASPECTS.size();if(!add)index-=ASPECTS.size();
        if(index<0||index>=ASPECTS.size())return false;
        refresh();if(!known(index))return false;
        boolean changed=tile.select(ASPECTS.get(index),add);if(changed)broadcastChanges();return changed;
    }
    @Override public ItemStack quickMoveStack(Player player,int slot){
        if(slot<0||slot>=slots.size())return ItemStack.EMPTY;var source=slots.get(slot);if(!source.hasItem())return ItemStack.EMPTY;
        var stack=source.getItem();var original=stack.copy();
        if(slot<10){if(!moveItemStackTo(stack,10,46,true))return ItemStack.EMPTY;}
        else if(!BloodInfuserEntity.isBlood(stack)||!moveItemStackTo(stack,0,1,false))return ItemStack.EMPTY;
        if(stack.isEmpty())source.set(ItemStack.EMPTY);else source.setChanged();source.onTake(player,stack);return original;
    }
}
