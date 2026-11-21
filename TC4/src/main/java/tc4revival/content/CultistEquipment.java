package tc4revival.content;

import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;

/** Equipment definitions retain the old iron and fortress armor values. */
public  class CultistEquipment {
    public enum Armor {
        ROBE_HELMET("cultist_robe_helmet","cultistrobehelm","ItemHelmetCultistRobe",ArmorItem.Type.HELMET,Style.ROBE),
        ROBE_CHEST("cultist_robe_chestplate","cultistrobechest","ItemChestplateCultistRobe",ArmorItem.Type.CHESTPLATE,Style.ROBE),
        ROBE_LEGS("cultist_robe_leggings","cultistrobelegs","ItemLeggingsCultistRobe",ArmorItem.Type.LEGGINGS,Style.ROBE),
        PLATE_HELMET("cultist_plate_helmet","cultistplatehelm","ItemHelmetCultistPlate",ArmorItem.Type.HELMET,Style.PLATE),
        PLATE_CHEST("cultist_plate_chestplate","cultistplatechest","ItemChestplateCultistPlate",ArmorItem.Type.CHESTPLATE,Style.PLATE),
        PLATE_LEGS("cultist_plate_leggings","cultistplatelegs","ItemLeggingsCultistPlate",ArmorItem.Type.LEGGINGS,Style.PLATE),
        LEADER_HELMET("cultist_leader_helmet","cultistplateleaderhelm","ItemHelmetCultistLeaderPlate",ArmorItem.Type.HELMET,Style.LEADER),
        LEADER_CHEST("cultist_leader_chestplate","cultistplateleaderchest","ItemChestplateCultistLeaderPlate",ArmorItem.Type.CHESTPLATE,Style.LEADER),
        LEADER_LEGS("cultist_leader_leggings","cultistplateleaderlegs","ItemLeggingsCultistLeaderPlate",ArmorItem.Type.LEGGINGS,Style.LEADER),
        BOOTS("cultist_boots","cultistboots","ItemBootsCultist",ArmorItem.Type.BOOTS,Style.BOOTS);
        public final String path,texture,legacy; public final ArmorItem.Type slot; public final Style style;
        Armor(String path,String texture,String legacy,ArmorItem.Type slot,Style style) { this.path=path; this.texture=texture; this.legacy=legacy; this.slot=slot; this.style=style; }
    }
    public enum Style { ROBE,PLATE,LEADER,BOOTS }
    public enum FortressMaterial implements ArmorMaterial {
        INSTANCE;
        @Override public int getDurabilityForType(ArmorItem.Type type) { return switch(type) { case HELMET->440; case CHESTPLATE->640; case LEGGINGS->600; case BOOTS->520; }; }
        @Override public int getDefenseForType(ArmorItem.Type type) { return switch(type) { case HELMET,BOOTS->3; case CHESTPLATE->7; case LEGGINGS->6; }; }
        @Override public int getEnchantmentValue() { return 25; }
        @Override public net.minecraft.sounds.SoundEvent getEquipSound() { return net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_IRON; }
        @Override public Ingredient getRepairIngredient() { return Ingredient.of(Items.IRON_INGOT); }
        @Override public String getName() { return "thaumcraft:cultist_leader"; }
        @Override public float getToughness() { return 0; }
        @Override public float getKnockbackResistance() { return 0; }
    }
    public enum CrimsonTier implements Tier {
        INSTANCE;
        @Override public int getUses() { return 200; }
        @Override public float getSpeed() { return 8; }
        @Override public float getAttackDamageBonus() { return 3.5f; }
        @Override public int getLevel() { return 4; }
        @Override public int getEnchantmentValue() { return 20; }
        @Override public Ingredient getRepairIngredient() { return Ingredient.of(TCItems.material(MaterialType.VOID)); }
    }
    private CultistEquipment() {}
}
