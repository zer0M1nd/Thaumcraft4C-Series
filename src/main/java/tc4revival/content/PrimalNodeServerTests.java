package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.TCAspects;

/** Node mutation tests intentionally isolate the pearl's destructive aftermath. Not yet run. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class PrimalNodeServerTests {
    private PrimalNodeServerTests() {}
    @GameTest(template="wand_test_empty")
    public static void capacityChangesPreserveChargeAndRejectNegativeBase(GameTestHelper test) {
        var pos=new BlockPos(2,2,2); test.setBlock(pos,TCArcane.AURA_NODE.get());
        var node=(AuraNodeBlockEntity)test.getBlockEntity(pos); var catalog=TCAspects.catalog();
        var air=catalog.require("aer"); var earth=catalog.require("terra"); var light=catalog.require("lux");
        node.configure(new AspectAmounts().add(air,1).add(earth,10).add(light,1),AuraNodeType.NORMAL,AuraNodeModifier.FADING,"pearl-test",false);
        node.take(earth,8);
        var random=new Rolls(new int[]{6,6,2,3,3,3,3,3,3,2},new int[]{0,5,1,0,0,1,0,0,0,1});
        node.rejuvenate(false,random); random.exhausted();
        test.assertTrue(node.base().amount(air)==1,"The attempted negative base reduction fails instead of creating -1");
        test.assertTrue(node.base().amount(earth)==13 && node.aspects().amount(earth)==2,"More capacity does not refill existing vis");
        test.assertTrue(node.base().amount(light)==0 && node.aspects().amount(light)==1,"Compound capacity can reach zero while its current charge remains");
        test.assertTrue(node.base().amount(catalog.require("ignis"))==1 && node.aspects().amount(catalog.require("ignis"))==1,"A missing primal gets capacity and one charge");
        test.assertTrue(node.nodeModifier()==AuraNodeModifier.PALE,"Fading improves by one modifier step");
        var reloaded=new AuraNodeBlockEntity(test.absolutePos(pos),node.getBlockState()); reloaded.load(node.saveWithoutMetadata());
        test.assertTrue(reloaded.base().amount(earth)==13 && reloaded.aspects().amount(earth)==2 && reloaded.nodeModifier()==AuraNodeModifier.PALE,"Capacity, charge and modifier survive serialization independently");
        test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void researchedPearlSeedsAllPrimalsWithoutFillingThem(GameTestHelper test) {
        var pos=new BlockPos(2,2,2); test.setBlock(pos,TCArcane.AURA_NODE.get());
        var node=(AuraNodeBlockEntity)test.getBlockEntity(pos);
        node.configure(new AspectAmounts(),AuraNodeType.PURE,null,"empty-pearl-test",false);
        var random=new Rolls(new int[]{4,4,4,4,4,4,5},new int[]{3,3,3,3,3,3,0});
        node.rejuvenate(true,random); random.exhausted();
        for (var aspect:TCAspects.catalog().primals()) test.assertTrue(node.base().amount(aspect)==3 && node.aspects().amount(aspect)==1,"Research allows capacity three while adding only one current vis");
        test.assertTrue(node.nodeType()==AuraNodeType.PURE && node.nodeModifier()==AuraNodeModifier.BRIGHT,"The pearl preserves node type and can improve a normal modifier");
        test.succeed();
    }
    private static  class Rolls extends LegacyRandomSource {
        private final int[] bounds, values; private int index;
        Rolls(int[] bounds,int[] values) { super(0); this.bounds=bounds; this.values=values; }
        @Override public int nextInt(int bound) {
            if (index>=values.length || bounds[index]!=bound) throw new AssertionError("Unexpected random bound at "+index+": "+bound);
            return values[index++];
        }
        @Override public boolean nextBoolean() { return nextInt(2)!=0; }
        void exhausted() { if (index!=values.length) throw new AssertionError("Missing random calls"); }
    }
}
