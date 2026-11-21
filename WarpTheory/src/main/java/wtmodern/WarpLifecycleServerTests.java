package wtmodern;

import com.mojang.authlib.GameProfile;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;
import tc4revival.research.*;

/** Actual lifecycle paths not covered by the pure event table or rendering checks. */
@GameTestHolder("warptheory") @PrefixGameTestTemplate(false)
public class WarpLifecycleServerTests {
    private static ServerPlayer player(GameTestHelper h,String name){var p=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8)),name));p.getInventory().clearContent();p.removeAllEffects();p.getAbilities().instabuild=false;WarpState.tag(p).getAllKeys().clear();p.getRandom().setSeed(172);p.setPos(h.absoluteVec(new Vec3(2,100,2)));warp(p,100);return p;}
    private static void warp(ServerPlayer p,int temp){KnowledgeSavedData.get(p.server).update(p.getUUID(),n->0,k->{for(var kind:PlayerKnowledge.WarpKind.values())k.setWarp(kind,0);k.setWarp(PlayerKnowledge.WarpKind.TEMPORARY,temp);return null;});}
    private static void pass(GameTestHelper h){com.mojang.logging.LogUtils.getLogger().info("WARPTHEORY_TEST_PASS {}",Thread.currentThread().getStackTrace()[2].getMethodName());h.succeed();}

    @GameTest(template="empty") public static void talismanPurifiesOnlyAtItsOriginalTickInterval(GameTestHelper h){var p=player(h,"WTTalismanCycle");warp(p,10000);var stack=new ItemStack(Content.AMULET.get());var context=new top.theillusivec4.curios.api.SlotContext("necklace",p,0,false,true);p.tickCount=499;((PureTalisman)stack.getItem()).curioTick(context,stack);h.assertTrue(WarpState.total(p)==10000&&WarpState.tag(p).getList("queue",8).isEmpty(),"Before 500 ticks neither warp nor event queue changes");p.tickCount=500;((PureTalisman)stack.getItem()).curioTick(context,stack);var queue=WarpState.tag(p).getList("queue",8);h.assertTrue(queue.size()==1,"500-tick amulet event is actually queued");var event=WarpRules.find(queue.getString(0));h.assertTrue(WarpState.total(p)==10000-event.cost(),"Talisman pays the selected event's exact temporary warp cost");pass(h);}

    @GameTest(template="empty") public static void somethingConsumptionAddsAllThreeWarpKinds(GameTestHelper h){var p=player(h,"WTSomethingUse");warp(p,0);var stack=new ItemStack(Content.SOMETHING.get(),2);Content.SOMETHING.get().finishUsingItem(stack,h.getLevel(),p);var k=WarpState.knowledge(p);h.assertTrue(stack.getCount()==1,"One portion consumed");h.assertTrue(k.warp(PlayerKnowledge.WarpKind.PERMANENT)>=4&&k.warp(PlayerKnowledge.WarpKind.PERMANENT)<=7&&k.warp(PlayerKnowledge.WarpKind.STICKY)>=5&&k.warp(PlayerKnowledge.WarpKind.STICKY)<=9&&k.warp(PlayerKnowledge.WarpKind.TEMPORARY)>=5&&k.warp(PlayerKnowledge.WarpKind.TEMPORARY)<=9,"All original random warp ranges applied to authoritative knowledge");pass(h);}

    @GameTest(template="empty") public static void tongueChangesOnlyTheMessageAndConsumesOneUse(GameTestHelper h){var p=player(h,"WTTongueUse");WarpEvents.start(p,"tongue");int before=WarpState.tag(p).getInt("tongue");var event=new net.minecraftforge.event.ServerChatEvent(p,"A careful researcher speaks.",net.minecraft.network.chat.Component.literal("A careful researcher speaks."));WarpEvents.chat(event);h.assertTrue(WarpState.tag(p).getInt("tongue")==before-1&&!event.getMessage().getString().equals(event.getRawText())&&event.getUsername().equals(p.getGameProfile().getName()),"Outgoing garbling changes body, preserves sender and consumes one use");WarpEvents.start(p,"ears");h.assertTrue(WarpState.tag(p).getInt("ears")>=10&&WarpState.tag(p).getInt("ears")<=39,"Incoming chat effect keeps original allowance");pass(h);}

    @GameTest(template="empty") public static void timeAndWindConsumeTheirActiveCounters(GameTestHelper h){var p=player(h,"WTTimeWind");var level=h.getLevel();var data=(ServerLevelData)level.getLevelData();long day=level.getDayTime(),time=level.getGameTime();try{WarpState.add(p,"acceleration",3);for(int i=0;i<3;i++)WarpEvents.runActive(p);h.assertTrue(level.getDayTime()==day+6&&!WarpState.tag(p).contains("acceleration"),"Three active ticks advance day time by six and terminate");data.setGameTime(time-time%20);WarpState.add(p,"wind",1);for(int i=0;i<20&&WarpState.tag(p).contains("wind");i++)WarpEvents.runActive(p);h.assertTrue(!WarpState.tag(p).contains("wind")&&p.getDeltaMovement().y>0,"World-second wind applies upward movement and consumes a use");}finally{level.setDayTime(day);data.setGameTime(time);}pass(h);}

    @GameTest(template="empty",batch="warp_lifecycle_entities") public static void batFriendLivestockAndWitherActuallySpawn(GameTestHelper h){var p=player(h,"WTSpawnCycle");var level=h.getLevel();var origin=p.blockPosition();var box=new AABB(origin).inflate(30,40,30);var before=new HashSet<UUID>();level.getEntities((Entity)null,box,e->true).forEach(e->before.add(e.getUUID()));try{for(var pos:BlockPos.betweenClosed(origin.offset(-10,-1,-10),origin.offset(10,-1,10)))level.setBlock(pos,Blocks.STONE.defaultBlockState(),2);for(String id:List.of("bats","friend","livestock","wither")){WarpState.add(p,id,1);for(int i=0;i<300&&WarpState.tag(p).contains(id);i++)WarpEvents.runActive(p);h.assertTrue(!WarpState.tag(p).contains(id),"Actual entity spawn consumes "+id+" counter");}var spawned=level.getEntities((Entity)null,box,e->!before.contains(e.getUUID()));h.assertTrue(spawned.stream().anyMatch(e->e.getType()==EntityType.BAT)&&spawned.stream().anyMatch(e->e.getType()==Content.FRIEND.get())&&spawned.stream().anyMatch(e->e.getType()==EntityType.WITHER),"Bat, custom friend and real wither were added to world");h.assertTrue(spawned.stream().anyMatch(e->List.of(EntityType.COW,EntityType.PIG,EntityType.SHEEP).contains(e.getType())&&e.getY()>p.getY()+15),"Original livestock falls from above");}finally{level.getEntities((Entity)null,box,e->!before.contains(e.getUUID())).forEach(Entity::discard);for(var pos:BlockPos.betweenClosed(origin.offset(-10,-1,-10),origin.offset(10,-1,10)))level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);}pass(h);}

    @GameTest(template="empty",batch="warp_lifecycle_blink") public static void blinkFindsSafeGroundAndBloodUsesRealCeilings(GameTestHelper h){var p=player(h,"WTBlinkBlood");var level=h.getLevel();var origin=p.blockPosition();long time=level.getGameTime();var data=(ServerLevelData)level.getLevelData();try{for(var pos:BlockPos.betweenClosed(origin.offset(-16,-1,-16),origin.offset(16,-1,16)))level.setBlock(pos,Blocks.STONE.defaultBlockState(),2);data.setGameTime(time-time%20);WarpState.add(p,"blink",1);for(int i=0;i<100&&WarpState.tag(p).contains("blink");i++)WarpEvents.runActive(p);h.assertTrue(!WarpState.tag(p).contains("blink")&&level.getBlockState(p.blockPosition().below()).blocksMotion()&&level.isEmptyBlock(p.blockPosition()),"Blink consumes one use and lands above solid floor in air");p.setPos(Vec3.atBottomCenterOf(origin));for(var pos:BlockPos.betweenClosed(origin.offset(-7,4,-7),origin.offset(7,4,7)))level.setBlock(pos,Blocks.STONE.defaultBlockState(),2);WarpState.add(p,"blood",1);for(int i=0;i<1000&&WarpState.tag(p).contains("blood");i++)WarpEvents.runActive(p);h.assertTrue(!WarpState.tag(p).contains("blood"),"Real solid ceiling over air generates the blood visual packet and consumes allowance");}finally{data.setGameTime(time);for(var pos:BlockPos.betweenClosed(origin.offset(-16,-1,-16),origin.offset(16,-1,16)))level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);for(var pos:BlockPos.betweenClosed(origin.offset(-7,4,-7),origin.offset(7,4,7)))level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);}pass(h);}

    @GameTest(template="empty",batch="warp_lifecycle_lightning",timeoutTicks=60) public static void lightningRequiresOpenSky(GameTestHelper h){
        var p=player(h,"WTLightningSky");var level=h.getLevel();var origin=p.blockPosition();var box=new AABB(origin).inflate(8);var before=new HashSet<UUID>();level.getEntities((Entity)null,box,e->true).forEach(e->before.add(e.getUUID()));
        for(var pos:BlockPos.betweenClosed(origin.offset(-3,3,-3),origin.offset(3,3,3)))level.setBlock(pos,Blocks.STONE.defaultBlockState(),2);
        // canSeeSky uses the asynchronously updated light engine, not only block states.
        h.runAfterDelay(10,()->{h.assertTrue(!level.canSeeSky(origin),"Roof skylight update completed");WarpState.add(p,"lightning",1);for(int i=0;i<1000;i++)WarpEvents.runActive(p);h.assertTrue(WarpState.tag(p).getInt("lightning")==1,"Roof prevents lightning and preserves queued use");for(var pos:BlockPos.betweenClosed(origin.offset(-3,3,-3),origin.offset(3,3,3)))level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);});
        h.runAfterDelay(20,()->{try{h.assertTrue(level.canSeeSky(origin),"Removed roof skylight update completed");for(int i=0;i<1000&&WarpState.tag(p).contains("lightning");i++)WarpEvents.runActive(p);h.assertTrue(!WarpState.tag(p).contains("lightning")&&level.getEntities((Entity)null,box,e->e.getType()==EntityType.LIGHTNING_BOLT&&!before.contains(e.getUUID())).size()==1,"Open sky receives exactly one real bolt and consumes one use");}finally{level.getEntities((Entity)null,box,e->!before.contains(e.getUUID())).forEach(Entity::discard);}pass(h);});
    }

    @GameTest(template="empty",batch="warp_lifecycle_fall",timeoutTicks=130) public static void fallReturnsPlayerAndContainerAfterEightyTicks(GameTestHelper h){
        var p=player(h,"WTFallCycle");var level=h.getLevel();
        // Forge FakePlayer's packet listener intentionally ignores teleport calls. Keep
        // the real vanilla teleport implementation, suppressing only outbound packets.
        p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(p.server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p){@Override public void send(net.minecraft.network.protocol.Packet<?> packet){} @Override public void send(net.minecraft.network.protocol.Packet<?> packet,net.minecraft.network.PacketSendListener listener){}};
        var start=p.position();var chestPos=p.blockPosition().below();level.setBlockAndUpdate(chestPos,Blocks.CHEST.defaultBlockState());var chest=(net.minecraft.world.level.block.entity.ChestBlockEntity)level.getBlockEntity(chestPos);var stack=new ItemStack(Items.DIAMOND,17);stack.setHoverName(net.minecraft.network.chat.Component.literal("Fall recovery sample"));chest.setItem(0,stack);WarpEvents.start(p,"fall");h.assertTrue(level.getBlockEntity(chestPos) instanceof VanishingBlock.Snapshot,"Starting actual fall hides the container in a recoverable block entity");h.assertTrue(!WarpEvents.start(p,"fall"),"Repeated fall cannot overwrite its only return point");p.setPos(start.add(0,-20,0));
        h.runAfterDelay(79,()->{WarpEvents.runActive(p);h.assertTrue(WarpState.tag(p).contains("fallReturn")&&p.getY()==start.y-20,"Fall does not terminate before original eighty-tick delay");});
        h.runAfterDelay(83,()->{WarpEvents.runActive(p);h.assertTrue(!WarpState.tag(p).contains("fallReturn")&&p.position().distanceTo(start)<.001&&p.fallDistance==0,"Player returns to exact saved location without fall damage");h.assertTrue(level.getBlockEntity(chestPos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity,"Real world ticks restore vanished container");var restored=((net.minecraft.world.level.block.entity.ChestBlockEntity)level.getBlockEntity(chestPos)).getItem(0);h.assertTrue(restored.getCount()==17&&restored.getHoverName().getString().equals("Fall recovery sample"),"Container contents and custom NBT survive full event");h.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(chestPos).inflate(2)).isEmpty(),"Full fall does not duplicate container drops");((net.minecraft.world.level.block.entity.ChestBlockEntity)level.getBlockEntity(chestPos)).clearContent();level.setBlock(chestPos,Blocks.AIR.defaultBlockState(),2);pass(h);});
    }

    @GameTest(template="empty",batch="warp_configuration") public static void configurationGatesGlobalEventsPermanentRemovalAndRebound(GameTestHelper h){
        var p=player(h,"WTConfigRules");boolean global=WarpConfig.GLOBAL.get(),permanent=WarpConfig.PERMANENT.get(),rebound=WarpConfig.REBOUND.get();var enabled=new LinkedHashMap<String,Boolean>();WarpConfig.EVENT_ENABLED.forEach((id,value)->enabled.put(id,value.get()));
        try{WarpConfig.GLOBAL.set(false);h.assertTrue(WarpConfig.enabled().stream().noneMatch(WarpRules.Event::global),"Global-event switch excludes every global event from natural selection");WarpConfig.EVENT_ENABLED.values().forEach(value->value.set(false));h.assertTrue(WarpState.queueOne(p,10000)==null,"Disabling every event safely leaves the queue empty");WarpConfig.EVENT_ENABLED.forEach((id,value)->value.set(enabled.get(id)));WarpConfig.PERMANENT.set(false);warp(p,5);KnowledgeSavedData.get(p.server).update(p.getUUID(),n->0,k->{k.setWarp(PlayerKnowledge.WarpKind.PERMANENT,7);return null;});WarpState.remove(p,100);h.assertTrue(WarpState.knowledge(p).warp(PlayerKnowledge.WarpKind.PERMANENT)==7&&WarpState.knowledge(p).warp(PlayerKnowledge.WarpKind.TEMPORARY)==0,"Permanent removal switch retains permanent warp but removes temporary warp");warp(p,100);WarpConfig.REBOUND.set(true);WarpState.purge(p);h.assertTrue(WarpState.total(p)==0&&WarpState.tag(p).getList("queue",8).isEmpty(),"Disabled rebound purifies without queued punishment");}
        finally{WarpConfig.GLOBAL.set(global);WarpConfig.PERMANENT.set(permanent);WarpConfig.REBOUND.set(rebound);WarpConfig.EVENT_ENABLED.forEach((id,value)->value.set(enabled.get(id)));}pass(h);
    }

    @GameTest(template="empty",batch="warp_configuration") public static void zeroOwnWarpDiscardsQueuedAndActiveEvents(GameTestHelper h){
        var p=player(h,"WTZeroOwnWarp");
        WarpState.queue(p,"poison");WarpState.add(p,"ears",20);WarpState.add(p,"wind",5);
        warp(p,0);p.tickCount=20;WarpEvents.tickPlayer(p);
        h.assertTrue(WarpState.tag(p).isEmpty(),"Cleansing removes queued and ongoing event counters");
        h.assertTrue(!WarpEvents.start(p,"poison")&&p.getEffect(net.minecraft.world.effect.MobEffects.POISON)==null,"Zero own warp rejects direct and queued events");
        pass(h);
    }

    @GameTest(template="empty",batch="warp_configuration") public static void warpWardHoldsQueuedEventsUntilItEnds(GameTestHelper h){
        var p=player(h,"WTWardQueue");
        WarpState.queue(p,"poison");
        h.assertTrue(WarpState.tag(p).getList("queue",8).size()==1,"Fixture queues one event");
        var ward=tc4revival.content.TCEffects.WARP_WARD.get();
        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(ward,600));
        p.tickCount=0;
        for(int i=0;i<200;i++){p.tickCount++;WarpEvents.tickPlayer(p);}
        h.assertTrue(WarpState.tag(p).getList("queue",8).size()==1&&p.getEffect(net.minecraft.world.effect.MobEffects.POISON)==null,"Warp ward holds the queue instead of starting an event");
        p.removeEffect(ward);
        for(int i=0;i<800&&WarpState.tag(p).getList("queue",8).size()>0;i++){p.tickCount++;WarpEvents.tickPlayer(p);}
        var poison=p.getEffect(net.minecraft.world.effect.MobEffects.POISON);
        h.assertTrue(WarpState.tag(p).getList("queue",8).isEmpty()&&poison!=null&&poison.getDuration()>=400,"Queued event runs once the ward ends");
        pass(h);
    }

    @GameTest(template="empty",batch="warp_configuration") public static void administrativeSwitchesAlsoHoldTheQueue(GameTestHelper h){
        var p=player(h,"WTAdminQueue");
        boolean enabled=WarpConfig.ENABLED.get(),disableWarp=tc4revival.config.TCConfig.SERVER.disableWarp.get();
        try{
            WarpState.queue(p,"poison");p.tickCount=0;
            WarpConfig.ENABLED.set(false);
            for(int i=0;i<200;i++){p.tickCount++;WarpEvents.tickPlayer(p);}
            h.assertTrue(WarpState.tag(p).getList("queue",8).size()==1&&p.getEffect(net.minecraft.world.effect.MobEffects.POISON)==null,"Addon master switch holds queued events");
            WarpConfig.ENABLED.set(true);tc4revival.config.TCConfig.SERVER.disableWarp.set(true);
            for(int i=0;i<200;i++){p.tickCount++;WarpEvents.tickPlayer(p);}
            h.assertTrue(WarpState.tag(p).getList("queue",8).size()==1&&p.getEffect(net.minecraft.world.effect.MobEffects.POISON)==null,"Wuss mode holds queued events");
            tc4revival.config.TCConfig.SERVER.disableWarp.set(false);p.getAbilities().instabuild=true;
            for(int i=0;i<200;i++){p.tickCount++;WarpEvents.tickPlayer(p);}
            h.assertTrue(WarpState.tag(p).getList("queue",8).size()==1&&p.getEffect(net.minecraft.world.effect.MobEffects.POISON)==null,"Creative mode holds queued events");
            p.getAbilities().instabuild=false;
            for(int i=0;i<400&&WarpState.tag(p).getList("queue",8).size()>0;i++){p.tickCount++;WarpEvents.tickPlayer(p);}
            h.assertTrue(p.getEffect(net.minecraft.world.effect.MobEffects.POISON)!=null,"Queue resumes once every switch allows it");
        }finally{WarpConfig.ENABLED.set(enabled);tc4revival.config.TCConfig.SERVER.disableWarp.set(disableWarp);p.getAbilities().instabuild=false;}
        pass(h);
    }
}
