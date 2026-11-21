package tc4revival.content;

import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class FrostFlightServerTests {
    @GameTest(template="wand_test_empty",timeoutTicks=100)
    public static void frostThreeModesNaturallyHitAndSlow(GameTestHelper test) {
        var level=test.getLevel();var base=test.absolutePos(new BlockPos(2,14,2));var forced=new ArrayList<ChunkPos>();
        for(int x=base.getX()>>4;x<=(base.getX()+20)>>4;x++)for(int z=base.getZ()>>4;z<=(base.getZ()+6)>>4;z++)if(level.setChunkForced(x,z,true))forced.add(new ChunkPos(x,z));
        var owner=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"FrostFlight"));owner.moveTo(Vec3.atCenterOf(base.above(10)));var shards=new ArrayList<FrostShardEntity>();var targets=new ArrayList<LivingEntity>();
        Runnable cleanup=()->{shards.forEach(Entity::discard);targets.forEach(Entity::discard);forced.forEach(c->level.setChunkForced(c.x,c.z,false));};
        try {
            for(int mode=0;mode<3;mode++) {
                var start=base.east(mode*8);var target=EntityType.COW.create(level);target.moveTo(Vec3.atBottomCenterOf(start.south(3)));target.setNoAi(true);target.setNoGravity(true);level.addFreshEntity(target);targets.add(target);
                var shard=new FrostShardEntity(level,owner,1,mode==1?1:mode==2?4:3,2,mode==1,mode==2);shard.setPos(Vec3.atBottomCenterOf(start).add(0,.8,0));shard.setDeltaMovement(0,0,1);level.addFreshEntity(shard);shards.add(shard);
            }
            test.runAfterDelay(8,()->{try {
                for(int mode=0;mode<3;mode++){var target=targets.get(mode);var slow=target.getEffect(MobEffects.MOVEMENT_SLOWDOWN);test.assertTrue(target.getHealth()<target.getMaxHealth()&&slow!=null&&slow.getAmplifier()==1&&slow.getDuration()>185,"Natural mode "+mode+" hit and slowness II");}
                test.assertTrue(shards.get(1).isRemoved(),"Natural scatter hit destroys fragile shard");
                tc4revival.TC4Revival.LOGGER.info("PASS frostThreeModesNaturallyHitAndSlow: three isolated natural flight lanes, real living targets");test.succeed();
            }finally{cleanup.run();}});
        }catch(RuntimeException|AssertionError failure){cleanup.run();throw failure;}
    }
    @GameTest(template="wand_test_empty")
    public static void frostMalformedSavedValuesStayFiniteAndBounded(GameTestHelper test) {
        int cases=0;
        for(float damage:new float[]{Float.NaN,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY,-1,0,1,100,101})for(int frost:new int[]{Integer.MIN_VALUE,-1,0,3,5,6,Integer.MAX_VALUE}) {
            var tag=new CompoundTag();tag.putFloat("damage",damage);tag.putInt("frost",frost);tag.putBoolean("fragile",true);var shard=new FrostShardEntity(TCEntities.FROST_SHARD.get(),test.getLevel());shard.readAdditionalSaveData(tag);
            float expected=Float.isFinite(damage)?Math.max(0,Math.min(100,damage)):0;test.assertTrue(shard.damage()==expected&&shard.frost()==Math.max(0,Math.min(5,frost)),"Malformed fields bounded on NBT load");test.assertTrue(Float.isFinite(shard.getBbWidth())&&shard.getBbWidth()>=.15f&&shard.getBbWidth()<=15.15001f,"Collision dimensions remain finite and bounded: "+shard.getBbWidth());var saved=new CompoundTag();shard.addAdditionalSaveData(saved);test.assertTrue(saved.getBoolean("fragile")&&saved.getFloat("damage")==expected,"Safe values persist while retaining fragile flag");shard.discard();cases++;
        }
        tc4revival.TC4Revival.LOGGER.info("PASS frostMalformedSavedValuesStayFiniteAndBounded: {} malformed damage/frost combinations",cases);test.succeed();
    }
}
