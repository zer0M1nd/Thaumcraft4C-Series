package thmodern.containment;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thmodern.Horizons;

public final class SoulJar {
    public static boolean contains(ItemStack stack){return stack.is(Horizons.CREATURE_JAR_ITEM.get())&&stack.hasTag()&&contains(stack.getTag());}
    public static boolean contains(CompoundTag tag){return tag.getBoolean("Soul")&&!tag.contains("Captured");}
    public static ItemStack create(ResourceLocation profession){
        var stack=new ItemStack(Horizons.CREATURE_JAR_ITEM.get());var tag=stack.getOrCreateTag();tag.putBoolean("Soul",true);if(profession!=null)tag.putString("Profession",profession.toString());return stack;
    }
    public static Villager body(ItemStack stack,Level level){
        if(!contains(stack))return null;var result=net.minecraft.world.entity.EntityType.VILLAGER.create(level);if(result==null)return null;
        var key=ResourceLocation.tryParse(stack.getTag().getString("Profession"));var profession=key==null?null:net.minecraft.core.registries.BuiltInRegistries.VILLAGER_PROFESSION.getOptional(key).orElse(null);
        result.setVillagerData(new VillagerData(VillagerType.PLAINS,profession==null?VillagerProfession.FARMER:profession,1));result.setPersistenceRequired();return result;
    }
}
