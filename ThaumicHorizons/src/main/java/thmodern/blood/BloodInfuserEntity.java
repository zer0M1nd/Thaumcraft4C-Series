package thmodern.blood;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.*;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.aspect.*;
import tc4revival.content.EssentiaTransport;
import thmodern.Horizons;

public final class BloodInfuserEntity extends BaseContainerBlockEntity implements WorldlyContainer,EssentiaTransport {
    public static final BloodFormula FORMULA=BloodFormula.bundled();
    private final NonNullList<ItemStack> inventory=NonNullList.withSize(10,ItemStack.EMPTY);
    private final LinkedHashMap<String,Integer> selected=new LinkedHashMap<>(),acquired=new LinkedHashMap<>();
    private int mode;
    private LazyOptional<IItemHandler> automation=LazyOptional.of(()->new SidedInvWrapper(this,Direction.DOWN));
    public BloodInfuserEntity(BlockPos pos,BlockState state){super(Horizons.BLOOD_INFUSER_ENTITY.get(),pos,state);}
    public int mode(){return mode;}
    public void mode(int value){mode=Math.max(0,Math.min(2,value));changed();}
    public Map<String,Integer> selection(){return Collections.unmodifiableMap(selected);}
    public int acquired(String key){return acquired.getOrDefault(key,0);}
    public boolean select(String key,boolean add){
        if(!FORMULA.supports(key))return false;
        int count=selected.getOrDefault(key,0);
        if(add){if(selected.values().stream().mapToInt(Integer::intValue).sum()>=FORMULA.maximum)return false;selected.put(key,count+1);}
        // Already imported essentia is committed to this batch. Never erase it on edit.
        else{if(count==0||count-1<acquired(key))return false;if(count==1)selected.remove(key);else selected.put(key,count-1);}
        changed();return true;
    }
    private int freeOutput(){for(int i=1;i<10;i++)if(inventory.get(i).isEmpty())return i;return -1;}
    private String wanted(){
        if(mode==0||!isBlood(inventory.get(0))||freeOutput()<0)return null;
        for(var entry:selected.entrySet())if(acquired(entry.getKey())<entry.getValue())return entry.getKey();return null;
    }
    public static boolean isBlood(ItemStack stack){return stack.is(Horizons.HUMAN_BLOOD.get())||stack.is(Horizons.BLOOD_PHIAL.get());}
    public void tick(){
        if(level==null||level.isClientSide||mode==0||!isBlood(inventory.get(0))||freeOutput()<0||selected.isEmpty())return;
        var wanted=wanted();
        if(wanted==null){
            var formula=FORMULA.calculate(selected,key->TCAspects.catalog().require(key).color());
            var effects=new ArrayList<net.minecraft.world.effect.MobEffectInstance>();
            for(var effect:formula.effects()){
                var type=ForgeRegistries.MOB_EFFECTS.getValue(net.minecraft.resources.ResourceLocation.parse(effect.id()));
                if(type==null)return; // A missing optional effect must not silently consume the batch.
                effects.add(new net.minecraft.world.effect.MobEffectInstance(type,effect.duration(),effect.amplifier()));
            }
            var output=new ItemStack(inventory.get(0).is(Horizons.BLOOD_PHIAL.get())?Horizons.INJECTION_PHIAL.get():Horizons.INJECTION.get());
            PotionUtils.setCustomEffects(output,effects);output.getOrCreateTag().putInt("CustomPotionColor",formula.color());
            inventory.set(freeOutput(),output);inventory.get(0).shrink(1);acquired.clear();if(mode==1)mode=0;changed();return;
        }
        var aspect=TCAspects.catalog().require(wanted);
        for(var side:Direction.values()){
            var pos=worldPosition.relative(side);var from=side.getOpposite();
            if(!connectable(side)||!level.hasChunkAt(pos)||!(level.getBlockEntity(pos) instanceof EssentiaTransport source)||!source.connectable(from)||!source.canOutput(from))continue;
            if(source.suction(from)>=128||source.minimumSuction()>128||source.essentiaAmount(from)<=0)continue;
            if(source.takeEssentia(aspect,1,from)>0){addEssentia(aspect,1,side);return;}
        }
    }
    private void changed(){setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    @Override public boolean connectable(Direction face){return face!=null&&face!=Direction.UP;}
    @Override public boolean canInput(Direction face){return connectable(face);}
    @Override public boolean canOutput(Direction face){return false;}
    @Override public Aspect suctionType(Direction face){var key=wanted();return connectable(face)&&key!=null?TCAspects.catalog().require(key):null;}
    @Override public int suction(Direction face){return suctionType(face)==null?0:128;}
    @Override public int minimumSuction(){return 0;}
    @Override public Aspect essentiaType(Direction face){return null;}
    @Override public int essentiaAmount(Direction face){return 0;}
    @Override public int takeEssentia(Aspect aspect,int amount,Direction face){return 0;}
    @Override public int addEssentia(Aspect aspect,int amount,Direction face){
        if(level==null||level.isClientSide||!connectable(face)||amount<=0||aspect==null||!aspect.tag().equals(wanted()))return 0;
        int accepted=Math.min(amount,selected.get(aspect.tag())-acquired(aspect.tag()));if(accepted<=0)return 0;
        acquired.merge(aspect.tag(),accepted,Integer::sum);changed();return accepted;
    }
    @Override protected Component getDefaultName(){return Component.translatable("container.bloodInfuser");}
    @Override protected AbstractContainerMenu createMenu(int id,Inventory inventory){return new BloodInfuserMenu(id,inventory,worldPosition,this);}
    @Override public int getContainerSize(){return 10;}
    @Override public boolean isEmpty(){return inventory.stream().allMatch(ItemStack::isEmpty);}
    @Override public ItemStack getItem(int slot){return slot>=0&&slot<10?inventory.get(slot):ItemStack.EMPTY;}
    @Override public ItemStack removeItem(int slot,int amount){var stack=ContainerHelper.removeItem(inventory,slot,amount);if(!stack.isEmpty())changed();return stack;}
    @Override public ItemStack removeItemNoUpdate(int slot){return ContainerHelper.takeItem(inventory,slot);}
    @Override public void setItem(int slot,ItemStack stack){if(slot<0||slot>=10)return;inventory.set(slot,stack);if(stack.getCount()>Math.min(64,stack.getMaxStackSize()))stack.setCount(Math.min(64,stack.getMaxStackSize()));changed();}
    @Override public void clearContent(){inventory.clear();changed();}
    @Override public boolean stillValid(Player player){return !isRemoved()&&level!=null&&level.getBlockEntity(worldPosition)==this&&player.distanceToSqr(worldPosition.getCenter())<=64;}
    @Override public boolean canPlaceItem(int slot,ItemStack stack){return slot==0&&isBlood(stack);}
    @Override public int[] getSlotsForFace(Direction face){return new int[]{0,1,2,3,4,5,6,7,8,9};}
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction face){return canPlaceItem(slot,stack);}
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction face){return slot>0&&slot<10;}
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,Direction side){return cap==ForgeCapabilities.ITEM_HANDLER&&!isRemoved()?automation.cast():super.getCapability(cap,side);}
    @Override public void invalidateCaps(){super.invalidateCaps();automation.invalidate();}
    @Override public void reviveCaps(){super.reviveCaps();automation=LazyOptional.of(()->new SidedInvWrapper(this,Direction.DOWN));}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);ContainerHelper.saveAllItems(tag,inventory);tag.putInt("Mode",mode);var list=new ListTag();selected.forEach((key,amount)->{var row=new CompoundTag();row.putString("Aspect",key);row.putInt("Selected",amount);row.putInt("Acquired",acquired(key));list.add(row);});tag.put("Formula",list);}
    @Override public void load(CompoundTag tag){
        super.load(tag);inventory.clear();ContainerHelper.loadAllItems(tag,inventory);mode=Math.max(0,Math.min(2,tag.getInt("Mode")));selected.clear();acquired.clear();int remaining=FORMULA.maximum;
        for(var value:tag.getList("Formula",Tag.TAG_COMPOUND)){
            var row=(CompoundTag)value;String key=row.getString("Aspect");int count=Math.min(remaining,Math.max(0,row.getInt("Selected")));
            if(!FORMULA.supports(key)||selected.containsKey(key)||count==0)continue;
            selected.put(key,count);acquired.put(key,Math.min(count,Math.max(0,row.getInt("Acquired"))));remaining-=count;
        }
    }
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
}
