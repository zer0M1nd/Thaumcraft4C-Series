package tc4revival.content;

import java.util.*;
import java.util.function.Consumer;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.PlayLevelSoundEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class HellbatAuditServerTests {
    private static net.minecraft.util.RandomSource random(Entity entity) {
        try {for(var field:Entity.class.getDeclaredFields())if(field.getType()==net.minecraft.util.RandomSource.class){field.setAccessible(true);return (net.minecraft.util.RandomSource)field.get(entity);}}
        catch(ReflectiveOperationException e){throw new AssertionError(e);}throw new AssertionError("Missing entity random");
    }
    // AttributesLib gives every living attacker 5% random critical hits. Baseline damage
    // cases neutralize only the fixture multiplier; production integration remains enabled.
    static void baselineDamage(net.minecraft.world.entity.LivingEntity attacker) {
        var critical = net.minecraftforge.registries.ForgeRegistries.ATTRIBUTES.getValue(
                new net.minecraft.resources.ResourceLocation("attributeslib", "crit_damage"));
        if (critical != null && attacker.getAttribute(critical) != null)
            attacker.getAttribute(critical).setBaseValue(1);
    }
    @GameTest(template="wand_test_empty",timeoutTicks=200)
    public static void hellbatCombatBranchesFollowWorldRandomAndLegacyCooldown(GameTestHelper test) {
        var level=test.getLevel();var base=Vec3.atCenterOf(test.absolutePos(new BlockPos(4,20,4)));
        var owner=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"BatCombat"));owner.moveTo(base.add(0,10,0));
        var explosions=new ArrayList<net.minecraft.world.level.Explosion>();
        Consumer<net.minecraftforge.event.level.ExplosionEvent.Start> capture=e->{if(e.getLevel()==level&&e.getExplosion().getDirectSourceEntity() instanceof FireBatEntity){explosions.add(e.getExplosion());e.setCanceled(true);}};
        MinecraftForge.EVENT_BUS.addListener(capture);int cases=0,blasts=0,bites=0,ignitions=0;
        try {
            for(int variant=0;variant<3;variant++)for(int potency:new int[]{0,2})for(boolean vampire:new boolean[]{false,true})for(int sample=0;sample<16;sample++) {
                var target=EntityType.COW.create(level);target.moveTo(base.add(1,0,0));target.setDeltaMovement(.1,.2,.3);
                var bat=new FireBatEntity(TCEntities.FIRE_BAT.get(),level);baselineDamage(bat);bat.moveTo(base);bat.summon(owner,target,potency,variant==1,variant==2,vampire);bat.setHealth(3);owner.removeEffect(net.minecraft.world.effect.MobEffects.REGENERATION);
                try {
                    long seed=104729L*sample;var oracle=net.minecraft.util.RandomSource.create(seed);
                    boolean blast=(variant==1||oracle.nextInt(10)==0)&&variant!=2;
                    boolean bite=!blast&&(vampire||oracle.nextBoolean());
                    if(blast)target.invulnerableTime=12;
                    explosions.clear();level.random.setSeed(seed);random(bat).setSeed(seed+987654321);
                    bat.customServerAiStep();
                    test.assertTrue(LegacyCombatCredit.remaining(target)==100,"Summoned hit grants legacy kill credit");
                    var regen=owner.getEffect(net.minecraft.world.effect.MobEffects.REGENERATION);
                    test.assertTrue(vampire?regen!=null&&regen.getDuration()==26&&regen.getAmplifier()==1:regen==null,"Vampire owner regeneration precedes attack branch");
                    test.assertTrue(bat.getHealth()==(vampire?4:3),"Vampire heals bat even before explosion");
                    if(blast) {
                        test.assertTrue(explosions.size()==1&&bat.isRemoved()&&target.invulnerableTime==0,"Explosion clears immunity and consumes bat even when Forge cancels blast");
                        var explosion=explosions.get(0);test.assertTrue(!explosion.interactsWithBlocks(),"Legacy explosion cannot destroy terrain");
                        float radius=0;for(var field:net.minecraft.world.level.Explosion.class.getDeclaredFields())if(field.getType()==float.class){field.setAccessible(true);radius=field.getFloat(explosion);}
                        test.assertTrue(Math.abs(radius-(1.5f+(variant==1?potency*.33f:0)))<1e-6,"Explosive potency radius");blasts++;
                    } else {
                        test.assertTrue(explosions.isEmpty()&&!bat.isRemoved(),"Non-explosion branch survives");
                        if(bite){test.assertTrue(target.getHealth()==target.getMaxHealth()-((variant==2?3:2)+potency),"Bite applies actual attribute damage: health="+target.getHealth()+",max="+target.getMaxHealth()+",attack="+bat.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)+",variant="+variant+",potency="+potency+",vampire="+vampire+",sample="+sample);test.assertTrue(target.getDeltaMovement().equals(new Vec3(.1,.2,.3))&&!target.hasImpulse,"Bite preserves pre-hit velocity");bites++;}
                        else {test.assertTrue(target.getHealth()==target.getMaxHealth()&&target.getRemainingFireTicks()==80,"Summoned fire branch ignites for four seconds");ignitions++;}
                        float health=target.getHealth();int fire=target.getRemainingFireTicks();
                        for(int tick=0;tick<19;tick++)bat.customServerAiStep();
                        test.assertTrue(target.getHealth()==health&&target.getRemainingFireTicks()==fire&&explosions.isEmpty(),"Next nineteen AI calls cannot attack again");
                    }
                    cases++;
                } finally {target.discard();bat.discard();}
            }
            test.assertTrue(blasts>0&&bites>0&&ignitions>0,"Matrix actually covers all three branches");
            var chance=net.minecraftforge.registries.ForgeRegistries.ATTRIBUTES.getValue(
                    new net.minecraft.resources.ResourceLocation("attributeslib","crit_chance"));
            var multiplier=net.minecraftforge.registries.ForgeRegistries.ATTRIBUTES.getValue(
                    new net.minecraft.resources.ResourceLocation("attributeslib","crit_damage"));
            if(chance!=null&&multiplier!=null) {
                var target=EntityType.COW.create(level);
                var bat=new FireBatEntity(TCEntities.FIRE_BAT.get(),level);
                try {
                    bat.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(2);
                    bat.getAttribute(chance).setBaseValue(1);
                    bat.getAttribute(multiplier).setBaseValue(1.5);
                    random(target).setSeed(1);
                    test.assertTrue(bat.doHurtTarget(target)&&target.getHealth()==7,
                            "Installed AttributesLib still applies controlled 1.5x critical through real hurt pipeline");
                } finally {target.discard();bat.discard();}
            }

            tc4revival.TC4Revival.LOGGER.info("PASS hellbatCombatBranchesFollowWorldRandomAndLegacyCooldown: {} cases, {} blasts/{} bites/{} ignitions; explosions intercepted",cases,blasts,bites,ignitions);test.succeed();
        } catch(ReflectiveOperationException e){throw new AssertionError(e);}
        finally {MinecraftForge.EVENT_BUS.unregister(capture);owner.removeAllEffects();}
    }

    private static ItemStack wand(FakePlayer player,int variant,int potency,boolean vampire,boolean full) {
        var focus=TCItems.HELLBAT_FOCUS.get();var core=new ItemStack(focus);
        if(potency>0){focus.applyUpgrade(core,FocusUpgrade.POTENCY,1);focus.applyUpgrade(core,FocusUpgrade.POTENCY,2);}
        if(variant>0)focus.applyUpgrade(core,variant==1?FocusUpgrade.BAT_BOMBS:FocusUpgrade.DEVIL_BATS,3);
        if(vampire)focus.applyUpgrade(core,FocusUpgrade.VAMPIRE_BATS,5);
        var result=TCItems.CASTING_WAND.get().create(WandParts.Rod.GREATWOOD,WandParts.Cap.GOLD,false,full);
        WandFocusItem.attach(result,core);player.setItemInHand(InteractionHand.MAIN_HAND,result);WandCooldowns.clear(player);return result;
    }
    @GameTest(template="wand_test_empty",timeoutTicks=100)
    public static void hellbatSummoningVariantsPaymentAndRejectedSpawnMatchLegacy(GameTestHelper test) {
        var level=test.getLevel();var base=test.absolutePos(new BlockPos(4,20,4));var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"BatAudit"));player.moveTo(Vec3.atBottomCenterOf(base));
        var forced=new ArrayList<ChunkPos>();for(int x=(base.getX()-2)>>4;x<=(base.getX()+2)>>4;x++)for(int z=base.getZ()>>4;z<=(base.getZ()+8)>>4;z++)if(level.setChunkForced(x,z,true))forced.add(new ChunkPos(x,z));
        test.runAfterDelay(20,()->{
            var spawned=new ArrayList<FireBatEntity>();var sounds=new ArrayList<SoundEvent>();boolean[] reject={false};
            Consumer<EntityJoinLevelEvent> capture=e->{if(e.getLevel()==level&&e.getEntity() instanceof FireBatEntity bat&&bat.distanceToSqr(player)<16){spawned.add(bat);if(reject[0])e.setCanceled(true);}};
            Consumer<PlayLevelSoundEvent> sound=e->{if(e.getLevel()==level&&e.getSound()!=null)sounds.add(e.getSound().value());};
            MinecraftForge.EVENT_BUS.addListener(capture);MinecraftForge.EVENT_BUS.addListener(sound);int cases=0;
            try {
                for(int variant=0;variant<3;variant++)for(int potency:new int[]{0,2})for(boolean vampire:new boolean[]{false,true})for(float pitch:new float[]{0,15}) {
                    var target=EntityType.COW.create(level);player.setYRot(0);player.setXRot(pitch);target.moveTo(player.getEyePosition().add(player.getLookAngle().scale(6)).add(0,-target.getEyeHeight(),0));level.addFreshEntity(target);
                    try {
                        var stack=wand(player,variant,potency,vampire,true);var before=stack.copy();spawned.clear();sounds.clear();
                        stack.use(level,player,InteractionHand.MAIN_HAND);
                        test.assertTrue(spawned.size()==1,"Actual wand use creates exactly one bat");var bat=spawned.get(0);
                        test.assertTrue(bat.getTarget()==target&&bat.summoned()&&!bat.hanging()&&bat.explosive()==(variant==1)&&bat.devil()==(variant==2)&&bat.vampire()==vampire,"Summon target and variant flags");
                        test.assertTrue(bat.getXRot()==0&&bat.getHealth()==5&&bat.getMaxHealth()==5,"Legacy pitch zero and five health including devil variant");
                        test.assertTrue(bat.getAttributeValue(Attributes.ATTACK_DAMAGE)==(variant==2?3:2)+potency,"Variant attack attribute");
                        var expected=player.position().add(-.16,player.getBbHeight()/2+.2+bat.getBbHeight(),0).add(player.getLookAngle().scale(.5));
                        test.assertTrue(bat.position().distanceTo(expected)<1e-6,"Legacy spawn offset");
                        for(var aspect:tc4revival.aspect.TCAspects.catalog().primals()) {int cost=switch(aspect.tag()){case "ignis"->200;case "aer","perditio"->100;default->0;};test.assertTrue(TCItems.CASTING_WAND.get().vis(before,aspect)-TCItems.CASTING_WAND.get().vis(stack,aspect)==cost,"Legacy top-level wand payment even for upgraded focus");}
                        test.assertTrue(sounds.contains(TCSounds.ICE.get())&&!sounds.contains(TCSounds.WAND_FAIL.get()),"Successful summon feedback");
                        var saved=new CompoundTag();bat.saveWithoutId(saved);var restored=new FireBatEntity(TCEntities.FIRE_BAT.get(),level);restored.load(saved);
                        test.assertTrue(restored.summoned()&&restored.explosive()==bat.explosive()&&restored.devil()==bat.devil()&&restored.vampire()==bat.vampire()&&restored.getTarget()==null,"NBT retains flags but not old target");
                        test.assertTrue(restored.getAttributeValue(Attributes.ATTACK_DAMAGE)==bat.getAttributeValue(Attributes.ATTACK_DAMAGE),"Inherited attributes persist");restored.discard();cases++;
                    } finally {target.discard();spawned.forEach(Entity::discard);spawned.clear();}
                }
                player.setXRot(0);var target=EntityType.COW.create(level);target.moveTo(player.position().add(0,0,6));level.addFreshEntity(target);
                try {
                    reject[0]=true;var stack=wand(player,0,0,false,true);var before=stack.copy();sounds.clear();stack.use(level,player,InteractionHand.MAIN_HAND);
                    test.assertTrue(spawned.size()==1&&!level.getEntitiesOfClass(FireBatEntity.class,player.getBoundingBox().inflate(8)).contains(spawned.get(0)),"Forge cancellation rejects world spawn");
                    test.assertTrue(sounds.contains(TCSounds.WAND_FAIL.get())&&!sounds.contains(TCSounds.ICE.get()),"Rejected spawn produces failure feedback only");
                    var fire=tc4revival.aspect.TCAspects.catalog().require("ignis");test.assertTrue(TCItems.CASTING_WAND.get().vis(before,fire)-TCItems.CASTING_WAND.get().vis(stack,fire)==200,"Old canceled-spawn payment retained");
                    spawned.clear();sounds.clear();reject[0]=false;stack=wand(player,0,0,false,false);stack.use(level,player,InteractionHand.MAIN_HAND);
                    test.assertTrue(spawned.isEmpty()&&sounds.contains(TCSounds.WAND_FAIL.get()),"No vis prevents spawn and reports failure");
                } finally {target.discard();}
                tc4revival.TC4Revival.LOGGER.info("PASS hellbatSummoningVariantsPaymentAndRejectedSpawnMatchLegacy: {} variant/potency/vampire/pitch cases plus cancellation and no-vis",cases);test.succeed();
            } finally {MinecraftForge.EVENT_BUS.unregister(capture);MinecraftForge.EVENT_BUS.unregister(sound);spawned.forEach(Entity::discard);WandCooldowns.clear(player);forced.forEach(c->level.setChunkForced(c.x,c.z,false));}
        });
    }
}

