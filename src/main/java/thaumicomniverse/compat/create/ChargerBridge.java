package thaumicomniverse.compat.create;

import java.util.*;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.*;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import tc4revival.content.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class ChargerBridge extends SmartBlockEntity implements thaumcraft.api.devices.DeviceExtensions.Component {
    private static final String PAYMENT="OmniverseArcaneBelt";
    private final WorkbenchChargerBlockEntity source;
    private ItemStack cachedInput=ItemStack.EMPTY;
    private ArcaneRecipe cachedRecipe;
    private int cachedSlot;
    private boolean searched;
    private long searchedAt;
    public ChargerBridge(WorkbenchChargerBlockEntity source) { super(source.getType(),source.getBlockPos(),source.getBlockState());this.source=source;setLevel(source.getLevel()); }
    @Override public void extensionAttached(){setLevel(source.getLevel());}
    @Override public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        behaviours.add(new BeltProcessingBehaviour(this).whenItemEnters(this::charge).whileItemHeld(this::charge));
    }
    private BeltProcessingBehaviour.ProcessingResult charge(TransportedItemStack item,TransportedItemStackHandlerBehaviour handler) {
        if(source.getLevel()==null||source.isRemoved()||!source.extendedSupport()||!thaumicomniverse.RevivalFeatures.Group.CREATE.enabled()
                )return BeltProcessingBehaviour.ProcessingResult.PASS;
        setLevel(source.getLevel());
        var craft=craft(item,handler);
        if(craft!=BeltProcessingBehaviour.ProcessingResult.PASS)return craft;
        if(!source.getLevel().isClientSide){
            boolean changed=VisCharging.charge(item.stack,source);
            var data=source.getPersistentData();
            if(!data.contains("OmniverseRepairVis"))data.put("OmniverseRepairVis",new CompoundTag());
            changed|=VisItemRepair.repair(item.stack,source.getLevel(),null,data.getCompound("OmniverseRepairVis"),source::consumeVis);
            source.setChanged();
            if(changed)handler.blockEntity.notifyUpdate();
        }
        return VisCharging.needsCharge(item.stack)||VisItemRepair.needsRepair(item.stack)
                ?BeltProcessingBehaviour.ProcessingResult.HOLD:BeltProcessingBehaviour.ProcessingResult.PASS;
    }
    private static ItemStack clean(ItemStack stack){
        var copy=stack.copyWithCount(1);
        if(copy.hasTag()){copy.getTag().remove(PAYMENT);if(copy.getTag().isEmpty())copy.setTag(null);}
        return copy;
    }
    private CraftingContainer grid(ItemStack stack,int slot){
        var menu=new AbstractContainerMenu(null,-1){
            @Override public ItemStack quickMoveStack(Player player,int index){return ItemStack.EMPTY;}
            @Override public boolean stillValid(Player player){return false;}
        };
        var grid=new TransientCraftingContainer(menu,3,3);grid.setItem(slot,stack);return grid;
    }
    private ArcaneRecipe recipe(ItemStack input){
        if(searched&&(cachedRecipe!=null||level.getGameTime()-searchedAt<20)&&ItemStack.isSameItemSameTags(cachedInput,input)
                &&(cachedRecipe==null||level.getRecipeManager().byKey(cachedRecipe.getId()).orElse(null)==cachedRecipe))return cachedRecipe;
        searched=true;searchedAt=level.getGameTime();cachedInput=input.copy();cachedRecipe=null;
        var recipes=new ArrayList<>(level.getRecipeManager().getAllRecipesFor(TCArcane.RECIPE_TYPE.get()));
        recipes.sort(Comparator.comparing(r->r.getId().toString()));
        for(var recipe:recipes)for(int slot=0;slot<9;slot++){
            var grid=grid(input,slot);
            if(recipe.matches(grid,level)&&!recipe.assemble(grid,level.registryAccess()).isEmpty()){
                cachedRecipe=recipe;cachedSlot=slot;return recipe;
            }
        }
        return null;
    }
    private BeltProcessingBehaviour.ProcessingResult craft(TransportedItemStack input,TransportedItemStackHandlerBehaviour handler){
        var stack=clean(input.stack);var recipe=recipe(stack);
        if(recipe==null)return BeltProcessingBehaviour.ProcessingResult.PASS;
        if(level.isClientSide)return BeltProcessingBehaviour.ProcessingResult.HOLD;
        var grid=grid(stack,cachedSlot);var costs=recipe.visCost(grid);
        // The relay owns reserved centivis. Putting credit on a stack would duplicate it when split.
        var stored=source.getPersistentData();
        if(!stored.contains(PAYMENT))stored.put(PAYMENT,new CompoundTag());
        var payment=stored.getCompound(PAYMENT);
        boolean ready=true,changed=false;
        for(var entry:costs.entrySet()){
            int total=tc4revival.content.ArcaneRecipe.hundredths(costs,entry.getKey()),paid=Math.max(0,payment.getInt(entry.getKey().tag()));
            int amount=source.consumeVis(entry.getKey(),Math.min(5,Math.max(0,total-paid)));
            if(amount>0){paid+=amount;payment.putInt(entry.getKey().tag(),paid);changed=true;}
            ready&=paid>=total;
        }
        if(changed)source.setChanged();
        if(!ready)return BeltProcessingBehaviour.ProcessingResult.HOLD;
        var outputs=new ArrayList<TransportedItemStack>();
        var result=input.copy();result.stack=recipe.assemble(grid,level.registryAccess());result.clearFanProcessingData();outputs.add(result);
        for(var remainder:recipe.getRemainingItems(grid))if(!remainder.isEmpty()){
            var extra=input.copy();extra.stack=remainder;extra.clearFanProcessingData();outputs.add(extra);
        }
        for(var entry:costs.entrySet())payment.putInt(entry.getKey().tag(),payment.getInt(entry.getKey().tag())-tc4revival.content.ArcaneRecipe.hundredths(costs,entry.getKey()));
        source.setChanged();
        input.stack.shrink(1);
        if(input.stack.hasTag()){input.stack.getTag().remove(PAYMENT);if(input.stack.getTag().isEmpty())input.stack.setTag(null);}
        var remaining=input.stack.isEmpty()?null:input.copy();
        handler.handleProcessingOnItem(input,TransportedItemStackHandlerBehaviour.TransportedResult.convertToAndLeaveHeld(outputs,remaining));
        handler.blockEntity.notifyUpdate();
        return BeltProcessingBehaviour.ProcessingResult.HOLD;
    }
}
