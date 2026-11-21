package gadomancymodern.disguise;

import net.minecraft.nbt.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionUtils;

public class ArmorDisguise {
    public static boolean disguised(ItemStack stack){return stack.hasTag()&&stack.getTag().contains("gadomancy",Tag.TAG_COMPOUND)&&stack.getTag().getCompound("gadomancy").contains("disguise");}
    public static boolean armor(ItemStack stack){return !stack.isEmpty()&&net.minecraft.world.entity.Mob.getEquipmentSlotForItem(stack).getType()==EquipmentSlot.Type.ARMOR;}
    public static boolean invisible(ItemStack stack){return stack.getItem() instanceof PotionItem&&PotionUtils.getMobEffects(stack).stream().anyMatch(effect->effect.getEffect()==net.minecraft.world.effect.MobEffects.INVISIBILITY);}
    public static boolean valid(ItemStack armor,ItemStack disguise){return !disguised(armor)&&!disguised(disguise)&&armor(armor)&&(invisible(disguise)||armor(disguise)&&net.minecraft.world.entity.Mob.getEquipmentSlotForItem(armor)==net.minecraft.world.entity.Mob.getEquipmentSlotForItem(disguise));}
    public static ItemStack apply(ItemStack armor,ItemStack disguise){
        var result=armor.copy();var data=result.getOrCreateTag().getCompound("gadomancy");
        if(invisible(disguise))data.putBoolean("disguise",true);else data.put("disguise",disguise.copyWithCount(1).save(new CompoundTag()));result.getOrCreateTag().put("gadomancy",data);return result;
    }
    public static ItemStack appearance(ItemStack stack){if(!disguised(stack))return stack;var tag=stack.getTag().getCompound("gadomancy");return tag.contains("disguise",Tag.TAG_COMPOUND)?ItemStack.of(tag.getCompound("disguise")):ItemStack.EMPTY;}
    public static ItemStack remove(ItemStack stack){
        if(!disguised(stack))return ItemStack.EMPTY;var returned=appearance(stack);var data=stack.getTag().getCompound("gadomancy");data.remove("disguise");if(data.isEmpty())stack.getTag().remove("gadomancy");if(stack.getTag().isEmpty())stack.setTag(null);return returned;
    }
}
