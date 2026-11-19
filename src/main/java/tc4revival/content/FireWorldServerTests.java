package tc4revival.content;

import java.util.UUID;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class FireWorldServerTests {
    private static RandomSource random(Entity entity) {
        // Test-only access: seed the existing generator without replacing production randomness.
        try { for(var field:Entity.class.getDeclaredFields())if(field.getType()==RandomSource.class){field.setAccessible(true);return (RandomSource)field.get(entity);} }
        catch(ReflectiveOperationException e){throw new AssertionError(e);}
        throw new AssertionError("Entity RandomSource field missing");
    }
    @GameTest(template="wand_test_empty")
    public static void emberIgnitionSixFacesAndOccupiedTargets(GameTestHelper test) {
        var level=test.getLevel();var origin=test.absolutePos(new BlockPos(3,10,3));
        var owner=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"EmberFaces"));owner.moveTo(Vec3.atCenterOf(origin));int cases=0,ignitions=0;
        try {
            for(var face:Direction.values())for(int rank:new int[]{0,1,2})for(int sample=0;sample<64;sample++)for(boolean occupied:new boolean[]{false,true}) {
                var target=origin.relative(face);level.setBlock(origin,Blocks.STONE.defaultBlockState(),2);level.setBlock(target.below(),Blocks.STONE.defaultBlockState(),2);level.setBlock(target,occupied?Blocks.GLASS.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
                long seed=sample*104729L;float draw=RandomSource.create(seed).nextFloat();var ember=new EmberEntity(level,owner,0,1,rank,20);random(ember).setSeed(seed);
                ember.onHit(new BlockHitResult(Vec3.atCenterOf(origin),face,origin,false));boolean expect=!occupied&&draw<.025f*rank;
                test.assertTrue(level.getBlockState(target).is(Blocks.FIRE)==expect,"Legacy .025 per rank and hit-face offset: "+face+" rank="+rank+" sample="+sample);
                test.assertTrue(!occupied||level.getBlockState(target).is(Blocks.GLASS),"Non-air is never replaced");test.assertTrue(ember.isRemoved(),"Every collision consumes ember");
                if(expect)ignitions++;cases++;level.setBlock(target,Blocks.AIR.defaultBlockState(),2);level.setBlock(target.below(),Blocks.AIR.defaultBlockState(),2);
            }
            test.assertTrue(ignitions>0,"Deterministic sample includes successful ignition");
            tc4revival.TC4Revival.LOGGER.info("PASS emberIgnitionSixFacesAndOccupiedTargets: {} cases, {} ignitions",cases,ignitions);test.succeed();
        } finally {for(var pos:BlockPos.betweenClosed(origin.offset(-2,-2,-2),origin.offset(2,2,2)))level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);}
    }
    @GameTest(template="wand_test_empty",timeoutTicks=200)
    public static void orbExplosionHurtsNearbyEntitiesWithoutBreakingBlocks(GameTestHelper test) {
        var level=test.getLevel();var center=test.absolutePos(new BlockPos(4,10,4));var chunk=new net.minecraft.world.level.ChunkPos(center);boolean added=level.setChunkForced(chunk.x,chunk.z,true);
        var owner=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"OrbWorld"));owner.moveTo(Vec3.atCenterOf(center.above(8)));int fireCount=0;
        try {
            for(boolean fiery:new boolean[]{false,true})for(int sample=0;sample<8;sample++) {
                for(var p:BlockPos.betweenClosed(center.offset(-4,-1,-4),center.offset(4,2,4)))level.setBlock(p,p.getY()==center.getY()-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
                var glass=center.offset(2,0,0);level.setBlock(glass,Blocks.GLASS.defaultBlockState(),2);
                var target=EntityType.IRON_GOLEM.create(level);target.moveTo(Vec3.atBottomCenterOf(center.south()));target.setNoAi(true);level.addFreshEntity(target);
                try {
                    var orb=new ExplosiveOrbEntity(level,owner,1.8f,fiery);orb.setPos(Vec3.atCenterOf(center));orb.onHit(new BlockHitResult(Vec3.atCenterOf(center),Direction.UP,center.below(),false));
                    test.assertTrue(orb.isRemoved()&&target.getHealth()<target.getMaxHealth(),"Actual world explosion hurts nearby entity on block impact");
                    test.assertTrue(level.getBlockState(glass).is(Blocks.GLASS),"Fragile block survives explosion");
                    int fires=0;for(var p:BlockPos.betweenClosed(center.offset(-4,0,-4),center.offset(4,2,4)))if(level.getBlockState(p).is(Blocks.FIRE))fires++;
                    test.assertTrue(fiery||fires==0,"Unupgraded explosion never creates fire");fireCount+=fires;
                    for(var p:BlockPos.betweenClosed(center.offset(-4,-1,-4),center.offset(4,-1,4)))test.assertTrue(level.getBlockState(p).is(Blocks.STONE),"Explosion preserves support blocks");
                    float after=target.getHealth();orb.onHit(new BlockHitResult(Vec3.atCenterOf(center),Direction.UP,center.below(),false));test.assertTrue(target.getHealth()==after,"Removed orb cannot explode again");
                }finally{target.discard();}
            }
            test.assertTrue(fireCount>0,"Fiery upgrade creates actual world fire without block destruction");
            tc4revival.TC4Revival.LOGGER.info("PASS orbExplosionHurtsNearbyEntitiesWithoutBreakingBlocks: 16 explosions, {} fire blocks",fireCount);test.succeed();
        }finally{for(var p:BlockPos.betweenClosed(center.offset(-4,-1,-4),center.offset(4,2,4)))level.setBlock(p,Blocks.AIR.defaultBlockState(),2);if(added)level.setChunkForced(chunk.x,chunk.z,false);}
    }
}
