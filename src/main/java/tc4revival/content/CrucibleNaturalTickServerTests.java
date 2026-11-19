package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class CrucibleNaturalTickServerTests {
    @GameTest(template="wand_test_empty",timeoutTicks=480)
    public static void cruciblePaidGraceUsesNaturalWorldTicks(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,2,2));
        for(var p:BlockPos.betweenClosed(pos.offset(-1,-1,-1),pos.offset(1,1,1)))level.setBlock(p,Blocks.STONE.defaultBlockState(),2);
        level.setBlock(pos.below(),TCArcane.NITOR.get().defaultBlockState(),2);
        level.setBlock(pos,TCArcane.CRUCIBLE.get().defaultBlockState(),2);
        var tile=(CrucibleBlockEntity)level.getBlockEntity(pos);
        var recipe=(CrucibleRecipe)level.getRecipeManager().byKey(ResourceLocation.parse("thaumcraft:crucible/pure_iron")).orElseThrow();
        var amounts=recipe.costs().add(TCAspects.catalog().require("aer"),10);
        var saved=new CompoundTag();saved.putShort("Heat",(short)200);saved.putString("FluidName","water");saved.putInt("Amount",1000);AspectNbt.write(saved,amounts);tile.load(saved);
        test.assertTrue(tile.contents().pay(recipe),"Loaded recipe pays and starts post-craft grace");
        // Bracket boundaries to avoid depending on GameTest runner versus BE tick ordering.
        test.runAfterDelay(349,()->verify(test,pos,tile,950,10,false));
        test.runAfterDelay(353,()->verify(test,pos,tile,948,9,false));
        test.runAfterDelay(455,()->verify(test,pos,tile,946,8,true));
    }
    private static void verify(GameTestHelper test,BlockPos pos,CrucibleBlockEntity tile,int water,int aspects,boolean last){
        try {
            test.assertTrue(test.getLevel().getBlockEntity(pos)==tile,"Original BE still naturally ticking");
            test.assertTrue(tile.contents().water()==water&&tile.contents().aspects().total()==aspects,"Natural post-payment grace and next period: expected "+water+"/"+aspects+", got "+tile.contents().water()+"/"+tile.contents().aspects().total());
        } catch(RuntimeException|AssertionError failure){clear(test,pos);throw failure;}
        if(last){clear(test,pos);tc4revival.TC4Revival.LOGGER.info("PASS cruciblePaidGraceUsesNaturalWorldTicks: no manual BE ticks, payment grace bracket 349/353 and next period 455");test.succeed();}
    }
    private static void clear(GameTestHelper test,BlockPos pos){var level=test.getLevel();level.removeBlockEntity(pos);for(var p:BlockPos.betweenClosed(pos.offset(-1,-1,-1),pos.offset(1,1,1)))level.setBlock(p,Blocks.AIR.defaultBlockState(),2);}
}
