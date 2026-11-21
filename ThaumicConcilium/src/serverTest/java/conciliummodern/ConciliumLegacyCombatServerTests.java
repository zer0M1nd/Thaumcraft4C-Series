package conciliummodern;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;
import tc4revival.content.PrimalArrowEntity;

/** Final source-audit regressions, to run with the complete isolated server batch. */
@GameTestHolder("thaumicconcilium") @PrefixGameTestTemplate(false)
public final class ConciliumLegacyCombatServerTests {
    private static <T extends Entity> T place(GameTestHelper test,T entity,double x,double y,double z){entity.setPos(test.absoluteVec(new Vec3(x,y,z)));test.getLevel().addFreshEntity(entity);if(entity instanceof Mob mob)mob.setNoAi(true);return entity;}
    private static CompoundTag save(Entity entity){var tag=new CompoundTag();entity.saveWithoutId(tag);return tag;}
    private static ServerPlayer player(GameTestHelper test,double x,double y,double z){
        var p=new ServerPlayer(test.getLevel().getServer(),test.getLevel(),new GameProfile(UUID.randomUUID(),"ConciliumLegacy"));
        new ServerGamePacketListenerImpl(p.server,new Connection(PacketFlow.SERVERBOUND){@Override public void send(net.minecraft.network.protocol.Packet<?> packet){}},p);
        p.setGameMode(GameType.SURVIVAL);p.setPos(test.absoluteVec(new Vec3(x,y,z)));test.getLevel().addNewPlayer(p);return p;
    }
    private static Goal combat(CrimsonBrigade entity){return entity.goalSelector.getAvailableGoals().stream().filter(goal->goal.getPriority()==2).findFirst().orElseThrow().getGoal();}
    private static boolean shotBy(Entity shot,LivingEntity owner){return shot instanceof net.minecraft.world.entity.projectile.Projectile projectile&&projectile.getOwner()==owner||shot instanceof CrimsonOrb orb&&orb.owner()==owner;}
    private static long shots(GameTestHelper test,LivingEntity owner){return test.getLevel().getEntitiesOfClass(Entity.class,owner.getBoundingBox().inflate(40),shot->shotBy(shot,owner)).size();}
    private static void clearShots(GameTestHelper test,LivingEntity owner){test.getLevel().getEntitiesOfClass(Entity.class,owner.getBoundingBox().inflate(40),shot->shotBy(shot,owner)).forEach(Entity::discard);}

