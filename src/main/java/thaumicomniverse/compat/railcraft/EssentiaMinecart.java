package thaumicomniverse.compat.railcraft;

import mods.railcraft.world.entity.vehicle.RailcraftMinecart;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import tc4revival.aspect.*;
import tc4revival.content.*;
import thaumcraft.api.aspects.IEssentiaStorage;
import thaumicomniverse.RevivalFeatures;

public class EssentiaMinecart extends RailcraftMinecart implements IEssentiaStorage,EssentiaSource {
    private static final EntityDataAccessor<CompoundTag> CONTENTS=SynchedEntityData.defineId(EssentiaMinecart.class,EntityDataSerializers.COMPOUND_TAG);
    public final RailEssentiaTank tank=new RailEssentiaTank(64,this::changed);
    public EssentiaMinecart(EntityType<?> type,Level level){super(type,level);}
    public EssentiaMinecart(ItemStack stack,double x,double y,double z,Level level){
        super(stack,ArcaneRail.CART.get(),x,y,z,level);loadFromItemStack(stack);
    }
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(CONTENTS,new CompoundTag());}
    private void changed(){if(!level().isClientSide)entityData.set(CONTENTS,tank.save());}
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key){super.onSyncedDataUpdated(key);if(CONTENTS.equals(key)&&level().isClientSide&&tank!=null)tank.load(entityData.get(CONTENTS));}
    @Override protected void loadFromItemStack(ItemStack stack){if(stack.hasTag())tank.load(stack.getTag());changed();}
    @Override protected void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.put("Essentia",tank.save());}
    @Override protected void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);tank.load(tag.getCompound("Essentia"));changed();}
    @Override public ItemStack getPickResult(){var result=super.getPickResult();result.getOrCreateTag().merge(tank.save());return result;}
    @Override protected Item getDropItem(){return ArcaneRail.CART_ITEM.get();}
    @Override public Type getMinecartType(){return Type.CHEST;}
    @Override public net.minecraft.world.level.block.state.BlockState getDefaultDisplayBlockState(){return TCArcane.ESSENTIA_JAR.get().defaultBlockState();}
    @Override public int getContainerSize(){return 0;}
    @Override protected boolean hasMenu(){return false;}
    @Override protected AbstractContainerMenu createMenu(int id,Inventory inventory){return null;}
    @Override public InteractionResult interact(Player player,InteractionHand hand){
        var held=player.getItemInHand(hand);var type=tank.aspect();boolean filled=held.is(TCItems.ESSENTIA_PHIAL.get()),empty=held.is(TCItems.EMPTY_PHIAL.get());
        if(filled){var content=TCItems.ESSENTIA_PHIAL.get().storedAspects(held);type=content.size()==1?content.aspects().get(0):null;}
        if((filled||empty)&&type!=null&&(filled?insert(type,8,true):extract(type,8,true))==8){
            if(!level().isClientSide){
                if(filled)insert(type,8,false);else extract(type,8,false);
                held.shrink(1);var result=filled?new ItemStack(TCItems.EMPTY_PHIAL.get()):EssentiaPhialItem.filled(type);
                if(!player.getInventory().add(result))player.drop(result,false);
                player.containerMenu.broadcastChanges();
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if(!level().isClientSide)player.displayClientMessage(Component.translatable("thaumicomniverse.rail.contents",tank.aspect()==null?Component.translatable("omniverse.machine.empty"):Component.literal(tank.aspect().displayName()),tank.amount(),tank.limit()),true);
        return InteractionResult.sidedSuccess(level().isClientSide);
    }
    @Override public AspectAmounts contents(){return tank.contents();}
    @Override public int capacity(Aspect aspect){return tank.capacity(aspect);}
    @Override public int insert(Aspect aspect,int count,boolean simulate){return RevivalFeatures.Group.RAILCRAFT.enabled()?tank.insert(aspect,count,simulate):0;}
    @Override public int extract(Aspect aspect,int count,boolean simulate){return tank.extract(aspect,count,simulate);}
    @Override public boolean contains(Aspect aspect,int count){return RevivalFeatures.Group.RAILCRAFT.enabled()&&count>=0&&tank.extract(aspect,count,true)==count;}
    @Override public boolean takeFromContainer(Aspect aspect,int count){if(!contains(aspect,count))return false;tank.extract(aspect,count,false);return true;}
    @Override public int availableAmount(Aspect aspect){return RevivalFeatures.Group.RAILCRAFT.enabled()?tank.extract(aspect,Integer.MAX_VALUE,true):0;}
    @Override public Object availabilityIdentity(){return getUUID();}
}
