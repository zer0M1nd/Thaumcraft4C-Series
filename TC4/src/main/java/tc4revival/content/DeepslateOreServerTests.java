package tc4revival.content;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.ItemTags;
import net.minecraft.resources.ResourceLocation;
import tc4revival.aspect.item.ItemStackAspects;
import tc4revival.aspect.TCAspects;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public class DeepslateOreServerTests {
    @GameTest(template="wand_test_empty") public static void variantsRetainOreBehaviorAndAddDarkness(GameTestHelper t){
        var config=tc4revival.config.TCConfig.SERVER.branchFeatures;boolean before=config.get();
        try{
            config.set(true);
            for(int i=0;i<8;i++){
                var base=TCArcane.ore(i);var deep=TCArcane.deepslateOre(i);
                var expected=ItemStackAspects.resolve(new ItemStack(base)).orElseThrow().copy();
                expected.add(TCAspects.catalog().require("tenebrae"),1);
                t.assertTrue(ItemStackAspects.resolve(new ItemStack(deep)).orElseThrow().view().equals(expected.view()),"Original aspects plus one darkness: "+i);
                var tag=ItemTags.create(ResourceLocation.parse(LegacyOreBlock.tag(i)));
                t.assertTrue(new ItemStack(base).is(tag)&&new ItemStack(deep).is(tag),"Shared ore ingredient tag: "+i);
                t.assertTrue(Math.abs(deep.defaultBlockState().getDestroySpeed(t.getLevel(),t.absolutePos(net.minecraft.core.BlockPos.ZERO))-2.25f)<.001,"Deepslate hardness ratio");
                t.assertTrue(deep.metadata()==i && deep.asItem().isEnabled(t.getLevel().enabledFeatures()),"Variant enabled");
            }
            for(var ore:java.util.List.of(TCArcane.ore(0),TCArcane.deepslateOre(0)))
                t.assertTrue(SpecialMining.apply(new ItemStack(ore),100,net.minecraft.util.RandomSource.create(1)).is(TCItems.cluster(ClusterType.CINNABAR)),"Both cinnabar variants support special-mining cluster conversion");
            config.set(false);
            t.assertTrue(!TCArcane.deepslateOre(0).asItem().isEnabled(t.getLevel().enabledFeatures()),"Branch gate disables variant");
        }finally{config.set(before);}
        t.succeed();
    }
}
