package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class CrucibleInteractionServerTests {
    @GameTest(template="wand_test_empty")
    public static void crucibleScaldAndRemovalPreserveLegacyBoundaries(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,2,2));var other=pos.east(2);
        var block=TCArcane.CRUCIBLE.get();var state=block.defaultBlockState();
        var storage=level.getDataStorage();String key="thaumcraft_flux_spills";
        var original=storage.computeIfAbsent(FluxSpills::load,FluxSpills::new,key);
        var queue=new FluxSpills();storage.set(key,queue);
        var cow=EntityType.COW.create(level);
        test.assertTrue(cow!=null,"Cow fixture created");
        try {
            for(var p:BlockPos.betweenClosed(pos.offset(-1,-1,-1),other.offset(1,1,1)))level.setBlock(p,Blocks.STONE.defaultBlockState(),2);
            level.setBlock(pos,state,2);level.setBlock(other,state,2);
            var tile=(CrucibleBlockEntity)level.getBlockEntity(pos);var second=(CrucibleBlockEntity)level.getBlockEntity(other);
            seed(tile,151,1,0);seed(second,151,1,0);
            cow.tickCount=10;float before=cow.getHealth();block.entityInside(state,level,pos,cow);
            test.assertTrue(cow.getHealth()==before-1,"Entity tick ten applies one point of scald damage");
            cow.invulnerableTime=0;before=cow.getHealth();
            for(int i=11;i<20;i++){cow.tickCount=i;block.entityInside(state,level,pos,cow);}
            test.assertTrue(cow.getHealth()==before,"Ticks between scald periods do not damage");
            cow.tickCount=20;
            block.entityInside(state,level,other,cow);
            test.assertTrue(cow.getHealth()==before-1,"Moving to another crucible uses the entity's tick cadence");
            for(int heat:new int[]{150,151})for(int water:new int[]{0,1}) {
                seed(tile,heat,water,0);cow.invulnerableTime=0;before=cow.getHealth();
                cow.tickCount=30;block.entityInside(state,level,pos,cow);
                test.assertTrue(cow.getHealth()==before-(heat==151&&water==1?1:0),"Scald requires heat above 150 and positive water");
            }
            seed(tile,200,1000,2001);tile.empty();
            test.assertTrue(tile.contents().water()==0&&tile.contents().aspects().isEmpty(),"Explicit empty clears water and all aspects");
            test.assertTrue(debt(queue)==744,"Odd total floors to 1000 attempts, 256 immediate");
            tile.empty();test.assertTrue(debt(queue)==744,"Repeated empty cannot replay spills");
            seed(tile,200,1,2001);level.setBlock(pos,Blocks.STONE.defaultBlockState(),3);
            test.assertTrue(level.getBlockEntity(pos)==null&&debt(queue)==1488,"Actual block replacement empties once and retains queued debt after BE removal");
        } finally {
            cow.discard();storage.set(key,original);
            level.removeBlockEntity(pos);level.removeBlockEntity(other);
            for(var p:BlockPos.betweenClosed(pos.offset(-1,-1,-1),other.offset(1,1,1)))level.setBlock(p,Blocks.AIR.defaultBlockState(),2);
        }
        tc4revival.TC4Revival.LOGGER.info("PASS crucibleScaldAndRemovalPreserveLegacyBoundaries: shared ten-callback scald, heat/water, odd empty, replay and actual removal debt");test.succeed();
    }
    private static long debt(FluxSpills data){long sum=0;for(var e:data.save(new CompoundTag()).getList("Pending",Tag.TAG_COMPOUND))sum+=((CompoundTag)e).getLong("Attempts");return sum;}
    private static void seed(CrucibleBlockEntity tile,int heat,int water,int amount){var tag=new CompoundTag();tag.putShort("Heat",(short)heat);tag.putString("FluidName","water");tag.putInt("Amount",water);var aspects=new AspectAmounts();if(amount>0)aspects.add(TCAspects.catalog().require("aer"),amount);AspectNbt.write(tag,aspects);tile.load(tag);}
}
