package tbmodern;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import thaumcraft.api.IRepairable;
import thaumcraft.api.IRunicArmor;
import thaumcraft.api.IVisDiscountGear;
import thaumcraft.api.IWarpingGear;
import thaumcraft.api.aspects.Aspect;

public final class Equipment {
    private Equipment() {}
    public static final Tier THAUMINITE = new Tier() {
        public int getUses() { return 974; }
        public float getSpeed() { return 7; }
        public float getAttackDamageBonus() { return 2.8F; }
        public int getLevel() { return 3; }
        public int getEnchantmentValue() { return 15; }
        public Ingredient getRepairIngredient() { return Ingredient.of(Content.item("thauminite_ingot")); }
    };
    public static final ArmorMaterial ARMOR = new ArmorMaterial() {
        public int getDurabilityForType(ArmorItem.Type type) { return switch(type) {
            case HELMET -> 11 * 27; case CHESTPLATE -> 16 * 27;
            case LEGGINGS -> 15 * 27; case BOOTS -> 13 * 27;
        }; }
        public int getDefenseForType(ArmorItem.Type type) { return switch(type) {
            case HELMET, BOOTS -> 3; case CHESTPLATE -> 8; case LEGGINGS -> 6;
        }; }
        public int getEnchantmentValue() { return 17; }
        public SoundEvent getEquipSound() { return SoundEvents.ARMOR_EQUIP_IRON; }
        public Ingredient getRepairIngredient() { return THAUMINITE.getRepairIngredient(); }
        public String getName() { return "thaumicbases:thauminite"; }
        public float getToughness() { return 0; }
        public float getKnockbackResistance() { return 0; }
    };
    public static final class Axe extends AxeItem implements IRepairable {
        public Axe() { super(THAUMINITE, 3, -3.1F, new Item.Properties()); }
    }
    public static final class Pick extends PickaxeItem implements IRepairable {
        public Pick() { super(THAUMINITE, 1, -2.8F, new Item.Properties()); }
    }
    public static final class Shovel extends ShovelItem implements IRepairable {
        public Shovel() { super(THAUMINITE, 1, -3F, new Item.Properties()); }
    }
    public static final class Hoe extends HoeItem implements IRepairable {
        public Hoe() { super(THAUMINITE, -2, -1, new Item.Properties()); }
    }
    public static final class Sword extends SwordItem implements IRepairable {
        public Sword() { super(THAUMINITE, 3, -2.4F, new Item.Properties()); }
    }
    public static final class Armor extends ArmorItem implements IRepairable, IVisDiscountGear, IRunicArmor {
        public Armor(Type type) { super(ARMOR, type, new Item.Properties()); }
        public int getVisDiscount(ItemStack stack, Player player, Aspect aspect) {
            return switch(getType()) { case HELMET -> 5; case CHESTPLATE -> 2; case LEGGINGS -> 3; case BOOTS -> 1; };
        }
        public int getRunicCharge(ItemStack stack) { return 0; }
        @Override public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
            return "thaumicbases:textures/armor/thauminite_" + (slot == EquipmentSlot.LEGS ? "2" : "1") + ".png";
        }
    }
    public static class Shears extends ShearsItem implements IRepairable {
        public Shears(int durability) { super(new Item.Properties().durability(durability)); }
    }
    public static final class VoidShears extends Shears implements IWarpingGear {
        public VoidShears() { super(184); }
        public int getWarp(ItemStack stack, Player player) { return 1; }
        @Override public Rarity getRarity(ItemStack stack) { return Rarity.UNCOMMON; }
        @Override public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean held) { mend(stack, level, entity); }
    }
    public static final class VoidLighter extends FlintAndSteelItem implements IRepairable, IWarpingGear {
        public VoidLighter() { super(new Item.Properties().durability(184)); }
        @Override public boolean canPerformAction(ItemStack stack, net.minecraftforge.common.ToolAction action) {
            // Botania 1.20.1 checks the exact vanilla item or its custom "light_fire" action.
            return super.canPerformAction(stack,action)
                    || action==net.minecraftforge.common.ToolAction.get("light_fire");
        }
        public int getWarp(ItemStack stack, Player player) { return 1; }
        @Override public Rarity getRarity(ItemStack stack) { return Rarity.UNCOMMON; }
        @Override public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean held) { mend(stack, level, entity); }
    }
    private static void mend(ItemStack stack, Level level, Entity entity) {
        if (!level.isClientSide && entity instanceof LivingEntity && entity.tickCount % 20 == 0 && stack.isDamaged())
            stack.setDamageValue(stack.getDamageValue() - 1);
    }
}
