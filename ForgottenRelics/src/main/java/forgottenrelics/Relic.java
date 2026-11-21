package forgottenrelics;

/**
 * Original registered items. Numeric costs are centivis, in air/earth/fire/water/order/entropy
 * order.
 */
public enum Relic {
    FALSE_JUSTICE("false_justice", "", 4, "", 0, 0, 0, 0, 0, 0),
    DEIFIC_AMULET("deific_amulet", "necklace", 0, "", 0, 0, 0, 0, 0, 0),
    PARADOX("paradox", "", 8, "", 0, 0, 0, 0, 0, 0),
    OBLIVION_AMULET("oblivion_amulet", "necklace", 4, "", 0, 0, 0, 0, 0, 0),
    ARCANUM("arcanum", "necklace", 0, "", 0, 0, 0, 0, 0, 0),
    DORMANT_ARCANUM("dormant_arcanum", "necklace", 0, "", 0, 0, 0, 0, 0, 0),
    FATE_TOME("fate_tome", "", 7, "fateTomeVisMult", 10000, 10000, 10000, 10000, 10000, 10000),
    DARK_SUN_RING("dark_sun_ring", "ring", 0, "", 0, 0, 0, 0, 0, 0),
    CHAOS_CORE("chaos_core", "", 2, "", 0, 0, 0, 0, 0, 0),
    MINING_CHARM("mining_charm", "ring", 0, "", 0, 0, 0, 0, 0, 0),
    ADVANCED_MINING_CHARM("advanced_mining_charm", "ring", 0, "", 0, 0, 0, 0, 0, 0),
    TELEKINESIS_TOME("telekinesis_tome", "", 4, "telekinesisTomeVisMult", 6, 0, 0, 0, 8, 0),
    ANCIENT_AEGIS("ancient_aegis", "belt", 0, "", 0, 0, 0, 0, 0, 0),
    MISSILE_TOME("missile_tome", "", 5, "nuclearFuryVisMult", 0, 0, 20, 0, 10, 15),
    CRIMSON_SPELL("crimson_spell", "", 3, "crimsonSpellVisMult", 0, 0, 480, 0, 0, 360),
    GHASTLY_SKULL("ghastly_skull", "", 3, "", 0, 0, 0, 0, 0, 0),
    OBELISK_DRAINER("obelisk_drainer", "", 4, "", 0, 0, 0, 0, 0, 0),
    ELDRITCH_SPELL("eldritch_spell", "", 4, "eldritchSpellVisMult", 0, 0, 0, 0, 0, 400),
    LUNAR_FLARES("lunar_flares", "", 3, "lunarFlaresVisMult", 35, 0, 50, 0, 65, 0),
    TELEPORTATION_TOME("teleportation_tome", "", 2, "discordTomeVisMult", 160, 0, 0, 0, 240, 240),
    SHINY_STONE("shiny_stone", "necklace", 0, "", 0, 0, 0, 0, 0, 0),
    SUPERPOSITION_RING("superposition_ring", "ring", 0, "", 0, 0, 0, 0, 0, 0),
    SOUL_TOME("soul_tome", "", 3, "soulTomeVisMult", 20, 25, 35, 0, 0, 50),
    DIMENSIONAL_MIRROR("dimensional_mirror", "", 0, "", 0, 0, 0, 0, 0, 0),
    WEATHER_STONE("weather_stone", "", 0, "weatherStoneVisMult", 0, 0, 0, 0, 0, 0),
    APOTHEOSIS("apotheosis", "", 5, "apotheosisVisMult", 0, 30, 60, 0, 50, 75),
    CHAOS_TOME("chaos_tome", "", 4, "chaosTomeVisMult", 100, 100, 100, 100, 100, 100),
    OMEGA_CORE("omega_core", "", 0, "", 0, 0, 0, 0, 0, 0),
    XP_TOME("xp_tome", "", 0, "", 0, 0, 0, 0, 0, 0),
    THUNDERPEAL("thunderpeal", "", 0, "thunderpealVisMult", 135, 0, 85, 0, 0, 0),
    TERROR_CROWN("terror_crown", "", 3, "", 0, 0, 0, 0, 0, 0),
    OVERTHROWER("overthrower", "", 2, "overthrowerVisMult", 0, 0, 8, 0, 5, 5),
    DISCORD_RING("discord_ring", "ring", 0, "", 0, 0, 0, 0, 0, 0),
    VOID_GRIMOIRE("void_grimoire", "", 3, "voidGrimoireVisMult", 0, 0, 0, 0, 9, 16),
    OBLIVION_STONE("oblivion_stone", "", 2, "", 0, 0, 0, 0, 0, 0);
    public final String id, slot, multiplier;
    public final int warp;
    private final int[] cost;

    Relic(String id, String slot, int warp, String multiplier, int... cost) {
        this.id = id;
        this.slot = slot;
        this.warp = warp;
        this.multiplier = multiplier;
        this.cost = cost.clone();
    }

    public int[] costs() {
        return cost.clone();
    }
}
