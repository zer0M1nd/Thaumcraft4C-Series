package tc4revival.content;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

public  class VoidArmorItem extends ArmorItem implements RepairableItem, RunicGear, WarpingGear {
    private enum Material implements ArmorMaterial {
        INSTANCE;
        public int getDurabilityForType(Type type) { return VoidEquipment.Armor.from(type).durability; }
        public int getDefenseForType(Type type) { return VoidEquipment.Armor.from(type).defense; }
        public int getEnchantmentValue() { return 10; }
        public SoundEvent getEquipSound() { return SoundEvents.ARMOR_EQUIP_IRON; }
        public Ingredient getRepairIngredient() { return Ingredient.of(TCItems.material(MaterialType.VOID)); }
        public String getName() { return "thaumcraft:void"; }
        public float getToughness() { return 0; }
        public float getKnockbackResistance() { return 0; }
    }
    public VoidArmorItem(VoidEquipment.Armor type) { super(Material.INSTANCE, type.type, new Properties().rarity(Rarity.UNCOMMON)); }
    @Override public Rarity getRarity(ItemStack stack) { return Rarity.UNCOMMON; }
    @Override public int warp(ItemStack stack, Player wearer) { return 1; }
    @Override public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String layer) {
        return "thaumcraft:textures/models/void_" + (getType() == Type.LEGGINGS ? "2" : "1") + ".png";
    }
    @Override public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
        // Modern Inventory ticks its armor list too; onArmorTick supplies the equipped pass.
        if (!level.isClientSide && !(holder instanceof Player player && player.getItemBySlot(getEquipmentSlot()) == stack))
            VoidEquipment.repair(stack, holder);
    }
    @Override public void onArmorTick(ItemStack stack, Level level, Player player) {
        if (!level.isClientSide) VoidEquipment.repair(stack, player);
    }
}
