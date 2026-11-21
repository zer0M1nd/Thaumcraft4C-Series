package tc4revival.content;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public  class VoidTools {
    private VoidTools() {}
    public static Item create(VoidEquipment.Tool type) {
        return switch (type) {
            case PICKAXE -> new Pick(); case AXE -> new Axe(); case SHOVEL -> new Shovel();
            case HOE -> new Hoe(); case SWORD -> new Sword();
        };
    }
    private interface WarpingTool extends WarpingGear {
        @Override default int warp(ItemStack stack, Player wearer) { return 1; }
    }
    private static  class Pick extends ThaumiumTools.Pick implements WarpingTool {
        Pick() { super(VoidToolTier.INSTANCE); }
        @Override public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) { VoidEquipment.repair(stack, holder); }
        @Override public boolean onLeftClickEntity(ItemStack stack, Player player, Entity target) { VoidEquipment.weaken(target, player, 80); return false; }
    }
    private static  class Axe extends ThaumiumTools.Axe implements WarpingTool {
        Axe() { super(VoidToolTier.INSTANCE); }
        @Override public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) { VoidEquipment.repair(stack, holder); }
        @Override public boolean onLeftClickEntity(ItemStack stack, Player player, Entity target) { VoidEquipment.weaken(target, player, 80); return false; }
    }
    private static  class Shovel extends ThaumiumTools.Shovel implements WarpingTool {
        Shovel() { super(VoidToolTier.INSTANCE); }
        @Override public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) { VoidEquipment.repair(stack, holder); }
        @Override public boolean onLeftClickEntity(ItemStack stack, Player player, Entity target) { VoidEquipment.weaken(target, player, 80); return false; }
    }
    private static  class Hoe extends ThaumiumTools.Hoe implements WarpingTool {
        Hoe() { super(VoidToolTier.INSTANCE); }
        @Override public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) { VoidEquipment.repair(stack, holder); }
        @Override public boolean onLeftClickEntity(ItemStack stack, Player player, Entity target) { VoidEquipment.weaken(target, player, 80); return false; }
    }
    private static  class Sword extends ThaumiumSwordItem implements WarpingTool {
        Sword() { super(VoidToolTier.INSTANCE); }
        @Override public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) { VoidEquipment.repair(stack, holder); }
        @Override public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
            VoidEquipment.weaken(target, attacker, 60);
            return super.hurtEnemy(stack, target, attacker);
        }
        @Override public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
            lines.add(Component.translatable("enchantment.special.sapless").withStyle(ChatFormatting.GOLD));
            super.appendHoverText(stack, level, lines, flag);
        }
    }
}
