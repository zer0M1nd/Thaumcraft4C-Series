package tc4revival.content;

import java.util.UUID;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class FrostImpactServerTests {
    @GameTest(template="wand_test_empty")
    public static void frostEntityReflectionSlowAndKnockbackPreserveLegacyBranches(GameTestHelper test) {
        var level=test.getLevel();var pos=Vec3.atCenterOf(test.absolutePos(new BlockPos(3,12,3)));var owner=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"FrostBranches"));owner.moveTo(pos.add(0,5,0));int cases=0;
        for(int axes=0;axes<8;axes++)for(boolean fragile:new boolean[]{false,true})for(boolean accepts:new boolean[]{false,true})for(int frost:new int[]{0,1,3}) {
            class Target extends Cow {
                int calls;DamageSource received;float amount;
                Target(){super(EntityType.COW,level);}
                @Override public boolean hurt(DamageSource source,float damage){calls++;received=source;amount=damage;if(accepts)setDeltaMovement(getDeltaMovement().add(1,2,3));return accepts;}
            }
            var target=new Target();target.moveTo(pos.add((axes&1)==0?0:1,(axes&2)==0?0:1,(axes&4)==0?0:1));var before=new Vec3(.4,.5,.6);target.setDeltaMovement(before);target.invulnerableTime=17;
            var shard=new FrostShardEntity(level,owner,1,6,frost,fragile,false);shard.setPos(pos);shard.setDeltaMovement(1,2,3);shard.onHit(new EntityHitResult(target));
            var expected=new Vec3((axes&1)==0?1:-1,(axes&2)==0?2:-1.8,(axes&4)==0?3:-3).scale(.66*.5);
            test.assertTrue(shard.getDeltaMovement().distanceTo(expected)<.000001,"Entity block-coordinate reflection axes="+axes);
            test.assertTrue(target.calls==1&&target.amount==6&&target.received.getDirectEntity()==shard&&target.received.getEntity()==owner&&target.received.is(DamageTypeTags.IS_PROJECTILE),"One attributed thrown-damage call");
            var slow=target.getEffect(MobEffects.MOVEMENT_SLOWDOWN);test.assertTrue(frost==0||!accepts?slow==null:slow!=null&&slow.getDuration()==200&&slow.getAmplifier()==frost-1,"Slowness requires accepted damage");
            var impulse=accepts?new Vec3(1,2,3).scale(fragile?.1:1):Vec3.ZERO;test.assertTrue(target.getDeltaMovement().distanceTo(before.add(impulse))<.000001,"Scatter scales only newly caused knockback, preserving previous velocity");
            test.assertTrue(shard.isRemoved()==fragile&&target.invulnerableTime==(fragile&&accepts?0:17),"Fragile clears immunity only after accepted damage");
            if(fragile){shard.onHit(new EntityHitResult(target));test.assertTrue(target.calls==1,"Removed shard cannot damage twice");}
            shard.discard();target.discard();cases++;
        }
        tc4revival.TC4Revival.LOGGER.info("PASS frostEntityReflectionSlowAndKnockbackPreserveLegacyBranches: {} axis/fragility/damage-acceptance/frost cases",cases);test.succeed();
    }
}
