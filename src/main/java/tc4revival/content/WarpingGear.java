package tc4revival.content;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Equipment contributes to effective warp without changing saved permanent/sticky warp. */
public interface WarpingGear {
    int warp(ItemStack stack, Player wearer);
    /** Addons may override this for a weapon/tool whose effective hand is different. */
    default boolean warpAppliesInSlot(ItemStack stack,net.minecraft.world.entity.EquipmentSlot slot) {
        var item=stack.getItem();
        boolean tool=item instanceof net.minecraft.world.item.TieredItem
                ||item instanceof net.minecraft.world.item.ProjectileWeaponItem
                ||item instanceof net.minecraft.world.item.TridentItem
                ||item instanceof net.minecraft.world.item.ShearsItem
                ||!item.getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.MAINHAND,stack)
                    .get(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).isEmpty();
        return !tool||slot==net.minecraft.world.entity.EquipmentSlot.MAINHAND;
    }
    static int contribution(ItemStack stack, Player wearer) {
        return stack.getItem() instanceof WarpingGear gear ? gear.warp(stack, wearer) : 0;
    }
    static int equipped(Player wearer) {
        return (int)wearer.getAttributeValue(TCAttributes.WARP.get());
    }
}
