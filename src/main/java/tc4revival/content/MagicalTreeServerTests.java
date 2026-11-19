package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Authored with the tree system; these tests have not yet been run. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class MagicalTreeServerTests {
    private MagicalTreeServerTests() {}
    @GameTest(template="tree_test_empty",timeoutTicks=200)
    public static void forestOreVeinsKeepNonStoneAndDoNotCreateDeepVariants(GameTestHelper test) {
        var level=test.getLevel(); var origin=test.absolutePos(new BlockPos(9,12,9));
        for (int x=0;x<18;x++) for (int y=-5;y<=5;y++) for (int z=0;z<18;z++)
            level.setBlock(origin.offset(x,y,z),(x%2==0 ? Blocks.STONE : Blocks.DEEPSLATE).defaultBlockState(),2);
        test.assertTrue(ForestOresFeature.vein(level,new net.minecraft.world.level.levelgen.LegacyRandomSource(17010),
                origin,Blocks.IRON_ORE,32),"A large vein intersects the stone substrate");
        int ore=0;
        for (int x=0;x<18;x++) for (int y=-5;y<=5;y++) for (int z=0;z<18;z++) {
            var state=level.getBlockState(origin.offset(x,y,z));
            if (x%2!=0) test.assertTrue(state.is(Blocks.DEEPSLATE),"Legacy forest deposits leave new substrates unchanged");
            if (state.is(Blocks.IRON_ORE)) ore++;
            test.assertTrue(!state.is(Blocks.DEEPSLATE_IRON_ORE),"Old forest generation does not introduce a new ore variant");
        }
        test.assertTrue(ore>0,"The generated deposit contains real vanilla iron ore");
        test.succeed();
    }
    @GameTest(template="tree_test_empty",timeoutTicks=200)
    public static void forestSedimentsUseRiverBedAndPreserveClayGrassBoundary(GameTestHelper test) {
        var level=test.getLevel(); var center=test.absolutePos(new BlockPos(17,6,17));
        for (int dx=-7;dx<=7;dx++) for (int dz=-7;dz<=7;dz++) {
            level.setBlock(center.offset(dx,-1,dz),Blocks.DIRT.defaultBlockState(),2);
            for (int dy=0;dy<=2;dy++) level.setBlock(center.offset(dx,dy,dz),Blocks.WATER.defaultBlockState(),2);
        }
        test.assertTrue(center.equals(ForestSedimentsFeature.bottom(level,center.getX(),center.getZ())),
                "Sediments select the water immediately above the bed, not the air above the river");
        var grass=center.offset(1,-1,0); var coarse=center.offset(-1,-1,0); var outside=center.offset(3,-1,0);
        level.setBlock(grass,Blocks.GRASS_BLOCK.defaultBlockState(),2);
        level.setBlock(coarse,Blocks.COARSE_DIRT.defaultBlockState(),2);
        var random=new net.minecraft.world.level.levelgen.LegacyRandomSource(1) {
            @Override public int nextInt(int bound) { return 0; }
        };
        test.assertTrue(ForestSedimentsFeature.disk(level,random,center,Blocks.CLAY.defaultBlockState()),"A submerged dirt bed accepts clay");
        test.assertTrue(level.getBlockState(coarse).is(Blocks.CLAY),"Old dirt metadata variants are also replaced");
        test.assertTrue(level.getBlockState(grass).is(Blocks.GRASS_BLOCK),"Clay preserves grass even inside its radius");
        test.assertTrue(level.getBlockState(outside).is(Blocks.DIRT),"The circular radius does not replace distant bed blocks");
        ForestSedimentsFeature.disk(level,random,center,Blocks.SAND.defaultBlockState());
        test.assertTrue(level.getBlockState(grass).is(Blocks.SAND),"Sand replaces grass");
        test.assertTrue(level.getBlockState(coarse).is(Blocks.CLAY),"Sand does not replace the preceding clay deposit");
        var forbiddenRandom=new net.minecraft.world.level.levelgen.LegacyRandomSource(1) {
            @Override public int nextInt(int bound) { throw new AssertionError("Dry centers must not consume a radius roll"); }
        };
        test.assertTrue(!ForestSedimentsFeature.disk(level,forbiddenRandom,center.above(4),Blocks.GRAVEL.defaultBlockState()),
                "A dry center is rejected before radius selection");
        test.succeed();
    }
    @GameTest(template="tree_test_empty",timeoutTicks=200)
    public static void forestOakRetainsShortenedHeightAndUsesVanillaMaterials(GameTestHelper test) {
        var level=test.getLevel(); var origin=new BlockPos(17,3,17); var absolute=test.absolutePos(origin);
        test.setBlock(origin.below(),Blocks.GRASS_BLOCK);
        test.setBlock(origin.above(8),Blocks.STONE);
        var growth=new ForestOakGrowth();
        test.assertTrue(growth.grow(level,absolute,new net.minecraft.world.level.levelgen.LegacyRandomSource(17010)),
                "An obstruction at eight blocks shortens an otherwise valid forest oak");
        test.assertTrue(level.getBlockState(absolute).is(Blocks.OAK_LOG),"Forest oak uses vanilla oak, not greatwood");
        test.assertTrue(level.getBlockState(absolute.above(8)).is(Blocks.STONE),"The limiting obstacle is preserved");
        int leaves=0;
        for (int x=-12;x<=12;x++) for (int z=-12;z<=12;z++) for (int y=0;y<24;y++) {
            var pos=absolute.offset(x,y,z);
            if (level.getBlockState(pos).is(Blocks.OAK_LEAVES)) leaves++;
            level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
        }
        test.assertTrue(leaves>0,"The shortened tree still generates a leafy crown");
        test.assertTrue(growth.grow(level,absolute,new net.minecraft.world.level.levelgen.LegacyRandomSource(901)),
                "The same generator grows a second tree after the obstacle is removed");
        boolean taller=false;
        for (int x=-12;x<=12;x++) for (int z=-12;z<=12;z++) for (int y=8;y<24;y++)
            taller |= !level.getBlockState(absolute.offset(x,y,z)).isAir();
        test.assertTrue(taller,"A reused generator rolls a fresh taller tree after the obstruction is removed");
        test.succeed();
    }
    @GameTest(template="tree_test_empty",timeoutTicks=200)
    public static void spiderGreatwoodPlacesNestWebsAndFilledLegacyChest(GameTestHelper test) {
        for (int x=0;x<35;x++) for (int z=0;z<35;z++) {
            test.setBlock(new BlockPos(x,1,z),Blocks.DIRT); test.setBlock(new BlockPos(x,2,z),Blocks.GRASS_BLOCK);
        }
        var origin=new BlockPos(17,3,17);
        var random=new net.minecraft.world.level.levelgen.LegacyRandomSource(17010) {
            private int horizontal;
            @Override public int nextInt(int bound) {
                if (bound==10) return 0;
                if (bound==14) return horizontal++%2==0 ? 8 : 7;
                throw new AssertionError("Unexpected decoration random bound "+bound);
            }
        };
        test.assertTrue(GreatwoodGrowth.grow(test.getLevel(),test.absolutePos(origin),random,true),"The entire spider tree grows on a valid flat site");
        var spawner=test.getBlockEntity(origin.below());
        test.assertTrue(spawner instanceof net.minecraft.world.level.block.entity.SpawnerBlockEntity,"A real spawner lies one block below the original origin");
        test.assertTrue(spawner.saveWithoutMetadata().getCompound("SpawnData").getCompound("entity").getString("id").equals("minecraft:cave_spider"),"The spawner targets cave spiders");
        test.assertTrue(test.getLevel().getBlockState(test.absolutePos(origin.east())).is(Blocks.COBWEB),"A sampled empty block touching the trunk becomes a web");
        var chest=test.getBlockEntity(origin.below(2));
        test.assertTrue(chest instanceof net.minecraft.world.level.block.entity.ChestBlockEntity && !((net.minecraft.world.level.block.entity.ChestBlockEntity)chest).isEmpty(),"The chest is immediately filled through the old dungeon pool");
        var pool=LegacyChestTables.require(net.minecraft.resources.ResourceLocation.parse("thaumcraft:dungeon"));
        test.assertTrue(pool.entries().size()==38 && pool.totalWeight()==169,"The source pool includes all vanilla, Forge and TC4 dungeon entries");
        test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void vanillaLogRemovalMarksFourEdgeLeavesAndPlacedLeavesPersist(GameTestHelper test) {
        var origin=new BlockPos(1,2,1); var block=TCArcane.GREATWOOD_LEAVES.get();
        test.setBlock(origin.offset(4,0,0),Blocks.OAK_LOG);
        for (int x=0;x<4;x++) test.setBlock(origin.offset(x,0,0),block.defaultBlockState());
        var level=test.getLevel(); var absolute=test.absolutePos(origin);
        test.assertTrue(MagicalLeavesBlock.supported(level,absolute),"A four-edge connected path reaches a vanilla log");
        test.setBlock(origin.offset(-1,0,0),block.defaultBlockState());
        test.assertTrue(!MagicalLeavesBlock.supported(level,absolute.west()),"Five edges exceed TC4's support radius");
        var placed=origin.offset(0,1,0); test.setBlock(placed,block.defaultBlockState().setValue(MagicalLeavesBlock.PERSISTENT,true));
        level.removeBlock(test.absolutePos(origin.offset(4,0,0)),false);
        test.assertTrue(level.getBlockState(absolute).getValue(MagicalLeavesBlock.CHECK_DECAY),"The vanilla onRemove dispatch must notify custom leaves four blocks away");
        block.randomTick(level.getBlockState(absolute),level,absolute,level.random);
        test.assertTrue(level.getBlockState(absolute).isAir(),"An unsupported pending leaf decays");
        var placedPos=test.absolutePos(placed); block.randomTick(level.getBlockState(placedPos),level,placedPos,level.random);
        test.assertTrue(level.getBlockState(placedPos).is(block),"Player-placed leaves keep their persistence bit even when marked"); test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void exhaustedSilverwoodCoreBecomesOrdinaryOrientedWood(GameTestHelper test) {
        var pos=new BlockPos(2,2,2); var state=TCArcane.SILVERWOOD_NODE_LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS,Direction.Axis.X);
        test.setBlock(pos,state); var node=(AuraNodeBlockEntity)test.getBlockEntity(pos);
        node.configure(new tc4revival.aspect.AspectAmounts(),AuraNodeType.PURE,null,"tree-test",false);
        for (int tick=0;tick<1200;tick++) node.tick();
        var result=test.getLevel().getBlockState(test.absolutePos(pos));
        test.assertTrue(result.is(TCArcane.SILVERWOOD_LOG.get()) && result.getValue(RotatedPillarBlock.AXIS)==Direction.Axis.X,"Node exhaustion removes the knot without destroying or rotating the trunk");
        test.assertTrue(test.getLevel().getBlockEntity(test.absolutePos(pos))==null,"The ordinary log must not retain a ticking orphan node"); test.succeed();
    }
}
