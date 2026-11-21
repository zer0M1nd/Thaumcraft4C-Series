package advancedthaumaturgy;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import java.util.List;

public class CrystalItem extends Item {
    public enum Upgrade { NONE, RECHARGE, COMPOUND, MULTIPLIER, STABILIZER, DISCOUNT, POTENCY }
    public final int color;
    public final Upgrade upgrade;
    public CrystalItem(int color, Upgrade upgrade) { super(new Properties()); this.color = color; this.upgrade = upgrade; }
    public static boolean has(ItemStack wand, Upgrade upgrade) {
        return upgrade != Upgrade.NONE && wand.hasTag() && wand.getTag().getCompound("advancedthaumaturgy").getBoolean(upgrade.name().toLowerCase(java.util.Locale.ROOT));
    }
    public static boolean enabled(ItemStack wand, Upgrade upgrade) { return ATConfig.enabled("wand_upgrades") && has(wand, upgrade); }
    public static boolean apply(ItemStack wand, Upgrade upgrade) {
        if (!(wand.getItem() instanceof MercurialWandItem) || upgrade == Upgrade.NONE || has(wand, upgrade)) return false;
        wand.getOrCreateTagElement("advancedthaumaturgy").putBoolean(upgrade.name().toLowerCase(java.util.Locale.ROOT), true);
        return true;
    }
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        if (upgrade != Upgrade.NONE) lines.add(Component.translatable("advancedthaumaturgy.upgrade." + upgrade.name().toLowerCase(java.util.Locale.ROOT)).withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
