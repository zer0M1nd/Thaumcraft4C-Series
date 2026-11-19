package thaumicomniverse.expansion;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.world.level.biome.*;
import net.minecraftforge.event.server.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumicomniverse.RevivalFeatures;
import tc4revival.content.TCBiomes;

/** A deterministic interior patch of the biome selected by TerraBlender, never a new region. */
@Mod.EventBusSubscriber(modid="thaumicomniverse")
public final class AncientForest {
    public static final ResourceKey<Biome> BIOME=ResourceKey.create(Registries.BIOME,ResourceLocation.parse("thaumicomniverse:ancient_magical_forest"));
    public static final net.minecraft.tags.TagKey<Biome> MAGICAL_FORESTS=
            net.minecraft.tags.TagKey.create(Registries.BIOME,ResourceLocation.parse("thaumcraft:magical_forests"));
    private record Binding(Holder<Biome> biome,long seed) {}
    private static volatile Binding binding;
    private static final ThreadLocal<Boolean> sampling=ThreadLocal.withInitial(()->false);
    private record Cell(int x,int z) {}
    private record Patch(int x,int z,int radius,boolean valid) {}
    private static final class Cache {
        Binding binding;
        Climate.Sampler sampler;
        BiomeSource source;
        final Map<Cell,Patch> patches=new LinkedHashMap<>(256,.75f,true){
            @Override protected boolean removeEldestEntry(Map.Entry<Cell,Patch> entry){return size()>2048;}
        };
    }
    private static final ThreadLocal<Cache> cache=ThreadLocal.withInitial(Cache::new);
    @SubscribeEvent public static void starting(ServerAboutToStartEvent event){
        binding=RevivalFeatures.ANCIENT_FOREST.get()
                ?new Binding(event.getServer().registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(BIOME),
                        event.getServer().getWorldData().worldGenOptions().seed()):null;
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event){binding=null;cache.remove();sampling.remove();}
    public static boolean isAncient(Holder<Biome> biome){return biome.is(BIOME);}
    /** Sample the complete downstream source, including TerraBlender's early return. */
    public static Holder<Biome> sample(BiomeSource source,int x,int y,int z,Climate.Sampler sampler){
        if(binding==null||sampling.get())return null;
        Holder<Biome> original;
        sampling.set(true);
        try{original=source.getNoiseBiome(x,y,z,sampler);}finally{sampling.set(false);}
        return select(source,x,y,z,sampler,original);
    }
    public static Set<Holder<Biome>> possible(Set<Holder<Biome>> original){
        var current=binding;
        if(current==null||original.contains(current.biome)||original.stream().noneMatch(b->b.is(TCBiomes.MAGICAL_FOREST)))return original;
        var result=new LinkedHashSet<>(original);result.add(current.biome);return Collections.unmodifiableSet(result);
    }
    public static Holder<Biome> select(BiomeSource source,int x,int y,int z,Climate.Sampler sampler,Holder<Biome> original){
        var current=binding;
        if(current==null||sampling.get()||!original.is(TCBiomes.MAGICAL_FOREST))return original;
        var local=cache.get();
        if(local.binding!=current||local.sampler!=sampler||local.source!=source){
            local.binding=current;local.sampler=sampler;local.source=source;local.patches.clear();
        }
        int cellX=Math.floorDiv(x,64),cellZ=Math.floorDiv(z,64);
        sampling.set(true);
        try {
            for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++){
                var key=new Cell(cellX+dx,cellZ+dz);
                long hash=mix(current.seed^((long)key.x*0x632be59bd9b4e019L)^((long)key.z*0x9e3779b97f4a7c15L));
                if((hash&3)==0)continue;
                int px=key.x*64+16+(int)((hash>>>8)&31),pz=key.z*64+16+(int)((hash>>>16)&31);
                int radius=16+(int)((hash>>>24)&7);
                if((long)(x-px)*(x-px)+(long)(z-pz)*(z-pz)>(long)radius*radius)continue;
                var patch=local.patches.computeIfAbsent(key,ignored->interior(source,sampler,px,pz,radius));
                if(patch.valid){
                    // Keep a local buffer even when the parent forest has an irregular
                    // outline. Large-patch suitability is a preference, not a requirement
                    // that every sampled climate column in a circle be identical.
                    boolean inset=true;
                    for(var face:Direction.Plane.HORIZONTAL){
                        if(!source.getNoiseBiome(x+face.getStepX()*2,y,z+face.getStepZ()*2,sampler).is(TCBiomes.MAGICAL_FOREST)){
                            inset=false;break;
                        }
                    }
                    if(inset)return current.biome;
                }
            }
            return original;
        }finally{sampling.set(false);}
    }
    private static Patch interior(BiomeSource source,Climate.Sampler sampler,int x,int z,int radius){
        // Quarts: a 128–184 block wide patch, sampled with a 16 block forest margin.
        // The final per-column check remains authoritative, including cave/river mappings.
        int extent=radius+4,forest=0,total=0,innerForest=0,innerTotal=0;
        if(!source.getNoiseBiome(x,20,z,sampler).is(TCBiomes.MAGICAL_FOREST))
            return new Patch(x,z,radius,false);
        for(int dx=-extent;dx<=extent;dx+=4)for(int dz=-extent;dz<=extent;dz+=4){
            if(dx*dx+dz*dz>extent*extent)continue;
            boolean magical=source.getNoiseBiome(x+dx,20,z+dz,sampler).is(TCBiomes.MAGICAL_FOREST);
            total++;if(magical)forest++;
            if(dx*dx+dz*dz<=radius*radius/4){innerTotal++;if(magical)innerForest++;}
        }
        return new Patch(x,z,radius,forest>=total*.60&&innerForest>=innerTotal*.75);
    }
    private static long mix(long value){value=(value^(value>>>30))*0xbf58476d1ce4e5b9L;value=(value^(value>>>27))*0x94d049bb133111ebL;return value^(value>>>31);}
    private AncientForest(){}
}
