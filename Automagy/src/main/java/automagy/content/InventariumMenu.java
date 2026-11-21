package automagy.content;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Ghost slots use vanilla menu synchronization; no storage item can be taken from this view. */
public final class InventariumMenu extends AbstractContainerMenu {
    public static final int PAGE_SIZE=60, DEMAND_SIZE=15, DISPLAY_SIZE=PAGE_SIZE+DEMAND_SIZE;
    private final net.minecraft.world.level.block.entity.BlockEntity scribe;
    private final SimpleContainer display=new SimpleContainer(DISPLAY_SIZE);
    private final ContainerData data=new SimpleContainerData(8+DISPLAY_SIZE*4);
    private final Player owner;
    private int refresh;
    private String query="";
    public InventariumMenu(int id,Inventory inventory,BlockPos pos){
        super(Content.INVENTARIUM_MENU.get(),id);owner=inventory.player;
        var tile=owner.level().getBlockEntity(pos);scribe=tile instanceof UnseenScribeBlockEntity||tile instanceof RequisitionTomeBlockEntity?tile:null;
        if(!owner.level().isClientSide)data.set(3,owner.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getBoolean("AutomagyInventariumQuantitySort")?1:0);
        int offset=requestTerminal()?30:0;
        for(int i=0;i<DISPLAY_SIZE;i++){
            final int cell=i;boolean demand=i>=PAGE_SIZE;
            int x=demand?-19+(i-PAGE_SIZE)%3*18:12+offset+i%10*18+(i%10>=5?6:0);
            int y=demand?35+(i-PAGE_SIZE)/3*18:8+i/10*18;
            addSlot(new Slot(display,i,x,y){
                @Override public boolean isActive(){return cell<PAGE_SIZE||requestTerminal();}
                @Override public boolean mayPlace(ItemStack stack){return false;}
                @Override public boolean mayPickup(Player player){return false;}
            });
        }
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,9+row*9+col,24+offset+col*18,140+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,24+offset+col*18,198));
        addSlot(new Slot(new SimpleContainer(1),0,22,169){public boolean isActive(){return requestTerminal();}public boolean mayPlace(ItemStack s){return false;}public boolean mayPickup(Player p){return false;}});
        addDataSlots(data);
    }
    public int page(){return data.get(0);}
    public int pages(){return Math.max(1,data.get(1));}
    public boolean omitted(){return data.get(2)!=0;}
    public boolean quantitySort(){return data.get(3)!=0;}
    public boolean requestsView(){return false;}
    public boolean localView(){return data.get(4+DISPLAY_SIZE*4)!=0;}
    public boolean hasLocalView(){return scribe instanceof UnseenScribeBlockEntity;}
    public int demandPage(){return data.get(5+DISPLAY_SIZE*4);}
    public int demandPages(){return Math.max(1,data.get(6+DISPLAY_SIZE*4));}
    public long amount(int slot){long result=0;for(int part=0;part<4;part++)result|=(data.get(4+slot*4+part)&0xffffL)<<(part*16);return result;}
    public boolean hasMirror(){return data.get(7+DISPLAY_SIZE*4)!=0;}
    private void update(){
        data.set(7+DISPLAY_SIZE*4,scribe instanceof RequisitionTomeBlockEntity tome&&tome.mirrorStatus(0)==1?1:0);
        List<InventariumContents.Entry> entries=new ArrayList<>(),requested=new ArrayList<>();boolean omitted=false;
        BlockPos linked=scribe instanceof UnseenScribeBlockEntity node?node.linkedCore():scribe instanceof RequisitionTomeBlockEntity tome?tome.linkedCore():null;
        if(linked!=null&&owner.level().hasChunkAt(linked)&&owner.level().getBlockEntity(linked) instanceof InventariumBlockEntity core&&(scribe instanceof RequisitionTomeBlockEntity?core.hasMemoryUser(scribe.getBlockPos()):core.hasNode(scribe.getBlockPos()))){
            var destination=net.minecraft.core.GlobalPos.of(owner.level().dimension(),scribe.getBlockPos());
            if(requestTerminal())for(var request:core.requests().requests())if(request.destination().equals(destination))requested.add(new InventariumContents.Entry(request.item().stack(),request.amount()));
            var snapshot=localView()&&scribe instanceof UnseenScribeBlockEntity node?InventariumContents.read(owner.level(),node.sources(),65536):core.contents();
            entries.addAll(snapshot.entries());omitted=snapshot.omitted();
        }

        Comparator<InventariumContents.Entry> byId=Comparator.<InventariumContents.Entry,String>comparing(e->e.item().getHoverName().getString(),String.CASE_INSENSITIVE_ORDER).thenComparing(e->BuiltInRegistries.ITEM.getKey(e.item().getItem()).toString());
        if(!query.isEmpty())entries.removeIf(entry->{var stack=entry.item();return !BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().contains(query)&&!stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(query);});
        entries.sort(quantitySort()?Comparator.comparingLong(InventariumContents.Entry::amount).reversed().thenComparing(byId):byId);
        data.set(1,Math.max(1,(entries.size()+PAGE_SIZE-1)/PAGE_SIZE));data.set(0,Math.min(page(),pages()-1));data.set(2,omitted?1:0);
        requested.sort(byId);data.set(6+DISPLAY_SIZE*4,Math.max(1,(requested.size()+DEMAND_SIZE-1)/DEMAND_SIZE));data.set(5+DISPLAY_SIZE*4,Math.min(demandPage(),demandPages()-1));
        for(int slot=0;slot<DISPLAY_SIZE;slot++){
            boolean demand=slot>=PAGE_SIZE;int index=demand?demandPage()*DEMAND_SIZE+slot-PAGE_SIZE:page()*PAGE_SIZE+slot;
            var list=demand?requested:entries;var entry=index<list.size()?list.get(index):null;
            display.setItem(slot,entry==null?ItemStack.EMPTY:entry.item());long count=entry==null?0:entry.amount();
            for(int part=0;part<4;part++)data.set(4+slot*4+part,(int)(count>>>(part*16))&0xffff);
        }
    }

    @Override public void broadcastChanges(){if(!owner.level().isClientSide&&refresh--<=0){refresh=19;update();}super.broadcastChanges();}
    public void search(Player player,String value){if(!player.level().isClientSide&&stillValid(player)&&value.length()<=80){query=value.strip().toLowerCase(Locale.ROOT);data.set(0,0);refresh=0;}}
    @Override public boolean clickMenuButton(Player player,int button){
        if(player.level().isClientSide||!stillValid(player)||button<0||button>6)return false;
        if(button==6){if(!(scribe instanceof RequisitionTomeBlockEntity)||!(player instanceof net.minecraft.server.level.ServerPlayer server))return false;
            net.minecraftforge.network.NetworkHooks.openScreen(server,new net.minecraft.world.SimpleMenuProvider((id,inv,p)->new MirrorSettingsMenu(id,inv,scribe.getBlockPos()),net.minecraft.network.chat.Component.translatable("Automagy.gui.scribe.mirrors")),scribe.getBlockPos());return true;
        }
        if(button==3){if(!hasLocalView())return false;data.set(4+DISPLAY_SIZE*4,localView()?0:1);data.set(0,0);}
        else if(button>=4){if(!requestTerminal())return false;data.set(5+DISPLAY_SIZE*4,Math.max(0,Math.min(demandPages()-1,demandPage()+(button==4?-1:1))));}
        else if(button==2){
            data.set(3,quantitySort()?0:1);
            var saved=player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);saved.putBoolean("AutomagyInventariumQuantitySort",quantitySort());player.getPersistentData().put(Player.PERSISTED_NBT_TAG,saved);
        }else data.set(0,Math.max(0,Math.min(pages()-1,page()+(button==0?-1:1))));
        refresh=0;broadcastChanges();return true;
    }
    @Override public boolean stillValid(Player player){return player==owner&&scribe!=null&&!scribe.isRemoved()&&scribe.getLevel()==player.level()&&player.level().getBlockEntity(scribe.getBlockPos())==scribe&&player.distanceToSqr(scribe.getBlockPos().getCenter())<=64&&(player.level().isClientSide||(scribe instanceof RequisitionTomeBlockEntity tome?tome.powered()&&tome.core()!=null:scribe instanceof UnseenScribeBlockEntity table&&table.powered()&&table.linkedCore()!=null));}
    public boolean requestTerminal(){return scribe instanceof RequisitionTomeBlockEntity;}
    public void request(Player player,int slot,ItemStack expected,int count,boolean cancel,boolean alt){
        if(player.level().isClientSide||!stillValid(player)||slot<0||slot>=DISPLAY_SIZE||!(scribe instanceof RequisitionTomeBlockEntity tome))return;
        if(count<1||count>64)return;
        ItemStack shown=display.getItem(slot);if(!shown.isEmpty()&&ItemStack.isSameItemSameTags(shown,expected)){
            var core=tome.core();if(core==null)return;
            var destination=net.minecraft.core.GlobalPos.of(player.level().dimension(),tome.getBlockPos());
            long pending=core.requests().requests().stream().filter(r->r.destination().equals(destination)&&r.item().matches(shown)).mapToLong(r->r.amount()).sum();
            long stock=core.contents().entries().stream().filter(e->ItemStack.isSameItemSameTags(e.item(),shown)).mapToLong(e->e.amount()).sum();
            int delta=automagy.logic.RequisitionControls.limit(cancel?-count:count,slot>=PAGE_SIZE,alt,stock,pending);
            if(delta<0)tome.cancel(shown,-delta);else if(delta>0)tome.request(shown,delta);
            refresh=0;broadcastChanges();
        }
    }
    @Override public void clicked(int slot,int button,ClickType type,Player player){
        if(slot>=0&&slot<DISPLAY_SIZE){
            // Request ghost-slot input is handled by the screen, including modifier keys.
            return;
        }
        if(slot==DISPLAY_SIZE+36){
            if(!player.level().isClientSide&&stillValid(player)&&scribe instanceof RequisitionTomeBlockEntity tome&&type==ClickType.PICKUP&&(button==0||button==1)&&!getCarried().isEmpty()){
                var offered=getCarried().copyWithCount(button==1?1:getCarried().getCount());var left=tome.deposit(offered);getCarried().shrink(offered.getCount()-left.getCount());broadcastChanges();
            }return;
        }
        super.clicked(slot,button,type,player);
    }
    @Override public ItemStack quickMoveStack(Player player,int slot){
        if(player.level().isClientSide||!stillValid(player)||slot<DISPLAY_SIZE||slot>=DISPLAY_SIZE+36||!(scribe instanceof RequisitionTomeBlockEntity tome))return ItemStack.EMPTY;
        var source=getSlot(slot);var before=source.getItem().copy();if(before.isEmpty())return ItemStack.EMPTY;var left=tome.deposit(before);if(left.getCount()==before.getCount())return ItemStack.EMPTY;source.set(left);source.onTake(player,before.copyWithCount(before.getCount()-left.getCount()));return before;
    }
}
