package tc4revival.content;

import java.util.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;

/** Replaces only the armor stage for players wearing fortress armor; later effects and enchantments remain intact. */
public  class FortressProtection {
    public static final net.minecraft.tags.TagKey<DamageType> MAGIC=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,net.minecraft.resources.ResourceLocation.parse("forge:is_magic"));
    private FortressProtection() {}
    public static boolean applies(LivingEntity wearer) {
        for(var stack:wearer.getArmorSlots())if(thaumcraft.api.equipment.ArmorProtectionProviders.applies(stack))return true;
        if (!(wearer instanceof Player)) return false;
        for (var stack:wearer.getArmorSlots()) if (stack.getItem() instanceof FortressArmorItem || stack.getItem() instanceof VoidRobeArmorItem || thaumcraft.api.equipment.ArmorProtectionProviders.applies(stack)) return true;
        return false;
    }
    public static float absorb(LivingEntity wearer,DamageSource source,float damage) {
        if (damage<=0) return damage;
        var slots=new EquipmentSlot[]{EquipmentSlot.FEET,EquipmentSlot.LEGS,EquipmentSlot.CHEST,EquipmentSlot.HEAD};
        double multiplier=.875;
        for (int i=1;i<slots.length;i++) {
            var stack=wearer.getItemBySlot(slots[i]);
            if (stack.getItem() instanceof FortressArmorItem) { multiplier+=.125; if (stack.hasTag() && stack.getTag().contains("mask")) multiplier+=.05; }
        }
        int fortressCount=0,masks=0;
        for(var slot:slots){
            var stack=wearer.getItemBySlot(slot);
            if(stack.getItem() instanceof FortressArmorItem){fortressCount++;if(stack.hasTag()&&stack.getTag().contains("mask"))masks++;}
            multiplier+=thaumcraft.api.equipment.ArmorProtectionProviders.fortressSetBonus(slot,stack);
        }
        boolean magic=source.is(MAGIC);
        boolean elemental=source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_EXPLOSION);
        boolean bypass=source.is(DamageTypeTags.BYPASSES_ARMOR);
        var pieces=new ArrayList<thaumcraft.api.equipment.ArmorProtectionProviders.SlotProtection>();
        var protection=new java.util.HashMap<Integer,thaumcraft.api.equipment.ArmorProtectionProviders.Protection>();
        for (int i=0;i<slots.length;i++) {
            var stack=wearer.getItemBySlot(slots[i]); if (!(stack.getItem() instanceof ArmorItem armor)) continue;
            var extension=thaumcraft.api.equipment.ArmorProtectionProviders.protection(
                new thaumcraft.api.equipment.ArmorProtectionProviders.Context(wearer,source,slots[i],stack,magic,elemental,bypass,multiplier,fortressCount,masks));
            if(extension.isPresent()){
                var supplied=extension.get();protection.put(i,supplied);
                pieces.add(new thaumcraft.api.equipment.ArmorProtectionProviders.SlotProtection(i,supplied));continue;
            }
            boolean fortress=armor instanceof FortressArmorItem;
            boolean robe=armor instanceof VoidRobeArmorItem;
            if (!fortress && !robe && bypass) continue;
            int priority=(fortress && (magic || elemental)) || (robe && magic)?1:0;
            double ratio=armor.getDefense()/25d;
            if (fortress) ratio=(magic?armor.getDefense()/35d:elemental?armor.getDefense()/20d:bypass?0:ratio)*multiplier;
            else if (robe) ratio=magic?armor.getDefense()/35d:bypass?0:ratio;
            var supplied=new thaumcraft.api.equipment.ArmorProtectionProviders.Protection(priority,ratio,Math.max(0,stack.getMaxDamage()+1-stack.getDamageValue()),
                (fortress||robe)&&source.is(DamageTypes.FALL),false);
            protection.put(i,supplied);pieces.add(new thaumcraft.api.equipment.ArmorProtectionProviders.SlotProtection(i,supplied));
        }
        var result=thaumcraft.api.equipment.ArmorProtectionProviders.settle(damage,pieces);
        if(protection.values().stream().anyMatch(thaumcraft.api.equipment.ArmorProtectionProviders.Protection::extinguish))wearer.clearFire();
        for (var wear:result.wear().entrySet()) {
            var slot=slots[wear.getKey()]; var stack=wearer.getItemBySlot(slot);
            if(protection.get(wear.getKey()).skipWear())continue;
            stack.hurtAndBreak(wear.getValue(),wearer,entity->entity.broadcastBreakEvent(slot));
        }
        return result.remaining();
    }
}
