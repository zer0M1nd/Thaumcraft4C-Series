package tc4revival.content;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class TaintSwarmServerTests {
    @GameTest(template="wand_test_empty",timeoutTicks=100)
    public static void taintHiveLifecycleAndPersistence(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(4,3,4));
        var hive=TCEntities.TAINT_HIVE.get().create(level);hive.moveTo(Vec3.atBottomCenterOf(pos));hive.setNoAi(true);hive.setNoGravity(true);
        level.setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());level.addFreshEntity(hive);
        test.assertTrue(hive.getMaxHealth()==75 && hive.getBbWidth()==1 && hive.sporeSize()==10,"Hive attributes and fixed bounds");
        for(int i=0;i<500;i++)hive.sporeActivity();
        test.assertTrue(hive.spawnCounter()==0 && level.getEntitiesOfClass(TaintSwarmEntity.class,new AABB(pos).inflate(3)).isEmpty(),"Expired timer waits for vulnerable player");
        var player=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"HiveTimerTest"));
        player.moveTo(Vec3.atBottomCenterOf(pos.east(4)));player.setGameMode(GameType.CREATIVE);level.addNewPlayer(player);
        try {
            hive.sporeActivity();test.assertTrue(hive.spawnCounter()==0,"Creative player does not activate colony");
            player.setGameMode(GameType.SURVIVAL);hive.sporeActivity();
            var swarms=level.getEntitiesOfClass(TaintSwarmEntity.class,new AABB(pos).inflate(3));
            test.assertTrue(hive.spawnCounter()==500 && swarms.size()==1,"One real swarm and reset timer on activation");swarms.forEach(Entity::discard);
            hive.playerTouch(player);test.assertTrue(hive.isAlive(),"Touch does not burst or destroy hive");
            for(int i=0;i<499;i++)hive.sporeActivity();
            test.assertTrue(hive.spawnCounter()==1 && level.getEntitiesOfClass(TaintSwarmEntity.class,new AABB(pos).inflate(3)).isEmpty(),"No premature second swarm");
            hive.sporeActivity();swarms=level.getEntitiesOfClass(TaintSwarmEntity.class,new AABB(pos).inflate(3));
            test.assertTrue(swarms.size()==1,"Exactly one swarm every 500 eligible ticks");swarms.forEach(Entity::discard);
            hive.sporeActivity();var saved=hive.saveWithoutId(new CompoundTag());
            var restored=TCEntities.TAINT_HIVE.get().create(level);restored.load(saved);
            test.assertTrue(restored.spawnCounter()==hive.spawnCounter() && restored.sporeSize()==10 && restored.getBbHeight()==1,"Hive spawn countdown persists");
            hive.hurt(level.damageSources().generic(),1000);
        } finally {player.discard();}
        test.runAfterDelay(4,()->{
            var deathSwarm=level.getEntitiesOfClass(TaintSwarmEntity.class,new AABB(pos).inflate(4));
            test.assertTrue(deathSwarm.size()==1,"Real death tick releases exactly one swarm");
            deathSwarm.forEach(Entity::discard);hive.discard();passed(test,"hive-player-cycle-save-and-death");
        });
    }
    @GameTest(template="wand_test_empty")
    public static void taintColonyGrowsFromCrustAndRespectsNeighbors(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(4,3,4));
        for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++) {
            level.setBlockAndUpdate(pos.offset(dx,-1,dz),Blocks.STONE.defaultBlockState());
            WorldBiomeChanges.changeColumn(level,pos.offset(dx,0,dz),TCBiomes.TAINTED_LAND);
        }
        level.setBlockAndUpdate(pos,TCArcane.TAINT_CRUST.get().defaultBlockState());level.setBlockAndUpdate(pos.above(),Blocks.AIR.defaultBlockState());
        var rolls=new net.minecraft.world.level.levelgen.LegacyRandomSource(0) {
            @Override public int nextInt(int bound) { return bound==200?0:bound/2; }
        };
        net.minecraftforge.common.ForgeConfigSpec.BooleanValue setting=tc4revival.config.TCConfig.SERVER_SPEC.getValues().get("monster_spawning.spawn_taint_spores");
        boolean previous=setting.get();
        try {
            setting.set(false);TCArcane.TAINT_CRUST.get().randomTick(level.getBlockState(pos),level,pos,rolls);
            test.assertTrue(level.getBlockState(pos).is(TCArcane.TAINT_CRUST.get()) && level.getEntitiesOfClass(TaintHiveEntity.class,new AABB(pos).inflate(2)).isEmpty(),"Disabled spore setting prevents colony generation");
            setting.set(true);TCArcane.TAINT_CRUST.get().randomTick(level.getBlockState(pos),level,pos,rolls);
        } finally {setting.set(previous);}
        var hives=level.getEntitiesOfClass(TaintHiveEntity.class,new AABB(pos).inflate(2));
        test.assertTrue(hives.size()==1 && level.isEmptyBlock(pos),"Actual crust random tick produces colony and consumes block");
        var neighbor=pos.east(3);level.setBlockAndUpdate(neighbor,TCArcane.TAINT_CRUST.get().defaultBlockState());
        test.assertTrue(!TaintSolidBlock.growHive(level,neighbor) && level.getBlockState(neighbor).is(TCArcane.TAINT_CRUST.get()),"Existing colony within sixteen blocks prevents duplication without eating crust");
        hives.forEach(Entity::discard);passed(test,"crust-generation-and-colony-spacing");
    }
    @GameTest(template="wand_test_empty")
    public static void taintSwarmCombatFlagsAndLoot(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(4,3,4));
        var swarm=TCEntities.TAINT_SWARM.get().create(level);swarm.moveTo(Vec3.atBottomCenterOf(pos));swarm.setNoAi(true);level.addFreshEntity(swarm);
        var victim=EntityType.COW.create(level);victim.moveTo(Vec3.atBottomCenterOf(pos.east()));victim.setNoAi(true);level.addFreshEntity(victim);
        victim.setDeltaMovement(.21,.14,-.31);var velocity=victim.getDeltaMovement();float initialHealth=victim.getHealth();
        swarm.strike(victim);
        test.assertTrue(victim.getHealth()==initialHealth-2 && victim.hasEffect(MobEffects.WEAKNESS),"Two damage plus 100-tick weakness");
        test.assertTrue(victim.getEffect(MobEffects.WEAKNESS).getDuration()==100 && victim.getDeltaMovement().equals(velocity) && !victim.hasImpulse,"Attack preserves motion without knockback");
        swarm.strike(victim);test.assertTrue(victim.getHealth()==initialHealth-2,"Attack cooldown blocks immediate repeat");
        test.assertTrue(!swarm.isPushable() && swarm.isIgnoringBlockTriggers() && !swarm.causeFallDamage(100,1,level.damageSources().fall()),"No pushing, pressure-plate triggers, or fall damage");
        var saved=swarm.saveWithoutId(new CompoundTag());saved.putByte("Flags",(byte)3);saved.putByte("damBonus",(byte)5);
        var loaded=TCEntities.TAINT_SWARM.get().create(level);loaded.load(saved);
        test.assertTrue(loaded.summoned() && loaded.getAttributeValue(Attributes.ATTACK_DAMAGE)==7,"Saved damage bonus is restored");
        loaded.summoned(false);test.assertTrue(loaded.saveWithoutId(new CompoundTag()).getByte("Flags")==1,"Clearing summoned flag preserves independent bits");
        loaded.summoned(true);float health=loaded.getHealth();loaded.customServerAiStep();
        test.assertTrue(loaded.getHealth()==health-5 && loaded.getTarget()==null,"Summoned swarm without a target decays and does not seek players");
        for(var entry:List.of(swarm,TCEntities.TAINT_HIVE.get().create(level)))test.assertTrue(tc4revival.aspect.entity.EntityAspects.resolve(entry).isPresent(),"Both new entities scan using old aspect mappings");
        swarm.discard();victim.discard();passed(test,"swarm-combat-summoned-nbt-and-scan");
    }
    @GameTest(template="wand_test_empty")
    public static void ordinaryTaintSporeStillGrowsAndBursts(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(4,3,4));
        level.setBlockAndUpdate(pos.below(2),Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(pos.below(),TCArcane.TAINT_FIBRES.get().defaultBlockState().setValue(TaintFibreBlock.VARIANT,4));
        var spore=TCEntities.TAINT_SPORE.get().create(level);spore.moveTo(Vec3.atBottomCenterOf(pos));spore.setSporeSize(9);spore.setNoAi(true);level.addFreshEntity(spore);
        for(int i=0;i<1200;i++)spore.sporeActivity();test.assertTrue(spore.sporeSize()==9,"Ordinary spore does not grow early");
        spore.sporeActivity();test.assertTrue(spore.sporeSize()==10,"Ordinary spore retains 1201-tick growth");
        var player=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"SporeTouchTest"));
        spore.playerTouch(player);
        var spiders=level.getEntitiesOfClass(TaintSpiderEntity.class,new AABB(pos).inflate(3));
        test.assertTrue(spore.isRemoved() && spiders.size()>=3 && spiders.size()<=8,"Touch bursts into old size-based spider count");
        test.assertTrue(level.getBlockState(pos.below()).getValue(TaintFibreBlock.VARIANT)==3,"Mature stalk returns to immature form");
        spiders.forEach(Entity::discard);passed(test,"ordinary-spore-growth-and-touch-regression");
    }
    private static void passed(GameTestHelper test,String name) { tc4revival.TC4Revival.LOGGER.info("Taint swarm server test PASS: {}",name);test.succeed(); }
}
