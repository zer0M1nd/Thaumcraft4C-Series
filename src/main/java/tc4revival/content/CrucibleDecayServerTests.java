package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class CrucibleDecayServerTests {
    @GameTest(template="wand_test_empty")
    public static void crucibleDecayCyclesAndComparatorMatchLegacy(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,2,2));
        var air=TCAspects.catalog().require("aer");var metal=TCAspects.catalog().require("metallum");
        level.setBlock(pos,TCArcane.CRUCIBLE.get().defaultBlockState(),2);
        var tile=(CrucibleBlockEntity)level.getBlockEntity(pos);
        try {
            // Contain world spills; invoke real BE ticks synchronously to inspect exact boundaries.
            for(var p:BlockPos.betweenClosed(pos.offset(-1,-1,-1),pos.offset(1,1,1)))
                if(!p.equals(pos))level.setBlock(p,Blocks.STONE.defaultBlockState(),2);
            level.setBlock(pos.below(),TCArcane.NITOR.get().defaultBlockState(),2);
            for(var aspect:java.util.List.of(air,metal)) {
                seed(tile,200,1000,new AspectAmounts().add(aspect,10));
                ticks(tile,200);
                test.assertTrue(tile.contents().aspects().amount(aspect)==10 && tile.contents().water()==1000,"No decomposition through initial tick 200");
                tile.tick();
                test.assertTrue(tile.contents().aspects().amount(aspect)==9 && tile.contents().water()==998,"First decay at 201 debits one aspect and 2 mB");
                long total=aspect.isPrimal()?9:10;
                test.assertTrue(tile.contents().aspects().total()==total,"Compound converts to exactly one component; primal disappears");
                if(!aspect.isPrimal()) {
                    int children=aspect.components().stream().mapToInt(id->tile.contents().aspects().amount(TCAspects.catalog().require(id))).sum();
                    test.assertTrue(children==1,"Output is an actual legacy component");
                }
                ticks(tile,100);test.assertTrue(tile.contents().water()==998,"Subsequent cycle waits 100 ticks");
                tile.tick();test.assertTrue(tile.contents().water()==996,"Subsequent decay at 101 ticks");
            }
            seed(tile,200,1000,new AspectAmounts());tile.contents().dissolve(new AspectAmounts().add(air,10));
            ticks(tile,250);test.assertTrue(tile.contents().water()==1000,"Dissolve resets grace period to 250 ticks");
            tile.tick();test.assertTrue(tile.contents().water()==998,"Dissolve grace ends at 251");
            var saved=tile.saveWithoutMetadata();tile.load(saved);
            ticks(tile,100);test.assertTrue(tile.contents().water()==998,"NBT retains elapsed decay phase");
            tile.tick();test.assertTrue(tile.contents().water()==996,"Reload continues the next decay at 101");
            // Overflow is independent of boiling and water, and stops exactly at 100.
            seed(tile,0,0,new AspectAmounts().add(air,102));
            ticks(tile,4);test.assertTrue(tile.contents().aspects().total()==102,"Overflow waits for fifth tick");
            tile.tick();test.assertTrue(tile.contents().aspects().total()==101,"Cold dry overflow at fifth tick");
            ticks(tile,5);test.assertTrue(tile.contents().aspects().total()==100,"Second overflow reaches limit");
            ticks(tile,205);test.assertTrue(tile.contents().aspects().total()==100,"No overflow at exact capacity or cold decay");
            for(int amount:new int[]{0,1,7,8,50,99,100,101,1000}) {
                seed(tile,0,0,amount==0?new AspectAmounts():new AspectAmounts().add(air,amount));
                int output=tile.getBlockState().getBlock().getAnalogOutputSignal(tile.getBlockState(),level,pos);
                int expected=switch(amount){case 0->0;case 1,7->1;case 8->2;case 50->8;case 99->14;case 100,101,1000->15;default->throw new AssertionError();};
                test.assertTrue(output==expected,"Legacy comparator boundary "+amount+": "+output+" != "+expected);
            }
        } finally {
            level.removeBlockEntity(pos);
            for(var p:BlockPos.betweenClosed(pos.offset(-1,-1,-1),pos.offset(1,1,1)))level.setBlock(p,Blocks.AIR.defaultBlockState(),2);
        }
        tc4revival.TC4Revival.LOGGER.info("PASS crucibleDecayCyclesAndComparatorMatchLegacy: initial/repeat/dissolve/reload grace, primal/compound, cold dry overflow and nine comparator boundaries");
        test.succeed();
    }
    private static void ticks(CrucibleBlockEntity tile,int count){for(int i=0;i<count;i++)tile.tick();}
    private static void seed(CrucibleBlockEntity tile,int heat,int water,AspectAmounts aspects){
        var tag=new CompoundTag();tag.putShort("Heat",(short)heat);tag.putString("FluidName","water");tag.putInt("Amount",water);AspectNbt.write(tag,aspects);tile.load(tag);
    }
}
