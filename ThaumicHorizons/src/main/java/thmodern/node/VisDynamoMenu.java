package thmodern.node;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import tc4revival.aspect.TCAspects;
import thmodern.Horizons;

public final class VisDynamoMenu extends AbstractContainerMenu{
    public static final java.util.List<String> ASPECTS=java.util.List.of("aer","terra","ignis","aqua","ordo","perditio");
    private final BlockPos pos;
    private final Inventory inventory;
    private final DataSlot flags=DataSlot.standalone();
    public VisDynamoMenu(int id,Inventory inventory,FriendlyByteBuf buffer){this(id,inventory,buffer.readBlockPos());}
    public VisDynamoMenu(int id,Inventory inventory,BlockPos pos){super(Horizons.VIS_DYNAMO_MENU.get(),id);this.pos=pos;this.inventory=inventory;addDataSlot(flags);refresh();}
    private void refresh(){if(!inventory.player.level().isClientSide&&inventory.player.level().getBlockEntity(pos) instanceof VisDynamoEntity node){int mask=0;for(int i=0;i<6;i++)if(node.enabled(TCAspects.catalog().require(ASPECTS.get(i))))mask|=1<<i;flags.set(mask);}}
    public boolean enabled(int index){return (flags.get()&(1<<index))!=0;}
    @Override public void broadcastChanges(){refresh();super.broadcastChanges();}
    @Override public boolean stillValid(Player player){return player.level().hasChunkAt(pos)&&player.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<=64&&player.level().getBlockEntity(pos) instanceof VisDynamoEntity;}
    @Override public boolean clickMenuButton(Player player,int id){
        if(player.level().isClientSide||id<0||id>=6||!stillValid(player))return false;
        if(player.level().getBlockEntity(pos) instanceof VisDynamoEntity node){node.toggle(TCAspects.catalog().require(ASPECTS.get(id)));broadcastChanges();return true;}return false;
    }
    @Override public ItemStack quickMoveStack(Player player,int slot){return ItemStack.EMPTY;}
}
