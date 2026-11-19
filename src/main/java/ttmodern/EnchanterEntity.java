package ttmodern;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.content.*;
import tc4revival.aspect.*;

public final class EnchanterEntity extends BlockEntity {
    private final LinkedHashMap<ResourceLocation,Integer> selected=new LinkedHashMap<>();
    private final Map<String,Integer> total=new TreeMap<>(),paid=new TreeMap<>();
    private boolean working;private ItemStack locked=ItemStack.EMPTY;
    private boolean loading;
    public final ItemStackHandler items=new ItemStackHandler(2){
        @Override public int getSlotLimit(int slot){return 1;}
        @Override public boolean isItemValid(int slot,ItemStack stack){return slot==1?stack.getItem() instanceof CastingWandItem&&!WandParts.rod(stack.getTag()).staff():!working&&!stack.is(Items.BOOK)&&!stack.is(Items.ENCHANTED_BOOK)&&stack.getItem().getEnchantmentValue(stack)>0;}
        @Override protected void onContentsChanged(int slot){if(loading)return;if(slot==0&&!working){selected.clear();total.clear();paid.clear();}changed();}
    };
    private net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler> automation=net.minecraftforge.common.util.LazyOptional.of(this::automationInventory);
    private net.minecraftforge.items.IItemHandler automationInventory(){
        return new net.minecraftforge.items.wrapper.RangedWrapper(items,0,2){
            @Override public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){return working?stack:super.insertItem(slot,stack,simulate);}
            @Override public ItemStack extractItem(int slot,int amount,boolean simulate){return working?ItemStack.EMPTY:super.extractItem(slot,amount,simulate);}
            @Override public boolean isItemValid(int slot,ItemStack stack){return !working&&super.isItemValid(slot,stack);}
        };
    }
    @Override public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> cap,Direction side){
        if(cap==net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER)return isRemoved()?net.minecraftforge.common.util.LazyOptional.empty():automation.cast();
        return super.getCapability(cap,side);
    }
    @Override public void invalidateCaps(){super.invalidateCaps();automation.invalidate();}
    @Override public void reviveCaps(){super.reviveCaps();automation=net.minecraftforge.common.util.LazyOptional.of(this::automationInventory);}
    public EnchanterEntity(BlockPos pos,BlockState state){super(Tinkerer.ENCHANTER_ENTITY.get(),pos,state);}
    public boolean working(){return working;}
    public Map<ResourceLocation,Integer> selected(){return Collections.unmodifiableMap(selected);}
    public Map<String,Integer> costs(){return Collections.unmodifiableMap(total);}
    public Map<String,Integer> paid(){return Collections.unmodifiableMap(paid);}
    public boolean usable(Player player){return level!=null&&!isRemoved()&&level.getBlockEntity(worldPosition)==this&&player.distanceToSqr(worldPosition.getCenter())<=64;}
    public static boolean unlocked(ServerPlayer player,EnchanterCatalog.Entry entry){return entry.unlocked(research->thaumcraft.api.ThaumcraftApiHelper.isResearchComplete(player,research));}
    public boolean canSelect(ResourceLocation id,ServerPlayer player){
        var entry=EnchanterCatalog.entries().get(id);var enchant=ForgeRegistries.ENCHANTMENTS.getValue(id);var tool=items.getStackInSlot(0);
        if(entry==null||enchant==null||tool.isEmpty()||tool.isEnchanted()||!unlocked(player,entry)||!enchant.canEnchant(tool))return false;
        var existing=EnchantmentHelper.getEnchantments(tool);if(existing.containsKey(enchant))return false;
        for(var other:existing.keySet())if(!enchant.isCompatibleWith(other))return false;
        for(var other:selected.keySet())if(!other.equals(id)&&!enchant.isCompatibleWith(ForgeRegistries.ENCHANTMENTS.getValue(other)))return false;return true;
    }
    public boolean choose(ServerPlayer player,ResourceLocation id,int rank){
        if(working||!usable(player))return false;
        if(rank==0){selected.remove(id);recalculate();changed();return true;}
        var enchant=ForgeRegistries.ENCHANTMENTS.getValue(id);if(enchant==null||rank<1||rank>enchant.getMaxLevel()||!canSelect(id,player))return false;
        selected.put(id,rank);recalculate();changed();return true;
    }
    private void recalculate(){total.clear();for(var selection:selected.entrySet()){var entry=EnchanterCatalog.entries().get(selection.getKey());if(entry!=null)entry.cost(selection.getValue()).forEach((aspect,amount)->addPrimal(aspect,amount,total,0));}}
    private static void addPrimal(String name,int amount,Map<String,Integer> target,int depth){
        var catalog=TCAspects.catalog();
        catalog.toPrimals(new AspectAmounts().add(catalog.require(name),amount),AspectCatalog.Reduction.SUM)
            .view().forEach((aspect,n)->target.merge(aspect.tag(),n,Math::addExact));
    }
    public boolean start(ServerPlayer player){
        if(!usable(player)||working)return false;
        String failure=startFailure();
        if(failure!=null){player.displayClientMessage(net.minecraft.network.chat.Component.translatable(failure).withStyle(net.minecraft.ChatFormatting.RED),true);return false;}
        if(selected.keySet().stream().anyMatch(id->!canSelect(id,player)))return false;
        recalculate();if(total.isEmpty())return false;locked=items.getStackInSlot(0).copy();paid.clear();working=true;changed();return true;
    }
    public String startFailure(){
        if(selected.isEmpty())return "ttmodern.enchanter.select";
        if(pillars().size()<6)return "ttmodern.enchanter.pillars";
        return null;
    }
    public List<BlockPos> pillars(){
        var found=new ArrayList<BlockPos>();if(level==null)return found;
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++){
            var base=worldPosition.offset(x,0,z);if(!level.hasChunkAt(base))continue;
            for(int height=0;height<=12;height++){
                var at=base.above(height);var state=level.getBlockState(at);
                // Legacy blockCosmeticSolid metadata 0 is the totem, not the tile (metadata 1).
                if(state.is(TCArcane.OBSIDIAN_TOTEM.get()))continue;
                if(height>=2&&state.is(TCArcane.NITOR.get()))found.add(at);break;
            }
            if(found.size()==6)return found;
        }return found;
    }
    public void tick(){
        if(level==null||level.isClientSide||!working)return;
        var pillars=pillars();if(!ItemStack.matches(locked,items.getStackInSlot(0))||pillars.size()<6){working=false;paid.clear();locked=ItemStack.EMPTY;changed();return;}
        if(total.entrySet().stream().allMatch(e->paid.getOrDefault(e.getKey(),0)>=e.getValue())){
            var result=locked.copy();for(var e:selected.entrySet()){var enchant=ForgeRegistries.ENCHANTMENTS.getValue(e.getKey());if(enchant==null){working=false;changed();return;}result.enchant(enchant,e.getValue());}
            working=false;locked=ItemStack.EMPTY;items.setStackInSlot(0,result);selected.clear();total.clear();paid.clear();changed();level.playSound(null,worldPosition,TCSounds.WAND.get(),net.minecraft.sounds.SoundSource.BLOCKS,1,1);return;
        }
        var stack=items.getStackInSlot(1);if(!(stack.getItem() instanceof CastingWandItem wand)||WandParts.rod(stack.getTag()).staff())return;
        var candidates=total.keySet().stream().filter(a->paid.getOrDefault(a,0)<total.get(a)&&wand.vis(stack,TCAspects.catalog().require(a))>=100).toList();if(candidates.isEmpty())return;
        String tag=candidates.get(level.random.nextInt(candidates.size()));var aspect=TCAspects.catalog().require(tag);
        if(wand.consume(stack,Map.of(aspect,1),a->wand.consumptionModifier(stack,null,a,true),true,true,level)){
            paid.merge(tag,1,Integer::sum);changed();
            var pillar=pillars.get(level.random.nextInt(pillars.size()));var server=(net.minecraft.server.level.ServerLevel)level;
            if(level.random.nextBoolean()){
                var origin=pillar.getCenter();var altar=worldPosition.getCenter().add(0,.3,0);var offset=origin.subtract(altar);
                thaumcraft.api.fx.LegacyEffects.emit(level,"essentia",origin,altar,aspect.color(),1,20,1,0,0);
                thaumcraft.api.fx.LegacyEffects.emit(level,"rune",origin,net.minecraft.world.phys.Vec3.ZERO,aspect.color(),1,20,2,0,0);thaumcraft.api.fx.LegacyEffects.emit(level,"rune",altar,net.minecraft.world.phys.Vec3.ZERO,aspect.color(),1,20,2,0,0);
            }
        }
    }
    private void changed(){setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.put("Items",items.serializeNBT());tag.putBoolean("Working",working);tag.put("Locked",locked.save(new CompoundTag()));var choices=new CompoundTag();selected.forEach((id,rank)->choices.putInt(id.toString(),rank));tag.put("Choices",choices);var costs=new CompoundTag();total.forEach(costs::putInt);tag.put("Total",costs);var charged=new CompoundTag();paid.forEach(charged::putInt);tag.put("Paid",charged);}
    @Override public void load(CompoundTag tag){super.load(tag);loading=true;try{items.deserializeNBT(tag.getCompound("Items"));}finally{loading=false;}working=tag.getBoolean("Working");locked=ItemStack.of(tag.getCompound("Locked"));selected.clear();var choices=tag.getCompound("Choices");for(String id:choices.getAllKeys()){var key=ResourceLocation.tryParse(id);var enchant=key==null?null:ForgeRegistries.ENCHANTMENTS.getValue(key);int rank=choices.getInt(id);if(enchant!=null&&rank>=1&&rank<=enchant.getMaxLevel())selected.put(key,rank);else working=false;}total.clear();paid.clear();var costs=tag.getCompound("Total");var charge=tag.getCompound("Paid");for(String aspect:costs.getAllKeys()){int value=costs.getInt(aspect);if(value>0){total.put(aspect,value);paid.put(aspect,Math.max(0,Math.min(value,charge.getInt(aspect))));}}if(selected.isEmpty()||total.isEmpty())working=false;}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
}
