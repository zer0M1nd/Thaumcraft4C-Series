package tc4revival.content;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Whole-room placement and encounter tests, retained for the coordinated server batch. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class EldritchServerTests {
    private EldritchServerTests() {}
    private static ServerLevel outer(GameTestHelper test) {
        var level=test.getLevel().getServer().getLevel(TCEldritch.OUTER_LANDS);
        test.assertTrue(level!=null,"Generated Outer Lands dimension must be loaded"); return level;
    }
    private static int reserveCenter(ServerLevel level) {
        var data=EldritchMazeSavedData.get(level.getServer());
        for (int x=20000;x<100000;x+=100) {
            int candidate=x;
            if (!data.hasMazeInRange(x,20000,63,63)
                    && data.reservations().stream().noneMatch(reservation->reservation.centerX()==candidate)) return x;
        }
        throw new IllegalStateException("No isolated eldritch test coordinate");
    }
    private static void pass(GameTestHelper test,String name) {
        com.mojang.logging.LogUtils.getLogger().info("Eldritch system test passed: {}",name); test.succeed();
    }
    @GameTest(template="wand_test_empty",timeoutTicks=1200)
    public static void eldritchMazePreparationBuildsActualRoomsAndPersistsProgress(GameTestHelper test) {
        var level=outer(test); int cx=reserveCenter(level),cz=20000;
        var altar=new BlockPos(cx*16+8,51,cz*16+8);
        test.assertTrue(EldritchMazeRuntime.schedule(level,cx,cz,11,11,6142),"New maze must reserve before background planning");
        var data=EldritchMazeSavedData.get(level.getServer());
        test.assertTrue(data.reservations().stream().anyMatch(reservation->reservation.centerX()==cx),"In-flight request must be saved");
        test.assertTrue(!EldritchMazeRuntime.request(level,altar),"Unplaced rooms cannot authorize portal opening");
        test.succeedWhen(()->{
            test.assertTrue(EldritchMazeRuntime.request(level,altar),"Wait for every actual room, not only the layout");
            var plan=data.plan(cx,cz).orElseThrow();
            test.assertTrue(level.getBlockState(altar.above(2)).is(TCEldritch.PORTAL.get()),"Entrance portal must exist");
            for (int z=0;z<11;z++) for (int x=0;x<11;x++) {
                var cell=plan.layout().cell(x,z); if (cell.packed()==0) continue;
                int roomX=plan.originX()+x,roomZ=plan.originZ()+z;
                test.assertTrue(data.isGenerated(roomX,roomZ),"Every planned room must have committed progress");
                if (cell.feature()==6) {
                    var center=new BlockPos(roomX*16+8,53,roomZ*16+8);
                    test.assertTrue(level.getBlockState(center.below()).is(TCEldritch.CAP.get()),"Key pedestal must be built");
                    var keys=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(center).inflate(5),
                            item->item.getItem().is(TCEldritch.RUNED_TABLET.get()));
                    test.assertTrue(keys.size()==1,"Key room must contain one real tablet");
                }
            }
            var reloaded=EldritchMazeSavedData.load(data.save(new net.minecraft.nbt.CompoundTag()));
            test.assertTrue(reloaded.isGenerated(cx,cz) && reloaded.plan(cx,cz).isPresent(),"Room progress and plan must survive reload");
        });
    }
    private record Hall(ServerLevel level,BlockPos lock,BlockPos center) {}
    private static Hall hall(GameTestHelper test,long seed) {
        var level=outer(test); int cx=reserveCenter(level),cz=20000;
        var layout=EldritchMazeLayout.generate(11,11,seed); var data=EldritchMazeSavedData.get(level.getServer());
        test.assertTrue(data.publish(cx,cz,layout),"Independent boss-room plan must publish");
        var plan=data.plan(cx,cz).orElseThrow(); BlockPos center=null,lock=null;
        for (int z=0;z<11;z++) for (int x=0;x<11;x++) {
            var cell=layout.cell(x,z); if (cell.feature()<2 || cell.feature()>5) continue;
            int roomX=plan.originX()+x,roomZ=plan.originZ()+z;
            EldritchMazePlacer.generate(level,roomX,roomZ,cell); data.markGenerated(roomX,roomZ);
            if (cell.feature()==2) center=new BlockPos(roomX*16+16,52,roomZ*16+16);
            for (var entity:level.getChunk(roomX,roomZ).getBlockEntities().values())
                if (entity instanceof EldritchLockBlockEntity) lock=entity.getBlockPos();
        }
        test.assertTrue(center!=null && lock!=null,"A real four-quarter boss hall must contain its entrance lock");
        return new Hall(level,lock,center);
    }
    @GameTest(template="wand_test_empty",timeoutTicks=400)
    public static void eldritchFourBossRoomsOpenWithTheirActualEncounters(GameTestHelper test) {
        for (int kind=0;kind<4;kind++) {
            var hall=hall(test,770+kind); var level=hall.level();
            EldritchBossEncounter.open(level,hall.lock(),kind);
            test.assertTrue(level.isEmptyBlock(hall.lock()),"Successful encounter must remove lock");
            for (var pos:BlockPos.betweenClosed(hall.lock().offset(-2,-2,-2),hall.lock().offset(2,2,2)))
                test.assertTrue(!level.getBlockState(pos).is(TCEldritch.DOOR_BARRIER.get()),"Entrance must be open");
            var mobs=level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,new AABB(hall.center()).inflate(24,12,24));
            if (kind==0) test.assertTrue(mobs.stream().filter(mob->mob instanceof EldritchGolemEntity).count()==1,"Golem room must spawn one golem");
            if (kind==1) test.assertTrue(mobs.stream().filter(mob->mob instanceof EldritchWardenEntity).count()==1,"Warden room must spawn one warden");
            if (kind==2) test.assertTrue(mobs.stream().filter(mob->mob instanceof CultistPortalEntity).count()==1,"Cultist room must spawn its wave portal");
            if (kind==3) test.assertTrue(mobs.stream().filter(mob->mob instanceof TaintacleEntity).count()==5,"Taint room must spawn all five tentacles");
            mobs.forEach(Entity::discard);
        }
        pass(test,"four-boss-rooms");
    }
    @GameTest(template="wand_test_empty",timeoutTicks=400)
    public static void eldritchRejectedBossGroupLeavesNoPartialEncounter(GameTestHelper test) {
        var hall=hall(test,1113); var bounds=new AABB(hall.center()).inflate(24,12,24); int[] joined={0};
        java.util.function.Consumer<net.minecraftforge.event.entity.EntityJoinLevelEvent> reject=event->{
            if (event.getLevel()==hall.level() && event.getEntity() instanceof TaintacleEntity && bounds.contains(event.getEntity().position()) && ++joined[0]==3)
                event.setCanceled(true);
        };
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(reject);
        boolean failed=false;
        try { EldritchBossEncounter.open(hall.level(),hall.lock(),3); }
        catch (IllegalStateException expected) { failed=true; }
        finally { net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(reject); }
        test.assertTrue(failed && joined[0]==3,"Test must reject a partially added group");
        test.assertTrue(hall.level().getEntitiesOfClass(TaintacleEntity.class,bounds).isEmpty(),"Rejected encounter must discard previously added tentacles");
        test.assertTrue(hall.level().getBlockState(hall.lock()).is(TCEldritch.LOCK.get()),"Failure must retain the lock for retry");
        pass(test,"boss-group-rollback");
    }
    @GameTest(template="wand_test_empty",timeoutTicks=600)
    public static void eldritchAltarRequiresResearchRestoresNodeAndChargesSixAspectsOnce(GameTestHelper test) {
        var outer=outer(test); int cx=reserveCenter(outer),cz=20000;
        var data=EldritchMazeSavedData.get(outer.getServer()); var layout=EldritchMazeLayout.generate(7,7,186);
        test.assertTrue(data.publish(cx,cz,layout),"Altar test must own a destination plan"); var plan=data.plan(cx,cz).orElseThrow();
        for (int z=0;z<7;z++) for (int x=0;x<7;x++) {
            var cell=layout.cell(x,z); if (cell.packed()==0) continue;
            EldritchMazePlacer.generate(outer,plan.originX()+x,plan.originZ()+z,cell); data.markGenerated(plan.originX()+x,plan.originZ()+z);
        }
        var level=test.getLevel(); var pos=new BlockPos(cx*16+8,70,cz*16+8);
        level.setBlock(pos,TCEldritch.ALTAR.get().defaultBlockState(),18);
        level.setBlock(pos.above(),TCArcane.AURA_NODE.get().defaultBlockState(),18);
        level.setBlock(pos.above(2),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),18);
        var node=(AuraNodeBlockEntity)level.getBlockEntity(pos.above());
        var aspects=new tc4revival.aspect.AspectAmounts(); aspects.add(tc4revival.aspect.TCAspects.catalog().require("aer"),17);
        node.configure(aspects,AuraNodeType.DARK,AuraNodeModifier.PALE,"altar-rollback",false);
        var before=node.saveWithoutMetadata();
        var altar=(EldritchMonumentBlockEntity)level.getBlockEntity(pos);
        for (int i=0;i<4;i++) {
            var eye=TCEldritch.ELDRITCH_EYE.get().getDefaultInstance(); altar.insertEye(eye);
            test.assertTrue(eye.isEmpty(),"Each inserted eye must be consumed");
        }
        var altarData=altar.saveWithoutMetadata();
        test.assertTrue(altar.eyes()==4 && altarData.getBoolean("spawner") && altarData.getByte("spawntype")==1,"Third eye must switch altar to guardian mode");
        var player=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"EldritchTest"));
        player.setPos(pos.getX()+2,pos.getY(),pos.getZ()); player.getAbilities().instabuild=false;
        var item=(CastingWandItem)TCItems.CASTING_WAND.get(); var wand=item.create(WandParts.Rod.SILVERWOOD,WandParts.Cap.GOLD,false,true);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,wand);
        var context=new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),net.minecraft.core.Direction.UP,pos,false));
        test.assertTrue(!altar.onWandUseFirst(wand,context).consumesAction(),"Without OCULUS the altar must reject activation");
        tc4revival.research.KnowledgeSavedData.get(level.getServer()).update(player.getUUID(),level.random::nextInt,knowledge->{ knowledge.rememberResearch("OCULUS"); return null; });
        var originalWand=wand.getTag().copy();
        test.assertTrue(!altar.onWandUseFirst(wand,context).consumesAction(),"Missing obelisk must reject after the tentative portal write");
        test.assertTrue(level.getBlockEntity(pos.above()) instanceof AuraNodeBlockEntity restored && before.equals(restored.saveWithoutMetadata()),"Rejected portal must restore complete dark-node data");
        test.assertTrue(originalWand.equals(wand.getTag()) && !altar.isOpen(),"Rejected portal must not charge or open altar");
        level.setBlock(pos.above(2),TCEldritch.OBELISK.get().defaultBlockState(),18);
        test.assertTrue(altar.onWandUseFirst(wand,context).consumesAction(),"Prepared maze, research, node and charged wand must open portal");
        for (var primal:tc4revival.aspect.TCAspects.catalog().primals()) test.assertTrue(item.vis(wand,primal)==0,"Gold-capped wand spends exactly 100 vis of every primal");
        test.assertTrue(altar.isOpen() && level.getBlockEntity(pos.above()) instanceof EldritchPortalBlockEntity,"Portal must be real and persisted");
        test.assertTrue(!altar.onWandUseFirst(wand,context).consumesAction(),"An open altar must not activate twice");
        pass(test,"altar-transaction-and-cost");
    }
}
