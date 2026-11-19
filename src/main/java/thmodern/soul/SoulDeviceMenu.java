package thmodern.soul;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.*;
import thmodern.Horizons;

public final class SoulDeviceMenu extends AbstractContainerMenu {
    public final BlockPos pos;
    public final int kind,deviceSlots;
    private final Player viewer;
    private final DataSlot progress=DataSlot.standalone(),souls=DataSlot.standalone(),energy=DataSlot.standalone();
    public static void open(Player player,BlockPos pos){
        if(!(player instanceof ServerPlayer server)||!player.level().hasChunkAt(pos)||player.distanceToSqr(pos.getCenter())>64)return;
        var tile=player.level().getBlockEntity(pos);int kind=tile instanceof SoulExtractorEntity?0:tile instanceof InspiratronEntity?1:-1;if(kind<0)return;
        net.minecraftforge.network.NetworkHooks.openScreen(server,new net.minecraft.world.SimpleMenuProvider((id,inventory,p)->new SoulDeviceMenu(id,inventory,pos,kind),net.minecraft.network.chat.Component.translatable("container.thaumichorizons.soul_device")),buf->{buf.writeBlockPos(pos);buf.writeVarInt(kind);});
    }
    public SoulDeviceMenu(int id,Inventory player,FriendlyByteBuf data){this(id,player,data.readBlockPos(),data.readVarInt());}
    public SoulDeviceMenu(int id,Inventory player,BlockPos pos,int kind){
        super(Horizons.SOUL_DEVICE_MENU.get(),id);this.pos=pos;this.kind=kind;viewer=player.player;deviceSlots=kind==0?1:kind==1?2:0;
        var tile=viewer.level().getBlockEntity(pos);IItemHandler contents=tile instanceof SoulExtractorEntity e?e.input:tile instanceof InspiratronEntity e?e.inventory:new ItemStackHandler(deviceSlots);
        for(int i=0;i<deviceSlots;i++)addSlot(new SlotItemHandler(contents,i,kind==0?64:15+i*131,kind==0?30:42));
        int inventoryY=kind==1?137:84;
        for(int y=0;y<3;y++)for(int x=0;x<9;x++)addSlot(new Slot(player,9+y*9+x,8+x*18,inventoryY+y*18));
        for(int x=0;x<9;x++)addSlot(new Slot(player,x,8+x*18,inventoryY+58));addDataSlot(progress);addDataSlot(souls);addDataSlot(energy);refresh();
    }
    public int progress(){return progress.get();}public int souls(){return souls.get();}public int energy(){return energy.get();}
    private void refresh(){if(viewer.level().isClientSide)return;var tile=viewer.level().getBlockEntity(pos);if(tile instanceof SoulExtractorEntity e)progress.set(e.remaining());else if(tile instanceof InspiratronEntity e)progress.set(e.progress());else if(tile instanceof SoulforgeEntity e){progress.set(e.progress());souls.set(e.souls());energy.set(e.energy());}}
    @Override public void broadcastChanges(){refresh();super.broadcastChanges();}
    @Override public boolean stillValid(Player player){if(!player.level().hasChunkAt(pos)||player.distanceToSqr(pos.getCenter())>64)return false;var tile=player.level().getBlockEntity(pos);return kind==0?tile instanceof SoulExtractorEntity:kind==1?tile instanceof InspiratronEntity:kind==2&&tile instanceof SoulforgeEntity;}
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(index<0||index>=slots.size()||!stillValid(player))return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;var stack=slot.getItem();var original=stack.copy();
        if(index<deviceSlots){if(!moveItemStackTo(stack,deviceSlots,slots.size(),true))return ItemStack.EMPTY;}
        else if(deviceSlots==0||!moveItemStackTo(stack,0,deviceSlots,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);return original;
    }
}
