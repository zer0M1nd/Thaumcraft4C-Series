package tc4revival.content;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.*;
import net.minecraft.util.RandomSource;
import net.minecraftforge.gametest.*;
@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class ObsidianTotemServerTests {
    @GameTest(template="wand_test_empty")
    public static void totemAdjacencyAndNodeStorage(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(4,3,4));var normal=TCArcane.OBSIDIAN_TOTEM.get();var embedded=TCArcane.OBSIDIAN_NODE_TOTEM.get();
        level.setBlockAndUpdate(pos,normal.defaultBlockState());test.assertTrue(level.getBlockState(pos).getValue(ObsidianTotemBlock.FORM)==0,"Isolated totem shows base faces");
        level.setBlockAndUpdate(pos.above(),embedded.defaultBlockState());test.assertTrue(level.getBlockState(pos).getValue(ObsidianTotemBlock.FORM)==1&&level.getBlockState(pos.above()).getValue(ObsidianTotemBlock.FORM)==2,"Mixed node and plain totem form shaded column and glyph head");
        var node=(AuraNodeBlockEntity)level.getBlockEntity(pos.above());var aspects=new tc4revival.aspect.AspectAmounts().add(tc4revival.aspect.TCAspects.catalog().require("aer"),20);
        node.configure(aspects,AuraNodeType.DARK,AuraNodeModifier.BRIGHT,"totem-test",false);var saved=node.saveWithoutMetadata();var restored=new AuraNodeBlockEntity(pos.above(),level.getBlockState(pos.above()));restored.load(saved);
        test.assertTrue(restored.nodeType()==AuraNodeType.DARK&&restored.aspects().total()==20&&restored.nodeId().equals("totem-test"),"Embedded node uses complete persistent node storage");
        level.removeBlock(pos,false);test.assertTrue(level.getBlockState(pos.above()).getValue(ObsidianTotemBlock.FORM)==0&&level.getBlockEntity(pos.above())==node&&node.aspects().total()==20,"Neighbor texture refresh preserves existing node");
        for(int x=-3;x<=3;x++)for(int y=-3;y<=3;y++)for(int z=-3;z<=3;z++)for(var side:Direction.Plane.HORIZONTAL) test.assertTrue(ObsidianTotemBlock.faceIndex(x%4+y%4+z%4+9,side.get3DDataValue())==Math.abs((side.get3DDataValue()+x%4+y%4+z%4)%4),"Signed coordinate glyph selection follows old remainder arithmetic");
        var drops=Block.getDrops(level.getBlockState(pos.above()),level,pos.above(),node);test.assertTrue(drops.size()==1&&drops.get(0).is(TCEldritch.OBSIDIAN_TILE.get().asItem()),"Embedded node drops obsidian tile instead of node item");pass(test,"adjacency-storage-loot");
    }
    @GameTest(template="wand_test_empty")
    public static void totemColumnGenerationBranches(GameTestHelper test) {
        var level=test.getLevel();var base=test.absolutePos(new BlockPos(4,2,4));
        for(int run=0;run<3;run++) {
            for(int y=0;y<=6;y++)level.setBlockAndUpdate(base.above(y),Blocks.AIR.defaultBlockState());level.setBlockAndUpdate(base,Blocks.STONE.defaultBlockState());
            if(run==0)level.setBlockAndUpdate(base.above(),Blocks.FERN.defaultBlockState());
            if(run==1)level.setBlockAndUpdate(base.above(2),Blocks.BEDROCK.defaultBlockState());
            if(run==2)level.setBlockAndUpdate(base.above(5),Blocks.STONE.defaultBlockState());
            var random=new net.minecraft.world.level.levelgen.LegacyRandomSource(7){@Override public int nextInt(int bound){return bound==4?0:super.nextInt(bound);}};
            RandomSource chosen=run==0?random:new net.minecraft.world.level.levelgen.LegacyRandomSource(9){@Override public int nextInt(int bound){return bound==4?3:super.nextInt(bound);}};
            test.assertTrue(ObsidianTotemFeature.generate(level,base,chosen)&&level.getBlockState(base).is(TCEldritch.OBSIDIAN_TILE.get()),"Valid ground becomes obsidian base");
            if(run==1)test.assertTrue(level.getBlockState(base.above()).is(TCArcane.OBSIDIAN_TOTEM.get())&&level.getBlockState(base.above(2)).is(Blocks.BEDROCK),"Early obstruction preserves short nodeless column");
            else if(run==2)test.assertTrue(level.getBlockState(base.above(5)).is(Blocks.STONE),"Fifth-block obstruction is preserved");
            else {var pos=base.above(2);test.assertTrue(level.getBlockState(pos).is(TCArcane.OBSIDIAN_NODE_TOTEM.get())&&level.getBlockEntity(pos) instanceof AuraNodeBlockEntity node&&node.nodeType()==AuraNodeType.DARK&&node.aspects().total()>0,"Early or forced fifth segment carries real dark node");}
        }
        level.setBlockAndUpdate(base,Blocks.GLASS.defaultBlockState());test.assertTrue(!ObsidianTotemFeature.generate(level,base,RandomSource.create(1)),"Unsupported ground rejects generation");pass(test,"generation-branches");
    }
    private static void pass(GameTestHelper test,String name){com.mojang.logging.LogUtils.getLogger().info("Obsidian totem test passed: {}",name);test.succeed();}
}
