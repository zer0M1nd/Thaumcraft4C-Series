package tc4revival.content;

import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.*;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraftforge.gametest.*;
import terrablender.api.*;
import terrablender.worldgen.IExtendedParameterList;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public class NaturalBiomeServerTests {
    @GameTest(template="wand_test_empty", timeoutTicks=600)
    public static void naturalBiomeWeightsCodecAndGenerator(GameTestHelper test) {
        var access=test.getLevel().registryAccess();
        var registry=access.registryOrThrow(Registries.BIOME);
        var vanilla=new ArrayList<Pair<Climate.ParameterPoint,ResourceKey<Biome>>>();
        ThaumcraftBiomeRegion.vanillaBiomes(vanilla::add);
        var region=new ThaumcraftBiomeRegion(TCBiomes.MAGICAL_FOREST,5);
        var mapped=new ArrayList<Pair<Climate.ParameterPoint,ResourceKey<Biome>>>();
        region.addBiomes(registry,mapped::add);
        test.assertTrue(vanilla.size()==mapped.size(),"Complete climate partition is preserved");
        int replaced=0;
        for(int i=0;i<vanilla.size();i++) {
            var before=vanilla.get(i);var after=mapped.get(i);
            test.assertTrue(before.getFirst().equals(after.getFirst()),"Native climate shape is unchanged");
            boolean forest=ThaumcraftBiomeRegion.usesForestClimate(before.getSecond());
            test.assertTrue(after.getSecond().equals(forest?TCBiomes.MAGICAL_FOREST:Region.DEFERRED_PLACEHOLDER),
                    "Only forest families are replaced; all other climates defer to the real source");
            if(forest)replaced++;
        }
        test.assertTrue(replaced>0,"Forest climate entries exist");
        region.configureWeight(0);mapped.clear();region.addBiomes(registry,mapped::add);
        test.assertTrue(mapped.stream().allMatch(p->p.getSecond().equals(Region.DEFERRED_PLACEHOLDER)),"Zero disables the region");
        region.configureWeight(Integer.MAX_VALUE);
        test.assertTrue(region.getWeight()==10000,"Old extreme weights cannot overflow TerraBlender");
        var source=MultiNoiseBiomeSource.createFromPreset(access.registryOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
                .getHolderOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD));
        var old=new LegacyNaturalBiomeSource(source,registry.getHolderOrThrow(TCBiomes.MAGICAL_FOREST),
                registry.getHolderOrThrow(TCBiomes.TAINTED_LAND),815491,5,2);
        var ops=RegistryOps.create(NbtOps.INSTANCE,access);
        var saved=LegacyNaturalBiomeSource.CODEC.encodeStart(ops,old).getOrThrow(false,m->{throw new IllegalStateException(m);});
        ((net.minecraft.nbt.CompoundTag)saved).putString("type","thaumcraft:legacy_natural");
        var restored=BiomeSource.CODEC.parse(ops,saved).getOrThrow(false,m->{throw new IllegalStateException(m);});
        test.assertTrue(restored instanceof MultiNoiseBiomeSource,"Old saves unwrap to native source on decode");
        test.assertTrue(Regions.get(RegionType.OVERWORLD).stream().anyMatch(r->r.getName().equals(TCBiomes.MAGICAL_FOREST.location())),"Production forest region registered");
        pass("climate-codec-weights");test.succeed();
    }

    @GameTest(template="wand_test_empty", timeoutTicks=1200)
    public static void naturalBiomeRealTerrain(GameTestHelper test) {
        var level=test.getLevel();var source=level.getChunkSource().getGenerator().getBiomeSource();
        test.assertTrue(source instanceof MultiNoiseBiomeSource,"TerraBlender uses the native overworld source without a TC wrapper");
        var parameters=(IExtendedParameterList<Holder<Biome>>)(Object)level.registryAccess().registryOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST).getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD).parameters();
        test.assertTrue(parameters.isInitialized(),"TerraBlender initialized the live source");
        var sampler=level.getChunkSource().randomState().sampler();
        for(var expected:java.util.List.of(TCBiomes.MAGICAL_FOREST,TCBiomes.TAINTED_LAND)) {
            test.assertTrue(source.possibleBiomes().stream().anyMatch(b->b.is(expected)),"Possible biomes includes "+expected.location());
            BlockPos found=null;
            search:for(int radius=64;radius<=4096;radius+=64)for(int x=-radius;x<=radius;x+=64)for(int z:new int[]{-radius,radius}) {
                if(source.getNoiseBiome(x,16,z,sampler).is(expected)){found=new BlockPos(QuartPos.toBlock(x),64,QuartPos.toBlock(z));break search;}
            }
            test.assertTrue(found!=null,"Natural search locates "+expected.location());
            var chunk=level.getChunkAt(found);
            test.assertTrue(chunk.getNoiseBiome(QuartPos.fromBlock(found.getX()),16,QuartPos.fromBlock(found.getZ())).is(expected),"Full chunk stores chosen biome");
            test.assertTrue(LegacySurfaceHeight.at(level,found.getX(),found.getZ())>level.getMinBuildHeight(),"Terrain generated");
            com.mojang.logging.LogUtils.getLogger().info("TC_BIOME_TERRAIN {} at {}",expected.location(),found);
        }
        pass("real-terrain");test.succeed();
    }

    @GameTest(template="wand_test_empty", timeoutTicks=1200)
    public static void terraBlenderFrequencySurvey(GameTestHelper test) {
        var access=test.getLevel().registryAccess();
        var settings=access.registryOrThrow(Registries.NOISE_SETTINGS).getHolderOrThrow(NoiseGeneratorSettings.OVERWORLD);
        long[] counts=new long[3];double legacyForest=0,legacyTaint=0;int eligible=0,total=0;
        var climateCounts=new java.util.TreeMap<String,Integer>();
        for(long seed:new long[]{815491,42,17010,20261007}) {
            var original=new ArrayList<Pair<Climate.ParameterPoint,Holder<Biome>>>();
            var registry=access.registryOrThrow(Registries.BIOME);
            ThaumcraftBiomeRegion.vanillaBiomes(p->original.add(p.mapSecond(registry::getHolderOrThrow)));
            var vanilla=new Climate.ParameterList<>(original);
            var modern=new Climate.ParameterList<>(original);
            var extended=(IExtendedParameterList<Holder<Biome>>)(Object)modern;
            extended.initializeForTerraBlender(access,RegionType.OVERWORLD,seed);
            var sampler=RandomState.create(settings.value(),access.lookupOrThrow(Registries.NOISE),seed).sampler();
            for(int x=-8192;x<8192;x+=128)for(int z=-8192;z<8192;z+=128) {
                var target=sampler.sample(x,16,z);var base=vanilla.findValue(target);
                climateCounts.merge(base.unwrapKey().orElseThrow().location().toString(),1,Integer::sum);
                var actual=extended.findValuePositional(target,x,16,z);
                total++;int selected=actual.is(TCBiomes.MAGICAL_FOREST)?1:actual.is(TCBiomes.TAINTED_LAND)?2:0;counts[selected]++;
                // A modern-climate proxy for TC4's old WARM/COOL weighted pools,
                // not a claim to have generated identical 1.7.10 terrain.
                if(LegacyNaturalBiomeSource.eligible(base)) {
                    eligible++;double denominator=(target.temperature()<=0?40:60)+7;
                    legacyForest+=5/denominator;legacyTaint+=2/denominator;
                }
                if(selected!=0)test.assertTrue(ThaumcraftBiomeRegion.usesForestClimate(base.unwrapKey().orElseThrow()),"TC biomes cannot escape forest climate mappings");
            }
        }
        com.mojang.logging.LogUtils.getLogger().info("TC_BIOME_SURVEY samples={} eligible={} forest={} taint={} oldPoolForestEstimate={} oldPoolTaintEstimate={}",
                total,eligible,counts[1],counts[2],legacyForest,legacyTaint);
        com.mojang.logging.LogUtils.getLogger().info("TC_BIOME_CLIMATES {}",climateCounts);
        test.assertTrue(counts[1]>0&&counts[2]>0,"Both biomes occur across seeds");
        test.assertTrue((double)counts[1]/counts[2]>1.8&&(double)counts[1]/counts[2]<3.3,"Relative frequency remains near legacy 5:2");
        test.assertTrue(counts[1]/legacyForest>.75&&counts[1]/legacyForest<1.25,
                "Default forest frequency stays within 25% of the legacy-pool proxy");
        test.assertTrue(counts[2]/legacyTaint>.75&&counts[2]/legacyTaint<1.25,
                "Default taint frequency stays within 25% of the legacy-pool proxy");
        pass("frequency-survey");test.succeed();
    }
    @GameTest(template="tree_test_empty", timeoutTicks=200)
    public static void magicalForestAlwaysUsesLegacyLargeOak(GameTestHelper test) {
        var level=test.getLevel();var base=test.absolutePos(new BlockPos(17,3,17));
        var generator=level.getChunkSource().getGenerator();
        // Use the registered production feature, including the silverwood/greatwood rolls.
        // First shorten a tree against an obstacle, then grow in clear air in the same feature.
        for(int attempt=0;attempt<2;attempt++) {
            for(var p:BlockPos.betweenClosed(base.offset(-12,0,-12),base.offset(12,25,12)))
                level.setBlock(p,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
            level.setBlock(base.below(),net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState(),2);
            if(attempt==0)level.setBlock(base.above(8),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),2);
            var random=new LegacyRandomSource(17010) {
                private int choices;
                @Override public int nextInt(int bound) {
                    if(choices++<2){test.assertTrue(bound==14||bound==10,"Legacy special-tree chances");return 1;}
                    return super.nextInt(bound);
                }
            };
            test.assertTrue(TCWorldFeatures.FOREST_TREE.get().place(new net.minecraft.world.level.levelgen.feature.FeaturePlaceContext<>(
                    java.util.Optional.empty(),level,generator,random,base,
                    net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.INSTANCE)),"Natural forest tree placed");
            int highestLeaf=-1,branches=0;
            for(var p:BlockPos.betweenClosed(base.offset(-12,0,-12),base.offset(12,25,12))) {
                var state=level.getBlockState(p);
                if(state.is(net.minecraft.world.level.block.Blocks.OAK_LEAVES))highestLeaf=Math.max(highestLeaf,p.getY()-base.getY());
                if(state.is(net.minecraft.world.level.block.Blocks.OAK_LOG)&&
                        state.getValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS)!=net.minecraft.core.Direction.Axis.Y)branches++;
            }
            if(attempt==1)test.assertTrue(highestLeaf>=10&&branches>0,"Unobstructed oak has the legacy tall, branching crown, even after a shortened tree");
        }
        var biome=level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(TCBiomes.MAGICAL_FOREST).value();
        boolean decoration=biome.getGenerationSettings().features().stream().flatMap(s->s.stream()).anyMatch(
                f->f.unwrapKey().map(k->k.location().toString().equals("thaumcraft:forest_decoration")).orElse(false));
        test.assertTrue(decoration,"Natural forest contains the TC forest decorator");
        pass("large-oak-production");test.succeed();
    }
    private static void pass(String name){com.mojang.logging.LogUtils.getLogger().info("TC_BIOME_PASS {}",name);}
}
