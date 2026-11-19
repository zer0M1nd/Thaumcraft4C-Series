package conciliummodern;

import com.mojang.authlib.GameProfile;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.gametest.*;
import tc4revival.content.*;

/** Real server entities and ticks; this class makes no client/network/visual claims. */
@GameTestHolder("thaumicconcilium")
@PrefixGameTestTemplate(false)
public final class RemainingMobsServerTests {
    private static void floor(GameTestHelper t){for(int x=1;x<30;x++)for(int z=1;z<30;z++)t.setBlock(new BlockPos(x,0,z),Blocks.STONE);}
    private static <T extends Entity> T spawn(GameTestHelper t,EntityType<T> type,int x,int y,int z){T e=Objects.requireNonNull(type.create(t.getLevel()));e.moveTo(t.absolutePos(new BlockPos(x,y,z)).getCenter());if(e instanceof Mob mob)mob.setPersistenceRequired();t.assertTrue(t.getLevel().addFreshEntity(e),"Entity must enter the server level");return e;}
    private static AABB area(GameTestHelper t){return new AABB(t.absolutePos(new BlockPos(1,-1,1)),t.absolutePos(new BlockPos(30,15,30)));}
    private static CompoundTag saved(Entity e){var tag=new CompoundTag();e.saveWithoutId(tag);return tag;}
    private static void near(GameTestHelper t,double wanted,double actual,String label){t.assertTrue(Math.abs(wanted-actual)<.002,label+": expected "+wanted+", got "+actual);}
    static String state(Entity entity){return entity.getType()+" uuid="+entity.getUUID()+" pos="+entity.position()+" tick="+entity.tickCount+" removed="+entity.isRemoved()+" reason="+entity.getRemovalReason()+(entity instanceof LivingEntity living?" health="+living.getHealth()+" deathTime="+living.deathTime:"")+(entity instanceof Mob mob?" noAi="+mob.isNoAi()+" target="+(mob.getTarget()==null?"null":mob.getTarget().getUUID()):"");}

