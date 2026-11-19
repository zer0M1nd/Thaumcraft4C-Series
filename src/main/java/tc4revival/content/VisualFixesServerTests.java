package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import tc4revival.aspect.TCAspects;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public class VisualFixesServerTests {
    @GameTest(template="wand_test_empty")
    public static void jarLightFollowsContentsIncludingFilteredVoidJarsAndReload(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,2,2));
        var aspect=TCAspects.catalog().require("aer");
        for(var block:java.util.List.of(TCArcane.ESSENTIA_JAR.get(),TCArcane.VOID_JAR.get())) {
            level.setBlockAndUpdate(pos,block.defaultBlockState());
            var jar=(EssentiaJarBlockEntity)level.getBlockEntity(pos);
            test.assertTrue(level.getBlockState(pos).getLightEmission(level,pos)==0,"Empty jar is dark");
            jar.label(aspect,net.minecraft.core.Direction.NORTH);
            test.assertTrue(level.getBlockState(pos).getLightEmission(level,pos)==0,"Label alone does not emit light");
            jar.addToContainer(aspect,1);
            test.assertTrue(level.getBlockState(pos).getLightEmission(level,pos)==9,"Filled jar emits light");
            var saved=jar.saveWithFullMetadata();
            jar.empty();
            test.assertTrue(level.getBlockState(pos).getLightEmission(level,pos)==0,"Emptying turns light off");
            jar.load(saved);jar.onLoad();
            test.assertTrue(level.getBlockState(pos).getLightEmission(level,pos)==9,"Reload restores light");
            jar.takeFromContainer(aspect,1);
            test.assertTrue(level.getBlockState(pos).getLightEmission(level,pos)==0,"Last extraction turns light off");
            jar.readItem(new net.minecraft.world.item.ItemStack(TCItems.ESSENTIA_JAR.get()));
            test.assertTrue(level.getBlockState(pos).getLightEmission(level,pos)==0,"Empty portable jar remains dark");
        }
        test.succeed();
    }
}
