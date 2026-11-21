package thaumcraft.api.worldgen;

import java.util.*;
import java.util.function.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import tc4revival.content.AuraNodeGeneration;
import tc4revival.aspect.AspectCatalog;
import thaumcraft.api.util.ExtensionRegistry;

/** Ordered worldgen extensions. Ore claims are exclusive; transformations do not reroll nodes. */
public final class NaturalGenerationRules {
    public record NodeContext(AspectCatalog catalog,AuraNodeGeneration.BiomeProfile biome,AuraNodeGeneration.Environment environment,
                              int rarity,boolean silverwood,boolean eerie,boolean small) {}
    public record ChanceContext(ServerLevel level,ChunkPos chunk,boolean previouslyGenerated) {}
    private record Ores(Predicate<FeaturePlaceContext<NoneFeatureConfiguration>> matches,Consumer<FeaturePlaceContext<NoneFeatureConfiguration>> generate) {}
    private static final ExtensionRegistry<Ores> ORES=new ExtensionRegistry<>();
    private static final ExtensionRegistry<BiFunction<ChanceContext,Double,Double>> CHANCE=new ExtensionRegistry<>();
    private static final ExtensionRegistry<BiFunction<NodeContext,AuraNodeGeneration.Result,AuraNodeGeneration.Result>> NODES=new ExtensionRegistry<>();
    private record Soil(String structure,Predicate<net.minecraft.world.level.block.Block> accepts) {}
    private static final ExtensionRegistry<Soil> SOILS=new ExtensionRegistry<>();
    public static void registerSoil(String id,int priority,String structure,Predicate<net.minecraft.world.level.block.Block> accepts){SOILS.register(id,priority,new Soil(structure,accepts));}
    public static boolean soil(String structure,net.minecraft.world.level.block.Block block,boolean fallback){
        for(var entry:SOILS.entries())if(entry.value().structure().equals(structure))return entry.value().accepts().test(block);return fallback;
    }
    public static boolean surface(String structure,net.minecraft.world.level.WorldGenLevel level,net.minecraft.core.BlockPos pos,BooleanSupplier fallback){
        if(SOILS.entries().stream().noneMatch(entry->entry.value().structure().equals(structure)))return fallback.getAsBoolean();
        int distance=0;while(!level.isEmptyBlock(pos.above(distance)))if(++distance>2)return false;
        var floor=pos.above(distance-1);var state=level.getBlockState(floor);
        return soil(structure,state.getBlock(),false)||(state.is(net.minecraft.world.level.block.Blocks.SNOW)||state.is(net.minecraft.world.level.block.Blocks.GRASS)||state.is(net.minecraft.world.level.block.Blocks.FERN))&&soil(structure,level.getBlockState(floor.below()).getBlock(),false);
    }
    private NaturalGenerationRules() {}
    public static void registerOreProvider(String id,int priority,Predicate<FeaturePlaceContext<NoneFeatureConfiguration>> matches,Consumer<FeaturePlaceContext<NoneFeatureConfiguration>> generate){ORES.register(id,priority,new Ores(matches,generate));}
    public static boolean generateOres(FeaturePlaceContext<NoneFeatureConfiguration> context){
        for(var entry:ORES.entries())if(entry.value().matches().test(context)){entry.value().generate().accept(context);return true;}return false;
    }
    public static void registerNodeChance(String id,int priority,BiFunction<ChanceContext,Double,Double> rule){CHANCE.register(id,priority,rule);}
    public static double nodeChance(ChanceContext context,double value){
        for(var entry:CHANCE.entries())value=entry.value().apply(context,value);
        if(!Double.isFinite(value)||value<0)throw new IllegalArgumentException("Invalid node chance");return value;
    }
    public static void registerNodeResult(String id,int priority,BiFunction<NodeContext,AuraNodeGeneration.Result,AuraNodeGeneration.Result> rule){NODES.register(id,priority,rule);}
    public static AuraNodeGeneration.Result nodeResult(NodeContext context,AuraNodeGeneration.Result value){
        for(var entry:NODES.entries())value=Objects.requireNonNull(entry.value().apply(context,value));return value;
    }
}
