package thaumcraft.api.aspects;

/** Stable public handle. Define new aspects in addon JSON, not by constructing unregistered objects. */
public  class Aspect {
    private final String tag;
    private static final java.util.Map<String,Aspect> HANDLES=new java.util.concurrent.ConcurrentHashMap<>();
    private Aspect(String tag) { this.tag=tag; }
    public static Aspect of(String tag) { return HANDLES.computeIfAbsent(tag,Aspect::new); }
    public static Aspect getAspect(String tag) { return tc4revival.aspect.TCAspects.catalog().find(tag).map(Aspect::wrap).orElse(null); }
    public static Aspect wrap(tc4revival.aspect.Aspect value) { return value==null?null:of(value.tag()); }
    public tc4revival.aspect.Aspect unwrap() { return tc4revival.aspect.TCAspects.catalog().require(tag); }
    public String getTag() { return tag; }
    public String getName() { return unwrap().displayName(); }
    public int getColor() { return unwrap().color(); }
    public boolean isPrimal() { return unwrap().isPrimal(); }
    public Aspect[] getComponents() { return unwrap().components().stream().map(Aspect::of).toArray(Aspect[]::new); }
    public net.minecraft.resources.ResourceLocation getImage() { return unwrap().texture(); }
    public static java.util.List<Aspect> getPrimalAspects() { return tc4revival.aspect.TCAspects.catalog().primals().stream().map(Aspect::wrap).toList(); }
    public static java.util.List<Aspect> getCompoundAspects() { return tc4revival.aspect.TCAspects.catalog().compounds().stream().map(Aspect::wrap).toList(); }
    @Override public boolean equals(Object other) { return other instanceof Aspect value&&tag.equals(value.tag); }
    @Override public int hashCode() { return tag.hashCode(); }
    @Override public String toString() { return tag; }
    public static final Aspect AIR=of("aer");
    public static final Aspect EARTH=of("terra");
    public static final Aspect FIRE=of("ignis");
    public static final Aspect WATER=of("aqua");
    public static final Aspect ORDER=of("ordo");
    public static final Aspect ENTROPY=of("perditio");
    public static final Aspect VOID=of("vacuos");
    public static final Aspect LIGHT=of("lux");
    public static final Aspect WEATHER=of("tempestas");
    public static final Aspect MOTION=of("motus");
    public static final Aspect COLD=of("gelum");
    public static final Aspect CRYSTAL=of("vitreus");
    public static final Aspect LIFE=of("victus");
    public static final Aspect POISON=of("venenum");
    public static final Aspect ENERGY=of("potentia");
    public static final Aspect EXCHANGE=of("permutatio");
    public static final Aspect METAL=of("metallum");
    public static final Aspect DEATH=of("mortuus");
    public static final Aspect FLIGHT=of("volatus");
    public static final Aspect DARKNESS=of("tenebrae");
    public static final Aspect SOUL=of("spiritus");
    public static final Aspect HEAL=of("sano");
    public static final Aspect TRAVEL=of("iter");
    public static final Aspect ELDRITCH=of("alienis");
    public static final Aspect MAGIC=of("praecantatio");
    public static final Aspect AURA=of("auram");
    public static final Aspect TAINT=of("vitium");
    public static final Aspect SLIME=of("limus");
    public static final Aspect PLANT=of("herba");
    public static final Aspect TREE=of("arbor");
    public static final Aspect BEAST=of("bestia");
    public static final Aspect FLESH=of("corpus");
    public static final Aspect UNDEAD=of("exanimis");
    public static final Aspect MIND=of("cognitio");
    public static final Aspect SENSES=of("sensus");
    public static final Aspect MAN=of("humanus");
    public static final Aspect CROP=of("messis");
    public static final Aspect MINE=of("perfodio");
    public static final Aspect TOOL=of("instrumentum");
    public static final Aspect HARVEST=of("meto");
    public static final Aspect WEAPON=of("telum");
    public static final Aspect ARMOR=of("tutamen");
    public static final Aspect HUNGER=of("fames");
    public static final Aspect GREED=of("lucrum");
    public static final Aspect CRAFT=of("fabrico");
    public static final Aspect CLOTH=of("pannus");
    public static final Aspect MECHANISM=of("machina");
    public static final Aspect TRAP=of("vinculum");
}
