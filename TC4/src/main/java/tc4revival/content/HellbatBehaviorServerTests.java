package tc4revival.content;

import com.mojang.authlib.GameProfile;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.effect.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class HellbatBehaviorServerTests {
    @GameTest(template="wand_test_empty")
    public static void hellbatImmunitiesHangingAndTwentiethAttack(GameTestHelper test) {
        var level=test.getLevel();var anchor=test.absolutePos(new BlockPos(4,12,4));var base=new BlockPos(anchor.getX(),100,anchor.getZ());
        var explosion=new net.minecraft.world.damagesource.DamageSource(level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(net.minecraft.world.damagesource.DamageTypes.EXPLOSION));
        var sources=List.of(level.damageSources().inFire(),level.damageSources().lava(),explosion,level.damageSources().generic(),level.damageSources().drown());
        for(int index=0;index<sources.size();index++) {
            var bat=new FireBatEntity(TCEntities.FIRE_BAT.get(),level);bat.moveTo(Vec3.atBottomCenterOf(base));
            try {
                boolean hurt=bat.hurt(sources.get(index),1);boolean immune=index<3;
                test.assertTrue(hurt!=immune&&bat.getHealth()==(immune?5:4)&&bat.hanging()==immune,"Fire/explosion immunity does not wake bat, accepted damage does: "+index);
                test.assertTrue(!bat.causeFallDamage(100,1,level.damageSources().fall()),"No fall damage callback");
                test.assertTrue(!bat.isPushable()&&bat.isIgnoringBlockTriggers(),"Legacy push and pressure-plate behavior");
            } finally {bat.discard();}
        }
        var ceiling=base.above();var old=level.getBlockState(ceiling);var hanging=new FireBatEntity(TCEntities.FIRE_BAT.get(),level);hanging.moveTo(Vec3.atBottomCenterOf(base));
        try {
            level.setBlock(ceiling,Blocks.STONE.defaultBlockState(),2);hanging.customServerAiStep();test.assertTrue(hanging.hanging(),"Supported bat remains hanging without nearby player");
            hanging.setDeltaMovement(.1,.2,.3);hanging.tick();test.assertTrue(hanging.getDeltaMovement().equals(Vec3.ZERO),"Hanging tick clears all motion");
            test.assertTrue(Math.abs(hanging.getY()-(101-hanging.getBbHeight()))<1e-6,"Hanging tick anchors body beneath ceiling");
            level.setBlock(ceiling,Blocks.AIR.defaultBlockState(),2);hanging.customServerAiStep();test.assertTrue(!hanging.hanging(),"Removing ceiling wakes bat");
        } finally {hanging.discard();level.setBlock(ceiling,old,2);}
        var owner=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"BatCooldown"));owner.moveTo(Vec3.atCenterOf(base.above(10)));owner.addEffect(new MobEffectInstance(MobEffects.REGENERATION,90,0));
        var target=EntityType.IRON_GOLEM.create(level);target.moveTo(Vec3.atBottomCenterOf(base.east()));var bat=new FireBatEntity(TCEntities.FIRE_BAT.get(),level);bat.moveTo(Vec3.atBottomCenterOf(base));bat.summon(owner,target,0,false,true,true);
        try {
            bat.customServerAiStep();test.assertTrue(target.getHealth()==97,"First bite occurred");target.invulnerableTime=0;
            for(int tick=1;tick<20;tick++)bat.customServerAiStep();test.assertTrue(target.getHealth()==97,"Cooldown excludes first nineteen following AI calls");
            bat.customServerAiStep();test.assertTrue(target.getHealth()==94,"Twentieth following AI call attacks again");
            var effect=owner.getEffect(MobEffects.REGENERATION);test.assertTrue(effect!=null&&effect.getDuration()==90&&effect.getAmplifier()==0,"Existing regeneration is never replaced or extended");
            tc4revival.TC4Revival.LOGGER.info("PASS hellbatImmunitiesHangingAndTwentiethAttack: five damage types, fall/push/trigger, hanging/support and exact cooldown/regeneration");test.succeed();
        } finally {bat.discard();target.discard();owner.removeAllEffects();}
    }
}
