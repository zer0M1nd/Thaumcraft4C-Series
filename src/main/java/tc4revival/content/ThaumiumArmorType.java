package tc4revival.content;

import java.util.List;
import net.minecraft.world.item.ArmorItem;

public enum ThaumiumArmorType {
    HELMET(ArmorItem.Type.HELMET, "thaumium_helmet", "thaumiumhelm", "ItemHelmetThaumium", 275, 2, "III", "I I"),
    CHESTPLATE(ArmorItem.Type.CHESTPLATE, "thaumium_chestplate", "thaumiumchest", "ItemChestplateThaumium", 400, 6, "I I", "III", "III"),
    LEGGINGS(ArmorItem.Type.LEGGINGS, "thaumium_leggings", "thaumiumlegs", "ItemLeggingsThaumium", 375, 5, "III", "I I", "I I"),
    BOOTS(ArmorItem.Type.BOOTS, "thaumium_boots", "thaumiumboots", "ItemBootsThaumium", 325, 2, "I I", "I I");

    private final ArmorItem.Type type;
    private final String path, texture, legacyName;
    private final int durability, defense;
    private final List<String> pattern;
    ThaumiumArmorType(ArmorItem.Type type, String path, String texture, String legacyName,
                     int durability, int defense, String... pattern) {
        this.type = type; this.path = path; this.texture = texture; this.legacyName = legacyName;
        this.durability = durability; this.defense = defense; this.pattern = List.of(pattern);
    }
    public ArmorItem.Type type() { return type; }
    public String path() { return path; }
    public String texture() { return texture; }
    public String legacyName() { return legacyName; }
    public int durability() { return durability; }
    public int defense() { return defense; }
    public List<String> pattern() { return pattern; }
    public static ThaumiumArmorType from(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> HELMET; case CHESTPLATE -> CHESTPLATE; case LEGGINGS -> LEGGINGS; case BOOTS -> BOOTS;
        };
    }
}
