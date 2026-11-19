package tc4revival.content;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.common.MinecraftForge;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class ChampionServerTests {
    @GameTest(template="wand_test_empty")
    public static void championAttributesAndReload(GameTestHelper test) {
        var level=test.getLevel();
        for(var kind:ChampionMobs.Kind.values()) {
            var mob=EntityType.ZOMBIE.create(level);
            double speed=mob.getAttributeBaseValue(Attributes.MOVEMENT_SPEED);
            double damage=mob.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
            ChampionMobs.make(mob,kind,true);
            test.assertTrue(ChampionMobs.kind(mob)==kind,"All thirteen attribute ids resolve: "+kind);
            test.assertTrue(mob.getMaxHealth()==50 && mob.getHealth()==45,"Health adds thirty, heals twenty-five");
            test.assertTrue(Math.abs(mob.getAttributeValue(Attributes.ATTACK_DAMAGE)-damage*(kind==ChampionMobs.Kind.MIGHTY?12:3))<.001,"Damage modifiers multiply for Mighty");
            test.assertTrue(Math.abs(mob.getAttributeValue(Attributes.MOVEMENT_SPEED)-speed*(kind==ChampionMobs.Kind.BOLD?1.3:1))<.001,"Bold speed modifier");
            test.assertTrue(mob.getAbsorptionAmount()==(kind==ChampionMobs.Kind.WARDED?10:0),"Warded uses base health");
            var data=new CompoundTag();mob.saveWithoutId(data);
            var copy=EntityType.ZOMBIE.create(level);copy.load(data);
            ChampionMobs.joined(new net.minecraftforge.event.entity.EntityJoinLevelEvent(copy,level));
            test.assertTrue(ChampionMobs.kind(copy)==kind && copy.getMaxHealth()==50 && copy.isPersistenceRequired(),"Reload keeps kind and attributes without reroll");
        }
        var creeper=EntityType.CREEPER.create(level);ChampionMobs.make(creeper,false);
        test.assertTrue(ChampionMobs.kind(creeper)==ChampionMobs.Kind.BOLD,"Creeper random selection is always Bold");
        var boss=TCEntities.ELDRITCH_GOLEM.get().create(level);float health=boss.getMaxHealth();
        ChampionMobs.make(boss,ChampionMobs.Kind.BOLD,false);
        test.assertTrue(boss.getMaxHealth()==health,"Shared boss superclass does not get common champion health");
        passed(test,"attributes-and-reload");
    }
    @GameTest(template="wand_test_empty")
    public static void championCombatAndRegeneration(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,2,3));
        var attacker=EntityType.ZOMBIE.create(level);attacker.moveTo(Vec3.atBottomCenterOf(pos));
        for(var kind:List.of(ChampionMobs.Kind.GRIM,ChampionMobs.Kind.FIERY,ChampionMobs.Kind.SICKLY,ChampionMobs.Kind.VENOMOUS,ChampionMobs.Kind.VAMPIRIC)) {
            var mob=EntityType.ZOMBIE.create(level);ChampionMobs.make(mob,kind,false);mob.setHealth(10);
            var cow=EntityType.COW.create(level);
            for(int i=0;i<64;i++) MinecraftForge.EVENT_BUS.post(new LivingHurtEvent(cow,level.damageSources().mobAttack(mob),6));
            boolean effect=switch(kind) {
                case GRIM -> cow.hasEffect(MobEffects.WITHER);
                case FIERY -> cow.isOnFire();
                case SICKLY -> cow.hasEffect(MobEffects.HUNGER);
                case VENOMOUS -> cow.hasEffect(MobEffects.POISON);
                case VAMPIRIC -> mob.getHealth()==mob.getMaxHealth();
                default -> false;
            };
            test.assertTrue(effect,"Offensive effect reaches target: "+kind);
            if(kind==ChampionMobs.Kind.SICKLY)test.assertTrue(cow.getEffect(MobEffects.HUNGER).getDuration()==500,"Hunger duration");
            if(kind==ChampionMobs.Kind.VENOMOUS)test.assertTrue(cow.getEffect(MobEffects.POISON).getDuration()==100,"Poison duration");
            if(kind==ChampionMobs.Kind.GRIM)test.assertTrue(cow.getEffect(MobEffects.WITHER).getDuration()==200,"Wither duration");
            var arrow=new net.minecraft.world.entity.projectile.Arrow(level,mob);
            var clean=EntityType.COW.create(level);mob.setHealth(10);
            MinecraftForge.EVENT_BUS.post(new LivingHurtEvent(clean,level.damageSources().arrow(arrow,mob),6));
            test.assertTrue(clean.getActiveEffects().isEmpty() && !clean.isOnFire() && mob.getHealth()==10,"Indirect arrow does not invoke melee champion effect");
        }
        var armored=EntityType.ZOMBIE.create(level);ChampionMobs.make(armored,ChampionMobs.Kind.ARMORED,false);
        var hit=new LivingHurtEvent(armored,level.damageSources().mobAttack(attacker),10);MinecraftForge.EVENT_BUS.post(hit);
        test.assertTrue(Math.abs(hit.getAmount()-7.6f)<.001,"Armored event damage is 19/25");
        var environment=new LivingHurtEvent(armored,level.damageSources().generic(),10);MinecraftForge.EVENT_BUS.post(environment);
        test.assertTrue(environment.getAmount()==10,"No direct living source means no armored effect");
        var spined=EntityType.ZOMBIE.create(level);ChampionMobs.make(spined,ChampionMobs.Kind.SPINED,false);
        var cow=EntityType.COW.create(level);float before=cow.getHealth();
        MinecraftForge.EVENT_BUS.post(new LivingHurtEvent(spined,level.damageSources().mobAttack(cow),2));
        test.assertTrue(before-cow.getHealth()>=1 && before-cow.getHealth()<=3,"Spined returns one to three damage");
        cow.invulnerableTime=0;before=cow.getHealth();
        MinecraftForge.EVENT_BUS.post(new LivingHurtEvent(spined,level.damageSources().thorns(cow),2));
        test.assertTrue(cow.getHealth()==before,"Thorns cannot recurse");
        var ward=EntityType.ZOMBIE.create(level);ChampionMobs.make(ward,ChampionMobs.Kind.WARDED,false);
        ward.setAbsorptionAmount(9);ward.tickCount=25;ward.invulnerableTime=1;
        ChampionMobs.tick(new LivingEvent.LivingTickEvent(ward));test.assertTrue(ward.getAbsorptionAmount()==9,"Warded pauses during invulnerability");
        ward.invulnerableTime=0;ChampionMobs.tick(new LivingEvent.LivingTickEvent(ward));ChampionMobs.tick(new LivingEvent.LivingTickEvent(ward));
        test.assertTrue(ward.getAbsorptionAmount()==10,"Warded regenerates to base-health cap");
        var undying=EntityType.ZOMBIE.create(level);ChampionMobs.make(undying,ChampionMobs.Kind.UNDYING,false);undying.setHealth(10);undying.tickCount=19;
        ChampionMobs.tick(new LivingEvent.LivingTickEvent(undying));undying.tickCount=20;ChampionMobs.tick(new LivingEvent.LivingTickEvent(undying));
        test.assertTrue(undying.getHealth()==11,"Undying restores one every twenty ticks");
        var infested=EntityType.ZOMBIE.create(level);infested.moveTo(Vec3.atBottomCenterOf(pos));ChampionMobs.make(infested,ChampionMobs.Kind.INFESTED,false);
        for(int i=0;i<64;i++)MinecraftForge.EVENT_BUS.post(new LivingHurtEvent(infested,level.damageSources().mobAttack(attacker),1));
        var spiders=level.getEntitiesOfClass(TaintSpiderEntity.class,new AABB(pos).inflate(3));
        test.assertTrue(!spiders.isEmpty() && spiders.size()<64,"Infested creates real spiders probabilistically");spiders.forEach(Entity::discard);
        passed(test,"combat-and-regeneration");
    }
    @GameTest(template="wand_test_empty")
    public static void championRewardBoundaries(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,2,3));
        for(var type:List.of(EntityType.ZOMBIE,TCEntities.TAINTACLE_GIANT.get(),TCEntities.CULTIST_PORTAL.get(),TCEntities.ELDRITCH_GOLEM.get())) {
            var mob=(Monster)type.create(level);mob.moveTo(Vec3.atBottomCenterOf(pos));ChampionMobs.make(mob,ChampionMobs.Kind.BOLD,false);
            for(int looting:List.of(0,5,10)) {
                var drops=new ArrayList<ItemEntity>();
                // The data-driven drop table resolves looting through Forge's loot context.
                java.util.function.Consumer<LootingLevelEvent> lootOverride=event->{if(event.getEntity()==mob)event.setLootingLevel(looting);};
                MinecraftForge.EVENT_BUS.addListener(net.minecraftforge.eventbus.api.EventPriority.LOWEST,lootOverride);
                try { ChampionMobs.drops(new LivingDropsEvent(mob,level.damageSources().generic(),drops,looting,true)); }
                finally { MinecraftForge.EVENT_BUS.unregister(lootOverride); }
                boolean boss=mob instanceof ThaumcraftBossEntity;
                test.assertTrue(drops.size()==(boss?0:1),"Only the original shared boss class excludes champion bags: "+type);
                if(!boss) {
                    test.assertTrue(drops.get(0).getItem().getItem() instanceof LootBagItem,"Champion reward is loot bag");
                    if(looting==10)test.assertTrue(drops.get(0).getItem().is(TCItems.lootBag(LootBagItem.Kind.values()[2])),"Looting caps rarity at rare; type="+type+" suppliedEventLooting="+looting+" actual="+drops.get(0).getItem().save(new CompoundTag()));
                    test.assertTrue(drops.get(0).getY()==mob.getEyeY(),"Bag origin is eye height");
                }
                var xp=level.getEntitiesOfClass(ExperienceOrb.class,new AABB(pos).inflate(3));
                int total=xp.stream().mapToInt(ExperienceOrb::getValue).sum();
                test.assertTrue(boss?total==0:total>=5&&total<=7,"Bonus experience totals five to seven");xp.forEach(Entity::discard);
            }
            var drops=new ArrayList<ItemEntity>();ChampionMobs.drops(new LivingDropsEvent(mob,level.damageSources().generic(),drops,0,false));
            test.assertTrue(drops.isEmpty(),"No recent player credit means no champion bag");
            var fake=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"ChampionLootTest"));
            ChampionMobs.drops(new LivingDropsEvent(mob,level.damageSources().playerAttack(fake),drops,0,true));
            test.assertTrue(drops.isEmpty(),"Fake-player final source excludes rewards");
        }
        passed(test,"reward-boundaries");
    }
    @GameTest(template="wand_test_empty")
    public static void championSelectionAndWarp(GameTestHelper test) {
        var level=test.getLevel();var config=tc4revival.config.TCConfig.SERVER;
        boolean enabled=config.championMobs.get(),disableWarp=config.disableWarp.get();
        var oldDifficulty=level.getDifficulty();
        try {
            level.getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL,true);config.championMobs.set(true);
            long seed=0;for(;;seed++) { level.random.setSeed(seed);if(level.random.nextInt(100)==0)break; }
            for(var type:List.of(TCEntities.TAINTACLE_SMALL.get(),TCEntities.TAINT_SPIDER.get(),EntityType.ZOMBIE,EntityType.WITCH)) {
                var mob=(Monster)type.create(level);mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(10);
                mob.moveTo(Vec3.atBottomCenterOf(test.absolutePos(new BlockPos(3,2,3))));level.random.setSeed(seed);
                ChampionMobs.joined(new net.minecraftforge.event.entity.EntityJoinLevelEvent(mob,level));
                test.assertTrue(ChampionMobs.kind(mob)!=null,"Legacy class whitelist includes subclasses at health threshold: "+type);
                int selected=ChampionMobs.id(mob);ChampionMobs.joined(new net.minecraftforge.event.entity.EntityJoinLevelEvent(mob,level));
                test.assertTrue(ChampionMobs.id(mob)==selected,"Repeated join never rerolls");
            }
            var small=TCEntities.TAINTACLE_SMALL.get().create(level);level.random.setSeed(seed);
            ChampionMobs.joined(new net.minecraftforge.event.entity.EntityJoinLevelEvent(small,level));
            test.assertTrue(ChampionMobs.id(small)==-1,"Default eight-health small tentacle remains normal");
            var modern=EntityType.HUSK.create(level);level.random.setSeed(seed);
            ChampionMobs.joined(new net.minecraftforge.event.entity.EntityJoinLevelEvent(modern,level));
            test.assertTrue(ChampionMobs.id(modern)==-1,"New-version mob content is not added to legacy whitelist");
            config.championMobs.set(false);
            var boss=TCEntities.ELDRITCH_GOLEM.get().create(level);
            ChampionMobs.joined(new net.minecraftforge.event.entity.EntityJoinLevelEvent(boss,level));
            test.assertTrue(ChampionMobs.kind(boss)!=null,"Boss whitelist override survives disabled common champions");
            config.disableWarp.set(false);
            var player=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"ChampionWarpTest"));
            var knowledge=tc4revival.research.KnowledgeSavedData.get(level.getServer());
            knowledge.ensure(player.getUUID(),player.getRandom()::nextInt);
            var warpKind=tc4revival.research.PlayerKnowledge.WarpKind.TEMPORARY;
            int before=knowledge.snapshot(player.getUUID()).orElseThrow().warp(warpKind);
            var warped=EntityType.ZOMBIE.create(level);ChampionMobs.make(warped,ChampionMobs.Kind.WARP,false);
            for(int sample=0;sample<64;sample++)ChampionMobs.hurt(new LivingHurtEvent(player,level.damageSources().mobAttack(warped),1));
            int gained=knowledge.snapshot(player.getUUID()).orElseThrow().warp(warpKind)-before;
            test.assertTrue(gained>0 && gained<192,"Warp strikes grant one to three temporary warp probabilistically");
            config.disableWarp.set(true);
            for(int sample=0;sample<64;sample++)ChampionMobs.hurt(new LivingHurtEvent(player,level.damageSources().mobAttack(warped),1));
            test.assertTrue(knowledge.snapshot(player.getUUID()).orElseThrow().warp(warpKind)==before+gained,"Disable-warp config blocks gain");
        } finally { config.championMobs.set(enabled);config.disableWarp.set(disableWarp);level.getServer().setDifficulty(oldDifficulty,true); }
        passed(test,"selection-inheritance-and-warp");
    }
    private static void passed(GameTestHelper test,String name) { tc4revival.TC4Revival.LOGGER.info("Champion server test PASS: {}",name);test.succeed(); }
}
