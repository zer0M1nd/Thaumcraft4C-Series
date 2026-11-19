package conciliummodern;

import com.mojang.authlib.GameProfile;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.research.KnowledgeSavedData;
import tc4revival.research.PlayerKnowledge.WarpKind;

/** Generate only real installed-host cases; absent paths are explicitly reported as not run. */
@GameTestHolder("thaumicconcilium") @PrefixGameTestTemplate(false)
public final class RemainingMobsOptionalServerTests {
    @GameTestGenerator public static Collection<TestFunction> installed(){
        var tests=new ArrayList<TestFunction>();
        if(RemainingMobs.available("dope_squid")){tests.add(test("dope_squid_actual_tobacco_owner_interaction",80,RemainingMobsOptionalServerTests::tobaccoInteraction));tests.add(test("dope_squid_actual_tobacco_warp_effect",80,RemainingMobsOptionalServerTests::tobaccoEffect));}else missing("dope_squid_positive","Horizons + Bases tobacco feature");
        if(RemainingMobs.available("withered_botanist")){tests.add(test("withered_botanist_actual_flowers",80,RemainingMobsOptionalServerTests::flowers));tests.add(test("withered_botanist_place_denial",80,RemainingMobsOptionalServerTests::flowersDenied));}else missing("withered_botanist_positive","Botania");
        if(WitcheryBridge.available()){tests.add(test("burned_witch_real_host_projectile",100,RemainingMobsOptionalServerTests::burnedWitchProjectile));tests.addAll(DoomCatWitcheryServerTests.tests());}else{missing("burned_witch_positive","registered real Witchery host");tests.add(test("witchery_absent_saved_mobs_dormant",240,RemainingMobsOptionalServerTests::dormant));}
        if(WitcheryBridge.alchemistAvailable())tests.add(test("sloppy_alchemist_actual_mount_free_reward",100,RemainingMobsOptionalServerTests::alchemist));else missing("sloppy_alchemist_chort_positive","registered real Witchery host + Blood Magic");
        return tests;
    }
    private static TestFunction test(String name,int ticks,Consumer<GameTestHelper> run){return new TestFunction("concilium_optional/"+name,"thaumicconcilium."+name,"thaumicconcilium:empty",ticks,0,true,run);}
    private static void missing(String branch,String dependency){com.mojang.logging.LogUtils.getLogger().info("CONCILIUM_TEST_NOT_RUN branch={} requires={}",branch,dependency);}
    private static ItemStack item(String id,int count){return new ItemStack(Objects.requireNonNull(ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(id))),count);}
    private static <T extends Entity>T spawn(GameTestHelper t,EntityType<T> type,int x,int y,int z){var mob=Objects.requireNonNull(type.create(t.getLevel()));mob.moveTo(t.absolutePos(new BlockPos(x,y,z)).getCenter());if(mob instanceof Mob living)living.setPersistenceRequired();t.assertTrue(t.getLevel().addFreshEntity(mob),"Entity inserted into server");return mob;}
    private static CompoundTag save(Entity mob){var tag=new CompoundTag();mob.saveWithoutId(tag);return tag;}
    private static void grass(GameTestHelper t){for(int x=5;x<=25;x++)for(int z=5;z<=25;z++)t.setBlock(new BlockPos(x,0,z),Blocks.GRASS_BLOCK);}
    private static long flowers(GameTestHelper t,BlockPos center){return BlockPos.betweenClosedStream(center.offset(-3,-1,-3),center.offset(3,3,3)).filter(p->{var key=ForgeRegistries.BLOCKS.getKey(t.getLevel().getBlockState(p).getBlock());return key!=null&&key.getNamespace().equals("botania")&&key.getPath().endsWith("_mystical_flower");}).count();}

    static void tobaccoInteraction(GameTestHelper t){
        grass(t);var level=t.getLevel();var squid=spawn(t,RemainingMobs.DOPE_SQUID.get(),16,1,16);var a=FakePlayerFactory.get(level,new GameProfile(UUID.randomUUID(),"SquidOwner"));var b=FakePlayerFactory.get(level,new GameProfile(UUID.randomUUID(),"SquidOther"));
        a.moveTo(squid.position().add(1,0,0));b.moveTo(squid.position().add(-1,0,0));level.addNewPlayer(a);level.addNewPlayer(b);
        try{t.assertTrue(level.getPlayerByUUID(a.getUUID())==a&&level.getPlayerByUUID(b.getUUID())==b,"Owner interaction fixtures are actual world players");a.getAbilities().instabuild=false;a.setItemInHand(InteractionHand.MAIN_HAND,item("thaumicbases:tobacco_leaves",3));squid.interact(a,InteractionHand.MAIN_HAND);
            t.assertTrue(squid.isOwnedBy(a)&&squid.isOrderedToSit(),"Real leaves tame and sit");t.assertTrue(a.getMainHandItem().getCount()==2,"One leaf consumed");squid.setHealth(20);squid.interact(a,InteractionHand.MAIN_HAND);t.assertTrue(squid.getHealth()==25&&a.getMainHandItem().getCount()==1,"Owner leaves heal five and consume one");
            a.setItemInHand(InteractionHand.MAIN_HAND,item("thaumicbases:tobacco_fighting",4));squid.interact(a,InteractionHand.MAIN_HAND);t.assertTrue(squid.tobacco().is(a.getMainHandItem().getItem())&&a.getMainHandItem().getCount()==4,"Selecting actual Tobacco does not consume it");b.setItemInHand(InteractionHand.MAIN_HAND,item("thaumicbases:tobacco_mining",3));squid.interact(b,InteractionHand.MAIN_HAND);t.assertTrue(squid.tobacco().is(a.getMainHandItem().getItem()),"Other player cannot replace owner's selection");boolean sitting=squid.isOrderedToSit();a.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);squid.interact(a,InteractionHand.MAIN_HAND);t.assertTrue(squid.isOrderedToSit()!=sitting,"Owner empty hand toggles sit");var restored=RemainingMobs.DOPE_SQUID.get().create(level);restored.load(save(squid));t.assertTrue(restored.tobacco().is(item("thaumicbases:tobacco_fighting",1).getItem())&&restored.isOwnedBy(a),"Tobacco and owner survive NBT reload");
        }finally{a.discard();b.discard();squid.discard();}t.succeed();
    }
    static void tobaccoEffect(GameTestHelper t){
        grass(t);var level=t.getLevel();var owner=FakePlayerFactory.get(level,new GameProfile(UUID.randomUUID(),"SquidEffect"));owner.moveTo(t.absolutePos(new BlockPos(16,1,16)).getCenter());owner.setGameMode(net.minecraft.world.level.GameType.CREATIVE);level.addNewPlayer(owner);var squid=spawn(t,RemainingMobs.DOPE_SQUID.get(),17,1,16);squid.tame(owner);squid.setOrderedToSit(false);owner.setItemInHand(InteractionHand.MAIN_HAND,item("thaumicbases:tobacco_fighting",1));squid.interact(owner,InteractionHand.MAIN_HAND);var knowledge=KnowledgeSavedData.get(level.getServer());
        try{for(int warp:new int[]{9,10,20}){owner.removeEffect(MobEffects.DAMAGE_BOOST);knowledge.update(owner.getUUID(),owner.getRandom()::nextInt,k->{k.setWarp(WarpKind.TEMPORARY,warp);k.setWarp(WarpKind.PERMANENT,7);return null;});var tag=save(squid);tag.putInt("SmokeClock",399);squid.load(tag);squid.aiStep();t.assertTrue(ConciliumFocus.warp(owner,WarpKind.TEMPORARY)==(warp>=10?warp-10:warp),"Ten-warp threshold and exact debit for balance "+warp);t.assertTrue(ConciliumFocus.warp(owner,WarpKind.PERMANENT)==7,"Permanent warp preserved");var effect=owner.getEffect(MobEffects.DAMAGE_BOOST);t.assertTrue(warp<10?effect==null:effect!=null&&effect.getAmplifier()==1&&effect.getDuration()==8000,"Real Bases fighting tobacco effect for balance "+warp);}}finally{owner.discard();squid.discard();}t.succeed();
    }
    static void flowers(GameTestHelper t){
        grass(t);var corpse=spawn(t,RemainingMobs.WITHERED_BOTANIST.get(),16,1,16);var pos=corpse.blockPosition();corpse.interact(t.makeMockPlayer(),InteractionHand.MAIN_HAND);long count=flowers(t,pos);t.assertTrue(corpse.isRemoved(),"Fertilizing consumes corpse");t.assertTrue(count==3||count==4,"Three or four actual Botania mystical flowers: "+count);BlockPos.betweenClosedStream(pos.offset(-3,-1,-3),pos.offset(3,3,3)).forEach(p->{var state=t.getLevel().getBlockState(p);if(ForgeRegistries.BLOCKS.getKey(state.getBlock()).getNamespace().equals("botania"))t.assertTrue(state.canSurvive(t.getLevel(),p),"Placed flower survives on real support");});t.succeed();
    }
    public static final class DenyFlowers {final AABB box;DenyFlowers(AABB box){this.box=box;}@SubscribeEvent public void place(BlockEvent.EntityPlaceEvent e){if(box.contains(e.getPos().getCenter()))e.setCanceled(true);}}
    static void flowersDenied(GameTestHelper t){
        grass(t);var corpse=spawn(t,RemainingMobs.WITHERED_BOTANIST.get(),16,1,16);var pos=corpse.blockPosition();var deny=new DenyFlowers(new AABB(pos).inflate(8));MinecraftForge.EVENT_BUS.register(deny);try{corpse.interact(t.makeMockPlayer(),InteractionHand.MAIN_HAND);t.assertTrue(flowers(t,pos)==0,"Forge placement denial blocks all flower writes");}finally{MinecraftForge.EVENT_BUS.unregister(deny);}t.succeed();
    }
    static void dormant(GameTestHelper t){
        grass(t);t.assertTrue(!RemainingMobs.available("chort")&&!RemainingMobs.available("sloppy_alchemist")&&!RemainingMobs.available("burned_witch"),"No-host creative/spawning gates closed");
        ConciliumTestWorld.ready(t,List.of(new BlockPos(8,1,8),new BlockPos(16,1,16),new BlockPos(24,4,24),new BlockPos(20,1,20)),()->{
            var chort=spawn(t,RemainingMobs.CHORT.get(),8,1,8);var sloppy=spawn(t,RemainingMobs.SLOPPY_ALCHEMIST.get(),16,1,16);var witch=spawn(t,RemainingMobs.BURNED_WITCH.get(),24,4,24);var target=spawn(t,EntityType.COW,20,1,20);chort.setTarget(target);sloppy.setTarget(target);witch.setTarget(target);
            t.runAfterDelay(8,()->{try{for(var mob:List.of(chort,sloppy,witch))t.assertTrue(!mob.isRemoved()&&mob.tickCount>0&&t.getLevel().getEntity(mob.getUUID())==mob,"Dormancy fixture survives real world ticks: "+RemainingMobsServerTests.state(mob));t.assertTrue(chort.isNoAi()&&sloppy.isNoAi()&&chort.getTarget()==null&&sloppy.getTarget()==null,"Saved ground mobs become dormant: "+RemainingMobsServerTests.state(chort)+"; "+RemainingMobsServerTests.state(sloppy));t.assertTrue(witch.getTarget()==null,"Saved witch clears its target without a host: "+RemainingMobsServerTests.state(witch));t.assertTrue(witch.getDeltaMovement().lengthSqr()<1e-8,"Saved witch stops moving without a host: motion="+witch.getDeltaMovement()+"; "+RemainingMobsServerTests.state(witch));t.assertTrue(t.getLevel().getEntitiesOfClass(ThrowableProjectile.class,new AABB(t.absolutePos(new BlockPos(1,0,1)),t.absolutePos(new BlockPos(31,15,31)))).isEmpty(),"No fake potion fallback");t.succeed();}finally{chort.discard();sloppy.discard();witch.discard();target.discard();}});
        });
    }
    static void alchemist(GameTestHelper t){
        grass(t);
        // This generated test spawns beyond the one-block template checked by the harness.
        ConciliumTestWorld.ready(t,List.of(new BlockPos(16,1,16)),()->{
            var level=t.getLevel();var mob=RemainingMobs.SLOPPY_ALCHEMIST.get().create(level);mob.moveTo(t.absolutePos(new BlockPos(16,1,16)).getCenter());mob.finalizeSpawn(level,level.getCurrentDifficultyAt(mob.blockPosition()),MobSpawnType.SPAWN_EGG,null,null);level.addFreshEntity(mob);t.assertTrue(mob.getPassengers().size()==1&&mob.getFirstPassenger() instanceof Chort,"Real spawning creates one mounted Chort");var rider=(Chort)mob.getFirstPassenger();rider.setNoAi(true);mob.setNoAi(true);
            t.assertTrue(rider.hurt(level.damageSources().fellOutOfWorld(),1000)&&rider.isDeadOrDying(),"Actual rider receives fatal damage");
            t.runAfterDelay(30,()->{t.assertTrue(mob.tickCount>=29,"Freed-mount observation follows real world ticks: "+RemainingMobsServerTests.state(mob));t.assertTrue(!mob.captive()&&mob.getPassengers().isEmpty(),"Killed rider frees actual mount");t.assertTrue(Math.abs(mob.getBbWidth()-.5)<.01&&Math.abs(mob.getBbHeight()-1.8)<.01,"Freed dimensions update");var tag=save(mob);var restored=RemainingMobs.SLOPPY_ALCHEMIST.get().create(level);restored.load(tag);restored.finalizeSpawn(level,level.getCurrentDifficultyAt(restored.blockPosition()),MobSpawnType.SPAWN_EGG,null,null);t.assertTrue(restored.getPassengers().isEmpty(),"Saved spawn flag prevents another rider");var player=FakePlayerFactory.get(level,new GameProfile(UUID.randomUUID(),"FreeAlchemist"));mob.interact(player,InteractionHand.MAIN_HAND);int reward=player.getInventory().items.stream().mapToInt(ItemStack::getCount).sum();t.assertTrue(mob.isRemoved()&&reward>=1&&reward<=3,"Freed interaction returns actual host reward exactly once");t.succeed();});
        });
    }
    public static final class ObserveBrew {final BurnedWitch witch;int count;ObserveBrew(BurnedWitch witch){this.witch=witch;}@SubscribeEvent public void joined(EntityJoinLevelEvent e){if(e.getEntity() instanceof ThrowableProjectile projectile&&projectile.getOwner()==witch){count++;if(!Double.isFinite(projectile.getDeltaMovement().lengthSqr()))throw new IllegalStateException("Non-finite real host projectile velocity");}}}
    static void burnedWitchProjectile(GameTestHelper t){
        grass(t);var level=t.getLevel();var player=FakePlayerFactory.get(level,new GameProfile(UUID.randomUUID(),"WitchTarget"));player.moveTo(t.absolutePos(new BlockPos(16,1,16)).getCenter());level.addNewPlayer(player);var witch=spawn(t,RemainingMobs.BURNED_WITCH.get(),20,7,16);var observer=new ObserveBrew(witch);MinecraftForge.EVENT_BUS.register(observer);t.runAtTickTime(90,()->{MinecraftForge.EVENT_BUS.unregister(observer);player.discard();});
        t.runAtTickTime(35,()->{MinecraftForge.EVENT_BUS.unregister(observer);player.discard();t.assertTrue(observer.count>=1,"Real host brew projectile spawned by normal witch AI");t.assertTrue(witch.getTarget()!=null,"Normal vulnerable player acquired");t.succeed();});
    }
}
