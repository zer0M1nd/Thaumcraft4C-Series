package tc4revival.content;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The hidden legacy box: compact storage and explicit NBT master, not automatic shared chests. */
public  class MagicBoxBlockEntity extends BaseContainerBlockEntity {
    private final List<ItemStack> contents=new ArrayList<>();
    private BlockPos master;
    private int masterDimension, linked=-1, sorting=-1;
    private net.minecraft.resources.ResourceLocation masterWorld;
    private int ticks;
    public MagicBoxBlockEntity(BlockPos pos,BlockState state) { super(TCArcane.MAGIC_BOX_ENTITY.get(),pos,state); }
    private MagicBoxBlockEntity inventory() {
        if (master!=null && level!=null && level.dimension().location().equals(masterWorld) && level.hasChunkAt(master) && level.getBlockEntity(master) instanceof MagicBoxBlockEntity box && !box.isRemoved()) return box;
        return this;
    }
    private List<ItemStack> items() { return inventory().contents; }
    @Override public int getContainerSize() { var box=inventory();return Math.max(27*Math.max(1,box.linked+1),27*((box.contents.size()+26)/27)); }
    @Override public boolean isEmpty() { return items().stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { var list=items(); return slot<0||slot>=list.size()?ItemStack.EMPTY:list.get(slot); }
    @Override public ItemStack removeItem(int slot,int amount) {
        if (slot<0 || amount<=0 || slot>=items().size()) return ItemStack.EMPTY;
        var stack=items().get(slot); var result=stack.split(amount); if(stack.isEmpty())items().set(slot,ItemStack.EMPTY);inventory().setChanged();return result;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) { return slot<0||slot>=items().size()?ItemStack.EMPTY:items().set(slot,ItemStack.EMPTY); }
    @Override public void setItem(int slot,ItemStack stack) {
        if(slot<0 || slot>=getContainerSize())return;var list=items();
        while(list.size()<=slot)list.add(ItemStack.EMPTY);
        list.set(slot,stack);
        inventory().setChanged();
    }
    @Override public void clearContent() { items().clear();inventory().setChanged(); }
    @Override public boolean stillValid(Player player) { return !isRemoved()&&level!=null&&level.getBlockEntity(worldPosition)==this&&player.distanceToSqr(worldPosition.getCenter())<=64; }
    @Override protected Component getDefaultName() { return Component.translatable("block.thaumcraft.magic_box"); }
    @Override protected AbstractContainerMenu createMenu(int id,Inventory player) { return new MagicBoxMenu(id,player,this); }
    public void tick() { if(inventory()==this && (linked<0 || ++ticks%20==0)) refreshLinks(); }
    public void refreshLinks() {
        if(level==null||inventory()!=this)return;
        // Count each loaded connected box once, including the origin only once.
        var visited=new LinkedHashSet<BlockPos>();var pending=new ArrayDeque<BlockPos>();pending.push(worldPosition);visited.add(worldPosition);
        while(!pending.isEmpty()&&visited.size()<1024) {
            var pos=pending.pop();
            for(var side:Direction.values()) {
                var next=pos.relative(side);
                if(visited.size()>=1024)break;
                if(level.hasChunkAt(next)&&level.getBlockEntity(next) instanceof MagicBoxBlockEntity&&!visited.contains(next)) {visited.add(next);pending.push(next);}
            }
        }
        linked=visited.size()-1;
    }
    @Override public void onLoad() { super.onLoad();sortContents(); }
    public void sortContents() {
        if(level==null || level.isClientSide || sorting<0)return;
        var list=items();list.removeIf(ItemStack::isEmpty);
        var merged=new ArrayList<ItemStack>();
        for(var stack:list) {
            var remaining=stack.copy();
            for(var current:merged)if(ItemStack.isSameItemSameTags(current,remaining)) {
                int moved=Math.min(remaining.getCount(),Math.max(0,current.getMaxStackSize()-current.getCount()));
                current.grow(moved);remaining.shrink(moved);
            }
            while(!remaining.isEmpty())merged.add(remaining.split(remaining.getMaxStackSize()));
        }
        Comparator<ItemStack> order=Comparator.comparing(stack->stack.getHoverName().getString(),String.CASE_INSENSITIVE_ORDER);
        if(sorting==1)order=order.reversed();
        if(sorting==2)order=Comparator.comparing((ItemStack stack)->net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()).getNamespace()).thenComparing(order);
        merged.sort(order);list.clear();list.addAll(merged);inventory().setChanged();
    }
    public void dropLocalContents() {
        if(level==null || level.isClientSide)return;
        for(var stack:contents)net.minecraft.world.Containers.dropItemStack(level,worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,stack);
        contents.clear();setChanged();
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);sorting=tag.getByte("sort");master=tag.contains("w_x")?new BlockPos(tag.getInt("w_x"),tag.getInt("w_y"),tag.getInt("w_z")):null;
        masterDimension=tag.getInt("w_d");
        masterWorld=tag.contains("MasterWorld")?net.minecraft.resources.ResourceLocation.tryParse(tag.getString("MasterWorld"))
                :switch(masterDimension){case 0->net.minecraft.world.level.Level.OVERWORLD.location();case -1->net.minecraft.world.level.Level.NETHER.location();case 1->net.minecraft.world.level.Level.END.location();default->null;};
        linked=-1;contents.clear();
        var list=tag.getList("Items",Tag.TAG_COMPOUND);for(int i=0;i<list.size();i++){var row=list.getCompound(i);var stack=ItemStack.of(row);int slot=row.contains("Slot",Tag.TAG_INT)?row.getInt("Slot"):contents.size();
            if(!stack.isEmpty() && slot>=0 && slot<27648){while(contents.size()<=slot)contents.add(ItemStack.EMPTY);contents.set(slot,stack);}}
        sortContents();
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);tag.putByte("sort",(byte)sorting);
        if(master!=null){tag.putInt("w_x",master.getX());tag.putInt("w_y",master.getY());tag.putInt("w_z",master.getZ());tag.putInt("w_d",masterDimension);if(masterWorld!=null)tag.putString("MasterWorld",masterWorld.toString());}
        var list=new ListTag();for(int slot=0;slot<contents.size();slot++)if(!contents.get(slot).isEmpty()){var row=contents.get(slot).save(new CompoundTag());row.putInt("Slot",slot);list.add(row);}tag.put("Items",list);
    }
    @Override public boolean triggerEvent(int id,int value) { return id==1||id==2||super.triggerEvent(id,value); }
}
