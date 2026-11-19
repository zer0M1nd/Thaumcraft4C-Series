package thaumicomniverse.expansion;
import java.util.Map;

/** Shared fuel values with no optional-mod class references. Base values are EU before multipliers. */
public final class EssentiaFuelCatalog {
    public static final Map<String,Integer> ENERGY=Map.ofEntries(
        Map.entry("aer",1000),Map.entry("terra",800),Map.entry("ignis",16000),Map.entry("aqua",5000),
        Map.entry("ordo",12000),Map.entry("perditio",4500),Map.entry("vacuos",8200),Map.entry("lux",13000),
        Map.entry("tempestas",61200),Map.entry("motus",20000),Map.entry("gelum",3900),Map.entry("vitreus",2000),
        Map.entry("victus",20000),Map.entry("venenum",17000),Map.entry("potentia",25600),Map.entry("permutatio",6000),
        Map.entry("metallum",2600),Map.entry("mortuus",7200),Map.entry("volatus",56000),Map.entry("tenebrae",9000),
        Map.entry("spiritus",37600),Map.entry("sano",24600),Map.entry("iter",16000),Map.entry("alienis",9700),
        Map.entry("praecantatio",36000),Map.entry("auram",42000),Map.entry("vitium",4700),Map.entry("limus",11800),
        Map.entry("herba",2600),Map.entry("arbor",4500),Map.entry("bestia",1000),Map.entry("corpus",5200),
        Map.entry("exanimis",9000),Map.entry("cognitio",2200),Map.entry("sensus",1400),Map.entry("humanus",16700),
        Map.entry("messis",9800),Map.entry("perfodio",1300),Map.entry("instrumentum",1200),Map.entry("meto",3600),
        Map.entry("telum",14200),Map.entry("tutamen",6200),Map.entry("fames",17600),Map.entry("lucrum",6000),
        Map.entry("fabrico",1100),Map.entry("pannus",600),Map.entry("machina",61200),Map.entry("vinculum",500),
        Map.entry("luxuria",79200),Map.entry("infernus",35700),Map.entry("superbia",10900),Map.entry("gula",64000),
        Map.entry("invidia",7700),Map.entry("desidia",600),Map.entry("ira",86200));
    public static int generatorTicks(int energy,double multiplier){
        return (int)Math.min(Integer.MAX_VALUE,Math.max(1,Math.round(Math.max(1,Math.round(energy*8d/4096))*multiplier)));
    }
    public static double durationMultiplier(){
        return thaumicomniverse.RevivalFeatures.CONFIG.loaded()?thaumicomniverse.RevivalFeatures.ESSENTIA_FUEL_DURATION.get():4;
    }
    /** GT burns 4 essentia at 4096 EU/t; 1 EU = 4 FE, then divide by four for this dynamo. */
    public static int dynamoEnergy(String aspect){
        var energy=ENERGY.get(aspect);
        return energy==null?0:(int)Math.min(Integer.MAX_VALUE,(long)generatorTicks(energy,durationMultiplier())*1024L);
    }
    private EssentiaFuelCatalog(){}
}
