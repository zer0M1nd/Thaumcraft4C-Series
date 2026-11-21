package tc4revival.content;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.*;
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class EtherealBloomServerTests {
    @GameTest(template="tree_test_empty")
    public static void bloomBiomeRestorationAndRange(GameTestHelper test){
        var level=test.getLevel();var origin=test.absolutePos(new BlockPos(8,4,8));var target=origin.east(4);var outside=origin.offset(7,0,7);
        var original=EtherealBloomBlockEntity.storedBiome(level,target).unwrapKey().orElseThrow();var farOriginal=EtherealBloomBlockEntity.storedBiome(level,outside).unwrapKey().orElseThrow();
        try{
            WorldBiomeChanges.changeColumn(level,target,TCBiomes.TAINTED_LAND);WorldBiomeChanges.changeColumn(level,outside,TCBiomes.TAINTED_LAND);
            test.assertTrue(EtherealBloomBlockEntity.cleanse(level,origin,target),"Polluted column is restored");
            test.assertTrue(EtherealBloomBlockEntity.storedBiome(level,target).is(EtherealBloomBlockEntity.restoration(EtherealBloomBlockEntity.generatedBiome(level,target))),"Restoration uses generator biome, never the currently edited biome");
            test.assertTrue(!EtherealBloomBlockEntity.cleanse(level,origin,outside)&&EtherealBloomBlockEntity.storedBiome(level,outside).is(TCBiomes.TAINTED_LAND),"Diagonal cell beyond old squared radius is unaffected");
            WorldBiomeChanges.changeColumn(level,target,TCBiomes.MAGICAL_FOREST);EtherealBloomBlockEntity.cleanse(level,origin,target);
            test.assertTrue(EtherealBloomBlockEntity.storedBiome(level,target).is(EtherealBloomBlockEntity.restoration(EtherealBloomBlockEntity.generatedBiome(level,target))),"Magical forest is also restored to generator biome");
            WorldBiomeChanges.changeColumn(level,target,Biomes.DESERT);test.assertTrue(!EtherealBloomBlockEntity.cleanse(level,origin,target)&&EtherealBloomBlockEntity.storedBiome(level,target).is(Biomes.DESERT),"Ordinary changed biomes remain untouched");
            var registry=level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME);
            test.assertTrue(EtherealBloomBlockEntity.restoration(registry.getHolderOrThrow(TCBiomes.TAINTED_LAND)).equals(Biomes.PLAINS),"Naturally generated taint falls back to plains");
        }finally{WorldBiomeChanges.changeColumn(level,target,original);WorldBiomeChanges.changeColumn(level,outside,farOriginal);}pass(test,"generator-restoration-range");
    }
    @GameTest(template="tree_test_empty")
    public static void bloomScheduledCleansingAndFibreDecay(GameTestHelper test){
        var level=test.getLevel();var origin=test.absolutePos(new BlockPos(8,4,8));
        long seed=197;var random=net.minecraft.util.RandomSource.create(seed);int x=random.nextInt(8)-random.nextInt(8),z=random.nextInt(8)-random.nextInt(8);var target=origin.offset(x,0,z);
        test.assertTrue(x*x+z*z+.25<=81,"Seed chooses a point within range");var original=EtherealBloomBlockEntity.storedBiome(level,target).unwrapKey().orElseThrow();
        try{
            WorldBiomeChanges.changeColumn(level,target,TCBiomes.TAINTED_LAND);level.setBlockAndUpdate(target.below(),Blocks.STONE.defaultBlockState());
            var fibre=TCArcane.TAINT_FIBRES.get();level.setBlockAndUpdate(target,TaintFibreBlock.faces(fibre.defaultBlockState(),level,target));
            var bloom=new EtherealBloomBlockEntity(origin,TCArcane.ETHEREAL_BLOOM.get().defaultBlockState());bloom.setLevel(level);bloom.counter=18;
            bloom.tick();test.assertTrue(EtherealBloomBlockEntity.storedBiome(level,target).is(TCBiomes.TAINTED_LAND),"No work before twentieth counter tick");level.random.setSeed(seed);bloom.tick();
            test.assertTrue(!EtherealBloomBlockEntity.storedBiome(level,target).is(TCBiomes.TAINTED_LAND)&&level.getBlockState(target).is(fibre),"Tick restores biome without directly deleting taint fibre");
            fibre.randomTick(level.getBlockState(target),level,target,net.minecraft.util.RandomSource.create(3));test.assertTrue(level.isEmptyBlock(target),"Existing fibre logic then removes fibre in the cleaned biome");
            var restored=new EtherealBloomBlockEntity(origin,bloom.getBlockState());restored.load(bloom.saveWithoutMetadata());test.assertTrue(restored.counter==0&&restored.growthCounter==0,"Reload restarts visual growth and randomized tick phase");
        }finally{WorldBiomeChanges.changeColumn(level,target,original);}pass(test,"tick-and-indirect-decay");
    }
    @GameTest(template="wand_test_empty")
    public static void bloomPlantAndRecipe(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,4,3));var block=TCArcane.ETHEREAL_BLOOM.get();var state=block.defaultBlockState();
        level.setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());test.assertTrue(state.canSurvive(level,pos)&&state.getLightEmission(level,pos)==15&&state.getCollisionShape(level,pos).isEmpty(),"Cave plant survives on solid stone, emits full light and has no collision");
        level.setBlockAndUpdate(pos.below(),Blocks.AIR.defaultBlockState());test.assertTrue(!state.canSurvive(level,pos),"Plant needs supporting surface");
        var costs=new AspectAmounts();for(String aspect:java.util.List.of("praecantatio","herba","sano"))costs.add(TCAspects.catalog().require(aspect),16);costs.add(TCAspects.catalog().require("vitium"),8);
        var input=new CrucibleRecipe.Input(new ItemStack(TCArcane.plant(MagicalPlantBlock.Kind.SHIMMERLEAF)),costs,key->key.equals("ETHEREALBLOOM"));var recipe=CrucibleRecipe.find(level,input).orElseThrow();
        test.assertTrue(recipe.assemble(input,level.registryAccess()).is(TCItems.ETHEREAL_BLOOM.get())&&recipe.remainder(costs).orElseThrow().isEmpty(),"Shimmerleaf recipe consumes old aspect quantities");
        test.assertTrue(!recipe.matches(new CrucibleRecipe.Input(input.getItem(0),costs,key->false),level),"Research remains required");pass(test,"plant-and-recipe");
    }
    private static void pass(GameTestHelper test,String name){com.mojang.logging.LogUtils.getLogger().info("Ethereal bloom test passed: {}",name);test.succeed();}
}
