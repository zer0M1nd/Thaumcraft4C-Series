package tc4revival.content;

import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class ShockPlacementServerTests {
    private static RandomSource random(Entity entity) {
        try{for(var field:Entity.class.getDeclaredFields())if(field.getType()==RandomSource.class){field.setAccessible(true);return (RandomSource)field.get(entity);}}
        catch(ReflectiveOperationException e){throw new AssertionError(e);}throw new AssertionError("Missing entity random source");
    }
    @GameTest(template="wand_test_empty",timeoutTicks=100)
    public static void earthShockTwentyPlacementAttemptsMatchSeededLegacyFootprint(GameTestHelper test) {
        var level=test.getLevel();var base=test.absolutePos(new BlockPos(10,14,10));var owner=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"ShockPlacement"));owner.moveTo(Vec3.atCenterOf(base.above(12)));int cases=0;
        try {
            for(var p:BlockPos.betweenClosed(base.offset(-8,0,-8),base.offset(8,0,8)))level.setBlock(p,Blocks.STONE.defaultBlockState(),2);
            for(int area:new int[]{4,6,8})for(int sample=0;sample<16;sample++) {
                for(var p:BlockPos.betweenClosed(base.offset(-8,1,-8),base.offset(8,1,8)))level.setBlock(p,Blocks.AIR.defaultBlockState(),2);
                long seed=sample*104729L;var oracle=RandomSource.create(seed);var expected=new HashSet<BlockPos>();
                for(int attempt=0;attempt<20;attempt++)expected.add(base.offset(oracle.nextInt(area)-oracle.nextInt(area),1,oracle.nextInt(area)-oracle.nextInt(area)));
                var orb=new ShockOrbEntity(level,owner,area,5);orb.setPos(Vec3.atCenterOf(base.above()));random(orb).setSeed(seed);orb.onHit(new BlockHitResult(orb.position(),Direction.UP,base,false));
                var actual=new HashSet<BlockPos>();for(var p:BlockPos.betweenClosed(base.offset(-8,1,-8),base.offset(8,1,8)))if(level.getBlockState(p).is(TCArcane.SHOCK_FIELD.get()))actual.add(p.immutable());
                test.assertTrue(actual.equals(expected),"Twenty legacy triangular draws produce unique ground cells: area="+area+" seed="+seed+" expected="+expected.size()+" actual="+actual.size());test.assertTrue(orb.isRemoved(),"Placement impact consumes orb");
                for(var p:BlockPos.betweenClosed(base.offset(-8,0,-8),base.offset(8,0,8)))test.assertTrue(level.getBlockState(p).is(Blocks.STONE),"Field placement preserves ground");cases++;
            }
            tc4revival.TC4Revival.LOGGER.info("PASS earthShockTwentyPlacementAttemptsMatchSeededLegacyFootprint: {} area/seed footprints, twenty draws each",cases);test.succeed();
        }finally{for(var p:BlockPos.betweenClosed(base.offset(-8,0,-8),base.offset(8,1,8)))level.setBlock(p,Blocks.AIR.defaultBlockState(),2);}
    }
}
