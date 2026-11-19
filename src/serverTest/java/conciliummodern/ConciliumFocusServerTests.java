package conciliummodern;

import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.*;
import tc4revival.content.*;
import tc4revival.research.KnowledgeSavedData;
import tc4revival.research.PlayerKnowledge.WarpKind;
import top.theillusivec4.curios.api.CuriosApi;

/** Real focus entry points, world/entity effects and payment boundaries, isolated from the product jar. */
@GameTestHolder("thaumicconcilium") @PrefixGameTestTemplate(false)
public final class ConciliumFocusServerTests {
    @GameTest(template="empty",timeoutTicks=100)
    public static void manipulationMovesUnpickableItemsButRespectsWallsAndRelease(GameTestHelper test) {
        var player=player(test,5,3,5);var focus=ConciliumItems.ETHEREAL_MANIPULATOR.get();var wand=wand(player,focus,true);
        var item=new ItemEntity(test.getLevel(),player.getX(),player.getEyeY(),player.getZ()+3,new ItemStack(Items.APPLE));item.setNoGravity(true);test.getLevel().addFreshEntity(item);aim(player,item.position());
        focus.cast(wand,test.getLevel(),player,InteractionHand.MAIN_HAND,null);var before=wand.copy();focus.usingTick(wand,player,0);
        test.assertTrue(item.getDeltaMovement().lengthSqr()>0&&!ItemStack.matches(before,wand),"Manipulator affects a non-pickable dropped item and charges vis");
        item.setDeltaMovement(Vec3.ZERO);BlockPos wall=BlockPos.containing(player.getEyePosition().add(0,0,1));test.getLevel().setBlock(wall,Blocks.STONE.defaultBlockState(),3);before=wand.copy();focus.usingTick(wand,player,0);
        test.assertTrue(item.getDeltaMovement().equals(Vec3.ZERO)&&ItemStack.matches(before,wand),"Opaque wall prevents remote movement and charge");
        EtherealManipulatorFocus.range(wand,Float.NaN);test.assertTrue(EtherealManipulatorFocus.range(wand)==5,"Malformed range is finite and bounded");EtherealManipulatorFocus.range(wand,30);test.assertTrue(EtherealManipulatorFocus.range(wand)==16,"Overlong range clamps to sixteen");focus.release(wand,test.getLevel(),player,0);test.assertTrue(EtherealManipulatorFocus.range(wand)==5,"Releasing resets stored range");item.discard();player.discard();test.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void coolingFiresSnowAndRefreezeOnlyAfterPayment(GameTestHelper test) {
        var player=player(test,6,3,6);var focus=ConciliumItems.COOLING_FOCUS.get();var empty=wand(player,focus,false);focus.cast(empty,test.getLevel(),player,InteractionHand.MAIN_HAND,null);focus.usingTick(empty,player,0);
        test.assertTrue(near(test,player,Snowball.class).isEmpty()&&!player.isUsingItem(),"Empty wand cannot fire free snowballs");
        var wand=wand(player,focus,true);var before=wand.copy();focus.cast(wand,test.getLevel(),player,InteractionHand.MAIN_HAND,null);focus.usingTick(wand,player,1);test.assertTrue(ItemStack.matches(before,wand),"Non-firing tick consumes no vis");focus.usingTick(wand,player,0);
        var snow=near(test,player,Snowball.class);test.assertTrue(snow.size()==1&&snow.get(0).getOwner()==player&&!ItemStack.matches(before,wand),"Normal cooling creates an owned actual snowball and pays");snow.forEach(Entity::discard);
        var core=WandFocusItem.attached(wand);for(int rank=1;rank<=4;rank++)focus.applyUpgrade(core,FocusUpgrade.POTENCY,rank);focus.applyUpgrade(core,FocusUpgrade.require("thaumicconcilium:refreeze"),5);WandFocusItem.attach(wand,core);before=wand.copy();focus.usingTick(wand,player,0);
        var shards=near(test,player,FrostShardEntity.class);test.assertTrue(shards.size()==1&&shards.get(0).getOwner()==player&&!ItemStack.matches(before,wand),"Rank-five refreeze actually substitutes a frost shard and uses upgraded cost");shards.forEach(Entity::discard);player.discard();test.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void conductorLocksDrainsAndRejectsDifferentWandSession(GameTestHelper test) {
        var player=player(test,5,2,5);var focus=ConciliumItems.VIS_CONDUCTOR.get();var wand=wand(player,focus,true);var crystal=crystal(test,5,3,8,"aer",8);aim(player,Vec3.atCenterOf(crystal.getBlockPos()));
        focus.cast(wand,test.getLevel(),player,InteractionHand.MAIN_HAND,null);var before=wand.copy();focus.usingTick(wand,player,0);test.assertTrue(crystal.amount()==8&&ItemStack.matches(before,wand),"Initial actual raycast locks crystal without draining it");focus.usingTick(wand,player,3);
        test.assertTrue(crystal.amount()==7&&crystal.draining()&&!ItemStack.matches(before,wand),"Subsequent third tick drains one actual crystal and pays");
        var substitute=wand.copy();player.setItemInHand(InteractionHand.MAIN_HAND,substitute);before=substitute.copy();focus.usingTick(substitute,player,6);test.assertTrue(crystal.amount()==7&&ItemStack.matches(before,substitute)&&!player.isUsingItem(),"Copied wand cannot use another item's casting session");
        focus.cancelPolishment(player);player.discard();test.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void conductorAuramRiftEffectRequiresDematerialization(GameTestHelper test) {
        var player=player(test,5,2,5);var focus=ConciliumItems.VIS_CONDUCTOR.get();var wand=wand(player,focus,true);var crystal=crystal(test,5,3,8,"auram",8);
        var rift=ConciliumEntities.RIFT.get().create(test.getLevel());var tag=new CompoundTag();tag.putInt("RiftSize",20);tag.putInt("RiftSeed",9);tag.putInt("MaxSize",20);tag.putInt("Phase",0);rift.load(tag);var at=test.absolutePos(new BlockPos(16,5,5));rift.setPos(Vec3.atCenterOf(at));test.getLevel().addFreshEntity(rift);
        aim(player,Vec3.atCenterOf(crystal.getBlockPos()));focus.cast(wand,test.getLevel(),player,InteractionHand.MAIN_HAND,null);focus.usingTick(wand,player,0);focus.usingTick(wand,player,3);aim(player,rift.position());test.assertTrue(FocusTargeting.pointed(player,0,32,1.1)==rift,"Test release ray reaches actual rift");focus.release(wand,test.getLevel(),player,0);
        var saved=new CompoundTag();rift.saveWithoutId(saved);test.assertTrue(saved.getInt("Phase")==0&&saved.getInt("MaxSize")==20,"Plain conductor cannot invoke dematerialization-only rift effect");
        var core=WandFocusItem.attached(wand);focus.applyUpgrade(core,FocusUpgrade.require("thaumicconcilium:dematerialization"),1);WandFocusItem.attach(wand,core);
        aim(player,Vec3.atCenterOf(crystal.getBlockPos()));focus.cast(wand,test.getLevel(),player,InteractionHand.MAIN_HAND,null);focus.usingTick(wand,player,0);focus.usingTick(wand,player,3);aim(player,rift.position());focus.release(wand,test.getLevel(),player,0);saved=new CompoundTag();rift.saveWithoutId(saved);
        test.assertTrue(saved.getInt("Phase")==1&&saved.getInt("MaxSize")==19,"Upgraded release collapses only the actually drained auram amount");rift.discard();player.discard();test.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void reflectionUsesSpendableXpAndConsumesOneCompound(GameTestHelper test) {
        var player=player(test,5,2,5);var focus=ConciliumItems.REFLECTION_FOCUS.get();var wand=wand(player,focus,true);var core=WandFocusItem.attached(wand);focus.applyUpgrade(core,FocusUpgrade.FRUGAL,1);focus.applyUpgrade(core,FocusUpgrade.POTENCY,2);focus.applyUpgrade(core,FocusUpgrade.POTENCY,3);WandFocusItem.attach(wand,core);
        player.giveExperiencePoints(10);player.totalExperience=100000;focus.cast(wand,test.getLevel(),player,InteractionHand.MAIN_HAND,null);focus.usingTick(wand,player,1);test.assertTrue(warp(player)==0,"Lifetime experience counter cannot bypass spendable threshold eleven");player.giveExperiencePoints(1);focus.usingTick(wand,player,1);
        test.assertTrue(warp(player)==3&&player.experienceLevel==0&&Math.round(player.experienceProgress*7)==2,"Threshold eleven spends nine XP and grants three temporary warp");
        var data=KnowledgeSavedData.get(player.server);data.update(player.getUUID(),player.getRandom()::nextInt,k->{for(var aspect:TCAspects.catalog().compounds())k.changePool(aspect,5);return null;});
        focus.applyUpgrade(core,FocusUpgrade.require("thaumicconcilium:oblivion"),5);WandFocusItem.attach(wand,core);int before=compoundPool(player);focus.usingTick(wand,player,1);test.assertTrue(compoundPool(player)==before,"Oblivion waits for five-tick cadence");focus.usingTick(wand,player,5);
        test.assertTrue(compoundPool(player)==before-1&&warp(player)==17,"Oblivion commits exactly one compound point and upgraded temporary warp");player.discard();test.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void signalCyclesFreelyThenFiresSavedVisualOnlyProjectile(GameTestHelper test) {
        var player=player(test,6,3,6);var focus=ConciliumItems.SIGNAL_FOCUS.get();var wand=wand(player,focus,true);player.setShiftKeyDown(true);var before=wand.copy();focus.cast(wand,test.getLevel(),player,InteractionHand.MAIN_HAND,null);
        test.assertTrue(WandFocusItem.attached(wand).getTag().getInt("Color")==1&&vis(wand)==vis(before)&&near(test,player,CompressedBlast.class).isEmpty(),"Sneaking changes color while preserving vis and spawning nothing");
        player.setShiftKeyDown(false);focus.cast(wand,test.getLevel(),player,InteractionHand.MAIN_HAND,null);var blasts=near(test,player,CompressedBlast.class);test.assertTrue(blasts.size()==1&&vis(wand)<vis(before),"Actual signal cast emits one paid projectile");var blast=blasts.get(0);var saved=new CompoundTag();blast.saveWithoutId(saved);
        var firework=ItemStack.of(saved.getCompound("FireworksItem"));test.assertTrue(firework.is(Items.FIREWORK_ROCKET)&&firework.getTag().getCompound("Fireworks").getList("Explosions",10).size()==1,"Signal carries exactly one original firework design");
        var cow=EntityType.COW.create(test.getLevel());cow.moveTo(blast.position());test.getLevel().addFreshEntity(cow);float health=cow.getHealth();saved.putInt("Life",1000);saved.putInt("LifeTime",1);
        // Motion is explicitly reset after NBT load; no vanilla rocket entity or damage path is used.
        saved.remove("Motion");blast.load(saved);blast.setDeltaMovement(Vec3.ZERO);blast.tick();test.assertTrue(blast.isRemoved()&&cow.getHealth()==health,"Ordinary signal explosion is visual and deals no inherited rocket damage");cow.discard();player.discard();test.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void impulseModesUseUpgradedCostAndPersistVacuum(GameTestHelper test) {
        var player=player(test,8,4,8);var focus=ConciliumItems.IMPULSE_FOCUS.get();var wand=wand(player,focus,true);var core=WandFocusItem.attached(wand);focus.applyUpgrade(core,FocusUpgrade.require("thaumicconcilium:vacuum"),5);WandFocusItem.attach(wand,core);var before=wand.copy();var result=focus.cast(wand,test.getLevel(),player,InteractionHand.MAIN_HAND,null);
        test.assertTrue(result.getResult().consumesAction()&&vis(wand)<vis(before),"Vacuum cast succeeds and pays: result="+result.getResult()+", before="+vis(before)+", after="+vis(wand));
        // Newly forced GameTest chunks can expose an inserted entity to world queries on the following tick.
        test.runAfterDelay(2,()->{
            var blasts=near(test,player,CompressedBlast.class);
            try{
                test.assertTrue(blasts.size()==1,"Vacuum creates exactly one nearby blast: count="+blasts.size()+", player="+player.position()+", found="+blasts.stream().map(e->e.getUUID()+" at "+e.position()+" owner="+e.getOwner()).toList());
                var blast=blasts.get(0);var saved=new CompoundTag();blast.saveWithoutId(saved);test.assertTrue(saved.getBoolean("vacuum")&&!saved.getBoolean("pneumoStrike")&&ItemStack.of(saved.getCompound("FireworksItem")).isEmpty(),"Saved blast preserves mode and separates impulse from signal");
                var target=EntityType.COW.create(test.getLevel());target.moveTo(blast.getX()+2,blast.getY(),blast.getZ(),0,0);test.getLevel().addFreshEntity(target);
                try{blast.setDeltaMovement(Vec3.ZERO);blast.tick();test.assertTrue(target.getDeltaMovement().x<0,"Vacuum's actual tick pulls a non-player towards projectile");}finally{target.discard();}
            }finally{blasts.forEach(Entity::discard);player.discard();}
            test.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void positiveBurstHealsCasterAndCapsAbsorptionAfterReload(GameTestHelper test) {
        var player=player(test,8,4,8);var focus=ConciliumItems.POSITIVE_FOCUS.get();var wand=wand(player,focus,true);var before=wand.copy();focus.cast(wand,test.getLevel(),player,InteractionHand.MAIN_HAND,null);var orbs=near(test,player,PositiveBurstOrb.class);
        test.assertTrue(orbs.size()>=4&&orbs.size()<=8&&vis(wand)<vis(before),"Positive burst emits original base orb count after paying randomized cost");player.setHealth(10);player.takeXpDelay=0;orbs.get(0).playerTouch(player);test.assertTrue(player.getHealth()==10.5f&&orbs.get(0).isRemoved()&&player.takeXpDelay==2,"Caster may collect one mote for half a heart-point with two-tick pickup delay");orbs.forEach(Entity::discard);
        var core=new ItemStack(focus);focus.applyUpgrade(core,FocusUpgrade.require("thaumicconcilium:fulfillment"),5);var original=ConciliumEntities.POSITIVE_ORB.get().create(test.getLevel());original.initialize(player,core,4,InteractionHand.MAIN_HAND);var saved=new CompoundTag();original.saveWithoutId(saved);var restored=ConciliumEntities.POSITIVE_ORB.get().create(test.getLevel());restored.load(saved);player.takeXpDelay=0;player.setAbsorptionAmount(19);restored.playerTouch(player);
        test.assertTrue(restored.mode()==2&&restored.isRemoved()&&player.getAbsorptionAmount()==20,"Reloaded fulfillment orb retains upgrade and caps absorption at twenty");original.discard();player.discard();test.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void spotlightRequiresWornRingAndWarmsRealTarget(GameTestHelper test) {
        var player=player(test,5,2,5);var focus=ConciliumItems.SPOTLIGHT.get();var wand=wand(player,focus,true);test.assertTrue(focus.cast(wand,test.getLevel(),player,InteractionHand.MAIN_HAND,null).getResult()==InteractionResult.FAIL,"Inventory alone cannot bypass worn-ring gate");ring(player);var core=WandFocusItem.attached(wand);focus.applyUpgrade(core,FocusUpgrade.require("thaumicconcilium:warming_up"),5);WandFocusItem.attach(wand,core);
        var cow=EntityType.COW.create(test.getLevel());cow.moveTo(player.getX(),player.getY(),player.getZ()+3,0,0);test.getLevel().addFreshEntity(cow);aim(player,cow.getEyePosition());var before=wand.copy();test.assertTrue(focus.cast(wand,test.getLevel(),player,InteractionHand.MAIN_HAND,null).getResult().consumesAction(),"Real Curios ring enables spotlight");focus.usingTick(wand,player,Integer.MAX_VALUE);
        test.assertTrue(cow.hasEffect(MobEffects.REGENERATION)&&cow.getEffect(MobEffects.REGENERATION).getAmplifier()==5&&vis(wand)<vis(before),"Warming mode applies original strong short regeneration and pays");WandCooldowns.clear(player);focus.release(wand,test.getLevel(),player,Integer.MAX_VALUE-20);test.assertTrue(WandCooldowns.secondsRemaining(player)==3,"Release cooldown uses elapsed charge ticks");WandCooldowns.clear(player);cow.discard();player.discard();test.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void warpExtractionSpawnsOwnedChargeReleasesAndTogglesOff(GameTestHelper test) {
        var player=player(test,8,3,8);var focus=ConciliumItems.WARP_EXTRACTION.get();var wand=wand(player,focus,true);ring(player);player.setShiftKeyDown(true);var before=wand.copy();focus.cast(wand,test.getLevel(),player,InteractionHand.MAIN_HAND,null);var charges=near(test,player,WarpCharge.class);
        test.assertTrue(charges.size()==1&&charges.get(0).ownedBy(player)&&!charges.get(0).forced()&&vis(wand)<vis(before),"Sneaking creates an owned paid non-forced warp charge");var charge=charges.get(0);var saved=new CompoundTag();charge.saveWithoutId(saved);var restored=ConciliumEntities.WARP_CHARGE.get().create(test.getLevel());restored.load(saved);test.assertTrue(restored.ownedBy(player)&&!restored.forced(),"Charge owner and mode survive entity NBT");
        focus.release(wand,test.getLevel(),player,0);test.assertTrue(Math.abs(charge.getDeltaMovement().length()-2.5)<.001,"Release launches owned non-forced charge at original speed");before=wand.copy();focus.cast(wand,test.getLevel(),player,InteractionHand.MAIN_HAND,null);test.assertTrue(charge.isRemoved()&&ItemStack.matches(before,wand),"Second sneaking cast dismisses existing charge without paying again");restored.discard();player.discard();test.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void wrathEnlargementAndBurnoutAffectRealEnemies(GameTestHelper test) {
        var player=player(test,7,2,7);var focus=ConciliumItems.WRATH_ATTRACTION.get();var wand=wand(player,focus,true);var core=WandFocusItem.attached(wand);focus.applyUpgrade(core,FocusUpgrade.ENLARGE,1);focus.applyUpgrade(core,FocusUpgrade.require("thaumicconcilium:burnout"),5);WandFocusItem.attach(wand,core);
        var zombie=EntityType.ZOMBIE.create(test.getLevel());zombie.moveTo(player.getX()+8,player.getY(),player.getZ(),0,0);test.getLevel().addFreshEntity(zombie);var before=wand.copy();focus.cast(wand,test.getLevel(),player,InteractionHand.MAIN_HAND,null);
        test.assertTrue(zombie.getTarget()==player&&zombie.hasEffect(MobEffects.WEAKNESS)&&zombie.getEffect(MobEffects.WEAKNESS).getAmplifier()==3&&vis(wand)<vis(before),"Enlarge reaches eight blocks and burnout marks actual monster with weakness IV");zombie.discard();near(test,player,WrathGlyph.class).forEach(Entity::discard);player.discard();test.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void taintAnimationHonorsProtectionThenConsumesRealFlux(GameTestHelper test) {
        var player=player(test,7,3,7);var focus=ConciliumItems.TAINT_ANIMATION.get();var wand=wand(player,focus,true);var pos=test.absolutePos(new BlockPos(8,3,7));test.getLevel().setBlock(pos,TCArcane.FLUX_GAS.get().defaultBlockState(),3);
        int[] denied={0};java.util.function.Consumer<net.minecraftforge.event.level.BlockEvent.BreakEvent> guard=event->{if(event.getPos().equals(pos)){denied[0]++;event.setCanceled(true);}};MinecraftForge.EVENT_BUS.addListener(net.minecraftforge.eventbus.api.EventPriority.HIGHEST,false,net.minecraftforge.event.level.BlockEvent.BreakEvent.class,guard);
        try {focus.cast(wand,test.getLevel(),player,InteractionHand.MAIN_HAND,null);focus.usingTick(wand,player,10);test.assertTrue(denied[0]==1&&test.getLevel().getBlockState(pos).is(TCArcane.FLUX_GAS.get())&&near(test,player,TaintSwarmEntity.class).isEmpty(),"Actual denied block break cannot consume flux or animate swarm");}finally{MinecraftForge.EVENT_BUS.unregister(guard);}
        focus.usingTick(wand,player,20);player.stopUsingItem();
        test.assertTrue(test.getLevel().getBlockState(pos).isAir()&&test.getLevel().getFluidState(pos).isEmpty(),"Permitted tick removes the actual gas FluidState");
        test.runAfterDelay(2,()->{var swarms=near(test,player,TaintSwarmEntity.class);try{test.assertTrue(swarms.size()==1,"One real swarm remains after chunk/entity tracking: "+swarms.size());}finally{swarms.forEach(Entity::discard);player.discard();}test.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void scribingRestoresAnotherPlayersShieldWithCapAndPayment(GameTestHelper test) {
        var player=player(test,5,2,5);var recipient=player(test,5,2,8);recipient.getAttribute(TCAttributes.RUNIC_SHIELD.get()).setBaseValue(5);RunicShieldEvents.restore(recipient,4);var focus=ConciliumItems.SCRIBING.get();var wand=wand(player,focus,true);aim(player,recipient.getEyePosition());var before=wand.copy();focus.cast(wand,test.getLevel(),player,InteractionHand.MAIN_HAND,null);focus.usingTick(wand,player,1);
        test.assertTrue(RunicShieldEvents.state(recipient).exactCharge()==4&&ItemStack.matches(before,wand),"Scribing waits for ten-tick cadence");focus.usingTick(wand,player,10);test.assertTrue(RunicShieldEvents.state(recipient).exactCharge()==5&&vis(wand)<vis(before),"Actual targeted player's shield restores to equipment cap and caster pays");recipient.discard();player.discard();test.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void alchemistSpreeConvertsProjectilesAndPreservesRejectedSource(GameTestHelper test) {
        var player=player(test,8,3,8);var focus=ConciliumItems.ALCHEMIST_SPREE.get();var wand=wand(player,focus,true);ring(player);var snow=new Snowball(test.getLevel(),player);snow.setPos(player.getX()+1,player.getEyeY(),player.getZ());test.getLevel().addFreshEntity(snow);
        java.util.function.Consumer<net.minecraftforge.event.entity.EntityJoinLevelEvent> reject=event->{if(event.getLevel()==test.getLevel()&&event.getEntity() instanceof FrostShardEntity)event.setCanceled(true);};MinecraftForge.EVENT_BUS.addListener(net.minecraftforge.eventbus.api.EventPriority.HIGHEST,false,net.minecraftforge.event.entity.EntityJoinLevelEvent.class,reject);
        try {focus.cast(wand,test.getLevel(),player,InteractionHand.MAIN_HAND,null);focus.usingTick(wand,player,0);test.assertTrue(snow.isAlive(),"Rejected replacement keeps original snowball");}finally{MinecraftForge.EVENT_BUS.unregister(reject);}
        var before=wand.copy();focus.usingTick(wand,player,0);var shards=near(test,player,FrostShardEntity.class);test.assertTrue(snow.isRemoved()&&shards.size()==1&&vis(wand)<vis(before),"Permitted alchemy converts original projectile after creating real frost shard");
        var egg=new ThrownEgg(test.getLevel(),player);egg.setPos(player.getX()+1,player.getEyeY(),player.getZ());test.getLevel().addFreshEntity(egg);focus.usingTick(wand,player,0);test.assertTrue(egg.isRemoved()&&near(test,player,TaintSpiderEntity.class).size()==1,"Egg branch creates actual taint spider");
        shards.forEach(Entity::discard);near(test,player,TaintSpiderEntity.class).forEach(Entity::discard);var fields=near(test,player,DistortionField.class);test.assertTrue(fields.size()==1,"Repeated casts reuse one visual field lease");for(var field:fields){for(int i=0;i<3;i++)field.tick();test.assertTrue(field.isRemoved(),"Abandoned field expires without further casting");}player.discard();test.succeed();
    }
    private static ServerPlayer player(GameTestHelper test,int x,int y,int z) {
        var p=new ServerPlayer(test.getLevel().getServer(),test.getLevel(),new GameProfile(UUID.randomUUID(),"ConciliumFocus"));
        new ServerGamePacketListenerImpl(p.server,new Connection(PacketFlow.SERVERBOUND){@Override public void send(net.minecraft.network.protocol.Packet<?> packet){}},p);p.setGameMode(GameType.SURVIVAL);
        var pos=test.absolutePos(new BlockPos(x,y,z));p.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);test.getLevel().addNewPlayer(p);return p;
    }
    private static ItemStack wand(ServerPlayer player,WandFocusItem focus,boolean charged) {var wand=TCItems.CASTING_WAND.get().create(WandParts.Rod.SILVERWOOD,WandParts.Cap.GOLD,false,charged);WandFocusItem.attach(wand,new ItemStack(focus));player.setItemInHand(InteractionHand.MAIN_HAND,wand);return wand;}
    private static long vis(ItemStack wand) {return TCAspects.catalog().primals().stream().mapToLong(a->((CastingWandItem)wand.getItem()).vis(wand,a)).sum();}
    private static void aim(ServerPlayer player,Vec3 target) {var d=target.subtract(player.getEyePosition());player.setYRot((float)Math.toDegrees(Math.atan2(-d.x,d.z)));player.setXRot((float)-Math.toDegrees(Math.atan2(d.y,Math.sqrt(d.x*d.x+d.z*d.z))));player.yRotO=player.getYRot();player.xRotO=player.getXRot();}
    private static <T extends Entity> List<T> near(GameTestHelper test,Entity center,Class<T> type) {return test.getLevel().getEntitiesOfClass(type,center.getBoundingBox().inflate(12));}
    private static void ring(ServerPlayer player) {var inventory=CuriosApi.getCuriosInventory(player).resolve().orElseThrow();inventory.reset();inventory.getStacksHandler("ring").orElseThrow().getStacks().setStackInSlot(0,new ItemStack(ConciliumItems.BLUSTERING_RING.get()));}
    private static int warp(ServerPlayer player) {return KnowledgeSavedData.get(player.server).snapshot(player.getUUID()).map(k->k.warp(WarpKind.TEMPORARY)).orElse(0);}
    private static int compoundPool(ServerPlayer player) {var k=KnowledgeSavedData.get(player.server).snapshot(player.getUUID()).orElseThrow();return TCAspects.catalog().compounds().stream().mapToInt(k::poolAmount).sum();}
    private static DestabilizedCrystalBlockEntity crystal(GameTestHelper test,int x,int y,int z,String aspect,int quantity) {
        var pos=test.absolutePos(new BlockPos(x,y,z));test.getLevel().setBlock(pos.below(),Blocks.STONE.defaultBlockState(),3);test.getLevel().setBlock(pos,ConciliumBlocks.CRYSTAL.get().defaultBlockState(),3);var result=(DestabilizedCrystalBlockEntity)test.getLevel().getBlockEntity(pos);var tag=new CompoundTag();tag.putString("aspect",aspect);tag.putInt("capacity",quantity);result.load(tag);return result;
    }
    private ConciliumFocusServerTests() {}
}
