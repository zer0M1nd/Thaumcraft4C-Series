package tc4revival.content;

import net.minecraft.world.item.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.Level;

/** Elemental ammunition participates in native bow and crossbow selection. */
public class PrimalArrowItem extends ArrowItem {
    private final int element;
    public PrimalArrowItem(int element) { super(new Properties());this.element=element; }
    public int element() { return element; }
    @Override public AbstractArrow createArrow(Level level,ItemStack stack,LivingEntity shooter) { return PrimalArrowEntity.create(shooter,element); }
    @Override public boolean isInfinite(ItemStack stack,ItemStack bow,Player player) {
        return bow.getEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.INFINITY_ARROWS)>0;
    }
}
