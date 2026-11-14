package advancedthaumaturgy;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
public class WandbenchMenu extends AbstractContainerMenu {
    public final WandbenchEntity entity;private final Player player;private final SimpleContainer result=new SimpleContainer(1);
    public WandbenchMenu(int id,Inventory inv,FriendlyByteBuf data){this(id,inv,(WandbenchEntity)inv.player.level().getBlockEntity(data.readBlockPos()));}
    public WandbenchMenu(int id,Inventory inv,WandbenchEntity bench){
        super(AdvancedThaumaturgy.BENCH_MENU.get(),id);entity=bench;player=inv.player;
        addSlot(new Slot(result,0,113,31){@Override public boolean mayPlace(ItemStack s){return false;}@Override public boolean mayPickup(Player p){return false;}});
        int[][] xy={{21,31},{42,13},{42,31},{42,49},{150,31}};
        for(int i=0;i<5;i++){final int index=i;addSlot(new Slot(bench,i,xy[i][0],xy[i][1]){@Override public boolean mayPlace(ItemStack s){return entity.canPlaceItem(index,s);}});}
        for(int y=0;y<3;y++)for(int x=0;x<9;x++)addSlot(new Slot(inv,9+x+y*9,8+x*18,84+y*18));
        for(int x=0;x<9;x++)addSlot(new Slot(inv,x,8+x*18,142));
    }
    @Override public boolean stillValid(Player p){return entity!=null&&entity.stillValid(p);}
    @Override public void broadcastChanges(){if(!player.level().isClientSide&&stillValid(player))result.setItem(0,entity.preview(player));super.broadcastChanges();}
    /** Preview can never participate in vanilla SWAP/THROW/CLONE/PICKUP_ALL paths. */
    @Override public void clicked(int slot,int button,ClickType type,Player p){
        if(!stillValid(p))return;
        if(slot!=0){super.clicked(slot,button,type,p);broadcastChanges();return;}
        if(p.level().isClientSide)return;
        if(type==ClickType.QUICK_MOVE){quickMoveStack(p,0);return;}
        if(type!=ClickType.PICKUP||!getCarried().isEmpty())return;
        var out=entity.craft(p);if(!out.isEmpty())setCarried(out);broadcastChanges();
    }
    @Override public ItemStack quickMoveStack(Player p,int index){
        if(!stillValid(p)||index<0||index>=slots.size())return ItemStack.EMPTY;
        if(index==0){
            if(p.level().isClientSide||p.getInventory().getFreeSlot()<0)return ItemStack.EMPTY;
            var made=entity.craft(p);if(!made.isEmpty()){p.getInventory().add(made.copy());broadcastChanges();}return made;
        }
        var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;var stack=slot.getItem();var before=stack.copy();
        boolean moved=index<6?moveItemStackTo(stack,6,42,true):moveItemStackTo(stack,1,6,false);
        if(!moved)return ItemStack.EMPTY;if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();broadcastChanges();return before;
    }
}
