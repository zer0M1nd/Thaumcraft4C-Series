package tc4revival.content;

import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class InfernalBonusAuditServerTests {
    @GameTest(template="wand_test_empty")
    public static void allLegacyInfernalBonusesHaveZeroBase(GameTestHelper test){
        String[] paths={"ore_gold","ore_iron","ore_cinnabar","ore_copper","cluster_gold","cluster_iron","cluster_cinnabar","cluster_copper","chicken","beef","pork","fish"};
        String[] outputs={"minecraft:gold_nugget","minecraft:iron_nugget","thaumcraft:quicksilver_drop","thaumcraft:copper_nugget","minecraft:gold_nugget","minecraft:iron_nugget","thaumcraft:quicksilver_drop","thaumcraft:copper_nugget","thaumcraft:chicken_nugget","thaumcraft:beef_nugget","thaumcraft:pork_nugget","thaumcraft:fish_nugget"};
        int cases=0;
        for(int i=0;i<paths.length;i++){
            var recipe=(InfernalBonusRecipe)test.getLevel().getRecipeManager().byKey(ResourceLocation.parse("thaumcraft:infernal_bonus/"+paths[i])).orElseThrow();
            var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.parse(outputs[i]));
            for(int bellows=0;bellows<=3;bellows++)for(int seed=0;seed<64;seed++){
                var reference=RandomSource.create(seed);int count=0;
                // Old TileArcaneFurnace roll, applied after API reset of output stack count.
                if(bellows==0){if(reference.nextInt(4)==0)count++;}
                else for(int b=0;b<bellows;b++)if(reference.nextFloat()<.44f)count++;
                ItemStack actual=recipe.roll(bellows,RandomSource.create(seed));
                test.assertTrue(actual.getCount()==count&&(count==0?actual.isEmpty():actual.is(item)),"Legacy bonus "+paths[i]+" bellows="+bellows+" seed="+seed);cases++;
            }
        }
        tc4revival.TC4Revival.LOGGER.info("PASS allLegacyInfernalBonusesHaveZeroBase: {} recipe/bellows/seed outputs",cases);test.succeed();
    }
}
