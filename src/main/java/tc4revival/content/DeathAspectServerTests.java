package tc4revival.content;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.TCAspects;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class DeathAspectServerTests {
    @GameTest(template="wand_test_empty")
    public static void deathAspectPlayerCreditAndPrimalBounds(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(4,3,4));
        var player=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"DeathVisTest"));
        var bounds=Map.of("aer",3,"ordo",3,"aqua",3,"terra",6);int total=0;
        for(int sample=0;sample<32;sample++) {
            var cow=EntityType.COW.create(level);cow.moveTo(Vec3.atBottomCenterOf(pos));cow.setNoAi(true);level.addFreshEntity(cow);
            cow.hurt(level.damageSources().playerAttack(player),1);cow.invulnerableTime=0;
            test.assertTrue(LegacyCombatCredit.remaining(cow)>0,"Real player hit starts vanilla credit countdown");
            level.random.setSeed(sample);cow.hurt(level.damageSources().generic(),1000);
            var orbs=level.getEntitiesOfClass(AspectOrbEntity.class,new AABB(pos).inflate(2));total+=orbs.size();
            var seen=new HashSet<String>();
            for(var orb:orbs) {
                var maximum=bounds.get(orb.aspect().tag());
                test.assertTrue(orb.aspect().isPrimal() && maximum!=null && orb.value()>=1 && orb.value()<=maximum,"Cow compound aspects reduce by addition into bounded primal charges");
                test.assertTrue(seen.add(orb.aspect().tag()),"At most one orb per distinct primal per death");
                test.assertTrue(orb.position().equals(cow.position()),"Drop starts at victim location");orb.discard();
            }
            cow.discard();
        }
        test.assertTrue(total>32 && total<128,"Both accepted and rejected primal rolls observed");
        passed(test,"player-hit-environmental-death-and-primal-bounds");
    }
    @GameTest(template="wand_test_empty")
    public static void deathAspectExclusionsAndTaintBranch(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(4,3,4));
        var player=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"DeathVisRules"));
        for(int mode=0;mode<3;mode++) {
            var victim=mode==2?EntityType.ARMOR_STAND.create(level):EntityType.COW.create(level);
            if(mode==2)test.assertTrue(tc4revival.aspect.entity.EntityAspects.resolve(victim).isEmpty(),"Unknown-aspect fixture remains undefined");
            victim.moveTo(Vec3.atBottomCenterOf(pos));level.addFreshEntity(victim);
            if(mode==1) { victim.hurt(level.damageSources().playerAttack(player),1);LegacyCombatCredit.set(victim,0);victim.invulnerableTime=0; }
            victim.hurt(mode==2?level.damageSources().playerAttack(player):level.damageSources().generic(),1000);
            test.assertTrue(level.getEntitiesOfClass(AspectOrbEntity.class,new AABB(pos).inflate(2)).isEmpty(),"No charge for uncredited/expired/unknown-aspect death "+mode);victim.discard();
        }
        var poisoned=EntityType.COW.create(level);poisoned.moveTo(Vec3.atBottomCenterOf(pos));level.addFreshEntity(poisoned);
        poisoned.addEffect(new MobEffectInstance(TCEffects.FLUX_TAINT.get(),200));level.random.setSeed(0);
        poisoned.hurt(level.damageSources().playerAttack(player),1000);
        test.assertTrue(level.getEntitiesOfClass(AspectOrbEntity.class,new AABB(pos).inflate(2)).isEmpty(),"Poisoned non-tainted victim converts without also dropping charge");
        var converted=level.getEntitiesOfClass(TaintedAnimalEntity.class,new AABB(pos).inflate(2));
        test.assertTrue(converted.size()==1,"Taint branch still creates one new form");
        var rules=level.getGameRules();boolean old=rules.getBoolean(GameRules.RULE_DOMOBLOOT);
        try {
            rules.getRule(GameRules.RULE_DOMOBLOOT).set(false,level.getServer());
            var tainted=converted.get(0);tainted.addEffect(new MobEffectInstance(TCEffects.FLUX_TAINT.get(),200));
            level.random.setSeed(0);tainted.hurt(level.damageSources().playerAttack(player),1000);
            var orbs=level.getEntitiesOfClass(AspectOrbEntity.class,new AABB(pos).inflate(2));
            test.assertTrue(!orbs.isEmpty(),"Already-tainted creature uses normal charge branch even while poisoned and mob loot disabled");
            orbs.forEach(Entity::discard);tainted.discard();
        } finally {rules.getRule(GameRules.RULE_DOMOBLOOT).set(old,level.getServer());}
        passed(test,"eligibility-exclusions-taint-mutual-exclusion-and-gamerule");
    }
    @GameTest(template="wand_test_empty")
    public static void summonedKillChargesFirstAvailableHotbarWand(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(4,3,4));
        var swarm=TCEntities.TAINT_SWARM.get().create(level);swarm.moveTo(Vec3.atBottomCenterOf(pos));swarm.setNoAi(true);swarm.summoned(true);level.addFreshEntity(swarm);
        var cow=EntityType.COW.create(level);cow.moveTo(Vec3.atBottomCenterOf(pos.east()));cow.setNoAi(true);cow.setHealth(2);level.addFreshEntity(cow);
        level.random.setSeed(0);swarm.strike(cow);
        test.assertTrue(!cow.isAlive() && LegacyCombatCredit.remaining(cow)==100,"Real summoned swarm strike supplies kill credit before death event");
        var orbs=level.getEntitiesOfClass(AspectOrbEntity.class,new AABB(pos).inflate(3));test.assertTrue(!orbs.isEmpty(),"Summoned kill drops actual charge entities");
        var player=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"DeathVisPickup"));
        var item=TCItems.CASTING_WAND.get();var full=item.create(WandParts.Rod.WOOD,WandParts.Cap.IRON,false,true);
        var first=item.create(WandParts.Rod.WOOD,WandParts.Cap.IRON,false,false);var second=first.copy();
        player.getInventory().setItem(0,full);player.getInventory().setItem(1,first);player.getInventory().setItem(2,second);
        for(var orb:orbs) {
            var aspect=orb.aspect();int value=orb.value();player.takeXpDelay=0;orb.playerTouch(player);
            test.assertTrue(orb.isRemoved() && item.vis(first,aspect)==100*value && item.vis(second,aspect)==0,"Death drop is collected by first non-full hotbar wand in centivis");
        }
        var aspect=TCAspects.catalog().require("aer");player.getInventory().clearContent();
        var extra=new AspectOrbEntity(level,pos.getX(),pos.getY(),pos.getZ(),aspect,1);level.addFreshEntity(extra);
        player.takeXpDelay=0;extra.playerTouch(player);test.assertTrue(!extra.isRemoved(),"No eligible wand leaves orb available");extra.discard();
        swarm.discard();cow.discard();passed(test,"summoned-kill-to-first-eligible-wand");
    }
    private static void passed(GameTestHelper test,String name) { tc4revival.TC4Revival.LOGGER.info("Death aspect server test PASS: {}",name);test.succeed(); }
}
