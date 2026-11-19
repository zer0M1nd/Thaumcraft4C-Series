package tc4revival.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.*;

/** Seeded, contiguous replacements of the legacy warm/cool land families. */
public  class LegacyNaturalBiomeSource extends BiomeSource {
    public static final Codec<LegacyNaturalBiomeSource> CODEC=RecordCodecBuilder.create(i->i.group(
            BiomeSource.CODEC.fieldOf("delegate").forGetter(s->s.delegate),
            Biome.CODEC.fieldOf("forest").forGetter(s->s.forest),
            Biome.CODEC.fieldOf("taint").forGetter(s->s.taint),
            Codec.LONG.fieldOf("seed").forGetter(s->s.seed),
            Codec.intRange(0,Integer.MAX_VALUE).fieldOf("forest_weight").forGetter(s->s.forestWeight),
            Codec.intRange(0,Integer.MAX_VALUE).fieldOf("taint_weight").forGetter(s->s.taintWeight)
    ).apply(i,LegacyNaturalBiomeSource::new));
    final BiomeSource delegate;
    final Holder<Biome> forest,taint;
    final long seed;
    final int forestWeight,taintWeight;
    private static final Set<String> LAND=Set.of("plains","sunflower_plains","forest","flower_forest","birch_forest","old_growth_birch_forest",
            "dark_forest","swamp","taiga","old_growth_pine_taiga","old_growth_spruce_taiga","windswept_hills","windswept_forest","windswept_gravelly_hills");
    public LegacyNaturalBiomeSource(BiomeSource delegate,Holder<Biome> forest,Holder<Biome> taint,long seed,int forestWeight,int taintWeight) {
        this.delegate=delegate;this.forest=forest;this.taint=taint;this.seed=seed;this.forestWeight=forestWeight;this.taintWeight=taintWeight;
    }
    @Override protected Codec<? extends BiomeSource> codec(){return CODEC;}
    @Override protected Stream<Holder<Biome>> collectPossibleBiomes(){
        return Stream.concat(delegate.possibleBiomes().stream(),Stream.of(forest,taint)).distinct();
    }
    static boolean eligible(Holder<Biome> biome){return biome.unwrapKey().map(k->k.location().getNamespace().equals("minecraft")&&LAND.contains(k.location().getPath())).orElse(false);}
    @Override public Holder<Biome> getNoiseBiome(int x,int y,int z,Climate.Sampler sampler) {
        var base=delegate.getNoiseBiome(x,y,z,sampler);
        if((forestWeight==0&&taintWeight==0)||!eligible(base))return base;
        // Legacy replacements are land columns. Underground climate must not
        // introduce taint beneath an otherwise ocean surface.
        int surfaceY=net.minecraft.core.QuartPos.fromBlock(64);
        if(!eligible(delegate.getNoiseBiome(x,surfaceY,z,sampler)))return base;
        // Climate/eligibility is a property of the selected region, not each sampled column.
        // Per-column modulo denominators turned temperature boundaries into thin taint ribbons.
        var region=region(seed,x,z);
        var cache=regionCache.get();
        if(cache.sampler!=sampler){cache.sampler=sampler;cache.values.clear();}
        int selected=cache.values.computeIfAbsent(region,key->regionSelection(key,sampler));
        return selected==1?forest:selected==2?taint:base;
    }
    private record Region(long hash,int x,int z) {}
    private static  class RegionCache {
        Climate.Sampler sampler;
        final java.util.Map<Region,Integer> values=new java.util.LinkedHashMap<>(256,.75f,true) {
            @Override protected boolean removeEldestEntry(java.util.Map.Entry<Region,Integer> entry){return size()>256;}
        };
    }
    private final ThreadLocal<RegionCache> regionCache=ThreadLocal.withInitial(RegionCache::new);
    private int regionSelection(Region region,Climate.Sampler sampler) {
        int surfaceY=net.minecraft.core.QuartPos.fromBlock(64);
        int vanillaWeight=sampler.sample(region.x(),surfaceY,region.z()).temperature()<=0?40:60;
        int selected=roll(region.hash(),vanillaWeight,forestWeight,taintWeight);
        if(selected==0)return 0;
        // A replacement must have a land interior; do not retain only the thin overlap of
        // an ocean/desert-centred region with an eligible biome along its edge.
        for(int dx=-16;dx<=16;dx+=16)for(int dz=-16;dz<=16;dz+=16)
            if(!eligible(delegate.getNoiseBiome(region.x()+dx,surfaceY,region.z()+dz,sampler)))return 0;
        return selected;
    }
    // Jittered Voronoi cells avoid hard square chunk borders; coordinates are biome quarts.
    private static Region region(long seed,int x,int z) {
        int cellX=Math.floorDiv(x,64),cellZ=Math.floorDiv(z,64);double nearest=Double.POSITIVE_INFINITY;
        Region chosen=null;
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++){
            long hash=mix(seed^((long)(cellX+dx)*0x632be59bd9b4e019L)^((long)(cellZ+dz)*0x9e3779b97f4a7c15L));
            double px=(cellX+dx)*64.0+16+((hash>>>16)&0xffff)*(32.0/65536),pz=(cellZ+dz)*64.0+16+((hash>>>32)&0xffff)*(32.0/65536);
            double distance=(px-x)*(px-x)+(pz-z)*(pz-z);
            if(distance<nearest){nearest=distance;chosen=new Region(hash,(int)Math.floor(px),(int)Math.floor(pz));}
        }
        return chosen;
    }
    static int selection(long seed,int x,int z,int vanillaWeight,int forestWeight,int taintWeight) {
        return roll(region(seed,x,z).hash(),vanillaWeight,forestWeight,taintWeight);
    }
    private static int roll(long chosen,int vanillaWeight,int forestWeight,int taintWeight) {
        long total=(long)vanillaWeight+forestWeight+taintWeight;
        long roll=Long.remainderUnsigned(mix(chosen+0x51ed2705L),total);
        return roll<forestWeight?1:roll<(long)forestWeight+taintWeight?2:0;
    }
    private static long mix(long value){value=(value^(value>>>30))*0xbf58476d1ce4e5b9L;value=(value^(value>>>27))*0x94d049bb133111ebL;return value^(value>>>31);}
}
