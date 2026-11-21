package tc4revival.content;

import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class ShockWorldServerTests {
    @GameTest(template="wand_test_empty",timeoutTicks=100)
    public static void shockOrbNaturallyHitsGroundAndDamagesNearbyMob(GameTestHelper test) {
        var level=test.getLevel();var base=test.absolutePos(new BlockPos(5,12,5));var forced=new ArrayList<ChunkPos>();for(int x=(base.getX()-4)>>4;x<=(base.getX()+4)>>4;x++)for(int z=(base.getZ()-4)>>4;z<=(base.getZ()+4)>>4;z++)if(level.setChunkForced(x,z,true))forced.add(new ChunkPos(x,z));
        for(var p:BlockPos.betweenClosed(base.offset(-4,0,-4),base.offset(4,0,4)))level.setBlock(p,Blocks.STONE.defaultBlockState(),2);
        var owner=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"ShockWorld"));owner.moveTo(Vec3.atCenterOf(base.above(10)));var cow=EntityType.COW.create(level);var orb=new ShockOrbEntity(level,owner,4,5);
        Runnable cleanup=()->{cow.discard();orb.discard();for(var p:BlockPos.betweenClosed(base.offset(-4,0,-4),base.offset(4,1,4)))level.setBlock(p,Blocks.AIR.defaultBlockState(),2);forced.forEach(c->level.setChunkForced(c.x,c.z,false));};
        test.runAfterDelay(20,()->{cow.moveTo(Vec3.atBottomCenterOf(base.offset(2,1,0)));cow.setNoAi(true);level.addFreshEntity(cow);orb.setPos(Vec3.atCenterOf(base.above(4)));orb.setDeltaMovement(0,-1,0);level.addFreshEntity(orb);});
        test.runAfterDelay(28,()->{try{test.assertTrue(orb.isRemoved()&&orb.tickCount<20,"Natural flight ends at ground well before expiry");test.assertTrue(cow.getHealth()<cow.getMaxHealth(),"Real nearby mob damaged by natural ground impact");test.assertTrue(level.getBlockState(base).is(Blocks.STONE),"Impact preserves ground");tc4revival.TC4Revival.LOGGER.info("PASS shockOrbNaturallyHitsGroundAndDamagesNearbyMob: world-only flight and collision callbacks");test.succeed();}finally{cleanup.run();}});
    }
    @GameTest(template="wand_test_empty")
    public static void shockFieldGroundSearchIncludesExactLowerBoundary(GameTestHelper test) {
        var level=test.getLevel();var origin=test.absolutePos(new BlockPos(10,24,10));var owner=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"ShockDepth"));owner.moveTo(Vec3.atCenterOf(origin.above(12)));int cases=0;
        for(int area:new int[]{4,6,8})for(boolean inside:new boolean[]{false,true}) {
            var floor=origin.below(area+(inside?0:1));
            try {
                for(var p:BlockPos.betweenClosed(floor.offset(-8,0,-8),floor.offset(8,0,8)))level.setBlock(p,Blocks.STONE.defaultBlockState(),2);
                var orb=new ShockOrbEntity(level,owner,area,5);orb.setPos(Vec3.atCenterOf(origin));orb.onHit(new BlockHitResult(orb.position(),Direction.UP,floor,false));
                int fields=0;for(var p:BlockPos.betweenClosed(floor.offset(-8,1,-8),floor.offset(8,1,8)))if(level.getBlockState(p).is(TCArcane.SHOCK_FIELD.get()))fields++;
                test.assertTrue(inside?fields>0:fields==0,"Ground at exact lower limit is eligible; one block deeper is excluded, area="+area);cases++;
            }finally{for(var p:BlockPos.betweenClosed(floor.offset(-8,0,-8),floor.offset(8,1,8)))level.setBlock(p,Blocks.AIR.defaultBlockState(),2);}
        }
        tc4revival.TC4Revival.LOGGER.info("PASS shockFieldGroundSearchIncludesExactLowerBoundary: {} area/depth boundary cases",cases);test.succeed();
    }
}
