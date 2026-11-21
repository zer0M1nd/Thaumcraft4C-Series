package timodern;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;
import tc4revival.content.*;
import tc4revival.aspect.TCAspects;

@GameTestHolder("thaumicinfusion") @PrefixGameTestTemplate(false)
public class InfusionBehaviorServerTests {
    static net.minecraft.server.level.ServerPlayer player(GameTestHelper t,String name,BlockPos p){
        var profile=new com.mojang.authlib.GameProfile(UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8)),name);
        var player=net.minecraftforge.common.util.FakePlayerFactory.get(t.getLevel(),profile);player.setPos(Vec3.atBottomCenterOf(p.above()));
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);player.setHealth(20);player.getInventory().clearContent();return player;
    }
    @GameTest(template="test_empty") public static void independentBestiaAndIgnisHooks(GameTestHelper t){
        var l=t.getLevel();var p=InfusionServerTests.put(t,"bestia","ignis");var s=l.getBlockState(p);
        t.assertTrue(!s.isValidSpawn(l,p,EntityType.PIG)&&!s.isValidSpawn(l,p,EntityType.ZOMBIE),"Bestia independently rejects passive and hostile spawning");
        t.assertTrue(s.isFireSource(l,p,Direction.UP)&&s.isFlammable(l,p,Direction.UP)&&s.getFlammability(l,p,Direction.UP)==0&&s.getFireSpreadSpeed(l,p,Direction.UP)==4096,"Ignis preserves original eternal fire and non-consuming spread rules");
        boolean previous=InfusionConfig.EFFECTS.get("ignis").get();InfusionConfig.EFFECTS.get("ignis").set(false);
        try{t.assertTrue(!s.isFireSource(l,p,Direction.UP)&&s.getFireSpreadSpeed(l,p,Direction.UP)==0,"Disabled effect restores base block properties");}
        finally{InfusionConfig.EFFECTS.get("ignis").set(previous);InfusionData.remove(l,p);}t.succeed();
    }
    @GameTest(template="test_empty") public static void sanoFoodGateAndRange(GameTestHelper t){
        var l=t.getLevel();var p=InfusionServerTests.put(t,"sano");var player=player(t,"TIHealing",p);l.addFreshEntity(player);player.setHealth(5);player.getFoodData().setFoodLevel(0);
        for(int i=0;i<10;i++)InfusionRuntime.effect(l,p,InfusionData.at(l,p),"sano",new HashSet<>());
        t.assertTrue(player.getHealth()==5,"Sano cannot heal an empty food bar");player.getFoodData().setFoodLevel(10);l.random.setSeed(876);
        for(int i=0;i<20;i++)InfusionRuntime.effect(l,p,InfusionData.at(l,p),"sano",new HashSet<>());
        t.assertTrue(player.getHealth()>5&&player.getFoodData().getFoodLevel()==10,"Sano heals inside its area without consuming food");
        player.setHealth(5);player.setPos(Vec3.atCenterOf(p.east(20)));for(int i=0;i<20;i++)InfusionRuntime.effect(l,p,InfusionData.at(l,p),"sano",new HashSet<>());
        t.assertTrue(player.getHealth()==5,"Player outside fifteen-block area is unaffected");player.discard();InfusionData.remove(l,p);t.succeed();
    }
    @GameTest(template="test_empty") public static void alienisOnlyUsesSafeCells(GameTestHelper t){
        var l=t.getLevel();var p=InfusionServerTests.put(t,"alienis");int previous=InfusionConfig.TELEPORT_RADIUS.get();InfusionConfig.TELEPORT_RADIUS.set(2);
        var target=p.east();var entity=EntityType.PIG.create(l);entity.setPos(Vec3.atBottomCenterOf(p.above()));
        try{
            for(var q:BlockPos.betweenClosed(p.offset(-2,-3,-2),p.offset(1,2,1)))l.setBlockAndUpdate(q,Blocks.STONE.defaultBlockState());
            l.setBlockAndUpdate(target,Blocks.AIR.defaultBlockState());l.setBlockAndUpdate(target.above(),Blocks.AIR.defaultBlockState());InfusionRuntime.randomTeleport(l,p,entity);
            t.assertTrue(entity.position().equals(Vec3.atBottomCenterOf(target)),"Alienis chooses the only air pair with non-air support");
            l.setBlockAndUpdate(target.above(),Blocks.STONE.defaultBlockState());var before=entity.position();InfusionRuntime.randomTeleport(l,p,entity);
            t.assertTrue(entity.position().equals(before),"Without a safe cell Alienis leaves entity in place");
        }finally{InfusionConfig.TELEPORT_RADIUS.set(previous);InfusionData.remove(l,p);}t.succeed();
    }
    static long spawnSeed(){for(long seed=0;;seed++)if(net.minecraft.util.RandomSource.create(seed).nextInt(1000)==1)return seed;}
    @GameTest(template="test_empty",timeoutTicks=80) public static void victusSpawnRequiresAndConsumesEssentia(GameTestHelper t){
        var l=t.getLevel();var p=InfusionServerTests.put(t,"victus");l.setBlockAndUpdate(p,Blocks.GLOWSTONE.defaultBlockState());l.setBlockAndUpdate(p.above(),Blocks.AIR.defaultBlockState());l.setBlockAndUpdate(p.above(2),Blocks.AIR.defaultBlockState());
        InfusionData.at(l,p).settings.putLong("spawnCooldown",Long.MAX_VALUE);var jarPos=p.east(2);l.setBlockAndUpdate(jarPos,TCArcane.ESSENTIA_JAR.get().defaultBlockState());var jar=(EssentiaJarBlockEntity)l.getBlockEntity(jarPos);jar.addToContainer(TCAspects.catalog().require("victus"),8);
        t.runAtTickTime(10,()->{
            t.assertTrue(l.getMaxLocalRawBrightness(p.above())>=8,"Spawn fixture is lit");l.random.setSeed(spawnSeed());InfusionRuntime.spawn(l,p,"victus");
            var mobs=l.getEntitiesOfClass(net.minecraft.world.entity.animal.Animal.class,new AABB(p).expandTowards(0,2,0));
            t.assertTrue(mobs.size()==1&&jar.amount()==0,"Victus creates an original passive mob and consumes eight nearby essentia");mobs.forEach(Entity::discard);
            l.random.setSeed(spawnSeed());InfusionRuntime.spawn(l,p,"victus");t.assertTrue(l.getEntitiesOfClass(net.minecraft.world.entity.animal.Animal.class,new AABB(p).expandTowards(0,2,0)).isEmpty(),"Failed fuel lookup does not create a free mob");InfusionData.remove(l,p);t.succeed();
        });
    }
    @GameTest(template="test_empty") public static void customizationAndExplosionProtection(GameTestHelper t){
        var l=t.getLevel();var p=InfusionServerTests.put(t,"sensus","fabrico","humanus");var player=player(t,"TIModelData",p);var runtime=new InfusionRuntime();
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Blocks.BRICKS));runtime.punch(new net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock(player,p,Direction.UP,net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.Action.START));
        var entry=InfusionData.at(l,p);t.assertTrue(entry.settings.contains("disguise"),"Sensus accepts a solid held block");
        player.setShiftKeyDown(true);runtime.punch(new net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock(player,p,Direction.UP,net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.Action.START));t.assertTrue(!entry.settings.contains("disguise"),"Sneak punch clears the disguise");player.setShiftKeyDown(false);
        var aspect=TCAspects.catalog().require("ignis");player.setItemInHand(InteractionHand.MAIN_HAND,EssentiaPhialItem.filled(aspect));runtime.punch(new net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock(player,p,Direction.UP,net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.Action.START));
        t.assertTrue(entry.settings.getInt("tint")==aspect.color(),"Fabrico stores the held phial's exact aspect color without consuming it");
        var broken=new net.minecraftforge.event.level.BlockEvent.BreakEvent(l,p,l.getBlockState(p),player);runtime.breakBlock(broken);t.assertTrue(broken.isCanceled()&&InfusionData.has(l,p,"humanus"),"Humanus survives ordinary player mining");
        player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);broken=new net.minecraftforge.event.level.BlockEvent.BreakEvent(l,p,l.getBlockState(p),player);runtime.breakBlock(broken);t.assertTrue(!broken.isCanceled()&&InfusionData.at(l,p)==null,"Creative removal clears positional data");t.succeed();
    }
    @GameTest(template="test_empty") public static void iterAndExanimisTeleportEventPaths(GameTestHelper t){
        var l=t.getLevel();var p=InfusionServerTests.put(t,"iter","exanimis");var q=p.east(3);l.setBlockAndUpdate(q,Blocks.STONE.defaultBlockState());
        var other=new InfusionData.Entry();other.aspects.add("iter");InfusionData.get(l).entries.put(q.asLong(),other);InfusionRuntime.putDestination(InfusionData.at(l,p).settings,"iter",l,q);
        // Forge FakePlayer's packet listener intentionally suppresses teleports. This fake records
        // the server's requested destination while the real effect path selects/gates that request.
        var profile=new com.mojang.authlib.GameProfile(UUID.nameUUIDFromBytes("TITeleports".getBytes(java.nio.charset.StandardCharsets.UTF_8)),"TITeleports");
        var player=new net.minecraftforge.common.util.FakePlayer(l,profile){@Override public void teleportTo(net.minecraft.server.level.ServerLevel level,double x,double y,double z,float yaw,float pitch){moveTo(x,y,z,yaw,pitch);}};
        player.setPos(Vec3.atBottomCenterOf(p.above()));l.addFreshEntity(player);var before=player.position();
        InfusionRuntime.effect(l,p,InfusionData.at(l,p),"iter",new HashSet<>());t.assertTrue(player.position().equals(before),"Iter requires crouching");
        player.setShiftKeyDown(true);InfusionRuntime.effect(l,p,InfusionData.at(l,p),"iter",new HashSet<>());
        t.assertTrue(player.position().equals(Vec3.atBottomCenterOf(q.above()))&&!player.isShiftKeyDown(),"Iter requests reciprocal destination and clears crouching after teleport");
        player.setPos(Vec3.atBottomCenterOf(p.above()));var runtime=new InfusionRuntime();runtime.die(new net.minecraftforge.event.entity.living.LivingDeathEvent(player,player.damageSources().generic()));
        player.setPos(Vec3.atCenterOf(p.east(8)));runtime.respawn(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent(player,false));
        t.assertTrue(player.position().equals(Vec3.atBottomCenterOf(p.above())),"Exanimis carries a death position into the player's respawn event");
        player.discard();InfusionData.remove(l,p);InfusionData.remove(l,q);t.succeed();
    }}
