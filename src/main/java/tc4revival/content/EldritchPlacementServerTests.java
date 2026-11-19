package tc4revival.content;

import com.mojang.authlib.GameProfile;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.*;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class EldritchPlacementServerTests {
    @GameTest(template="wand_test_empty",timeoutTicks=300)
    public static void eldritchBossPlacementEventBoundaries(GameTestHelper test) {
        var level=test.getLevel().getServer().getLevel(TCEldritch.OUTER_LANDS);
        test.assertTrue(level!=null,"Outer Lands dimension exists");
        var data=EldritchMazeSavedData.get(level.getServer());
        int cx=18000,cz=21000;
        while(data.hasMazeInRange(cx,cz,63,63))cx+=100;
        var layout=EldritchMazeLayout.generate(11,11,9261);
        test.assertTrue(data.publish(cx,cz,layout),"Publish independent saved maze cells");
        var plan=data.plan(cx,cz).orElseThrow(); var rooms=new ArrayList<BlockPos>();
        for(int z=0;z<layout.height();z++)for(int x=0;x<layout.width();x++) {
            int feature=layout.cell(x,z).feature();
            if(feature>=2 && feature<=5)rooms.add(new BlockPos((plan.originX()+x)*16+8,80,(plan.originZ()+z)*16+8));
        }
        test.assertTrue(rooms.size()==4,"All four boss-room quarters present");
        var entrance=new BlockPos(cx*16+8,80,cz*16+8);
        var positions=new ArrayList<>(rooms);positions.add(entrance);positions.add(rooms.get(0).offset(31,0,31));positions.add(rooms.get(0).offset(40,0,0));
        var forced=new HashSet<net.minecraft.world.level.ChunkPos>();
        for(var pos:positions) {
            var chunk=new net.minecraft.world.level.ChunkPos(pos);
            if(!level.getForcedChunks().contains(chunk.toLong())) {forced.add(chunk);level.setChunkForced(chunk.x,chunk.z,true);}
        }
        Runnable cleanup=()->forced.forEach(chunk->level.setChunkForced(chunk.x,chunk.z,false));
        test.startSequence().thenWaitUntil(()->test.assertTrue(positions.stream().allMatch(level::isPositionEntityTicking),"Wait for remote entity sections, not only block chunks"))
                .thenExecute(()->{try {check(test,level,rooms,entrance);}finally {cleanup.run();}}).thenSucceed();
        test.runAtTickTime(299,cleanup);
    }
    private static void check(GameTestHelper test,ServerLevel level,List<BlockPos> rooms,BlockPos entrance) {
        var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"BossPlaceTest"));player.setGameMode(GameType.SURVIVAL);
        var boss=TCEntities.ELDRITCH_GOLEM.get().create(level);
        var portal=TCEntities.CULTIST_PORTAL.get().create(level);
        try {
            var first=rooms.get(0);level.getChunk(first);boss.setPos(first.getX(),first.getY(),first.getZ());boss.setNoAi(true);
            test.assertTrue(level.addFreshEntity(boss),"Registered boss is added to the real entity index");
            test.assertTrue(!level.getEntitiesOfClass(ThaumcraftBossEntity.class,new net.minecraft.world.phys.AABB(first).inflate(32)).isEmpty(),"Added boss is visible to queries before testing events");
            for(var pos:rooms) {
                level.getChunk(pos);
                test.assertTrue(post(level,pos,player,false),"Single placement is blocked in each boss quarter");
                test.assertTrue(post(level,pos,player,true),"Multi-placement subclass is blocked by the registered listener");
            }
            player.setGameMode(GameType.CREATIVE);test.assertTrue(!post(level,first,player,false),"Creative placement remains allowed");player.setGameMode(GameType.SURVIVAL);
            test.assertTrue(!post(level,first,null,false),"Non-player placement remains allowed");
            test.assertTrue(!post(test.getLevel(),test.absolutePos(new BlockPos(2,3,2)),player,false),"Overworld placement is unaffected");
            level.getChunk(entrance);boss.setPos(entrance.getX(),80,entrance.getZ());
            test.assertTrue(!post(level,entrance,player,false),"Nearby boss alone cannot restrict a non-boss room");
            level.getChunk(first.offset(31,0,31));boss.setPos(first.getX()+31,80,first.getZ()+31);
            test.assertTrue(post(level,first,player,false),"Diagonal distance beyond 32 still lies inside the legacy cube");
            level.getChunk(first.offset(40,0,0));boss.setPos(first.getX()+40,80,first.getZ());
            test.assertTrue(!post(level,first,player,false),"Boss outside the 32-block cube does not restrict placement");
            boss.discard();portal.setPos(first.getX(),80,first.getZ());level.addFreshEntity(portal);
            test.assertTrue(!post(level,first,player,true),"Cultist wave portal is not a ThaumcraftBoss subclass");
            portal.discard();test.assertTrue(!post(level,first,player,false),"Restriction ends after boss removal");
        } finally {boss.discard();portal.discard();}
        tc4revival.TC4Revival.LOGGER.info("Eldritch placement PASS: four-quarters-single-multi-player-dimension-cube-boss-type-removal");
    }
    private static boolean post(ServerLevel level,BlockPos pos,FakePlayer player,boolean multiple) {
        var snapshot=BlockSnapshot.create(level.dimension(),level,pos);
        BlockEvent.EntityPlaceEvent event=multiple
                ?new BlockEvent.EntityMultiPlaceEvent(List.of(snapshot,BlockSnapshot.create(level.dimension(),level,pos.above())),Blocks.STONE.defaultBlockState(),player)
                :new BlockEvent.EntityPlaceEvent(snapshot,Blocks.STONE.defaultBlockState(),player);
        return MinecraftForge.EVENT_BUS.post(event);
    }
}
