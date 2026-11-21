package forgottenrelics;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

import vazkii.botania.api.mana.ManaItemHandler;

public class TerrorCrownItem extends ArmorItem
        implements thaumcraft.api.IWarpingGear, thaumcraft.api.IGoggles {
    private static final ArmorMaterial NOBLE_GOLD =
            new ArmorMaterial() {
                public int getDurabilityForType(Type type) {
                    return switch (type) {
                        case HELMET -> 440;
                        case CHESTPLATE -> 640;
                        case LEGGINGS -> 600;
                        case BOOTS -> 520;
                    };
                }

                public int getDefenseForType(Type type) {
                    return switch (type) {
                        case HELMET, BOOTS -> 3;
                        case CHESTPLATE -> 9;
                        case LEGGINGS -> 6;
                    };
                }

                public int getEnchantmentValue() {
                    return 0;
                }

                public net.minecraft.sounds.SoundEvent getEquipSound() {
                    return net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_GOLD;
                }

                public net.minecraft.world.item.crafting.Ingredient getRepairIngredient() {
                    return net.minecraft.world.item.crafting.Ingredient.of(Items.GOLD_INGOT);
                }

                public String getName() {
                    return "forgottenrelics:noble_gold";
                }

                public float getToughness() {
                    return 0;
                }

                public float getKnockbackResistance() {
                    return 0;
                }
            };

    public TerrorCrownItem() {
        super(NOBLE_GOLD, Type.HELMET, new Properties().rarity(Rarity.EPIC));
    }

    @Override
    public int getWarp(ItemStack stack, Player player) {
        return 3;
    }

    @Override
    public boolean showNodes(ItemStack stack, LivingEntity wearer) {
        return true;
    }

    @Override
    public boolean showIngamePopups(ItemStack stack, LivingEntity wearer) {
        return true;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canApplyAtEnchantingTable(
            ItemStack stack, net.minecraft.world.item.enchantment.Enchantment enchantment) {
        return false;
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return false;
    }

    @Override
    public boolean isValidRepairItem(ItemStack armor, ItemStack material) {
        return material.is(Items.GOLD_INGOT);
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return "forgottenrelics:textures/armor/crown_prs.png";
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Level level,
            java.util.List<net.minecraft.network.chat.Component> lines,
            TooltipFlag flags) {
        super.appendHoverText(stack, level, lines, flags);
        lines.add(
                net.minecraft.network.chat.Component.translatable(
                                "tooltip.forgottenrelics.terror_crown")
                        .withStyle(net.minecraft.ChatFormatting.GRAY));
    }

    @Override
    public void inventoryTick(
            ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (entity instanceof ServerPlayer p
                && stack.isDamaged()
                && ManaItemHandler.instance().requestManaExact(stack, p, 200, true))
            stack.setDamageValue(stack.getDamageValue() - 1);
    }

    @Override
    public void onArmorTick(ItemStack stack, Level level, Player wearer) {
        if (!(wearer instanceof ServerPlayer p)) return;
        if (p.tickCount % 10 == 0) {
            var mobs =
                    level.getEntitiesOfClass(
                            Mob.class,
                            p.getBoundingBox().inflate(24),
                            m ->
                                    m instanceof net.minecraft.world.entity.monster.Enemy
                                            && m.isAlive());
            for (var mob : mobs) {
                var targets = mobs.stream().filter(m -> m != mob).toList();
                if (!targets.isEmpty())
                    mob.setTarget(targets.get(p.getRandom().nextInt(targets.size())));
            }
        }
        var target = Relics.pointed(p, 32);
        if (target == null) return;
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 2));
        target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 1));
        if (!target.hasEffect(MobEffects.WITHER))
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 40));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 2));
    }
}
