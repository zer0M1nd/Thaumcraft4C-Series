package conciliummodern;

import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.gametest.*;
import tc4revival.content.*;
import top.theillusivec4.curios.api.CuriosApi;

/** Higher-risk upgrade branches are exercised through the production cast/tick/release entry points. */
@GameTestHolder("thaumicconcilium") @PrefixGameTestTemplate(false)
public final class ConciliumFocusAdvancedServerTests {
    @GameTest(template="empty",timeoutTicks=100)
    public static void alchemyConvertsEmberPrimalAndWaterWithActualUpgradeEffects(GameTestHelper test) {
        var player=player(test,8,3,8);var focus=ConciliumItems.ALCHEMIST_SPREE.get();var wand=wand(player,focus);ring(player);
        var core=WandFocusItem.attached(wand);focus.applyUpgrade(core,FocusUpgrade.POTENCY,1);focus.applyUpgrade(core,FocusUpgrade.POTENCY,2);focus.applyUpgrade(core,FocusUpgrade.require("thaumicconcilium:primal_essence"),3);WandFocusItem.attach(wand,core);
        var ember=new EmberEntity(test.getLevel(),player,0,4,0,20);ember.setPos(player.getX()+1,player.getEyeY(),player.getZ());test.getLevel().addFreshEntity(ember);
        focus.cast(wand,test.getLevel(),player,InteractionHand.MAIN_HAND,null);focus.usingTick(wand,player,0);
        var explosives=near(player,ExplosiveOrbEntity.class);test.assertTrue(ember.isRemoved()&&explosives.size()==1,"Actual ember becomes one explosive orb");
        var explosive=explosives.get(0);var saved=new CompoundTag();explosive.saveWithoutId(saved);
        test.assertTrue(saved.getFloat("Strength")==12&&saved.getBoolean("Ignition")&&explosive.getOwner()==player,"Primal essence ignites explosive orb and potency multiplies the source ember damage");explosive.discard();
        var primal=new PrimalOrbEntity(test.getLevel(),player,false);primal.setPos(player.getX()+1,player.getEyeY(),player.getZ());test.getLevel().addFreshEntity(primal);focus.usingTick(wand,player,0);
        var pechs=near(player,PechBlastEntity.class);test.assertTrue(primal.isRemoved()&&pechs.size()==1&&pechs.get(0).getOwner()==player,"Primal orb becomes owned actual Pech blast");
        var cow=EntityType.COW.create(test.getLevel());var target=test.absolutePos(new BlockPos(17,3,17));cow.moveTo(target.getX()+.5,target.getY(),target.getZ()+.5,0,0);cow.setNoAi(true);test.getLevel().addFreshEntity(cow);
        var pech=pechs.get(0);pech.setPos(cow.getX(),cow.getY()+.6,cow.getZ()-1.5);pech.setDeltaMovement(0,0,1);for(int i=0;i<4&&!pech.isRemoved();i++)pech.tick();
        test.assertTrue(pech.isRemoved()&&cow.hasEffect(MobEffects.POISON)&&cow.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)&&cow.hasEffect(MobEffects.WEAKNESS),"Converted primal-essence Pech blast actually hits and applies all three effects");
        test.assertTrue(cow.getEffect(MobEffects.POISON).getAmplifier()==2&&cow.getHealth()==6,"Converted Pech blast keeps potency in damage and effect level");cow.discard();
        var water=ConciliumEntities.CLEAR_WATER.get().create(test.getLevel());water.setOwner(player);water.setPos(player.getX()+1,player.getEyeY(),player.getZ());test.getLevel().addFreshEntity(water);focus.usingTick(wand,player,0);
        var taint=near(player,ThickTaintProjectile.class);test.assertTrue(water.isRemoved()&&taint.size()==1&&taint.get(0).getOwner()==player,"Clear-water bottle becomes one actual owned thick-taint projectile");taint.forEach(Entity::discard);near(player,DistortionField.class).forEach(Entity::discard);player.discard();test.succeed();
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void alchemyEldritchOrbTriggersActualShadowbeam(GameTestHelper test) {
        var player=player(test,8,3,8);var focus=ConciliumItems.ALCHEMIST_SPREE.get();var wand=wand(player,focus);ring(player);
        var cow=EntityType.COW.create(test.getLevel());cow.moveTo(player.getX(),player.getY(),player.getZ()+6,0,0);cow.setNoAi(true);test.getLevel().addFreshEntity(cow);aim(player,cow.getEyePosition());
        var orb=new EldritchOrbEntity(test.getLevel(),player);orb.setPos(player.getX()+1,player.getEyeY(),player.getZ());test.getLevel().addFreshEntity(orb);float before=cow.getHealth();
        focus.cast(wand,test.getLevel(),player,InteractionHand.MAIN_HAND,null);focus.usingTick(wand,player,0);
        test.assertTrue(orb.isRemoved()&&cow.getHealth()<before,"Actual eldritch-orb conversion consumes the source and the reflected beam damages a real target");cow.discard();near(player,DistortionField.class).forEach(Entity::discard);player.discard();test.succeed();
    }

