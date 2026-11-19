package thaumcraft.api.research;

/** Read-only views of the server-authoritative JSON definitions. */
public  class ResearchCategories {
    private static final java.util.Map<String,java.util.function.BooleanSupplier> ENABLED=new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<net.minecraft.resources.ResourceLocation,java.util.function.Predicate<String>> FILTERS=new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<String,String> REVEAL_REQUIREMENTS=new java.util.concurrent.ConcurrentHashMap<>();
    /** A legacy concealed-unless condition affects visibility only, never purchase or note prerequisites. */
    public static void registerRevealRequirement(String research,String revealResearch) {
        if(research==null||research.isBlank()||revealResearch==null||revealResearch.isBlank())
            throw new IllegalArgumentException("Empty reveal research key");
        if(REVEAL_REQUIREMENTS.putIfAbsent(research,revealResearch)!=null)
            throw new IllegalArgumentException("Duplicate reveal requirement: "+research);
    }
    public static boolean revealed(String research,java.util.function.Predicate<String> completed,boolean prerequisites) {
        var requirement=REVEAL_REQUIREMENTS.get(research);
        return prerequisites||(requirement!=null&&completed.test(requirement));
    }
    /** Additional conjunctive feature-group filters; registration and saved research progress stay intact. */
    public static void registerEnablementFilter(net.minecraft.resources.ResourceLocation id,java.util.function.Predicate<String> enabled) {
        if(FILTERS.putIfAbsent(java.util.Objects.requireNonNull(id),java.util.Objects.requireNonNull(enabled))!=null)
            throw new IllegalArgumentException("Duplicate research filter: "+id);
    }
    /** Register during common setup on both sides. The supplier reads already synchronized runtime configuration. */
    public static void registerEnablement(String research,java.util.function.BooleanSupplier enabled) {
        if(research==null||research.isBlank())throw new IllegalArgumentException("Empty research key");
        if(ENABLED.putIfAbsent(research,java.util.Objects.requireNonNull(enabled))!=null)throw new IllegalArgumentException("Duplicate research enablement: "+research);
    }
    public static boolean enabled(String research) {var condition=ENABLED.get(research);return (condition==null||condition.getAsBoolean())&&FILTERS.values().stream().allMatch(filter->filter.test(research));}
    public static java.util.List<tc4revival.research.ResearchCatalog.Category> getCategories() { return tc4revival.research.ResearchCatalogs.bundled().categories(); }
    public static java.util.Optional<tc4revival.research.ResearchDefinition> getResearch(String key) { return tc4revival.research.ResearchCatalogs.bundled().find(key); }
    public static java.util.List<tc4revival.research.ResearchPage> getPages(String key) { return tc4revival.research.ResearchPages.bundled().require(key); }
}
