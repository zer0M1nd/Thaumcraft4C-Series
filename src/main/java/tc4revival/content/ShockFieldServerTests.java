package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class ShockFieldServerTests {
    @GameTest(template="wand_test_empty")
    public static void shockFieldContactAndRemovalFollowLegacyRandomOrder(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,10,3));var field=TCArcane.SHOCK_FIELD.get();var state=field.defaultBlockState();int removed=0;
        class Target extends Cow {
            float amount;DamageSource source;int calls;
            Target(){super(EntityType.COW,level);}
            @Override public boolean hurt(DamageSource incoming,float damage){calls++;source=incoming;amount=damage;return false;}
        }
        try {
            test.assertTrue(state.isAir()&&state.canBeReplaced()&&state.getCollisionShape(level,pos).isEmpty()&&state.getShape(level,pos).isEmpty()&&state.getLightEmission(level,pos)==8,"Legacy air-like, replaceable, empty shapes and light eight");
            for(int sample=0;sample<256;sample++) {
                long seed=sample*104729L;var oracle=RandomSource.create(seed);int damage=1+oracle.nextInt(2);boolean expires=oracle.nextInt(100)==0;
                level.setBlock(pos,state,2);var target=new Target();target.setDeltaMovement(2,-3,4);level.random.setSeed(seed);field.entityInside(state,level,pos,target);
                test.assertTrue(target.calls==1&&target.amount==damage&&target.source.is(net.minecraft.world.damagesource.DamageTypes.MAGIC),"One magic damage call, one or two points");
                test.assertTrue(target.getDeltaMovement().distanceTo(new Vec3(1.6,-3,3.2))<.000001,"Horizontal drag applies despite rejected damage, vertical velocity preserved");
                test.assertTrue(level.getBlockState(pos).is(Blocks.AIR)==expires,"One-percent removal uses random draw after damage");if(expires)removed++;target.discard();
            }
            test.assertTrue(removed>0&&removed<256,"Samples cover persistence and disappearance");
            level.setBlock(pos,state,2);field.randomTick(state,level,pos,RandomSource.create(1));test.assertTrue(level.getBlockState(pos).is(Blocks.AIR),"Random tick unconditionally removes field");
            level.setBlock(pos,state,2);field.tick(state,level,pos,RandomSource.create(1));test.assertTrue(level.getBlockState(pos).is(Blocks.AIR),"Scheduled tick unconditionally removes field");
            tc4revival.TC4Revival.LOGGER.info("PASS shockFieldContactAndRemovalFollowLegacyRandomOrder: 256 contacts, {} removals, two expiration callbacks and block properties",removed);test.succeed();
        }finally{level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);}
    }
}
