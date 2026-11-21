package tc4revival.aspect;

/** Stable names for legacy content. Metadata is defined exclusively in aspect_registry.json. */
public enum LegacyAspect {
    AIR("aer"),
    EARTH("terra"),
    FIRE("ignis"),
    WATER("aqua"),
    ORDER("ordo"),
    ENTROPY("perditio"),
    VOID("vacuos"),
    LIGHT("lux"),
    WEATHER("tempestas"),
    MOTION("motus"),
    COLD("gelum"),
    CRYSTAL("vitreus"),
    LIFE("victus"),
    POISON("venenum"),
    ENERGY("potentia"),
    EXCHANGE("permutatio"),
    METAL("metallum"),
    DEATH("mortuus"),
    FLIGHT("volatus"),
    DARKNESS("tenebrae"),
    SOUL("spiritus"),
    HEAL("sano"),
    TRAVEL("iter"),
    ELDRITCH("alienis"),
    MAGIC("praecantatio"),
    AURA("auram"),
    TAINT("vitium"),
    SLIME("limus"),
    PLANT("herba"),
    TREE("arbor"),
    BEAST("bestia"),
    FLESH("corpus"),
    UNDEAD("exanimis"),
    MIND("cognitio"),
    SENSES("sensus"),
    MAN("humanus"),
    CROP("messis"),
    MINE("perfodio"),
    TOOL("instrumentum"),
    HARVEST("meto"),
    WEAPON("telum"),
    ARMOR("tutamen"),
    HUNGER("fames"),
    GREED("lucrum"),
    CRAFT("fabrico"),
    CLOTH("pannus"),
    MECHANISM("machina"),
    TRAP("vinculum");

    private final String tag;
    LegacyAspect(String tag) { this.tag=tag; }
    public String tag() { return tag; }
    public Aspect create() { return AspectDefinitions.bundled().require(tag); }
}