    @GameTest(template="empty",timeoutTicks=240)
    public static void purificationConvertsAllSixFormsAndPreservesRejectedSources(GameTestHelper test) {
        ConciliumTestWorld.ready(test,List.of(new BlockPos(6,3,6),new BlockPos(6,3,9)),()->purifySixReady(test));
    }
    private static void purifySixReady(GameTestHelper test){
        var player=player(test,6,3,6);var focus=ConciliumItems.SPOTLIGHT.get();var wand=wand(player,focus);ring(player);var core=WandFocusItem.attached(wand);focus.applyUpgrade(core,FocusUpgrade.require("thaumicconcilium:taint_purification"),5);WandFocusItem.attach(wand,core);focus.cast(wand,test.getLevel(),player,InteractionHand.MAIN_HAND,null);
        var owned=new ArrayList<Entity>();
        try{
        for(var kind:TaintedAnimalEntity.Kind.values()) {
            EntityType<?> expected=switch(kind){case CHICKEN->EntityType.CHICKEN;case COW->EntityType.COW;case PIG->EntityType.PIG;case SHEEP->EntityType.SHEEP;case VILLAGER->EntityType.VILLAGER;case CREEPER->EntityType.CREEPER;};
            var source=TCEntities.taintedAnimal(kind).create(test.getLevel());source.moveTo(player.getX(),player.getY(),player.getZ()+3,0,0);source.setNoAi(true);source.setPersistenceRequired();source.setCustomName(Component.literal("Purified "+kind));owned.add(source);test.assertTrue(test.getLevel().addFreshEntity(source)&&test.getLevel().getEntity(source.getUUID())==source,"Tainted source is actually visible before purification: "+kind);aim(player,source.getEyePosition());float health=source.getHealth();
            test.assertTrue(FocusTargeting.pointed(player,0,50,.1,e->e instanceof LivingEntity)==source,"Actual partial-tick-zero ray reaches "+kind);
            int[] denied={0};java.util.function.Consumer<EntityJoinLevelEvent> reject=event->{if(event.getLevel()==test.getLevel()&&event.getEntity().getType()==expected){event.setCanceled(true);denied[0]++;}};MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,false,EntityJoinLevelEvent.class,reject);
            try {focus.usingTick(wand,player,Integer.MAX_VALUE);test.assertTrue(denied[0]==1&&source.isAlive()&&source.getHealth()==health,"Actual rejected replacement keeps original tainted "+kind+"; denials="+denied[0]);}
            finally {MinecraftForge.EVENT_BUS.unregister(reject);}
            focus.usingTick(wand,player,Integer.MAX_VALUE);var converted=test.getLevel().getEntitiesOfClass(Mob.class,source.getBoundingBox().inflate(1),e->e.getType()==expected&&e.isAlive());
            owned.addAll(converted);test.assertTrue(source.isRemoved()&&converted.size()==1&&converted.get(0).getName().getString().equals("Purified "+kind)&&converted.get(0).isPersistenceRequired(),"Actual purification retains name/persistence for "+kind+"; source="+RemainingMobsServerTests.state(source)+" converted="+converted.stream().map(RemainingMobsServerTests::state).toList());converted.forEach(Entity::discard);
        }
        test.succeed();
        }finally{owned.forEach(Entity::discard);player.discard();}
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void purificationHandlesEntityAndBiomeIndependentlyWithProtection(GameTestHelper test) {
        var player=player(test,8,3,5);var focus=ConciliumItems.SPOTLIGHT.get();var wand=wand(player,focus);ring(player);var core=WandFocusItem.attached(wand);focus.applyUpgrade(core,FocusUpgrade.require("thaumicconcilium:taint_purification"),5);WandFocusItem.attach(wand,core);
        for(int x=7;x<=9;x++)for(int y=1;y<=6;y++)test.setBlock(new BlockPos(x,y,13),Blocks.STONE);
        var first=TCEntities.TAINTED_COW.get().create(test.getLevel());first.moveTo(player.getX(),player.getY(),player.getZ()+3,0,0);first.setNoAi(true);test.getLevel().addFreshEntity(first);aim(player,first.getEyePosition());
        var ray=player.pick(50,0,false);test.assertTrue(ray instanceof BlockHitResult&&ray.getType()==HitResult.Type.BLOCK,"Fixture ray also reaches wall behind targeted animal");var hit=((BlockHitResult)ray).getBlockPos();var old=test.getLevel().getBiome(hit).unwrapKey().orElseThrow();var columns=new ArrayList<BlockPos>();for(int dx=-4;dx<=4;dx++)for(int dz=-4;dz<=4;dz++)columns.add(hit.offset(dx,0,dz));WorldBiomeChanges.changeColumns(test.getLevel(),columns,TCBiomes.TAINTED_LAND);
        java.util.function.Consumer<BlockEvent.EntityPlaceEvent> guard=event->{if(event.getLevel()==test.getLevel()&&event.getPos().equals(hit))event.setCanceled(true);};MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,false,BlockEvent.EntityPlaceEvent.class,guard);
        try {
            test.assertTrue(test.getLevel().getBiome(hit).is(TCBiomes.TAINTED_LAND),"Fixture provides real tainted biome behind animal");focus.cast(wand,test.getLevel(),player,InteractionHand.MAIN_HAND,null);focus.usingTick(wand,player,Integer.MAX_VALUE);
            test.assertTrue(first.isRemoved()&&test.getLevel().getBiome(hit).is(TCBiomes.TAINTED_LAND),"Denied biome edit leaves biome unchanged while independent entity purification still works");
        }finally{MinecraftForge.EVENT_BUS.unregister(guard);}
        near(player,net.minecraft.world.entity.animal.Cow.class).forEach(Entity::discard);var second=TCEntities.TAINTED_COW.get().create(test.getLevel());second.moveTo(first.position());second.setNoAi(true);test.getLevel().addFreshEntity(second);aim(player,second.getEyePosition());
        try {focus.usingTick(wand,player,Integer.MAX_VALUE);test.assertTrue(second.isRemoved()&&test.getLevel().getNoiseBiome(net.minecraft.core.QuartPos.fromBlock(hit.getX()),net.minecraft.core.QuartPos.fromBlock(hit.getY()),net.minecraft.core.QuartPos.fromBlock(hit.getZ())).is(TCBiomes.MAGICAL_FOREST),"One permitted actual tick purifies both tainted animal and the hit biome column");}
        finally {WorldBiomeChanges.changeColumns(test.getLevel(),columns,old);first.discard();second.discard();near(player,net.minecraft.world.entity.animal.Cow.class).forEach(Entity::discard);player.discard();}
        test.succeed();
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void conductorLauncherPreservesRejectedWispAndPersistsRealCharge(GameTestHelper test) {
        var player=player(test,8,3,8);var focus=ConciliumItems.VIS_CONDUCTOR.get();var wand=wand(player,focus);var core=WandFocusItem.attached(wand);focus.applyUpgrade(core,FocusUpgrade.require("thaumicconcilium:wisp_launcher"),1);focus.applyUpgrade(core,FocusUpgrade.POTENCY,2);focus.applyUpgrade(core,FocusUpgrade.EXTEND,3);WandFocusItem.attach(wand,core);
        var wisp=TCEntities.WISP.get().create(test.getLevel());wisp.setAspectTag("ignis");wisp.setNoAi(true);wisp.setPos(player.getX(),player.getY()+1,player.getZ()+1.5);test.getLevel().addFreshEntity(wisp);aim(player,wisp.getEyePosition());focus.cast(wand,test.getLevel(),player,InteractionHand.MAIN_HAND,null);focus.usingTick(wand,player,0);
        test.assertTrue(wisp.getDeltaMovement().lengthSqr()>0,"Launcher actually manipulates targeted native wisp");java.util.function.Consumer<EntityJoinLevelEvent> reject=event->{if(event.getLevel()==test.getLevel()&&event.getEntity() instanceof ChargedWisp)event.setCanceled(true);};MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,false,EntityJoinLevelEvent.class,reject);
        try {focus.release(wand,test.getLevel(),player,0);test.assertTrue(wisp.isAlive()&&near(player,ChargedWisp.class).isEmpty(),"Canceled charge spawn preserves original native wisp");}finally{MinecraftForge.EVENT_BUS.unregister(reject);}
        focus.cast(wand,test.getLevel(),player,InteractionHand.MAIN_HAND,null);focus.usingTick(wand,player,0);focus.release(wand,test.getLevel(),player,0);var charges=near(player,ChargedWisp.class);test.assertTrue(wisp.isRemoved()&&charges.size()==1,"Release consumes exactly one native wisp only after actual charge spawn");
        var charge=charges.get(0);var saved=new CompoundTag();charge.saveWithoutId(saved);test.assertTrue(charge.getOwner()==player&&charge.aspectTag().equals("ignis")&&saved.getInt("power")==1&&saved.getInt("LifeTime")>=40&&saved.getInt("LifeTime")<=59,"Charge carries actual caster, aspect, potency and extend lifetime");
        var restored=ConciliumEntities.CHARGED_WISP.get().create(test.getLevel());restored.load(saved);var again=new CompoundTag();restored.saveWithoutId(again);test.assertTrue(restored.getOwner()==player&&again.getInt("LifeTime")==saved.getInt("LifeTime")&&restored.aspectTag().equals("ignis"),"Native entity NBT retains charge ownership and behavior");
        charge.discard();restored.discard();player.discard();test.succeed();
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void manipulatorPreservesOriginalSpeedCenterAndNearestEntityOcclusion(GameTestHelper test) {
        var player=player(test,8,3,8);var focus=ConciliumItems.ETHEREAL_MANIPULATOR.get();var wand=wand(player,focus);var item=new net.minecraft.world.entity.item.ItemEntity(test.getLevel(),player.getX(),player.getEyeY(),player.getZ()+4,new ItemStack(Items.APPLE));item.setNoGravity(true);test.getLevel().addFreshEntity(item);aim(player,item.position());focus.cast(wand,test.getLevel(),player,InteractionHand.MAIN_HAND,null);focus.usingTick(wand,player,0);
        var destination=player.position().add(0,player.getBbHeight()/2+.5,0).add(player.getLookAngle().scale(4));var expected=destination.subtract(item.position().add(0,item.getBbHeight()/2,0));if(expected.lengthSqr()>1)expected=expected.normalize();expected=expected.scale(.3333f);
        test.assertTrue(item.getDeltaMovement().distanceTo(expected)<.000001&&item.getDeltaMovement().length()<=.33331,"Manipulator uses native TT center-relative motion and maximum speed .3333");
        var cow=EntityType.COW.create(test.getLevel());cow.moveTo(player.getX(),player.getY(),player.getZ()+2,0,0);cow.setNoAi(true);test.getLevel().addFreshEntity(cow);item.setDeltaMovement(Vec3.ZERO);var before=wand.copy();focus.usingTick(wand,player,0);
        test.assertTrue(item.getDeltaMovement().equals(Vec3.ZERO)&&ItemStack.matches(before,wand),"Nearer living entity occludes otherwise movable dropped item without charging vis");cow.discard();
        item.setPos(player.getX(),player.getY()+.2,player.getZ()+.2);aim(player,item.position());before=wand.copy();focus.usingTick(wand,player,0);test.assertTrue(item.getDeltaMovement().equals(Vec3.ZERO)&&ItemStack.matches(before,wand),"Original one-block minimum target range is retained");item.discard();player.discard();test.succeed();
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void conductorAndWarpTractionClampDistantTargetsToOneBlockPerTick(GameTestHelper test) {
        var player=player(test,8,3,8);var conductor=ConciliumItems.VIS_CONDUCTOR.get();var wand=wand(player,conductor);var core=WandFocusItem.attached(wand);conductor.applyUpgrade(core,FocusUpgrade.require("thaumicconcilium:wisp_launcher"),1);WandFocusItem.attach(wand,core);
        var wisp=TCEntities.WISP.get().create(test.getLevel());wisp.setNoAi(true);wisp.setAspectTag("aer");wisp.setPos(player.getX(),player.getEyeY(),player.getZ()+8);test.getLevel().addFreshEntity(wisp);aim(player,wisp.getEyePosition());conductor.cast(wand,test.getLevel(),player,InteractionHand.MAIN_HAND,null);conductor.usingTick(wand,player,0);
        var destination=player.position().add(player.getLookAngle().scale(1.5)).add(0,1,0);var expected=destination.subtract(wisp.position().add(0,wisp.getBbHeight()/2,0)).normalize();test.assertTrue(wisp.getDeltaMovement().distanceTo(expected)<.000001&&Math.abs(wisp.getDeltaMovement().length()-1)<.000001,"Conductor does not accelerate distant native wisp beyond original one-block speed");conductor.cancelPolishment(player);wisp.discard();
        var extraction=ConciliumItems.WARP_EXTRACTION.get();wand=wand(player,extraction);ring(player);player.setShiftKeyDown(true);extraction.cast(wand,test.getLevel(),player,InteractionHand.MAIN_HAND,null);var charge=near(player,WarpCharge.class).get(0);charge.setPos(player.getX(),player.getEyeY(),player.getZ()+8);aim(player,charge.position());player.setShiftKeyDown(false);extraction.cast(wand,test.getLevel(),player,InteractionHand.MAIN_HAND,null);extraction.usingTick(wand,player,0);
        destination=player.position().add(player.getLookAngle().scale(1.5)).add(0,2,0);expected=destination.subtract(charge.position().add(0,charge.getBbHeight()/2,0)).normalize();test.assertTrue(charge.getDeltaMovement().distanceTo(expected)<.000001&&Math.abs(charge.getDeltaMovement().length()-1)<.000001,"Warp extraction retains original center-relative bounded pull before its separate 2.5-speed release");charge.discard();player.discard();test.succeed();
    }

    private static ServerPlayer player(GameTestHelper test,int x,int y,int z) {var p=new ServerPlayer(test.getLevel().getServer(),test.getLevel(),new GameProfile(UUID.randomUUID(),"ConciliumAdv"));new ServerGamePacketListenerImpl(p.server,new Connection(PacketFlow.SERVERBOUND){@Override public void send(net.minecraft.network.protocol.Packet<?> packet){}},p);p.setGameMode(GameType.SURVIVAL);var pos=test.absolutePos(new BlockPos(x,y,z));p.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);test.getLevel().addNewPlayer(p);return p;}
    private static ItemStack wand(ServerPlayer player,WandFocusItem focus) {var stack=TCItems.CASTING_WAND.get().create(WandParts.Rod.SILVERWOOD,WandParts.Cap.GOLD,false,true);WandFocusItem.attach(stack,new ItemStack(focus));player.setItemInHand(InteractionHand.MAIN_HAND,stack);return stack;}
    private static void ring(ServerPlayer player) {var inventory=CuriosApi.getCuriosInventory(player).resolve().orElseThrow();inventory.reset();inventory.getStacksHandler("ring").orElseThrow().getStacks().setStackInSlot(0,new ItemStack(ConciliumItems.BLUSTERING_RING.get()));}
    private static void aim(ServerPlayer player,Vec3 target) {var d=target.subtract(player.getEyePosition());player.setYRot((float)Math.toDegrees(Math.atan2(-d.x,d.z)));player.setXRot((float)-Math.toDegrees(Math.atan2(d.y,Math.sqrt(d.x*d.x+d.z*d.z))));player.yRotO=player.getYRot();player.xRotO=player.getXRot();}
    private static <T extends Entity> List<T> near(ServerPlayer player,Class<T> type) {return player.level().getEntitiesOfClass(type,player.getBoundingBox().inflate(12));}
    private ConciliumFocusAdvancedServerTests() {}
}