    @GameTest(template="empty",timeoutTicks=40)
    public static void dissolvedRejectsOrdinaryDamageAndAttacksThroughVoid(GameTestHelper t){
        floor(t);var mob=spawn(t,RemainingMobs.DISSOLVED.get(),5,1,5);mob.setNoAi(true);var target=spawn(t,EntityType.COW,10,1,5);target.setNoAi(true);
        float health=mob.getHealth();for(var source:List.of(t.getLevel().damageSources().generic(),t.getLevel().damageSources().magic(),t.getLevel().damageSources().onFire(),t.getLevel().damageSources().mobAttack(target))){mob.invulnerableTime=0;t.assertTrue(!mob.hurt(source,12),"Dissolved must reject ordinary damage "+source.getMsgId());near(t,health,mob.getHealth(),"Rejected damage changes no health");}
        mob.invulnerableTime=0;t.assertTrue(mob.hurt(t.getLevel().damageSources().inWall(),3),"In-wall exception remains damageable");near(t,health-3,mob.getHealth(),"In-wall damage");
        mob.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(7);float before=target.getHealth();t.assertTrue(mob.doHurtTarget(target),"Void attack lands");near(t,before-7,target.getHealth(),"Attack uses current attack attribute");mob.invulnerableTime=0;t.assertTrue(mob.hurt(t.getLevel().damageSources().fellOutOfWorld(),2),"Void exception remains damageable");t.succeed();
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void dissolvedPortableHoleFocusInteraction(GameTestHelper t){
        floor(t);var mob=spawn(t,RemainingMobs.DISSOLVED.get(),5,1,5);mob.setNoAi(true);var player=t.makeMockPlayer();var wand=new ItemStack(TCItems.CASTING_WAND.get());player.setItemInHand(InteractionHand.MAIN_HAND,wand);mob.interact(player,InteractionHand.MAIN_HAND);t.assertTrue(mob.isAlive(),"Empty wand cannot drive away Dissolved");
        WandFocusItem.attach(wand,new ItemStack(TCItems.SHOCK_FOCUS.get()));mob.interact(player,InteractionHand.MAIN_HAND);t.assertTrue(mob.isAlive(),"Other focus cannot drive away Dissolved");WandFocusItem.attach(wand,new ItemStack(TCItems.PORTABLE_HOLE_FOCUS.get()));mob.interact(player,InteractionHand.MAIN_HAND);t.assertTrue(mob.isDeadOrDying(),"Actual portable-hole wand interaction drives away Dissolved");t.succeed();
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void samuraiTypesDamageReductionAndSaveReload(GameTestHelper t){
        floor(t);for(int type=0;type<3;type++){var mob=spawn(t,RemainingMobs.SAMURAI.get(),4+type*8,1,5);mob.setNoAi(true);mob.setSamuraiType(type);float before=mob.getHealth();t.assertTrue(mob.hurt(t.getLevel().damageSources().generic(),35),"Damage accepted");near(t,before-35/(type==0?3.5:type==1?5:10),mob.getHealth(),"Single original damage divisor for type "+type);var target=spawn(t,EntityType.IRON_GOLEM,4+type*8,1,8);target.setNoAi(true);before=target.getHealth();mob.doHurtTarget(target);near(t,before-new int[]{17,20,25}[type],target.getHealth(),"Original melee damage for type "+type);var tag=saved(mob);var restored=RemainingMobs.SAMURAI.get().create(t.getLevel());restored.load(tag);t.assertTrue(restored.samuraiType()==type,"Samurai type survives actual entity NBT reload");near(t,mob.getHealth(),restored.getHealth(),"Health survives reload");t.assertTrue(mob.isAlliedTo(restored),"Samurai recognize own kind");}t.succeed();
    }
    @GameTest(template="empty",timeoutTicks=80)
    public static void overanimatedDeathProducesSixUniquePiecesWithoutParentLoot(GameTestHelper t){
        floor(t);var fixture=new PieceFixture(area(t),false);MinecraftForge.EVENT_BUS.register(fixture);var mob=spawn(t,RemainingMobs.OVERANIMATED.get(),16,1,16);mob.setNoAi(true);
        try{t.assertTrue(mob.hurt(t.getLevel().damageSources().fellOutOfWorld(),1000)&&mob.isDeadOrDying(),"Real damage starts parent death: "+state(mob));}catch(RuntimeException|Error error){MinecraftForge.EVENT_BUS.unregister(fixture);mob.discard();throw error;}
        t.runAtTickTime(5,()->{try{var pieces=t.getLevel().getEntitiesOfClass(ThaumGib.class,area(t));t.assertTrue(saved(mob).getInt("SpawnedPieces")==63,"Parent commits all six real insertions before removal: "+state(mob)+" joined="+fixture.joined.size());t.assertTrue(mob.isRemoved(),"Parent completes death: "+state(mob));t.assertTrue(pieces.size()==6,"Six real inserted gib entities, got "+pieces.size()+"; "+fixture.joined.stream().map(RemainingMobsServerTests::state).toList());t.assertTrue(pieces.stream().map(ThaumGib::gibType).distinct().count()==6,"All six distinct gib types");t.assertTrue(t.getLevel().getEntitiesOfClass(ItemEntity.class,area(t)).isEmpty(),"No parent item drops");t.assertTrue(t.getLevel().getEntitiesOfClass(ExperienceOrb.class,area(t)).isEmpty(),"No parent experience");t.succeed();}finally{MinecraftForge.EVENT_BUS.unregister(fixture);fixture.joined.forEach(Entity::discard);mob.discard();}});
    }
    public static final class PieceFixture {
        final AABB region;final boolean rejectOne;final List<ThaumGib> joined=new ArrayList<>();boolean rejected;
        PieceFixture(AABB region,boolean rejectOne){this.region=region;this.rejectOne=rejectOne;}
        @SubscribeEvent public void joined(EntityJoinLevelEvent e){if(e.getEntity() instanceof ThaumGib gib&&region.contains(gib.position())){joined.add(gib);gib.setPersistenceRequired();if(rejectOne){gib.setNoAi(true);gib.setNoGravity(true);gib.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);if(gib.gibType()==3&&!rejected){rejected=true;e.setCanceled(true);}}}}
    }
    @GameTest(template="empty",timeoutTicks=90)
    public static void partialSplitSurvivesReloadWithoutDuplicatingAcceptedPieces(GameTestHelper t){
        floor(t);var reject=new PieceFixture(area(t),true);MinecraftForge.EVENT_BUS.register(reject);var mob=spawn(t,RemainingMobs.OVERANIMATED.get(),16,1,16);var reloaded=new Overanimated[1];mob.setNoAi(true);
        try{t.assertTrue(mob.hurt(t.getLevel().damageSources().fellOutOfWorld(),1000)&&mob.isDeadOrDying(),"Real damage starts parent death: "+state(mob));}catch(RuntimeException|Error error){MinecraftForge.EVENT_BUS.unregister(reject);mob.discard();throw error;}
        t.runAtTickTime(3,()->{try{t.assertTrue(reject.rejected,"Fixture rejected one real entity insertion: "+state(mob)+" joined="+reject.joined.size());var tag=saved(mob);t.assertTrue(Integer.bitCount(tag.getInt("SpawnedPieces"))==5,"Only five accepted pieces are committed: "+tag.getInt("SpawnedPieces"));mob.discard();var restored=RemainingMobs.OVERANIMATED.get().create(t.getLevel());reloaded[0]=restored;restored.load(tag);t.assertTrue(t.getLevel().addFreshEntity(restored),"Dying parent reloads");}catch(RuntimeException|Error error){MinecraftForge.EVENT_BUS.unregister(reject);reject.joined.forEach(Entity::discard);mob.discard();if(reloaded[0]!=null)reloaded[0].discard();throw error;}});
        t.runAtTickTime(30,()->{try{var pieces=t.getLevel().getEntitiesOfClass(ThaumGib.class,area(t));t.assertTrue(reloaded[0]!=null&&saved(reloaded[0]).getInt("SpawnedPieces")==63,"Reloaded parent commits every piece: "+(reloaded[0]==null?"not reloaded":state(reloaded[0])));t.assertTrue(pieces.size()==6,"Retry after NBT reload does not duplicate accepted pieces: "+pieces.size()+"; "+reject.joined.stream().map(RemainingMobsServerTests::state).toList());t.assertTrue(pieces.stream().map(ThaumGib::gibType).distinct().count()==6,"Missing piece is retried");t.assertTrue(t.getLevel().getEntitiesOfClass(Overanimated.class,area(t)).isEmpty(),"Completed parent removed");t.succeed();}finally{MinecraftForge.EVENT_BUS.unregister(reject);reject.joined.forEach(Entity::discard);mob.discard();if(reloaded[0]!=null)reloaded[0].discard();}});
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void gibTypesPersistAndClampMalformedSavedValues(GameTestHelper t){
        for(int type=0;type<6;type++){var gib=RemainingMobs.THAUM_GIB.get().create(t.getLevel());gib.setGibType(type);var tag=saved(gib);var restored=RemainingMobs.THAUM_GIB.get().create(t.getLevel());restored.load(tag);t.assertTrue(restored.gibType()==type,"Gib type "+type+" survives entity reload");tag.putInt("Type",Integer.MIN_VALUE);restored.load(tag);t.assertTrue(restored.gibType()==0,"Negative type is clamped");tag.putInt("Type",Integer.MAX_VALUE);restored.load(tag);t.assertTrue(restored.gibType()==5,"Huge type is clamped");}t.succeed();
    }
    @GameTest(template="empty",timeoutTicks=240)
    public static void gibKilledByLootingPlayerDropsRealThaumcraftMeatNuggets(GameTestHelper t){
        floor(t);var positions=java.util.stream.IntStream.range(0,9).mapToObj(i->new BlockPos(4+i*2,1,12)).toList();
        ConciliumTestWorld.ready(t,positions,()->{
            var player=FakePlayerFactory.get(t.getLevel(),new GameProfile(UUID.randomUUID(),"GibLootTest"));var sword=new ItemStack(Items.DIAMOND_SWORD);sword.enchant(Enchantments.MOB_LOOTING,3);player.setItemInHand(InteractionHand.MAIN_HAND,sword);
            var victims=new ArrayList<ThaumGib>();var drops=new ArrayList<ItemEntity>();var deathEvents=new HashSet<UUID>();
            java.util.function.Consumer<net.minecraftforge.event.entity.living.LivingDropsEvent> capture=event->{if(victims.contains(event.getEntity())){deathEvents.add(event.getEntity().getUUID());drops.addAll(event.getDrops());}};
            MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST,false,net.minecraftforge.event.entity.living.LivingDropsEvent.class,capture);
            try{
                for(var pos:positions){var gib=spawn(t,RemainingMobs.THAUM_GIB.get(),pos.getX(),pos.getY(),pos.getZ());gib.setNoAi(true);victims.add(gib);t.assertTrue(gib.hurt(t.getLevel().damageSources().playerAttack(player),1000)&&gib.isDeadOrDying(),"Actual player hit kills each gib");}
                t.assertTrue(deathEvents.size()==9,"Nine distinct real deaths emit drops: "+deathEvents.size());
            }catch(RuntimeException|Error error){victims.forEach(Entity::discard);drops.forEach(Entity::discard);player.discard();throw error;}
            finally{MinecraftForge.EVENT_BUS.unregister(capture);}
            t.runAfterDelay(2,()->{try{
                t.assertTrue(drops.size()==9&&drops.stream().allMatch(e->!e.isRemoved()&&t.getLevel().getEntity(e.getUUID())==e),"All nine observed drop entities are actually inserted and visible: "+drops.stream().map(RemainingMobsServerTests::state).toList());
                int count=drops.stream().mapToInt(e->e.getItem().getCount()).sum();t.assertTrue(count>=27&&count<=36,"Looting applies to all nine genuine deaths: "+count);var valid=Set.of("thaumcraft:beef_nugget","thaumcraft:chicken_nugget","thaumcraft:pork_nugget");t.assertTrue(drops.stream().allMatch(e->valid.contains(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(e.getItem().getItem()).toString())),"Drops are actual registered core meat nuggets");t.succeed();
            }finally{victims.forEach(Entity::discard);drops.forEach(Entity::discard);player.discard();}});
        });
    }
    private static BlockPos column(GameTestHelper t){var top=t.absolutePos(new BlockPos(16,10,16));for(int dy=0;dy<3;dy++)for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)t.getLevel().setBlock(top.offset(x,-dy,z),Blocks.STONE.defaultBlockState(),3);t.getLevel().setBlock(top.below(3),Blocks.BEDROCK.defaultBlockState(),3);return top;}
    @GameTest(template="empty",timeoutTicks=210)
    public static void upcomingHoleWarnsOpensAndRestoresWithoutDestroyingInventoryBlocks(GameTestHelper t){
        var top=column(t);var chest=top.east();t.getLevel().setBlock(chest,Blocks.CHEST.defaultBlockState(),3);((net.minecraft.world.level.block.entity.ChestBlockEntity)t.getLevel().getBlockEntity(chest)).setItem(0,new ItemStack(Items.DIAMOND,7));var hole=RemainingMobs.UPCOMING_HOLE.get().create(t.getLevel());hole.moveTo(top.above().getCenter());t.getLevel().addFreshEntity(hole);
        t.runAtTickTime(25,()->t.assertTrue(t.getLevel().getBlockState(top).is(Blocks.STONE),"Warning interval does not alter terrain"));
        t.runAtTickTime(35,()->{t.assertTrue(t.getLevel().getBlockState(top).is(TCArcane.PORTABLE_HOLE.get()),"Center opened through real PortableHole API");t.assertTrue(t.getLevel().getBlockState(top.west()).is(TCArcane.PORTABLE_HOLE.get()),"Side opened");t.assertTrue(t.getLevel().getBlockState(chest).is(Blocks.CHEST),"Inventory block never displaced");t.assertTrue(hole.isRemoved(),"Front stops at bedrock");});
        t.runAtTickTime(180,()->{for(int dy=0;dy<3;dy++)t.assertTrue(t.getLevel().getBlockState(top.below(dy)).is(Blocks.STONE),"Layer "+dy+" restored");t.assertTrue(((net.minecraft.world.level.block.entity.ChestBlockEntity)t.getLevel().getBlockEntity(chest)).getItem(0).getCount()==7,"Inventory preserved");t.succeed();});
    }
    public static final class DenyHole {
        final BlockPos top;DenyHole(BlockPos top){this.top=top;}
        @SubscribeEvent public void breaking(BlockEvent.BreakEvent event){if(event.getPos().equals(top))event.setCanceled(true);}
    }
    @GameTest(template="empty",timeoutTicks=80)
    public static void upcomingHoleHonorsActualForgeBreakDenial(GameTestHelper t){
        var top=column(t);var deny=new DenyHole(top);MinecraftForge.EVENT_BUS.register(deny);t.runAtTickTime(70,()->MinecraftForge.EVENT_BUS.unregister(deny));var hole=RemainingMobs.UPCOMING_HOLE.get().create(t.getLevel());hole.moveTo(top.above().getCenter());t.getLevel().addFreshEntity(hole);
        t.runAtTickTime(40,()->{MinecraftForge.EVENT_BUS.unregister(deny);t.assertTrue(hole.isRemoved(),"Denied opening terminates front");for(int dy=0;dy<3;dy++)for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)t.assertTrue(t.getLevel().getBlockState(top.offset(x,-dy,z)).is(Blocks.STONE),"No block changed through protection denial");t.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=60)
    public static void upcomingHoleResumesSavedFrontWithFiniteRemainingLayers(GameTestHelper t){
        var top=column(t);var hole=RemainingMobs.UPCOMING_HOLE.get().create(t.getLevel());hole.moveTo(top.above().getCenter());var tag=saved(hole);tag.putInt("Age",28);tag.putInt("Remaining",2);tag.putLong("Front",top.asLong());hole.load(tag);t.getLevel().addFreshEntity(hole);
        t.runAtTickTime(8,()->{t.assertTrue(hole.isRemoved(),"Finite resumed front terminates");t.assertTrue(t.getLevel().getBlockState(top).is(TCArcane.PORTABLE_HOLE.get()),"Resumed first layer opens");t.assertTrue(t.getLevel().getBlockState(top.below()).is(TCArcane.PORTABLE_HOLE.get()),"Final center opens");t.assertTrue(t.getLevel().getBlockState(top.below().east()).is(Blocks.STONE),"Final layer omits side holes");t.assertTrue(t.getLevel().getBlockState(top.below(2)).is(Blocks.STONE),"Saved remaining count is honored");t.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=60)
    public static void squidSittingKeepsGlobalSmokePhaseAndSavedOwnership(GameTestHelper t){
        floor(t);var squid=spawn(t,RemainingMobs.DOPE_SQUID.get(),16,1,16);var player=t.makeMockPlayer();squid.tame(player);squid.setOrderedToSit(true);squid.setInSittingPose(true);var tag=saved(squid);tag.putInt("SmokeClock",398);squid.load(tag);
        t.runAtTickTime(8,()->{try{var after=saved(squid);t.assertTrue(after.getInt("SmokeClock")<30,"Smoke phase wraps while sitting rather than pausing");t.assertTrue(!squid.getPersistentData().contains("SmokeTicks"),"Sitting suppresses smoke emission");var restored=RemainingMobs.DOPE_SQUID.get().create(t.getLevel());restored.load(after);t.assertTrue(player.getUUID().equals(restored.getOwnerUUID())&&restored.isTame(),"Owner UUID and tame state survive NBT reload without an online owner");t.assertTrue(restored.isOrderedToSit()&&restored.isInSittingPose(),"Sitting order and pose survive NBT reload");t.assertTrue(restored.tobacco().isEmpty(),"Original unselected tobacco survives reload");t.succeed();}finally{squid.discard();}});
    }
    public static final class SpitImpact {
        final ChortSpit projectile;Entity hit,owner;String result="no impact";SpitImpact(ChortSpit projectile){this.projectile=projectile;}
        @SubscribeEvent(priority=EventPriority.LOWEST,receiveCanceled=true) public void impact(ProjectileImpactEvent event){if(event.getProjectile()==projectile){owner=projectile.getOwner();var ray=event.getRayTraceResult();hit=ray instanceof EntityHitResult entityHit?entityHit.getEntity():null;result=ray.getType()+" at="+ray.getLocation()+" hit="+(hit==null?"none":state(hit))+" canceled="+event.isCanceled()+" result="+event.getImpactResult();}}
    }
    @GameTest(template="empty",timeoutTicks=60)
    public static void chortSpitReallyCollidesAndUsesOwnerDamage(GameTestHelper t){
        floor(t);var owner=spawn(t,RemainingMobs.CHORT.get(),4,1,10);owner.setNoAi(true);owner.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(8);var target=spawn(t,EntityType.COW,12,1,10);target.setNoAi(true);var projectile=RemainingMobs.CHORT_SPIT.get().create(t.getLevel());projectile.setOwner(owner);projectile.moveTo(owner.getX()+1,owner.getY()+.7,owner.getZ());projectile.shoot(1,0,0,1.8f,0);var impact=new SpitImpact(projectile);MinecraftForge.EVENT_BUS.register(impact);t.getLevel().addFreshEntity(projectile);
        t.runAtTickTime(12,()->{try{t.assertTrue(!owner.isRemoved(),"Fixture owner remains present through collision: "+state(owner));t.assertTrue(impact.hit==target,"Actual spit impact reaches the intended cow: "+impact.result+" projectile="+state(projectile));t.assertTrue(impact.owner==owner,"Impact resolves the real damage owner: "+impact.result);t.assertTrue(projectile.isRemoved(),"Spit removed after real collision: "+state(projectile));near(t,2,target.getHealth(),"Cow takes owner's eight damage through thrown source; "+impact.result);t.succeed();}finally{MinecraftForge.EVENT_BUS.unregister(impact);projectile.discard();owner.discard();target.discard();}});
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void impDealEventPersistsAndTicksOnlyAtEndPhase(GameTestHelper t){
        var player=FakePlayerFactory.get(t.getLevel(),new GameProfile(UUID.randomUUID(),"ImpPactTest"));var event=new com.ilya3point999k.thaumicconcilium.api.event.WitcheryImpDealEvent(player,ItemStack.EMPTY,null);MinecraftForge.EVENT_BUS.post(event);t.assertTrue(ImpPacts.remaining(player)==72000,"Actual public deal event grants original duration");
        MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.START,player));t.assertTrue(ImpPacts.remaining(player)==72000,"START does not double-decrement");MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,player));t.assertTrue(ImpPacts.remaining(player)==71999,"END decrements once");var clone=FakePlayerFactory.get(t.getLevel(),new GameProfile(UUID.randomUUID(),"ImpPactClone"));MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.player.PlayerEvent.Clone(clone,player,true));t.assertTrue(ImpPacts.remaining(clone)==71999,"Death clone preserves remaining pact");t.succeed();
    }
}
