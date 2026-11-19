package conciliummodern;

import java.util.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.*;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.registries.*;

/** World-generation registration and per-village candidate selection. No optional host classes load here. */
public final class ConciliumWorldgen {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue GENERATE_TOWERS, GENERATE_BURNING_SITES;
    public static final ForgeConfigSpec.IntValue TOWER_CHANCE, BURNING_WEIGHT;
    static {
        var builder=new ForgeConfigSpec.Builder();builder.push("world");
        GENERATE_TOWERS=builder.comment("Generate the original Crimson Avanpost in the Overworld; requires Thaumic Dyes.")
                .define("generateCrimsonAvanposts",true);
        TOWER_CHANCE=builder.comment("One tower attempt per N chunks, before the original flat-terrain test. Zero disables.")
                .defineInRange("crimsonAvanpostGenChance",300,0,Integer.MAX_VALUE);
        GENERATE_BURNING_SITES=builder.comment("Add Burning Sites to villages when a Witchery host supplies stockades and witch hunters.")
                .define("generateBurningSites",true);
        BURNING_WEIGHT=builder.comment("Village house-pool weight, with at most one Burning Site per village. Zero disables.")
                .defineInRange("burningSitesGenChance",10,0,150);
        builder.pop();SPEC=builder.build();
    }
    public static final DeferredRegister<Feature<?>> FEATURES=DeferredRegister.create(ForgeRegistries.FEATURES,ThaumicConcilium.ID);
    public static final RegistryObject<CrimsonTowerFeature> TOWER=FEATURES.register("cultist_tower",CrimsonTowerFeature::new);
    public static final DeferredRegister<StructurePoolElementType<?>> ELEMENTS=DeferredRegister.create(Registries.STRUCTURE_POOL_ELEMENT,ThaumicConcilium.ID);
    public static final RegistryObject<StructurePoolElementType<BurningSiteElement>> BURNING_SITE=
            ELEMENTS.register("burning_site",()->()->BurningSiteElement.CODEC);
    private static final Set<ResourceLocation> VILLAGE_HOUSES=Set.of(
            new ResourceLocation("minecraft","village/plains/houses"),
            new ResourceLocation("minecraft","village/desert/houses"),
            new ResourceLocation("minecraft","village/savanna/houses"),
            new ResourceLocation("minecraft","village/snowy/houses"),
            new ResourceLocation("minecraft","village/taiga/houses"));

    public static void register(IEventBus bus) {
        FEATURES.register(bus);ELEMENTS.register(bus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON,SPEC,"thaumicconcilium-worldgen.toml");
    }
    public static boolean burningSiteAvailable() {
        if(!GENERATE_BURNING_SITES.get()||BURNING_WEIGHT.get()==0)return false;
        return WitcheryBridge.host().map(host->host.stockade()!=null&&host.witchHunter()!=null).orElse(false);
    }
    /** Called for each connector of one Placer. Its accepted pieces, including saved pool elements, own the limit. */
    public static List<StructurePoolElement> villageCandidates(Registry<StructureTemplatePool> pools,
            List<? super PoolElementStructurePiece> pieces,StructureTemplatePool pool,RandomSource random) {
        List<StructurePoolElement> original=pool.getShuffledTemplates(random);
        ResourceLocation key=pools.getKey(pool);
        if(!burningSiteAvailable()||key==null||!VILLAGE_HOUSES.contains(key))return original;
        for(Object piece:pieces)
            if(piece instanceof PoolElementStructurePiece p&&containsBurningSite(p.getElement()))return original;
        var result=new ArrayList<>(original);
        var addition=new BurningSiteElement(StructureTemplatePool.Projection.RIGID);
        for(int i=0;i<BURNING_WEIGHT.get();i++)result.add(addition);
        // Preserve every original weighted candidate, including the vanilla empty candidate.
        for(int i=result.size()-1;i>0;i--)Collections.swap(result,i,random.nextInt(i+1));
        return result;
    }
    private static boolean containsBurningSite(StructurePoolElement element) {return element instanceof BurningSiteElement;}
    private ConciliumWorldgen() {}
}
