package conciliummodern;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.gametest.*;
import tc4revival.content.*;

@GameTestHolder("thaumicconcilium") @PrefixGameTestTemplate(false)
public final class ConciliumBossServerTests {
    private static <T extends Entity> T place(GameTestHelper test,T entity,int x,int y,int z){entity.setPos(test.absoluteVec(new Vec3(x+.5,y,z+.5)));test.getLevel().addFreshEntity(entity);return entity;}
    private static CompoundTag save(Entity entity){var tag=new CompoundTag();entity.saveWithoutId(tag);return tag;}
    private static ServerPlayer player(GameTestHelper test){
        var p=new ServerPlayer(test.getLevel().getServer(),test.getLevel(),new GameProfile(UUID.randomUUID(),"ConciliumBoss"));
        new ServerGamePacketListenerImpl(p.server,new Connection(PacketFlow.SERVERBOUND){@Override public void send(net.minecraft.network.protocol.Packet<?> packet){}},p);
        p.setGameMode(GameType.SURVIVAL);p.setPos(test.absoluteVec(new Vec3(8.5,3,8.5)));test.getLevel().addNewPlayer(p);return p;
    }
    @GameTest(template="empty") public static void brightestRequiresFifteenDischargesAndRewardsOnce(GameTestHelper test){
        var boss=place(test,ConciliumEntities.BRIGHTEST.get().create(test.getLevel()),12,3,12);boss.setNoAi(true);
        test.assertTrue(!boss.hurt(test.getLevel().damageSources().generic(),1000)&&boss.getHealth()==15,"Ordinary damage cannot defeat brightest one");
        test.assertTrue(!boss.shortCircuit(boss)&&boss.getHealth()==15,"Spawn grace rejects early discharges");boss.tickCount=21;
        for(int i=0;i<14;i++)test.assertTrue(boss.shortCircuit(boss),"Each accepted discharge advances the fight");
        test.assertTrue(boss.isAlive()&&boss.getHealth()==1,"Fourteen discharges leave one health");
        var restored=ConciliumEntities.BRIGHTEST.get().create(test.getLevel());restored.load(save(boss));
        test.assertTrue(restored.getHealth()==1&&save(restored).getInt("ShortCircuits")==14,"Fight progress survives serialized reload");
        test.assertTrue(boss.shortCircuit(boss)&&!boss.isAlive(),"Fifteenth discharge defeats boss");boss.die(test.getLevel().damageSources().generic());
        var drops=test.getLevel().getEntitiesOfClass(ItemEntity.class,boss.getBoundingBox().inflate(4));
        test.assertTrue(drops.stream().filter(e->e.getItem().is(ConciliumItems.PRIMORDIAL_LIFE.get())).mapToInt(e->e.getItem().getCount()).sum()==1,"Exactly one primordial life reward");
        test.assertTrue(drops.stream().filter(e->e.getItem().is(TCItems.WISP_ESSENCE.get())).count()==1,"Exactly one essence reward");
        test.assertTrue(save(boss).getBoolean("Rewarded")&&!boss.shortCircuit(boss),"Reward marker and dead fight reject repetition");drops.forEach(Entity::discard);boss.discard();test.succeed();
    }
    @GameTest(template="empty") public static void chargedWispActuallyDischargesAndPersistsOwner(GameTestHelper test){
        var boss=place(test,ConciliumEntities.BRIGHTEST.get().create(test.getLevel()),12,4,12);boss.setNoAi(true);boss.tickCount=21;
        var owner=place(test,EntityType.PIG.create(test.getLevel()),18,4,12);owner.setNoAi(true);
        var wisp=ConciliumEntities.CHARGED_WISP.get().create(test.getLevel());wisp.initialize(owner,"aer",3,2);wisp.setPos(boss.position());wisp.setDeltaMovement(Vec3.ZERO);test.getLevel().addFreshEntity(wisp);
        var clone=ConciliumEntities.CHARGED_WISP.get().create(test.getLevel());clone.load(save(wisp));
        test.assertTrue(clone.getOwner()==owner&&clone.aspectTag().equals("aer")&&save(clone).getInt("power")==3,"Projectile owner and strength survive reload");
        wisp.tick();test.assertTrue(wisp.isRemoved()&&boss.getHealth()==14,"Actual projectile tick discharges once and consumes itself");owner.discard();boss.discard();test.succeed();
    }
    @GameTest(template="empty") public static void portalNeedsNearbyPlayerAndDoesNotAdvanceRejectedSpawn(GameTestHelper test){
        var portal=place(test,CrimsonContent.PORTAL.get().create(test.getLevel()),12,3,12);portal.activateImmediately();portal.tick();
        test.assertTrue(portal.stage()==0,"No nearby player means no reinforcement progress");var p=player(test);
        java.util.function.Consumer<EntityJoinLevelEvent> reject=e->{if(e.getLevel()==test.getLevel()&&e.getEntity() instanceof CultistMob)e.setCanceled(true);};
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,false,EntityJoinLevelEvent.class,reject);
        try{portal.activateImmediately();portal.tick();test.assertTrue(portal.stage()==0,"Rejected real minion spawn does not advance wave");}finally{MinecraftForge.EVENT_BUS.unregister(reject);}
        portal.activateImmediately();portal.tick();test.assertTrue(portal.stage()==1,"Accepted minion advances one wave");
        var copy=CrimsonContent.PORTAL.get().create(test.getLevel());copy.load(save(portal));test.assertTrue(copy.stage()==1,"Wave progress survives reload");
        test.getLevel().getEntitiesOfClass(Mob.class,portal.getBoundingBox().inflate(5)).forEach(Entity::discard);p.discard();test.succeed();
    }
    @GameTest(template="empty") public static void finalPortalWaveCreatesOneEquippedPontifex(GameTestHelper test){
        var p=player(test);var portal=place(test,CrimsonContent.PORTAL.get().create(test.getLevel()),12,3,12);
        var joined=new java.util.ArrayList<CrimsonPontifex>();
        java.util.function.Consumer<EntityJoinLevelEvent> capture=event->{if(event.getLevel()==test.getLevel()&&event.getEntity() instanceof CrimsonPontifex boss&&boss.distanceToSqr(portal)<9)joined.add(boss);};
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST,false,EntityJoinLevelEvent.class,capture);
        Runnable cleanup=()->{MinecraftForge.EVENT_BUS.unregister(capture);joined.forEach(Entity::discard);portal.discard();p.discard();};
        try{
            var tag=save(portal);tag.putInt("stage",13);tag.putInt("StageCounter",0);tag.putBoolean("SummonsBoss",true);portal.load(tag);portal.tick();
            String state="stage="+portal.stage()+", health="+portal.getHealth()+", alive="+portal.isAlive()+", summonsBoss="+portal.summonsBoss()+", joined="+joined.size()+", position="+portal.position();
            test.assertTrue(!portal.isAlive(),"Successful final wave closes portal: "+state);
            test.assertTrue(!portal.summonsBoss(),"Successful final wave clears pending boss flag: "+state);
            test.assertTrue(joined.size()==1,"Final wave submits exactly one boss to entity join: "+state);
            // Newly forced chunks may not expose their entity sections until a following server tick.
            test.runAfterDelay(2,()->{
                try{
                    var bosses=test.getLevel().getEntitiesOfClass(CrimsonPontifex.class,portal.getBoundingBox().inflate(6));
                    test.assertTrue(bosses.size()==1&&bosses.get(0)==joined.get(0),"The submitted boss is actually visible in the world; visible="+bosses.size()+", "+state);
                    var boss=bosses.get(0);test.assertTrue(boss.getMainHandItem().is(CrimsonContent.HAMMER.get())&&boss.getItemBySlot(EquipmentSlot.CHEST).is(CrimsonContent.CHEST.get()),"Actual spawn equips original boss loadout");
                    portal.tick();test.assertTrue(joined.size()==1&&test.getLevel().getEntitiesOfClass(CrimsonPontifex.class,portal.getBoundingBox().inflate(6)).size()==1,"Closed portal cannot summon a duplicate");test.succeed();
                }finally{cleanup.run();}
            });
        }catch(RuntimeException|Error failure){cleanup.run();throw failure;}
    }
    @GameTest(template="empty") public static void pontifexShieldCountsNearbyLivingCultists(GameTestHelper test){
        var alone=CrimsonContent.PONTIFEX.get().create(test.getLevel());var guarded=CrimsonContent.PONTIFEX.get().create(test.getLevel());
        // Bosses otherwise receive independent random champions on join, including seventy-five absorption health.
        for(var boss:java.util.List.of(alone,guarded)){boss.getAttribute(ChampionMobs.CHAMPION.get()).setBaseValue(-1);boss.setNoAi(true);}
        place(test,alone,5,3,5);place(test,guarded,23,3,23);var ally=place(test,TCEntities.CULTIST_KNIGHT.get().create(test.getLevel()),24,3,23);ally.setNoAi(true);
        try{
            test.assertTrue(ChampionMobs.kind(alone)==null&&ChampionMobs.kind(guarded)==null&&alone.getAbsorptionAmount()==0&&guarded.getAbsorptionAmount()==0,"Both damage probes begin without a random champion or shield");
            float soloBefore=alone.getHealth(),guardedBefore=guarded.getHealth();
            alone.hurt(test.getLevel().damageSources().generic(),30);float soloLoss=soloBefore-alone.getHealth();
            guarded.hurt(test.getLevel().damageSources().generic(),30);float guardedLoss=guardedBefore-guarded.getHealth();
            test.assertTrue(Math.abs(soloLoss-10)<.001&&Math.abs(guardedLoss-7.5)<.001&&alone.cultists()==0&&guarded.cultists()==1,"Nearby living cultist changes actual damage from 30/3 to 30/4; solo="+soloLoss+", guarded="+guardedLoss+", allies="+alone.cultists()+"/"+guarded.cultists());test.succeed();
        }finally{alone.discard();guarded.discard();ally.discard();}
    }
}
