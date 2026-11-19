package tc4revival.content;

import java.util.List;

public enum ThaumiumToolType {
    PICKAXE("thaumium_pickaxe", "thaumiumpick", "ItemPickThaumium", "perfodio", "III", " S ", " S "),
    AXE("thaumium_axe", "thaumiumaxe", "ItemAxeThaumium", "instrumentum", "II", "SI", "S "),
    SHOVEL("thaumium_shovel", "thaumiumshovel", "ItemShovelThaumium", "instrumentum", "I", "S", "S"),
    HOE("thaumium_hoe", "thaumiumhoe", "ItemHoeThaumium", "meto", "II", "S ", "S "),
    SWORD("thaumium_sword", "thaumiumsword", "ItemSwordThaumium", "telum", "I", "I", "S");
    private final String path, texture, legacyName, bonus;
    private final List<String> pattern;
    ThaumiumToolType(String path, String texture, String legacyName, String bonus, String... pattern) {
        this.path = path; this.texture = texture; this.legacyName = legacyName; this.bonus = bonus; this.pattern = List.of(pattern);
    }
    public String path() { return path; }
    public String texture() { return texture; }
    public String legacyName() { return legacyName; }
    public String bonus() { return bonus; }
    public int bonusAmount() { return this == SWORD ? LegacySwordRules.weaponAspect(2) : 4; }
    public List<String> pattern() { return pattern; }
}
