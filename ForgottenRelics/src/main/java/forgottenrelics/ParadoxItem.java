package forgottenrelics;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public class ParadoxItem extends SwordItem
        implements thaumcraft.api.IWarpingGear, thaumcraft.api.IRepairable {
    private static final Tier MATERIAL =
            new Tier() {
                public int getUses() {
                    return 3000;
                }

                public float getSpeed() {
                    return 16;
                }

                public float getAttackDamageBonus() {
                    return -4;
                }

                public int getLevel() {
                    return 4;
                }

                public int getEnchantmentValue() {
                    return 100;
                }

                public net.minecraft.world.item.crafting.Ingredient getRepairIngredient() {
                    return net.minecraft.world.item.crafting.Ingredient.EMPTY;
                }
            };

    public ParadoxItem() {
        super(MATERIAL, 3, -2.4f, new Properties().rarity(Rarity.EPIC));
    }

    @Override
    public int getWarp(ItemStack stack, Player player) {
        return 8;
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity target) {
        if (!player.level().isClientSide
                && (!(target instanceof LivingEntity living) || Relics.canAffect(player, living))) {
            float cap = (float) RelicsConfig.value("paradoxDamageCap"),
                    damage = player.getRandom().nextFloat() * cap;
            var source = player.damageSources().playerAttack(player);
            target.hurt(source, damage);
            player.hurt(source, cap - damage);
        }
        return true;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Level level,
            java.util.List<net.minecraft.network.chat.Component> lines,
            TooltipFlag flag) {
        lines.add(
                net.minecraft.network.chat.Component.translatable("tooltip.forgottenrelics.paradox")
                        .withStyle(net.minecraft.ChatFormatting.GRAY));
    }

    @Override
    public void inventoryTick(
            ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide && entity.tickCount % 20 == 0 && stack.isDamaged())
            stack.setDamageValue(stack.getDamageValue() - 1);
    }
}
