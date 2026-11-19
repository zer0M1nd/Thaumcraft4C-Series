package tc4revival.content;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class TaintedAnimalServerTests {
    @GameTest(template="wand_test_empty")
    public static void taintedDeathConvertsSixFormsAndFallback(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(4,3,4));
        var vanilla=List.of(EntityType.CHICKEN,EntityType.COW,EntityType.CREEPER,EntityType.PIG,EntityType.SHEEP,EntityType.VILLAGER);
        for(int i=0;i<vanilla.size();i++) {
            var original=vanilla.get(i).create(level);original.moveTo(pos.getX()+.25,pos.getY(),pos.getZ()+.75,73,35);
            original.setNoAi(true);original.setNoGravity(true);original.setCustomName(net.minecraft.network.chat.Component.literal("OriginalOnly"));
            level.addFreshEntity(original);original.addEffect(new MobEffectInstance(TCEffects.FLUX_TAINT.get(),200));
            test.assertTrue(original.hurt(level.damageSources().generic(),1000),"Lethal damage accepted");
            var results=level.getEntitiesOfClass(TaintedAnimalEntity.class,new AABB(pos).inflate(1));
            test.assertTrue(results.size()==1 && original.isRemoved(),"Exactly one conversion for "+vanilla.get(i));
            var result=results.get(0);var kind=TaintedAnimalEntity.Kind.values()[i];
            test.assertTrue(result.getType()==TCEntities.taintedAnimal(kind),"Correct old form "+kind);
            test.assertTrue(result.position().equals(original.position()) && result.getYRot()==73 && result.getXRot()==0,"Conversion location and orientation");
            test.assertTrue(!result.hasCustomName() && !result.hasEffect(TCEffects.FLUX_TAINT.get()),"No name or poison is copied");
            test.assertTrue(result.getMaxHealth()==kind.health && result.getAttributeValue(Attributes.ATTACK_DAMAGE)==kind.damage,"Old attributes");
            test.assertTrue(result.removeWhenFarAway(10000)==(kind==TaintedAnimalEntity.Kind.COW),"Only cow can naturally despawn");
            var scan=tc4revival.aspect.entity.EntityAspects.resolve(result).orElseThrow();
            test.assertTrue(scan.legacyIdentity().equals("Thaumcraft."+kind.legacy),"Legacy scan identity");
            result.discard();
        }
        var horse=EntityType.HORSE.create(level);horse.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
        horse.moveTo(pos.getX(),pos.getY(),pos.getZ());horse.setNoAi(true);level.addFreshEntity(horse);
        horse.addEffect(new MobEffectInstance(TCEffects.FLUX_TAINT.get(),200));horse.hurt(level.damageSources().generic(),1000);
        var slimes=level.getEntitiesOfClass(ThaumicSlimeEntity.class,new AABB(pos).inflate(1));
        test.assertTrue(slimes.size()==1 && slimes.get(0).slimeSize()==7,"Other poisoned mobs produce health-scaled slime capped at seven");
        slimes.forEach(Entity::discard);
        var clean=EntityType.PIG.create(level);clean.moveTo(pos.getX(),pos.getY(),pos.getZ());level.addFreshEntity(clean);clean.hurt(level.damageSources().generic(),1000);
        var tainted=TCEntities.TAINTED_PIG.get().create(level);tainted.moveTo(pos.getX(),pos.getY(),pos.getZ());level.addFreshEntity(tainted);
        tainted.addEffect(new MobEffectInstance(TCEffects.FLUX_TAINT.get(),200));tainted.hurt(level.damageSources().generic(),1000);
        test.assertTrue(level.getEntitiesOfClass(TaintedAnimalEntity.class,new AABB(pos).inflate(1),Entity::isAlive).isEmpty()
                && level.getEntitiesOfClass(ThaumicSlimeEntity.class,new AABB(pos).inflate(1)).isEmpty(),"Clean deaths and already-tainted deaths do not convert");
        clean.discard();tainted.discard();passed(test,"death-six-forms-and-fallback");
    }
    @GameTest(template="wand_test_empty")
    public static void taintedSheepShearingAndGrassConversion(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(4,3,4));
        var sheep=TCEntities.TAINTED_SHEEP.get().create(level);sheep.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5);sheep.setNoAi(true);level.addFreshEntity(sheep);
        var shears=new ItemStack(Items.SHEARS);var drops=sheep.onSheared(null,shears,level,pos,100);
        test.assertTrue(drops.size()>=1 && drops.size()<=3 && drops.stream().allMatch(stack->stack.is(Items.PURPLE_WOOL)&&stack.getCount()==1),"One to three purple wool, fortune ignored");
        test.assertTrue(!sheep.isShearable(shears,level,pos) && sheep.onSheared(null,shears,level,pos,0).isEmpty(),"Repeated shearing rejected");
        var loaded=TCEntities.TAINTED_SHEEP.get().create(level);loaded.load(sheep.saveWithoutId(new CompoundTag()));
        test.assertTrue(loaded.sheared(),"Sheared state survives NBT");
        level.setBlockAndUpdate(pos.below(),Blocks.GRASS_BLOCK.defaultBlockState());level.setBlockAndUpdate(pos,Blocks.GRASS.defaultBlockState());
        var goal=new TaintedAnimalEntity.ConvertGrassGoal(sheep);goal.start();
        for(int i=0;i<35;i++)goal.tick();test.assertTrue(level.getBlockState(pos).is(Blocks.GRASS),"No early grass conversion");
        goal.tick();
        test.assertTrue(level.getBlockState(pos).is(TCArcane.TAINT_FIBRES.get()) && level.getBlockState(pos.below()).is(Blocks.GRASS_BLOCK),"Fibres replace grass, ground remains grass block");
        test.assertTrue(level.getBiomeManager().getNoiseBiomeAtPosition(pos).is(TCBiomes.TAINTED_LAND),"Grass conversion taints biome column");
        test.assertTrue(!sheep.sheared(),"Eating grass regrows wool");
        sheep.discard();passed(test,"sheep-shearing-grass-and-nbt");
    }
    @GameTest(template="wand_test_empty",timeoutTicks=140)
    public static void taintedCreeperFuseAndExplosion(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(4,3,4));
        var creeper=TCEntities.TAINTED_CREEPER.get().create(level);creeper.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5);creeper.setNoAi(true);creeper.setNoGravity(true);
        var saved=creeper.saveWithoutId(new CompoundTag());saved.putShort("Fuse",(short)100);saved.putByte("ExplosionRadius",(byte)12);saved.putBoolean("powered",true);creeper.load(saved);
        test.assertTrue(creeper.powered() && creeper.saveWithoutId(new CompoundTag()).getShort("Fuse")==100,"Legacy fuse and powered NBT preserved");
        var victim=EntityType.COW.create(level);victim.moveTo(creeper.getX()+4,creeper.getY(),creeper.getZ());victim.setNoAi(true);victim.setNoGravity(true);level.addFreshEntity(victim);
        victim.setInvulnerable(true);
        float health=victim.getHealth();test.assertTrue(creeper.doHurtTarget(victim) && victim.getHealth()==health,"Creeper melee does no damage");
        var wall=pos.above(2);level.setBlockAndUpdate(wall,Blocks.GLASS.defaultBlockState());
        creeper.swell(1);level.addFreshEntity(creeper);
        test.runAfterDelay(90,()->test.assertTrue(creeper.isAlive(),"Configured 100-tick fuse has not fired early"));
        test.runAfterDelay(102,()->{
            test.assertTrue(creeper.isRemoved(),"Configured fuse controls detonation");
            test.assertTrue(level.getBlockState(wall).is(Blocks.GLASS),"Explosion does not destroy blocks even with saved radius 12");
            test.assertTrue(victim.hasEffect(TCEffects.FLUX_TAINT.get()),"Taint poison reaches nearby non-tainted living entity");
            victim.discard();passed(test,"creeper-fixed-fuse-no-block-damage-and-poison");
        });
    }
    @GameTest(template="wand_test_empty")
    public static void taintedEggKeepsSpawnerAndOldPlacement(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,3,3));
        var player=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"TaintEggTest"));
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        level.setBlockAndUpdate(pos,Blocks.SPAWNER.defaultBlockState());var before=level.getBlockEntity(pos).saveWithFullMetadata();
        var egg=new ItemStack(TCItems.taintedEgg(TaintedAnimalEntity.Kind.CHICKEN),2);player.setItemInHand(InteractionHand.MAIN_HAND,egg);
        var result=egg.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false)));
        test.assertTrue(result.consumesAction() && egg.getCount()==1,"Successful survival egg consumes one");
        test.assertTrue(before.equals(level.getBlockEntity(pos).saveWithFullMetadata()),"TC egg does not change a vanilla spawner");
        var chickens=level.getEntitiesOfClass(TaintedAnimalEntity.class,new AABB(pos.above()).inflate(.8));
        test.assertTrue(chickens.size()==1 && chickens.get(0).getType()==TCEntities.TAINTED_CHICKEN.get(),"Spawned adjacent registered form");
        chickens.forEach(Entity::discard);passed(test,"egg-old-placement-without-spawner-edit");
    }
    @GameTest(template="wand_test_empty")
    public static void taintedMeleeAndVillageDefense(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,3,3));
        var cow=TCEntities.TAINTED_COW.get().create(level);var target=EntityType.PIG.create(level);
        cow.moveTo(Vec3.atBottomCenterOf(pos));target.moveTo(Vec3.atBottomCenterOf(pos.east()));
        cow.setNoAi(true);target.setNoAi(true);target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);target.setHealth(100);
        level.addFreshEntity(cow);level.addFreshEntity(target);cow.setTarget(target);
        var melee=new TaintMeleeGoal(cow,Animal.class,false);
        for(int i=0;i<21;i++) { target.invulnerableTime=0;melee.tick(); }
        test.assertTrue(target.getHealth()==88,"Native melee attacks at ticks zero and twenty");
        var chicken=TCEntities.TAINTED_CHICKEN.get().create(level);
        test.assertTrue(!chicken.causeFallDamage(100,1,level.damageSources().fall()) && chicken.getHealth()==8,"Chicken fall immunity");
        cow.discard();target.discard();
        var resident=TCEntities.TAINTED_VILLAGER.get().create(level);resident.moveTo(Vec3.atBottomCenterOf(pos));resident.setNoAi(true);level.addFreshEntity(resident);
        var home=pos.west();var manager=level.getPoiManager();
        manager.add(home,level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.POINT_OF_INTEREST_TYPE).getHolderOrThrow(net.minecraft.world.entity.ai.village.poi.PoiTypes.HOME));
        try {
            TaintedVillageGoals.updateHome(resident);
            test.assertTrue(resident.hasRestriction(),"Modern home POI supplies village restriction");
            var attacker=EntityType.ZOMBIE.create(level);attacker.moveTo(Vec3.atBottomCenterOf(pos.east()));attacker.setNoAi(true);level.addFreshEntity(attacker);
            var golem=EntityType.IRON_GOLEM.create(level);golem.moveTo(Vec3.atBottomCenterOf(pos.south()));golem.setNoAi(true);level.addFreshEntity(golem);
            resident.setLastHurtByMob(attacker);
            var defense=new TaintedVillageGoals.Defend(golem);
            test.assertTrue(defense.canUse(),"Village remembers a tainted resident's aggressor");defense.start();
            test.assertTrue(golem.getTarget()==attacker,"Golem acquires reported aggressor");
            attacker.discard();golem.discard();
        } finally {manager.remove(home);resident.discard();}
        passed(test,"melee-cadence-chicken-fall-and-village-defense");
    }
    private static void passed(GameTestHelper test,String name) { tc4revival.TC4Revival.LOGGER.info("Tainted animal server test PASS: {}",name);test.succeed(); }
}
