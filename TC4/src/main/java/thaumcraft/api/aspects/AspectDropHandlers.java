package thaumcraft.api.aspects;

import java.util.*;
import java.util.function.Function;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import tc4revival.aspect.Aspect;
import tc4revival.content.AspectOrbEntity;
import thaumcraft.api.util.ExtensionRegistry;

/** Plans transform already-rolled drops. The host alone delivers and accounts accepted quantities. */
public final class AspectDropHandlers {
    public record Drop(ServerLevel level,LivingEntity victim,Aspect aspect,int amount,Vec3 position,Vec3 velocity) {}
    public record Conversion(ItemStack item,int aspectCost,Inventory receiver,CompoundTag entityData) {
        public Conversion {
            item=item.copyWithCount(1);entityData=entityData.copy();
            if(item.isEmpty()||aspectCost<1)throw new IllegalArgumentException("Invalid aspect drop conversion");
        }
        @Override public ItemStack item(){return item.copy();}
        @Override public CompoundTag entityData(){return entityData.copy();}
    }
    private static final ExtensionRegistry<Function<Drop,Optional<Conversion>>> HANDLERS=new ExtensionRegistry<>();
    private AspectDropHandlers() {}
    /** Pure planner: do not insert, spawn, consume, or reroll inside the callback. */
    public static void registerHandler(String id,int priority,Function<Drop,Optional<Conversion>> handler){HANDLERS.register(id,priority,handler);}
    public static boolean spawn(ServerLevel level,LivingEntity victim,AspectOrbEntity original){
        int remaining=original.value();
        for(var entry:HANDLERS.entries()){
            if(remaining==0)break;
            Optional<Conversion> plan;
            try{plan=Objects.requireNonNull(entry.value().apply(new Drop(level,victim,original.aspect(),remaining,original.position(),original.getDeltaMovement())));}
            catch(RuntimeException failure){org.slf4j.LoggerFactory.getLogger(AspectDropHandlers.class).warn("Drop planner failed: "+entry.id(),failure);continue;}
            if(plan.isEmpty())continue;var conversion=plan.get();
            int items=remaining/conversion.aspectCost();
            while(items>0){
                var stack=conversion.item();int batch=Math.min(items,stack.getMaxStackSize());stack.setCount(batch);
                if(conversion.receiver()!=null){
                    var receiver=conversion.receiver();var before=new ArrayList<ItemStack>();
                    for(int slot=0;slot<receiver.getContainerSize();slot++)before.add(receiver.getItem(slot).copy());
                    try{receiver.add(stack);}
                    catch(RuntimeException failure){
                        // Nothing has spawned yet. Revert partial insertion before trying world delivery.
                        for(int slot=0;slot<before.size();slot++)receiver.setItem(slot,before.get(slot));
                        receiver.setChanged();stack=conversion.item();stack.setCount(batch);
                        org.slf4j.LoggerFactory.getLogger(AspectDropHandlers.class).warn("Drop insertion rolled back",failure);
                    }
                }
                int accepted=batch-stack.getCount();
                // Account inventory delivery before a spawn event can reject the remainder.
                remaining-=accepted*conversion.aspectCost();items-=accepted;
                if(!stack.isEmpty()){
                    var entity=new ItemEntity(level,original.getX(),original.getY(),original.getZ(),stack);
                    entity.getPersistentData().merge(conversion.entityData());
                    int delivered=stack.getCount();
                    boolean spawned;
                    try{spawned=level.addFreshEntity(entity);}
                    catch(RuntimeException failure){
                        // An event may throw after attachment. Never retry an entity already in the world.
                        spawned=level.getEntity(entity.getUUID())==entity;
                        org.slf4j.LoggerFactory.getLogger(AspectDropHandlers.class).warn("Drop spawn callback failed; attached={}",spawned,failure);
                    }
                    if(!spawned)break;
                    remaining-=delivered*conversion.aspectCost();items-=delivered;
                }
            }
        }
        if(remaining==0)return true;
        if(remaining==original.value())return level.addFreshEntity(original);
        var remainder=new AspectOrbEntity(level,original.getX(),original.getY(),original.getZ(),original.aspect(),remaining);
        remainder.setDeltaMovement(original.getDeltaMovement());return level.addFreshEntity(remainder);
    }
}
