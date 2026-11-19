package tmmodern;

import com.mojang.authlib.GameProfile;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;
import tc4revival.content.*;
import tc4revival.research.*;

/** Real server operations that exercise costs and world effects, rather than just registries. */
@GameTestHolder("taintedmagic") @PrefixGameTestTemplate(false)
public class TMBehaviorServerTests {
    static ServerPlayer player(GameTestHelper t,String name){
        var p=FakePlayerFactory.get(t.getLevel(),new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)),name));
        p.setGameMode(GameType.SURVIVAL);p.getInventory().clearContent();p.removeAllEffects();
        p.setPos(Vec3.atCenterOf(t.absolutePos(new BlockPos(2,4,2))));p.setYRot(0);p.setXRot(0);
        p.getAbilities().mayfly=false;p.getAbilities().flying=false;return p;
    }
    static ItemStack wand(ServerPlayer p,String focus){
        var s=TCItems.CASTING_WAND.get().create(WandParts.Rod.GREATWOOD,WandParts.Cap.GOLD,false,true);
        WandFocusItem.attach(s,new ItemStack(TaintedMagic.item("focus_"+focus)));p.setItemInHand(InteractionHand.MAIN_HAND,s);return s;
    }
    static int vis(ItemStack s,String key){return ((CastingWandItem)s.getItem()).vis(s,MagicFocus.aspect(key));}
    static MagicFocus focus(ItemStack wand){return (MagicFocus)WandFocusItem.attached(wand).getItem();}

    @GameTest(template="test_empty") public static void warpwoodWholeVisAndWarpWard(GameTestHelper t){
        var p=player(t,"TMRecharge");var knowledge=KnowledgeSavedData.get(p.server);
        knowledge.update(p.getUUID(),p.getRandom()::nextInt,k->{k.changeWarp(PlayerKnowledge.WarpKind.PERMANENT,100-k.warp(PlayerKnowledge.WarpKind.PERMANENT));return null;});
        int period=Math.max(1,Math.round(TMConfig.WARP_REFRESH.get().floatValue()/100));
        for(String rod:List.of("warpwood","warpwood_staff")){
            var w=TCItems.CASTING_WAND.get().create(WandParts.Rod.require("taintedmagic:"+rod),WandParts.Cap.GOLD,false,false);
            p.tickCount=period-1;w.getItem().inventoryTick(w,t.getLevel(),p,0,true);
            t.assertTrue(vis(w,"aer")==0,"Warpwood waits for original permanent-warp period");
            p.tickCount=period;w.getItem().inventoryTick(w,t.getLevel(),p,0,true);
            for(String a:List.of("aer","terra","ignis","aqua","ordo","perditio"))t.assertTrue(vis(w,a)==100,"Original addVis gives one whole vis to "+a+" for "+rod);
            p.addEffect(new MobEffectInstance(TCEffects.WARP_WARD.get(),100));p.tickCount=period*2;
            w.getItem().inventoryTick(w,t.getLevel(),p,0,true);t.assertTrue(vis(w,"aer")==100,"Warp ward suspends recharge");p.removeAllEffects();
        }
        t.succeed();
    }
    @GameTest(template="test_empty") public static void maceChargesOnlyAcceptedMelee(GameTestHelper t){
        var p=player(t,"TMMace");var w=wand(p,"mage_mace");var f=focus(w);int before=vis(w,"perditio");
        f.cast(w,t.getLevel(),p,InteractionHand.MAIN_HAND,null);f.usingTick(w,p,100);
        t.assertTrue(vis(w,"perditio")==before&&!f.costPerTick(WandFocusItem.attached(w)),"Mace has no continuous/right-click fee");
        var target=EntityType.PIG.create(t.getLevel());var event=new net.minecraftforge.event.entity.living.LivingAttackEvent(target,p.damageSources().playerAttack(p),5);
        new TMRuntime().attack(event);t.assertTrue(!event.isCanceled()&&vis(w,"perditio")==before-10,"Successful melee spends original 0.1 perditio");
        w.getOrCreateTag().putInt("perditio",0);event=new net.minecraftforge.event.entity.living.LivingAttackEvent(target,p.damageSources().playerAttack(p),5);new TMRuntime().attack(event);
        t.assertTrue(event.isCanceled(),"Empty mace cannot apply charged attack damage");t.succeed();
    }
    @GameTest(template="test_empty") public static void lumosReplacesHitAndConsumesVis(GameTestHelper t){
        var p=player(t,"TMLumos");var w=wand(p,"lumos");var f=focus(w);var pos=t.absolutePos(new BlockPos(3,2,3));var l=t.getLevel();
        l.setBlockAndUpdate(pos.below(),Blocks.DIRT.defaultBlockState());l.setBlockAndUpdate(pos,Blocks.GRASS.defaultBlockState());l.setBlockAndUpdate(pos.above(),Blocks.STONE.defaultBlockState());
        int aer=vis(w,"aer"),ignis=vis(w,"ignis");f.cast(w,l,p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
        t.assertTrue(l.getBlockState(pos).is(TaintedMagic.block("lumos"))&&l.getBlockState(pos.above()).is(Blocks.STONE),"Replaceable hit is replaced rather than offset into an occupied neighbor");
        t.assertTrue(vis(w,"aer")==aer-10&&vis(w,"ignis")==ignis-25,"Lumos takes its original costs once");
        var solid=pos.east(2);l.setBlockAndUpdate(solid,Blocks.STONE.defaultBlockState());l.setBlockAndUpdate(solid.above(),Blocks.AIR.defaultBlockState());
        w.getOrCreateTag().putInt("ignis",0);aer=vis(w,"aer");f.cast(w,l,p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(solid),Direction.UP,solid,false));
        t.assertTrue(l.isEmptyBlock(solid.above())&&vis(w,"aer")==aer,"Insufficient component prevents placement and partial debit");t.succeed();
    }
    @GameTest(template="test_empty") public static void diffusionActualCastAndEnergyFailure(GameTestHelper t){
        var p=player(t,"TMDiffusion");var w=wand(p,"dark_matter");var item=focus(w);var fs=WandFocusItem.attached(w);
        item.applyUpgrade(fs,FocusUpgrade.require("taintedmagic:diffusion"),5);item.applyUpgrade(fs,FocusUpgrade.POTENCY,1);WandFocusItem.attach(w,fs);
        int perditio=vis(w,"perditio"),ignis=vis(w,"ignis");item.cast(w,t.getLevel(),p,InteractionHand.MAIN_HAND,null);item.usingTick(w,p,100);
        var shots=t.getLevel().getEntitiesOfClass(DarkMatterProjectile.class,p.getBoundingBox().inflate(4),e->e.getOwner()==p);
        t.assertTrue(shots.size()==3&&shots.stream().allMatch(e->e.diffusion()&&e.damage==13),"Diffusion creates two plus potency damaging projectiles");
        t.assertTrue(vis(w,"perditio")==perditio-15&&vis(w,"ignis")==ignis-10,"Continuous call charges once, not once per projectile");
        shots.forEach(Entity::discard);w.getOrCreateTag().putInt("perditio",0);item.usingTick(w,p,99);
        t.assertTrue(!p.isUsingItem()&&t.getLevel().getEntitiesOfClass(DarkMatterProjectile.class,p.getBoundingBox().inflate(4),e->e.getOwner()==p).isEmpty(),"Empty wand stops diffusion without spawning another projectile");t.succeed();
    }
    @GameTest(template="test_empty") public static void swarmAndShardRequireAndUseActualTarget(GameTestHelper t){
        var l=t.getLevel();var p=player(t,"TMTargets");var w=wand(p,"taint_swarm");var f=focus(w);int before=vis(w,"terra");
        f.cast(w,l,p,InteractionHand.MAIN_HAND,null);t.assertTrue(vis(w,"terra")==before,"No target does not debit swarm energy");
        var target=EntityType.ZOMBIE.create(l);target.setNoAi(true);target.setPos(p.position().add(0,0,4));l.addFreshEntity(target);
        f.cast(w,l,p,InteractionHand.MAIN_HAND,null);var swarms=l.getEntitiesOfClass(TaintSwarmEntity.class,p.getBoundingBox().inflate(4));
        t.assertTrue(swarms.size()==1&&swarms.get(0).getTarget()==target&&vis(w,"terra")==before-50,"Swarm attaches to the pointed target and spends one cast");swarms.forEach(Entity::discard);
        w=wand(p,"vis_shard");f=focus(w);before=vis(w,"aer");f.cast(w,l,p,InteractionHand.MAIN_HAND,null);
        var shards=l.getEntitiesOfClass(HomingShardProjectile.class,p.getBoundingBox().inflate(4));t.assertTrue(shards.size()==1&&target.getUUID().equals(shards.get(0).target)&&vis(w,"aer")==before-10,"Homing shard retains pointed entity and real cost");
        shards.forEach(Entity::discard);target.discard();t.succeed();
    }
    @GameTest(template="test_empty") public static void homingShardLifetimeAndPersistentRetarget(GameTestHelper t){
        var l=t.getLevel();var p=player(t,"TMHoming");var far=EntityType.PIG.create(l);far.setNoAi(true);far.setPos(p.position().add(0,0,40));l.getChunk(far.blockPosition());l.addFreshEntity(far);
        var shard=new HomingShardProjectile(TaintedMagic.SHARD.get(),l);shard.setOwner(p);shard.setPos(p.position());shard.target=far.getUUID();shard.targetType=net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(far.getType());shard.tickCount=10;shard.tick();
        t.assertTrue(!shard.isRemoved()&&shard.getDeltaMovement().z>0,"Ordinary living target stays valid beyond the Persistent retarget threshold: removed="+shard.isRemoved()+", motion="+shard.getDeltaMovement()+", target="+l.getEntity(far.getUUID()));
        far.discard();var near=EntityType.PIG.create(l);near.setNoAi(true);near.setPos(p.position().add(0,0,5));l.addFreshEntity(near);shard.persistent=true;shard.tick();
        t.assertTrue(!shard.isRemoved()&&near.getUUID().equals(shard.target),"Persistent retargets immediately when original target is dead");
        near.discard();shard.tick();t.assertTrue(shard.isRemoved(),"No living replacement ends the shard immediately");t.succeed();
    }
    @GameTest(template="test_empty") public static void shockwaveNearDamageAndFarKnockback(GameTestHelper t){
        var l=t.getLevel();var p=player(t,"TMShockwave");var near=EntityType.PIG.create(l);near.setNoAi(true);near.setPos(p.position().add(4,0,0));l.addFreshEntity(near);
        var far=EntityType.PIG.create(l);far.setNoAi(true);far.setPos(p.position().add(10,0,0));l.addFreshEntity(far);float a=near.getHealth(),b=far.getHealth();var w=wand(p,"shockwave");int before=vis(w,"aer");
        focus(w).cast(w,l,p,InteractionHand.MAIN_HAND,null);t.assertTrue(near.getHealth()==a-2&&far.getHealth()==b,"Only targets within seven blocks take direct damage");
        t.assertTrue(near.getDeltaMovement().x>0&&far.getDeltaMovement().x>0&&far.getDeltaMovement().y==1.5&&vis(w,"aer")==before-150,"Both targets receive outward force and caster pays once");near.discard();far.discard();t.succeed();
    }
    @GameTest(template="test_empty") public static void flyteConsumptionAndRevocation(GameTestHelper t){
        var p=player(t,"TMFlyte");var w=wand(p,"lumos");var charm=new ItemStack(TaintedMagic.item("flyte_charm"));charm.setHoverName(net.minecraft.network.chat.Component.literal("Named charm"));p.getInventory().add(charm);var runtime=new TMRuntime();var tick=new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,p);
        int before=vis(w,"aer");runtime.tick(tick);t.assertTrue(p.getAbilities().mayfly&&vis(w,"aer")==before,"Carried charm grants flight without charging idle use");
        p.getAbilities().flying=true;runtime.tick(tick);t.assertTrue(vis(w,"aer")==before-15,"Flying drains original 0.15 aer each tick");
        w.getOrCreateTag().putInt("aer",0);runtime.tick(tick);t.assertTrue(!p.getAbilities().mayfly&&!p.getAbilities().flying,"Running out of charge revokes charm-owned flight");t.succeed();
    }
    @GameTest(template="test_empty") public static void gateKeyTeleportAndObstruction(GameTestHelper t){
        var l=t.getLevel();var p=new net.minecraftforge.common.util.FakePlayer(l,new GameProfile(UUID.nameUUIDFromBytes("TMGate".getBytes(StandardCharsets.UTF_8)),"TMGate")){@Override public void teleportTo(double x,double y,double z){setPos(x,y,z);}};p.setPos(Vec3.atCenterOf(t.absolutePos(new BlockPos(2,4,2))));var key=new ItemStack(TaintedMagic.item("gate_key"));var pos=t.absolutePos(new BlockPos(3,2,3));l.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());p.setItemInHand(InteractionHand.MAIN_HAND,key);
        key.useOn(new net.minecraft.world.item.context.UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false)));
        l.setBlockAndUpdate(pos.above(),Blocks.AIR.defaultBlockState());l.setBlockAndUpdate(pos.above(2),Blocks.AIR.defaultBlockState());key.getItem().finishUsingItem(key,l,p);
        t.assertTrue(p.position().equals(Vec3.atBottomCenterOf(pos.above()))&&p.hasEffect(MobEffects.CONFUSION),"Charged bound key teleports to its saved position and adds nausea: player="+p.position()+", expected="+Vec3.atBottomCenterOf(pos.above())+", nausea="+p.hasEffect(MobEffects.CONFUSION)+", nbt="+key.getTag());
        p.setPos(Vec3.atCenterOf(pos.east(4)));var before=p.position();l.setBlockAndUpdate(pos.above(),Blocks.STONE.defaultBlockState());key.getItem().finishUsingItem(key,l,p);
        t.assertTrue(p.position().equals(before)&&!key.isEmpty(),"Blocked destination fails without consuming or moving the key user");t.succeed();
    }
    @GameTest(template="test_empty") public static void flyteUsesOffhandAndPaysExactlyOnce(GameTestHelper t){
        var p=player(t,"TMOffhandFlyte");var w=wand(p,"lumos");
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STONE));
        p.setItemInHand(InteractionHand.OFF_HAND,w);
        p.getInventory().add(new ItemStack(TaintedMagic.item("flyte_charm")));
        var runtime=new TMRuntime();var tick=new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,p);
        int before=vis(w,"aer");runtime.tick(tick);
        t.assertTrue(p.getAbilities().mayfly&&vis(w,"aer")==before,"Only offhand wand grants flight; preview is free");
        p.getAbilities().flying=true;runtime.tick(tick);
        t.assertTrue(vis(w,"aer")==before-15,"Offhand payment is made once per flying tick");
        p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);runtime.tick(tick);
        t.assertTrue(!p.getAbilities().mayfly&&!p.getAbilities().flying,"Removing the only source safely revokes charm flight");
        t.succeed();
    }
    @GameTest(template="test_empty") public static void chargedKatanaKeepsOriginalArmorBypass(GameTestHelper t){
        var l=t.getLevel();var p=player(t,"TMKatanaCharge");var sword=new ItemStack(TaintedMagic.item("voidmetal_katana"));sword.getOrCreateTag().putInt("inscription",0);p.setItemInHand(InteractionHand.MAIN_HAND,sword);p.setShiftKeyDown(true);
        var target=EntityType.ZOMBIE.create(l);target.setNoAi(true);target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);target.setHealth(100);
        target.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.DIAMOND_CHESTPLATE));target.setPos(p.position().add(0,0,2.5));l.addFreshEntity(target);l.random.setSeed(1);
        var source=KatanaItem.chargedSource(p);t.assertTrue(source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)&&!source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE),"Charged strike retains old packet type independently of ordinary fire inscription");
        sword.getItem().releaseUsing(sword,l,p,72000-20);float dealt=100-target.getHealth();
        t.assertTrue(Math.abs(dealt-17.5f*1.5f)<.01||Math.abs(dealt-17.5f*2.5f)<.01,"Charged multiplier bypasses worn armor: "+dealt);
        t.assertTrue(!target.hasEffect(MobEffects.WEAKNESS)&&!target.isOnFire(),"Charge packet does not invent normal-hit sapping/inscription effects");target.discard();p.setShiftKeyDown(false);t.succeed();
    }
    @GameTest(template="test_empty") public static void katanaHealingAndCooldown(GameTestHelper t){
        var l=t.getLevel();var p=player(t,"TMKatanaHeal");var sword=new ItemStack(TaintedMagic.item("thaumium_katana"));sword.getOrCreateTag().putInt("inscription",2);p.setItemInHand(InteractionHand.MAIN_HAND,sword);p.setHealth(4);
        var target=EntityType.PIG.create(l);target.setNoAi(true);target.setPos(p.position().add(2,0,0));l.addFreshEntity(target);
        sword.getItem().releaseUsing(sword,l,p,72000-19);t.assertTrue(p.getHealth()==4,"Short charge does not activate inscription");
        sword.getItem().releaseUsing(sword,l,p,72000-20);t.assertTrue(p.getHealth()==12&&p.hasEffect(MobEffects.REGENERATION)&&p.hasEffect(MobEffects.DAMAGE_RESISTANCE)&&target.hasEffect(MobEffects.WITHER),"Fully charged healing inscription restores half missing health and affects nearby entities");
        t.assertTrue(sword.getTag().getInt("cooldown")==140&&sword.getItem().use(l,p,InteractionHand.MAIN_HAND).getResult()==InteractionResult.FAIL,"Original seven-second cooldown prevents beginning another charge");
        sword.getItem().inventoryTick(sword,l,p,0,true);t.assertTrue(sword.getTag().getInt("cooldown")==139,"Cooldown decrements in inventory");target.discard();t.succeed();
    }
    @GameTest(template="test_empty") public static void disassemblerHoeModesAndCenterGate(GameTestHelper t){
        var l=t.getLevel();var p=player(t,"TMHoeModes");var tool=new ItemStack(TaintedMagic.item("thaumic_disassembler"));p.setItemInHand(InteractionHand.MAIN_HAND,tool);var center=t.absolutePos(new BlockPos(3,2,3));
        var ctx=new net.minecraft.world.item.context.UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(center),Direction.UP,center,false));
        for(int mode=0;mode<4;mode++){
            for(var q:BlockPos.betweenClosed(center.offset(-2,0,-2),center.offset(2,0,2))){l.setBlockAndUpdate(q,Blocks.DIRT.defaultBlockState());l.setBlockAndUpdate(q.above(),Blocks.AIR.defaultBlockState());}
            tool.getOrCreateTag().putInt("charge",1000);tool.getTag().putInt("mode",mode);tool.useOn(ctx);int count=0;
            for(var q:BlockPos.betweenClosed(center.offset(-2,0,-2),center.offset(2,0,2)))if(l.getBlockState(q).is(Blocks.FARMLAND))count++;
            int expected=new int[]{9,1,25,0}[mode];t.assertTrue(count==expected&&DisassemblerItem.charge(tool)==1000-expected*25,"Hoe mode "+mode+" tills exact area and charges per changed block: count="+count+", charge="+DisassemblerItem.charge(tool));
        }
        l.setBlockAndUpdate(center,Blocks.STONE.defaultBlockState());tool.getTag().putInt("mode",0);tool.useOn(ctx);
        t.assertTrue(l.getBlockState(center.east()).is(Blocks.DIRT)&&DisassemblerItem.charge(tool)==1000,"Invalid center blocks area hoe activation");t.succeed();
    }}
