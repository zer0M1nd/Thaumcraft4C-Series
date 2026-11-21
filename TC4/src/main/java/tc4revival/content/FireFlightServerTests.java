package tc4revival.content;

import java.util.*;
import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class FireFlightServerTests {
    @GameTest(template="wand_test_empty",timeoutTicks=100)
    public static void fireProjectilesNaturallyCollideWithEntitiesAndWalls(GameTestHelper test) {
        var level=test.getLevel();var base=test.absolutePos(new BlockPos(2,12,2));var forced=new ArrayList<ChunkPos>();
        for(int x=base.getX()>>4;x<=(base.getX()+28)>>4;x++)for(int z=base.getZ()>>4;z<=(base.getZ()+8)>>4;z++)if(level.setChunkForced(x,z,true))forced.add(new ChunkPos(x,z));
        var owner=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"FireFlight"));owner.moveTo(Vec3.atCenterOf(base.above(10)));
        var shots=new ArrayList<Entity>();var targets=new ArrayList<LivingEntity>();var walls=new ArrayList<BlockPos>();
        Runnable clear=()->{shots.forEach(Entity::discard);targets.forEach(Entity::discard);walls.forEach(p->level.setBlock(p,Blocks.AIR.defaultBlockState(),2));forced.forEach(c->level.setChunkForced(c.x,c.z,false));};
        try {
            for(int lane=0;lane<4;lane++) {
                var start=base.east(lane*8);var end=start.south(4);
                if(lane<2){var cow=EntityType.COW.create(level);cow.moveTo(Vec3.atBottomCenterOf(end));cow.setNoAi(true);cow.setNoGravity(true);level.addFreshEntity(cow);targets.add(cow);}
                else for(int height=0;height<3;height++){var wall=end.above(height);walls.add(wall);level.setBlock(wall,Blocks.GLASS.defaultBlockState(),2);}
                Entity shot=lane%2==0?new EmberEntity(level,owner,0,4,1,20):new ExplosiveOrbEntity(level,owner,1,false);
                shot.setPos(Vec3.atBottomCenterOf(start).add(0,.8,0));shot.setDeltaMovement(0,0,1);level.addFreshEntity(shot);shots.add(shot);
            }
            test.runAfterDelay(12,()->{
                try {
                    for(var shot:shots)test.assertTrue(shot.isRemoved(),"World ticker moves both projectile types into targets and walls: lane="+shots.indexOf(shot)+" ticks="+shot.tickCount+" pos="+shot.position()+" block="+level.getBlockState(shot.blockPosition()));
                    for(var target:targets)test.assertTrue(target.getHealth()<target.getMaxHealth(),"Each separated entity lane receives natural projectile hit");
                    test.assertTrue(targets.get(0).isOnFire(),"Natural ember collision ignites target");
                    for(var wall:walls)test.assertTrue(level.getBlockState(wall).is(Blocks.GLASS),"Natural collisions do not destroy glass walls");
                    tc4revival.TC4Revival.LOGGER.info("PASS fireProjectilesNaturallyCollideWithEntitiesAndWalls: four isolated flight lanes, world-only callbacks");test.succeed();
                }finally{clear.run();}
            });
        }catch(RuntimeException|AssertionError failure){clear.run();throw failure;}
    }
    @GameTest(template="wand_test_empty")
    public static void emberSpawnDurationRoundTripsAndBothTypesBuildPackets(GameTestHelper test) {
        var level=test.getLevel();var owner=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"FireSpawn"));
        for(int duration:new int[]{20,30}) {
            var source=new EmberEntity(level,owner,.25f,4.5f,2,duration);var target=new EmberEntity(TCEntities.EMBER.get(),level);var buffer=new FriendlyByteBuf(Unpooled.buffer());
            try {source.writeSpawnData(buffer);test.assertTrue(buffer.readableBytes()==1&&buffer.getUnsignedByte(0)==duration,"Legacy spawn payload is one duration byte");target.readSpawnData(buffer);test.assertTrue(target.duration()==duration&&!buffer.isReadable(),"Spawn receiver consumes exact duration");}
            finally{buffer.release();}
            test.assertTrue(source.getAddEntityPacket()!=null,"Actual Forge spawn packet constructed for ember");
        }
        var orb=new ExplosiveOrbEntity(level,owner,1.8f,true);test.assertTrue(orb.getAddEntityPacket()!=null,"Actual Forge spawn packet constructed for orb");
        tc4revival.TC4Revival.LOGGER.info("PASS emberSpawnDurationRoundTripsAndBothTypesBuildPackets: two duration payloads and actual packet construction; no client receiver executed");test.succeed();
    }
}