    @GameTest(template="empty") public static void archerFiresAcrossHeightDifferenceUsingHorizontalRange(GameTestHelper test){
        var archer=place(test,CrimsonContent.ARCHER.get().create(test.getLevel()),5.5,3,5.5);archer.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.BOW));
        var target=place(test,EntityType.COW.create(test.getLevel()),23.5,13,5.5);archer.setTarget(target);var goal=combat(archer);
        test.assertTrue(goal.canUse()&&goal.canContinueToUse(),"Target remains inside original 3D continuation radius");goal.start();
        for(int i=0;i<20;i++)goal.tick();
        var arrows=test.getLevel().getEntitiesOfClass(PrimalArrowEntity.class,archer.getBoundingBox().inflate(40),arrow->arrow.getOwner()==archer);
        test.assertTrue(arrows.size()==1,"18 horizontal / 10 vertical blocks still permits original 20-tick shot");
        test.assertTrue(archer.getDeltaMovement().x<0,"Original horizontal retreat threshold is preserved");clearShots(test,archer);archer.discard();target.discard();test.succeed();
    }
    @GameTest(template="empty") public static void emptyHandedArcherKeepsOriginalTwentyTickMelee(GameTestHelper test){
        var archer=place(test,CrimsonContent.ARCHER.get().create(test.getLevel()),5.5,3,5.5);archer.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
        var target=place(test,EntityType.COW.create(test.getLevel()),7,3,5.5);target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);target.setHealth(100);archer.setTarget(target);var goal=combat(archer);
        test.assertTrue(goal.canUse(),"Losing the bow does not disable the original combat goal");goal.start();goal.tick();float first=target.getHealth();
        test.assertTrue(first<100&&Math.abs(archer.getDeltaMovement().x-.25)<.00001,"Unarmed archer attacks and approaches at original speed");
        target.invulnerableTime=0;for(int i=0;i<19;i++)goal.tick();test.assertTrue(target.getHealth()==first,"Melee cannot attack before twenty ticks");
        goal.tick();test.assertTrue(target.getHealth()<first,"Twentieth tick delivers the next actual melee attack");archer.discard();target.discard();test.succeed();
    }
    @GameTest(template="empty") public static void archerNeedsSightToAcquireThaumaturgeButRangerDoesNot(GameTestHelper test){
        var archer=place(test,CrimsonContent.ARCHER.get().create(test.getLevel()),5.5,3,5.5);var ranger=place(test,CrimsonContent.RANGER.get().create(test.getLevel()),5.5,3,6.5);
        var paladin=place(test,CrimsonContent.PALADIN.get().create(test.getLevel()),5.5,3,7.5);
        var mage=place(test,ConciliumEntities.THAUMATURGE.get().create(test.getLevel()),16.5,3,5.5);
        try{
        for(int y=1;y<=9;y++)for(int z=2;z<=10;z++)test.setBlock(10,y,z,net.minecraft.world.level.block.Blocks.STONE);
        var archerTarget=archer.targetSelector.getAvailableGoals().stream().filter(goal->goal.getPriority()==3).findFirst().orElseThrow().getGoal();
        var rangerTarget=ranger.targetSelector.getAvailableGoals().stream().filter(goal->goal.getPriority()==3).findFirst().orElseThrow().getGoal();
        var paladinTarget=paladin.targetSelector.getAvailableGoals().stream().filter(goal->goal.getPriority()==3).findFirst().orElseThrow().getGoal();
        test.assertTrue(!archerTarget.canUse(),"Archer does not acquire ordinary thaumaturge through a wall");
        test.assertTrue(rangerTarget.canUse(),"Ranger retains original acquisition without a line-of-sight requirement");
        test.assertTrue(paladinTarget.canUse(),"Paladin also retains original acquisition without a line-of-sight requirement");test.succeed();
        }finally{archer.discard();ranger.discard();paladin.discard();mage.discard();}
    }
    @GameTest(template="empty") public static void paladinUsesDistanceDelayAndNeverShootsBeyondTwentyFour(GameTestHelper test){
        var paladin=place(test,CrimsonContent.PALADIN.get().create(test.getLevel()),3.5,3,5.5);var target=player(test,15.5,3,5.5);paladin.setTarget(target);var goal=combat(paladin);
        test.assertTrue(goal.canUse(),"Twelve-block player is eligible");goal.start();goal.tick();test.assertTrue(shots(test,paladin)==0,"Initial sight does not cause an instant shot");
        for(int i=0;i<29;i++)goal.tick();test.assertTrue(shots(test,paladin)==0,"Twelve-block range retains the thirty-tick windup");
        goal.tick();test.assertTrue(shots(test,paladin)==1,"Distance-interpolated cooldown fires exactly once");clearShots(test,paladin);goal.stop();
        target.setPos(test.absoluteVec(new Vec3(28.5,3,5.5)));test.assertTrue(goal.canUse(),"Distant target remains eligible for pathfinding");goal.start();
        for(int i=0;i<100;i++)goal.tick();test.assertTrue(shots(test,paladin)==0,"Twenty-five-block target cannot be fired upon");paladin.discard();target.discard();test.succeed();
    }
    @GameTest(template="empty") public static void pontifexSpellRetaliationIsIndependentAndSurvivesSave(GameTestHelper test){
        var boss=place(test,CrimsonContent.PONTIFEX.get().create(test.getLevel()),8.5,3,8.5);var melee=place(test,EntityType.COW.create(test.getLevel()),9.5,3,8.5);var attacker=player(test,16.5,3,8.5);
        boss.hurt(test.getLevel().damageSources().playerAttack(attacker),3);boss.setTarget(melee);boss.customServerAiStep();
        var tag=save(boss);test.assertTrue(boss.getTarget()==melee&&tag.hasUUID("SpellTarget")&&tag.getUUID("SpellTarget").equals(attacker.getUUID()),"Melee target does not overwrite retaliating spell target");
        test.assertTrue(tag.getInt("SpellAggroCooldown")>=198&&tag.getInt("SpellAggroCooldown")<=199,"Original 200-tick retaliation window starts independently");
        var copy=CrimsonContent.PONTIFEX.get().create(test.getLevel());copy.load(tag);copy.customServerAiStep();test.assertTrue(save(copy).getUUID("SpellTarget").equals(attacker.getUUID()),"Serialized UUID resolves the same real player");boss.discard();melee.discard();attacker.discard();test.succeed();
    }
    @GameTest(template="empty") public static void playerShacklesAffectCultistsWhileCultistFriendlyFireIsFiltered(GameTestHelper test){
        var caster=player(test,5.5,3,5.5);var victim=place(test,CrimsonContent.PALADIN.get().create(test.getLevel()),10.5,3,5.5);
        var playerShot=new EtherealShackles(caster);test.getLevel().addFreshEntity(playerShot);test.assertTrue(playerShot.canHitEntity(victim),"Player spell can collide with hostile cultist");playerShot.onHit(new EntityHitResult(victim));
        var effect=victim.getEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);test.assertTrue(playerShot.isRemoved()&&effect!=null&&effect.getDuration()==20&&effect.getAmplifier()==4,"Actual player impact applies original mob shackles effect");
        var cultist=place(test,CrimsonContent.PALADIN.get().create(test.getLevel()),6.5,3,5.5);var friendlyShot=new EtherealShackles(cultist);test.assertTrue(!friendlyShot.canHitEntity(victim),"Cultist-owned projectile still excludes cultist allies");caster.discard();victim.discard();cultist.discard();test.succeed();
    }
    @GameTest(template="empty") public static void coatedDroppedItemNeverIgnitesAndRetainsItsIdentity(GameTestHelper test){
        var coated=new ItemStack(Items.STICK);coated.getOrCreateTag().putBoolean(NetherMembraneRecipe.TAG,true);
        var item=place(test,new ItemEntity(test.getLevel(),0,0,0,coated),5.5,3,5.5);UUID identity=item.getUUID();item.setSecondsOnFire(15);item.lavaHurt();
        test.assertTrue(item.fireImmune()&&!item.isOnFire()&&!item.displayFireAnimation()&&item.getRemainingFireTicks()<=0,"Membrane prevents direct and lava ignition, including render fire state");
        test.assertTrue(!item.hurt(test.getLevel().damageSources().inFire(),100)&&!item.isRemoved()&&item.getUUID().equals(identity),"Fire damage is rejected without replacing the item entity");
        var ordinary=place(test,new ItemEntity(test.getLevel(),0,0,0,new ItemStack(Items.STICK)),8.5,3,5.5);ordinary.setSecondsOnFire(15);
        test.assertTrue(!ordinary.fireImmune()&&ordinary.isOnFire(),"Uncoated dropped items retain vanilla ignition");item.discard();ordinary.discard();test.succeed();
    }
    @GameTest(template="empty") public static void pontifexRobeRefreshesChainOnEveryArmorTick(GameTestHelper test){
        var wearer=player(test,5.5,3,5.5);wearer.setItemSlot(EquipmentSlot.HEAD,new ItemStack(CrimsonContent.HELMET.get()));wearer.setItemSlot(EquipmentSlot.CHEST,new ItemStack(CrimsonContent.CHEST.get()));wearer.setItemSlot(EquipmentSlot.LEGS,new ItemStack(CrimsonContent.LEGS.get()));wearer.setItemSlot(EquipmentSlot.FEET,new ItemStack(CrimsonContent.BOOTS.get()));
        EtherealState.toggleRobe(wearer);wearer.removeEffect(CrimsonControl.CHAINED.get());wearer.tickCount=7;CrimsonContent.CHEST.get().onArmorTick(wearer.getItemBySlot(EquipmentSlot.CHEST),test.getLevel(),wearer);
        test.assertTrue(wearer.getEffect(CrimsonControl.CHAINED.get())!=null&&wearer.getEffect(CrimsonControl.CHAINED.get()).getDuration()==100,"Full toggled robe refreshes outside twenty-tick boundaries");
        wearer.discard();test.succeed();
    }
    @GameTest(template="empty") public static void allThreeBrigadesRetainInheritedCultistRareDrop(GameTestHelper test){
        var killer=player(test,4.5,3,4.5);var sword=new ItemStack(Items.DIAMOND_SWORD);sword.enchant(net.minecraft.world.item.enchantment.Enchantments.MOB_LOOTING,100);killer.setItemSlot(EquipmentSlot.MAINHAND,sword);
        try{
        for(var type:java.util.List.of(CrimsonContent.ARCHER.get(),CrimsonContent.RANGER.get(),CrimsonContent.PALADIN.get())){
            var victim=place(test,type.create(test.getLevel()),8.5,3,4.5);var drops=new java.util.ArrayList<ItemEntity>();
            // Forge owns captureDrops during death; observe the emitted event instead of a replaced external list.
            java.util.function.Consumer<net.minecraftforge.event.entity.living.LivingDropsEvent> capture=event->{if(event.getEntity()==victim)drops.addAll(event.getDrops());};
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(net.minecraftforge.eventbus.api.EventPriority.LOWEST,false,net.minecraftforge.event.entity.living.LivingDropsEvent.class,capture);
            try{
                test.assertTrue(victim.hurt(test.getLevel().damageSources().playerAttack(killer),1000)&&victim.isDeadOrDying(),"Actual player hit kills the brigade");
                test.assertTrue(drops.stream().anyMatch(drop->drop.getItem().is(tc4revival.content.TCEldritch.CRIMSON_RITES.get())),"Guaranteed-looting probe reaches the inherited cultist rare-drop path for "+type+"; drops="+drops.stream().map(drop->drop.getItem().toString()).toList());
            }finally{net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(capture);drops.forEach(Entity::discard);victim.discard();}
        }
        test.succeed();
        }finally{killer.discard();}
    }
}
