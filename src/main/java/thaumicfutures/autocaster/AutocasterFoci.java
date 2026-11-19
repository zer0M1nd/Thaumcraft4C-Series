package thaumicfutures.autocaster;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import tc4revival.content.*;

/** Data-pack extensible attack set. Special cases mirror the maid integration's actual focus contracts. */
public final class AutocasterFoci {
    public static final TagKey<Item> ATTACK=ItemTags.create(ResourceLocation.parse("thaumicfutures:autocaster_attack_foci"));
    public static String id(ItemStack focus) { return net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(focus.getItem()).toString(); }
    private static int upgrade(ItemStack focus,String id) {
        if(focus.getItem() instanceof WandFocusItem item)
            for(var upgrade:FocusUpgrade.values())if(upgrade.key.equals(id))return item.upgradeLevel(focus,upgrade.id);
        return 0;
    }
    public static boolean offensive(ItemStack focus) {
        if(!(focus.getItem() instanceof WandFocusItem))return false;
        return switch(id(focus)) {
            case "thaumicbases:focus_experience"->upgrade(focus,"thaumicbases:decomposing")>0;
            default->focus.is(ATTACK);
        };
    }
    public static double range(ItemStack focus) {
        if(!offensive(focus))return 0;
        return switch(id(focus)) {
            case "thaumcraft:focus_fire"->((WandFocusItem)focus.getItem()).upgradeLevel(focus,FocusUpgrade.FIREBALL.id)>0?20:10;
            case "taintedmagic:focus_mage_mace"->3;
            case "taintedmagic:focus_shockwave"->6;
            case "thaumichorizons:focus_disintegration", "thaumichorizons:focus_liquefaction"->9;
            case "thaumicbases:focus_experience"->1.2;
            case "thaumicfutures:focus_shard", "taintedmagic:focus_vis_shard", "taintedmagic:focus_taint_swarm", "thaumcraft:focus_hellbat"->32;
            default->20;
        };
    }
    public static boolean suitable(ItemStack focus,LivingEntity target) {
        if(!offensive(focus))return false;
        return switch(id(focus)) {
            case "thaumcraft:focus_fire"->!target.fireImmune();
            case "thaumichorizons:focus_disintegration"->target instanceof Mob;
            case "thaumichorizons:focus_liquefaction"->target instanceof Mob && !target.fireImmune();
            case "thaumicbases:focus_experience"->!(target instanceof Player)
                    && !(target instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon)
                    && !(target instanceof net.minecraft.world.entity.boss.wither.WitherBoss)
                    && !target.getType().is(TagKey.create(Registries.ENTITY_TYPE,ResourceLocation.parse("forge:bosses")))
                    && !target.getType().is(TagKey.create(Registries.ENTITY_TYPE,ResourceLocation.parse("thaumicbases:decomposition_blacklist")));
            case "thaumicconcilium:cooling_focus"->upgrade(focus,"thaumicconcilium:refreeze")>0 || target instanceof net.minecraft.world.entity.monster.Blaze;
            default->true;
        };
    }
    public static boolean acceptsWand(ItemStack stack) { return stack.getItem() instanceof CastingWandItem wand && !wand.isSceptre(stack); }
    public static boolean acceptsBattery(ItemStack stack) {
        return net.minecraftforge.fml.ModList.get().isLoaded("ic2") && Power.accepts(stack);
    }
    public static boolean hasPower(ItemStack focus,ItemStack battery) {
        return !id(focus).equals("emt:energy_ball_focus") || acceptsBattery(battery) && Power.available(battery);
    }
    /** Lazy linkage keeps IC2 optional; the core itself performs the EU payment. */
    private static class Power {
        static boolean accepts(ItemStack stack) { return !stack.isEmpty() && stack.getItem() instanceof ic2.api.item.IElectricItem; }
        static boolean available(ItemStack stack) { return ic2.api.item.ElectricItem.manager.discharge(stack,5120,4,true,false,true)>=5120; }
    }
    private AutocasterFoci() {}
}
