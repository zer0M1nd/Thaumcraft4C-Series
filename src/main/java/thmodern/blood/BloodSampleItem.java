package thmodern.blood;

import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;

public final class BloodSampleItem extends Item {
    public BloodSampleItem(){super(new Properties());}
    public static boolean eligible(LivingEntity entity){
        if(!(entity instanceof Mob)||entity instanceof net.minecraft.world.entity.npc.AbstractVillager||entity.getMobType()==MobType.UNDEAD)return false;
        var type=entity.getType().getCategory();
        return type==MobCategory.CREATURE||type==MobCategory.AMBIENT||type==MobCategory.WATER_CREATURE||type==MobCategory.WATER_AMBIENT||type==MobCategory.UNDERGROUND_WATER_CREATURE||type==MobCategory.AXOLOTLS;
    }
    public static CompoundTag genetics(LivingEntity entity){
        var tag=new CompoundTag();entity.saveWithoutId(tag);tag.putString("id",net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString());
        // Captured genes cannot carry equipment, cargo, passengers, active effects or another entity's UUID.
        for(String key:java.util.List.of("UUID","Pos","Motion","Rotation","Passengers","Leash","Items","Inventory","HandItems","ArmorItems","ArmorItem","SaddleItem","Saddle","ChestedHorse","HandDropChances","ArmorDropChances","ActiveEffects","Brain","ForgeCaps","FallDistance","Fire","Air","DeathTime","HurtTime","HurtByTimestamp","Health","PortalCooldown","LootTable","DeathLootTable","DeathLootTableSeed"))tag.remove(key);
        stripItems(tag);return tag;
    }
    private static void stripItems(CompoundTag tag){
        for(var key:java.util.List.copyOf(tag.getAllKeys())){
            var value=tag.get(key);
            if(value instanceof CompoundTag compound){if(compound.contains("id",Tag.TAG_STRING)&&compound.contains("Count",Tag.TAG_ANY_NUMERIC))tag.remove(key);else stripItems(compound);}
            else if(value instanceof ListTag list){for(int i=list.size()-1;i>=0;i--)if(list.get(i) instanceof CompoundTag compound){if(compound.contains("id",Tag.TAG_STRING)&&compound.contains("Count",Tag.TAG_ANY_NUMERIC))list.remove(i);else stripItems(compound);}}
        }
    }
    @Override public Component getName(ItemStack stack){
        var name=super.getName(stack);return stack.hasTag()?Component.empty().append(name).append(": ").append(stack.getTag().getString("critterName")):name;
    }
    @Override public void appendHoverText(ItemStack stack,net.minecraft.world.level.Level level,java.util.List<Component> lines,TooltipFlag flag){lines.add(Component.translatable("thaumichorizons.sample.clone_cost"));}
}
