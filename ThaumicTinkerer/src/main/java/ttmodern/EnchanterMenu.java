package ttmodern;

import java.util.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public final class EnchanterMenu extends AbstractContainerMenu {
    public final EnchanterEntity entity;public final List<EnchanterCatalog.Entry> options;
    public static List<EnchanterCatalog.Entry> options(net.minecraft.server.level.ServerPlayer player){return EnchanterCatalog.entries().values().stream().filter(e->EnchanterEntity.unlocked(player,e)).sorted(Comparator.comparing(e->e.enchantment().toString())).toList();}
    public static void writeOptions(FriendlyByteBuf buffer,List<EnchanterCatalog.Entry> options){buffer.writeVarInt(options.size());for(var entry:options){buffer.writeResourceLocation(entry.enchantment());buffer.writeResourceLocation(entry.icon());buffer.writeVarInt(entry.base().size());entry.base().forEach((aspect,amount)->{buffer.writeUtf(aspect,64);buffer.writeVarInt(amount);});}}
    private static List<EnchanterCatalog.Entry> readOptions(FriendlyByteBuf buffer){int count=buffer.readVarInt();if(count<0||count>4096)throw new IllegalArgumentException("Too many enchantments");var list=new ArrayList<EnchanterCatalog.Entry>();for(int i=0;i<count;i++){var id=buffer.readResourceLocation();var icon=buffer.readResourceLocation();int n=buffer.readVarInt();if(n<0||n>256)throw new IllegalArgumentException("Invalid costs");var costs=new TreeMap<String,Integer>();for(int j=0;j<n;j++)costs.put(buffer.readUtf(64),buffer.readVarInt());list.add(new EnchanterCatalog.Entry(id,"",icon,Map.copyOf(costs)));}return List.copyOf(list);}
    public EnchanterMenu(int id,Inventory inventory,FriendlyByteBuf data){this(id,inventory,(EnchanterEntity)inventory.player.level().getBlockEntity(data.readBlockPos()),readOptions(data));}
    public EnchanterMenu(int id,Inventory inventory,EnchanterEntity entity,List<EnchanterCatalog.Entry> options){
        super(Tinkerer.ENCHANTER_MENU.get(),id);this.entity=Objects.requireNonNull(entity);this.options=List.copyOf(options);
        addSlot(new SlotItemHandler(entity.items,0,6,6){@Override public boolean mayPickup(Player p){return !entity.working();}});addSlot(new SlotItemHandler(entity.items,1,6,31));
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,col+row*9+9,8+col*18,84+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,8+col*18,142));
    }
    @Override public boolean stillValid(Player player){return entity.usable(player);}
    @Override public boolean clickMenuButton(Player player,int id){
        if(!stillValid(player)||!(player instanceof net.minecraft.server.level.ServerPlayer server))return false;
        if(id==0)return entity.start(server);if(id<1)return false;int index=(id-1)/4,action=(id-1)%4;if(index>=options.size())return false;
        var key=options.get(index).enchantment();var enchant=net.minecraftforge.registries.ForgeRegistries.ENCHANTMENTS.getValue(key);if(enchant==null)return false;
        int current=entity.selected().getOrDefault(key,0);
        int rank=switch(action){case 0->current==0?1:current;case 1->0;case 2->Math.max(0,current-1);default->Math.min(enchant.getMaxLevel(),current+1);};
        return entity.choose(server,key,rank);
    }
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(!stillValid(player)||index<0||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.mayPickup(player))return ItemStack.EMPTY;var stack=slot.getItem();if(stack.isEmpty())return ItemStack.EMPTY;var copy=stack.copy();
        boolean moved=index<2?moveItemStackTo(stack,2,38,false):entity.items.isItemValid(1,stack)?moveItemStackTo(stack,1,2,false):entity.items.isItemValid(0,stack)&&moveItemStackTo(stack,0,1,false);
        if(!moved)return ItemStack.EMPTY;if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);return copy;
    }
}
