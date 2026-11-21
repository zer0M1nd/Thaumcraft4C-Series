package tc4revival.content;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.item.*;
import tc4revival.aspect.recipe.*;
import tc4revival.research.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class DeviceScanMetadataServerTests {
    @GameTest(template="wand_test_empty")
    public static void deviceScanUsesLegacyPickedMetadataInsteadOfPartNumbers(GameTestHelper test) {
        checkPickedMetadata(test,true);
    }
    @GameTest(template="wand_test_empty")
    public static void devicePickedMetadataIdentityAndContinuity(GameTestHelper test) {
        checkPickedMetadata(test,false);
    }
    private static void checkPickedMetadata(GameTestHelper test,boolean compareLegacyAmounts) {
        var blocks=List.of(TCArcane.METAL_GRATE.get(),TCArcane.ARCANE_PRESSURE_PLATE.get(),TCArcane.THAUMATORIUM.get(),
                TCArcane.ADVANCED_ALCHEMY_FURNACE.get(),TCArcane.INFERNAL_FURNACE.get(),TCArcane.INFUSION_PILLAR.get(),TCArcane.INFUSION_PILLAR_TOP.get());
        // Independent old getItem/getDamageValue/damageDropped results, not read from generated rules.
        String[] names={"blockMetalDevice","blockWoodenDevice","blockMetalDevice","blockAlchemyFurnace","blockArcaneFurnace","blockStoneDevice","blockStoneDevice"};
        int[] metadata={5,2,9,0,0,7,6};int states=0;
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,2,2));
        for(int family=0;family<blocks.size();family++) {
            var key=new RecipeAspectData.Key("Thaumcraft:"+names[family],metadata[family]);
            var expectedHash=Integer.toString(LegacyItemMappings.identity().scanHash(key));
            var expectedAspects=new StackAspectBonuses(tc4revival.aspect.TCAspects.catalog()).apply(RecipeAspects.newEngine().object(key),null,null,List.of());
            ScanTarget first=null;
            for(var state:blocks.get(family).getStateDefinition().getPossibleStates()) {
                level.setBlock(pos,Blocks.AIR.defaultBlockState(),18);level.setBlock(pos,state,18);
                test.assertTrue(level.getBlockState(pos).equals(state),"Actual device state remains in fixture: "+state);
                var mapping=BlockScanTargets.target(state).orElseThrow();
                test.assertTrue(key.equals(mapping.legacyKey()),"Old picked identity, not part number: "+state);
                var target=ScanTargetResolver.block(level,pos).orElseThrow();
                test.assertTrue(target.identity().orElseThrow().equals(expectedHash),"Actual scan hash uses picked metadata: "+state);
                if(compareLegacyAmounts)test.assertTrue(target.aspects().view().equals(BlockAspects.resolve(state).orElseGet(()->BlockScanTargets.aspects(mapping)).view()),"Current authored aspects take priority while old scan identity stays compatible: "+state);
                test.assertTrue(target.clue().isPresent(),"Picked identity supplies research clue input");
                if(first!=null)test.assertTrue(first.identity().equals(target.identity())&&first.continuity().equals(target.continuity()),"State changes do not create duplicate scan identities within the old picked family");
                else first=target;
                states++;
            }
        }
        tc4revival.TC4Revival.LOGGER.info("PASS picked metadata checks: {} families, {} states, legacy amounts compared={}",blocks.size(),states,compareLegacyAmounts);test.succeed();
    }
}
