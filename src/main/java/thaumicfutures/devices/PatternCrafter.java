package thaumicfutures.devices;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.*;

public class PatternCrafter extends SyncedDevice {
    private int preset,mask=511;
    private boolean custom;
    private UUID editor;
    private InteractionHand hand;
    private ItemStack editingWand=ItemStack.EMPTY;
    private int selectedSlot,cursor=-1,draft;
    private final List<ItemStack> pending=new ArrayList<>();
    public float rotation,previousRotation,speed;
    private int animationTicks;
    private final CraftingContainer grid=new TransientCraftingContainer(new AbstractContainerMenu(null,-1) {
        @Override public ItemStack quickMoveStack(Player player,int index) { return ItemStack.EMPTY; }
        @Override public boolean stillValid(Player player) { return false; }
    },3,3);
    public PatternCrafter(BlockPos pos,BlockState state) { super(Devices.CRAFTER_TILE.get(),pos,state); }
    public int mask() { return custom?mask:CraftingPatterns.preset(preset); }
    public int previewMask() { return cursor>=0?draft:mask(); }
    public int cursor() { return cursor; }
    public boolean custom() { return custom; }
    public int preset() { return preset; }
    public boolean busy() { return cursor>=0; }
    private boolean validEditor(Player player) {
        return player!=null && player.isAlive() && !player.isSpectator() && player.level()==level && player.distanceToSqr(Vec3Center.x(worldPosition),Vec3Center.y(worldPosition),Vec3Center.z(worldPosition))<=64
                && player.getInventory().selected==selectedSlot && player.getItemInHand(hand)==editingWand;
    }
    public void setNext(Player player,InteractionHand usedHand) {
        if(editor!=null && !validEditor(level.getPlayerByUUID(editor)))cancel(true);
        if(editor!=null && !editor.equals(player.getUUID())) {
            player.displayClientMessage(Component.translatable("thaumicfutures.crafter.busy"),true);return;
        }
        if(cursor<0) {
            editor=player.getUUID();hand=usedHand;editingWand=player.getItemInHand(hand);selectedSlot=player.getInventory().selected;cursor=0;draft=0;
        }
        if(hand!=usedHand || !validEditor(player)) { cancel(true);return; }
        draft=CraftingPatterns.set(draft,cursor,!player.isShiftKeyDown());cursor++;
        if(cursor==9) {
            mask=draft;custom=true;cancel(false);
            player.displayClientMessage(Component.translatable("thaumicfutures.crafter.complete",Integer.bitCount(mask)),true);
        } else player.displayClientMessage(Component.translatable("thaumicfutures.crafter.progress",cursor,cursor+1),true);
        sound();sync();
    }
    public void cyclePreset(Player player) {
        if(editor!=null && !editor.equals(player.getUUID()) && validEditor(level.getPlayerByUUID(editor))) {
            player.displayClientMessage(Component.translatable("thaumicfutures.crafter.busy"),true);return;
        }
        boolean returning=custom || busy();cancel(false);custom=false;
        if(!returning)preset=(preset+1)%CraftingPatterns.count();
        player.displayClientMessage(Component.translatable("thaumicfutures.crafter.preset",preset+1),true);sound();sync();
    }
    private void sound() { level.playSound(null,worldPosition,tc4revival.content.TCSounds.KEY.get(),net.minecraft.sounds.SoundSource.BLOCKS,.5f,1); }
    private void cancel(boolean notify) {
        if(notify && editor!=null && level!=null) { var player=level.getPlayerByUUID(editor);if(player!=null)player.displayClientMessage(Component.translatable("thaumicfutures.crafter.cancel"),true); }
        editor=null;editingWand=ItemStack.EMPTY;cursor=-1;draft=0;sync();
    }
    public void tick() {
        if(level==null)return;
        if(level.isClientSide) { previousRotation=rotation;if(animationTicks>0) { animationTicks--;speed+=1; }else speed*=.8f;rotation+=speed;return; }
        if(editor!=null && !validEditor(level.getPlayerByUUID(editor)))cancel(true);
        if(level.getGameTime()%20!=0 || level.hasNeighborSignal(worldPosition))return;
        var below=handler(worldPosition.below(),Direction.UP);
        if(below==null)return;
        flush(below);if(!pending.isEmpty() || busy() || mask()==0)return;
        var above=handler(worldPosition.above(),Direction.DOWN);if(above==null || above==below)return;
        for(int slot=0;slot<above.getSlots();slot++) {
            var material=above.getStackInSlot(slot).copy();if(material.isEmpty())continue;
            int needed=Integer.bitCount(mask());var plan=extraction(above,material,needed);if(plan==null)continue;
            for(int i=0;i<9;i++)grid.setItem(i,CraftingPatterns.enabled(mask(),i)?material.copyWithCount(1):ItemStack.EMPTY);
            var recipe=level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,grid,level).orElse(null);
            if(recipe==null)continue;
            var result=recipe.assemble(grid,level.registryAccess());if(result.isEmpty() || !result.isItemEnabled(level.enabledFeatures()))continue;
            var products=new ArrayList<ItemStack>();products.add(result.copy());
            for(var remainder:recipe.getRemainingItems(grid))if(!remainder.isEmpty())products.add(remainder.copy());
            if(!fits(below,products))continue;
            var taken=new ArrayList<ItemStack>();boolean valid=true;
            for(int i=0;i<plan.length;i++)if(plan[i]>0) {
                var actual=above.extractItem(i,plan[i],false);if(!actual.isEmpty())taken.add(actual);
                if(actual.getCount()!=plan[i] || !ItemStack.isSameItemSameTags(material,actual)) { valid=false;break; }
            }
            if(!valid) {
                // A capability can change between simulation and commit. Return everything actually
                // extracted, retaining any refused remainder in this tile instead of losing it.
                for(var item:taken) { var rest=ItemHandlerHelper.insertItemStacked(above,item,false);if(!rest.isEmpty())pending.add(rest); }
                sync();return;
            }
            pending.addAll(products);setChanged();flush(below);
            level.blockEvent(worldPosition,getBlockState().getBlock(),1,0);return;
        }
    }
    private IItemHandler handler(BlockPos pos,Direction side) {
        if(!level.hasChunkAt(pos))return null;var entity=level.getBlockEntity(pos);
        return entity==null?null:entity.getCapability(ForgeCapabilities.ITEM_HANDLER,side).orElse(null);
    }
    private static int[] extraction(IItemHandler source,ItemStack material,int needed) {
        int[] plan=new int[source.getSlots()];
        for(int slot=0;slot<source.getSlots() && needed>0;slot++) {
            var available=source.extractItem(slot,needed,true);
            if(!available.isEmpty() && ItemStack.isSameItemSameTags(material,available)) { plan[slot]=Math.min(needed,available.getCount());needed-=plan[slot]; }
        }
        return needed==0?plan:null;
    }
    /** Reserve all results together, including multiple different recipe remainders. */
    private static boolean fits(IItemHandler target,List<ItemStack> products) {
        var shadow=new ItemStack[target.getSlots()];for(int i=0;i<shadow.length;i++)shadow[i]=target.getStackInSlot(i).copy();
        for(var item:products) {
            int remaining=item.getCount();
            for(int i=0;i<shadow.length && remaining>0;i++) {
                if(!shadow[i].isEmpty() && !ItemStack.isSameItemSameTags(shadow[i],item))continue;
                var probe=item.copyWithCount(Math.min(item.getMaxStackSize(),target.getSlotLimit(i)));
                int accepted=probe.getCount()-target.insertItem(i,probe,true).getCount();
                int reserved=Math.max(0,shadow[i].getCount()-target.getStackInSlot(i).getCount());
                int take=Math.min(remaining,Math.max(0,accepted-reserved));
                if(take>0) { shadow[i]=item.copyWithCount(shadow[i].getCount()+take);remaining-=take; }
            }
            if(remaining>0)return false;
        }
        return true;
    }
    private void flush(IItemHandler target) {
        for(var iterator=pending.listIterator();iterator.hasNext();) {
            var item=iterator.next();var rest=ItemHandlerHelper.insertItemStacked(target,item,false);
            if(rest.isEmpty())iterator.remove();else iterator.set(rest);setChanged();
        }
    }
    public void dropPending() { if(level!=null && !level.isClientSide)for(var item:pending)Block.popResource(level,worldPosition,item);pending.clear();cancel(false); }
    @Override public boolean triggerEvent(int id,int value) {
        if(id==1) { animationTicks=10;if(level!=null && level.isClientSide)level.playLocalSound(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,tc4revival.content.TCSounds.PUMP.get(),net.minecraft.sounds.SoundSource.BLOCKS,.2f,1.7f,false);return true; }
        return super.triggerEvent(id,value);
    }
    @Override public void setRemoved() { editor=null;editingWand=ItemStack.EMPTY;cursor=-1;super.setRemoved(); }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);tag.putInt("Preset",preset);tag.putInt("Pattern",mask);tag.putBoolean("Custom",custom);
        var items=new ListTag();for(var item:pending)items.add(item.save(new CompoundTag()));tag.put("Pending",items);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);preset=Math.floorMod(tag.getInt("Preset"),CraftingPatterns.count());mask=tag.getInt("Pattern")&511;custom=tag.getBoolean("Custom");
        pending.clear();for(var raw:tag.getList("Pending",Tag.TAG_COMPOUND)) { var item=ItemStack.of((CompoundTag)raw);if(!item.isEmpty())pending.add(item); }
        cursor=tag.contains("Cursor")?tag.getInt("Cursor"):-1;draft=tag.getInt("Draft")&511;
    }
    @Override public CompoundTag getUpdateTag() { var tag=super.getUpdateTag();tag.remove("Pending");tag.putInt("Cursor",cursor);tag.putInt("Draft",draft);return tag; }
    private static final class Vec3Center { static double x(BlockPos p){return p.getX()+.5;}static double y(BlockPos p){return p.getY()+.5;}static double z(BlockPos p){return p.getZ()+.5;} }
}
