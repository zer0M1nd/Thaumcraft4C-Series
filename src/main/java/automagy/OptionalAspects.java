package automagy;

/** Resolves optional aspects lazily, so data-pack reloads and absent addons remain safe. */
public final class OptionalAspects {
    private OptionalAspects() {}
    public static java.util.Optional<tc4revival.aspect.Aspect> nether(){
        String tag=AutomagyConfig.NETHER_ASPECT.get();
        return resolve(tc4revival.aspect.TCAspects.catalog(),tag);
    }
    public static java.util.Optional<tc4revival.aspect.Aspect> resolve(tc4revival.aspect.AspectCatalog catalog,String tag){
        if(tag.isBlank())return java.util.Optional.empty();
        var exact=catalog.find(tag);
        // Keep existing configs/custom tags, while recognizing the modern port's default ID.
        return exact.isPresent()||!tag.equals("infernus")?exact:catalog.find("forbidden_magic:infernus");
    }
}
