package thmodern.vat;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import thmodern.Horizons;
import thmodern.blood.BloodSampleItem;

public final class VatClone {
    public static boolean input(ItemStack stack){return sample(stack)||stack.is(Horizons.CORPSE_EFFIGY.get());}
    public static boolean sample(ItemStack stack){return stack.is(Horizons.BLOOD_SAMPLE.get())||stack.is(Items.BEEF)||stack.is(Items.PORKCHOP)||stack.is(Items.CHICKEN);}
    public static Map<String,Integer> costs(ItemStack stack){
        var costs=new LinkedHashMap<String,Integer>();costs.put("victus",4);
        // Modern creature infusions persist their paid cloning surcharge with the genes.
        var extra=(stack.hasTag()?stack.getTag():new CompoundTag()).getCompound("critter").getCompound("ForgeData").getCompound("thaumichorizons:infusions").getCompound("CloneCosts");
        for(var key:extra.getAllKeys()){
            int count=extra.getInt(key);
            if(count<0||count>32763||(!costs.containsKey(key)&&costs.size()>=128))throw new IllegalArgumentException("Invalid clone surcharge");
            if(count>0)costs.merge(key,count,Integer::sum);
        }
        return costs;
    }
    /** Prepare before consuming anything. The returned subject is not added to the world. */
    public static LivingEntity prepare(Level level,ItemStack stack){
        Entity entity;
        if(stack.is(Horizons.BLOOD_SAMPLE.get())){
            if(!stack.hasTag())return null;var tag=stack.getTag().getCompound("critter").copy();
            if(tag.isEmpty()||tag.sizeInBytes()>65536)return null;
            tag.remove("UUID");tag.remove("Passengers");
            entity=EntityType.create(tag,level).orElse(null);
            if(!(entity instanceof LivingEntity living)||!BloodSampleItem.eligible(living))return null;
            // Sanitize again on use: commands or third-party mods may have authored this stack.
            entity=EntityType.create(BloodSampleItem.genetics(living),level).orElse(null);
        }else if(stack.is(Items.BEEF))entity=EntityType.COW.create(level);
        else if(stack.is(Items.PORKCHOP))entity=EntityType.PIG.create(level);
        else if(stack.is(Items.CHICKEN))entity=EntityType.CHICKEN.create(level);
        else return null;
        if(!(entity instanceof LivingEntity subject))return null;
        subject.setUUID(UUID.randomUUID());subject.setHealth(subject.getMaxHealth());
        if(subject instanceof TamableAnimal pet){pet.setTame(false);pet.setOwnerUUID(null);pet.setOrderedToSit(false);}
        if(subject instanceof net.minecraft.world.entity.animal.horse.AbstractHorse horse){horse.setTamed(false);horse.setOwnerUUID(null);}
        return subject;
    }
}
