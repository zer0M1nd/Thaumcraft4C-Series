package tc4revival.content;

import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class HellbatWorldServerTests {
    @GameTest(template="wand_test_empty",timeoutTicks=100)
    public static void hellbatWorldTicksBiteHealAndExplodeWithoutBreakingTerrain(GameTestHelper test) {
        var level=test.getLevel();var base=test.absolutePos(new BlockPos(5,20,5));
        var forced=new ArrayList<ChunkPos>();for(int x=(base.getX()-4)>>4;x<=(base.getX()+24)>>4;x++)for(int z=(base.getZ()-4)>>4;z<=(base.getZ()+4)>>4;z++)if(level.setChunkForced(x,z,true))forced.add(new ChunkPos(x,z));
        var owner=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"BatWorld"));owner.moveTo(Vec3.atCenterOf(base.above(12)));
        var biteTarget=EntityType.IRON_GOLEM.create(level);var bombTarget=EntityType.IRON_GOLEM.create(level);
        var vampire=new FireBatEntity(TCEntities.FIRE_BAT.get(),level);var bomb=new FireBatEntity(TCEntities.FIRE_BAT.get(),level);
        var saved=new HashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        test.runAfterDelay(20,()->{
            // Both arenas must be clear, including the one beyond the template boundary.
            for(int offset:new int[]{0,20})for(var p:BlockPos.betweenClosed(base.offset(offset-4,0,-4),base.offset(offset+4,4,4))){saved.put(p.immutable(),level.getBlockState(p));level.setBlock(p,Blocks.AIR.defaultBlockState(),2);}
            for(int offset:new int[]{0,20})for(var p:BlockPos.betweenClosed(base.offset(offset-4,-1,-4),base.offset(offset+4,-1,4))){saved.put(p.immutable(),level.getBlockState(p));level.setBlock(p,Blocks.STONE.defaultBlockState(),2);}
            var glass=base.offset(22,0,0);saved.putIfAbsent(glass,level.getBlockState(glass));level.setBlock(glass,Blocks.GLASS.defaultBlockState(),2);
            biteTarget.moveTo(Vec3.atBottomCenterOf(base.east()));bombTarget.moveTo(Vec3.atBottomCenterOf(base.east(21)));
            biteTarget.setNoAi(true);bombTarget.setNoAi(true);level.addFreshEntity(biteTarget);level.addFreshEntity(bombTarget);
            vampire.summon(owner,biteTarget,0,false,true,true);vampire.setHealth(3);vampire.moveTo(Vec3.atBottomCenterOf(base));level.addFreshEntity(vampire);
            bomb.summon(owner,bombTarget,2,true,false,false);bomb.moveTo(Vec3.atBottomCenterOf(base.east(20)));level.addFreshEntity(bomb);
        });
        test.runAfterDelay(28,()->{
            try {
                test.assertTrue(vampire.tickCount>0&&!vampire.isRemoved()&&biteTarget.getHealth()==biteTarget.getMaxHealth()-3,"World AI performs devil-vampire bite using actual damage");
                test.assertTrue(vampire.getHealth()==4,"Natural vampire attack heals one health");
                var effect=owner.getEffect(MobEffects.REGENERATION);test.assertTrue(effect!=null&&effect.getAmplifier()==1&&effect.getDuration()>0,"Natural attack grants owner regeneration");
                test.assertTrue(bomb.isRemoved()&&bombTarget.getHealth()<bombTarget.getMaxHealth(),"World AI triggers real explosion and hurts actual nearby golem: removed="+bomb.isRemoved()+", ticks="+bomb.tickCount+", bomb="+bomb.position()+", target="+bombTarget.position()+", health="+bombTarget.getHealth()+"/"+bombTarget.getMaxHealth()+", ticking="+level.isPositionEntityTicking(bomb.blockPosition()));
                test.assertTrue(level.getBlockState(base.offset(22,0,0)).is(Blocks.GLASS),"Actual explosion preserves nearby glass");
                for(int offset:new int[]{0,20})for(var p:BlockPos.betweenClosed(base.offset(offset-4,-1,-4),base.offset(offset+4,-1,4)))test.assertTrue(level.getBlockState(p).is(Blocks.STONE),"Explosion preserves ground");
                tc4revival.TC4Revival.LOGGER.info("PASS hellbatWorldTicksBiteHealAndExplodeWithoutBreakingTerrain: natural AI, real bite/regeneration/explosion and terrain preservation");test.succeed();
            } finally {vampire.discard();bomb.discard();biteTarget.discard();bombTarget.discard();owner.removeAllEffects();saved.forEach((p,state)->level.setBlock(p,state,2));forced.forEach(c->level.setChunkForced(c.x,c.z,false));}
        });
    }

    @GameTest(template="wand_test_empty",timeoutTicks=100)
    public static void hellbatReloadLosesOwnerAndTargetAndExpiresNaturally(GameTestHelper test) {
        var level=test.getLevel();var base=test.absolutePos(new BlockPos(5,20,5));var pos=Vec3.atCenterOf(base);
        int cx=base.getX()>>4,cz=base.getZ()>>4;boolean forced=level.setChunkForced(cx,cz,true);
        var owner=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"BatReload"));owner.moveTo(pos.add(0,10,0));var target=EntityType.COW.create(level);target.moveTo(pos.add(1,0,0));
        var original=new FireBatEntity(TCEntities.FIRE_BAT.get(),level);original.summon(owner,target,2,false,true,true);original.moveTo(pos);
        var tag=new net.minecraft.nbt.CompoundTag();original.saveWithoutId(tag);var restored=new FireBatEntity(TCEntities.FIRE_BAT.get(),level);restored.load(tag);
        test.assertTrue(restored.getTarget()==null,"Newly loaded bat has no persisted target");
        // Supply a new target for one isolated attack; the old vampire owner must still be absent.
        restored.setTarget(target);restored.setHealth(3);restored.customServerAiStep();
        test.assertTrue(restored.getHealth()==4&&!owner.hasEffect(MobEffects.REGENERATION),"Loaded vampire heals itself but does not retain owner");
        restored.setTarget(null);restored.setHealth(5);
        test.runAfterDelay(20,()->{restored.moveTo(pos);level.addFreshEntity(restored);});
        test.runAfterDelay(75,()->{
            try {test.assertTrue(!restored.isAlive(),"Targetless summoned bat dies through natural world damage/immunity ticks");tc4revival.TC4Revival.LOGGER.info("PASS hellbatReloadLosesOwnerAndTargetAndExpiresNaturally: NBT owner/target loss and natural summoned expiry");test.succeed();}
            finally {restored.discard();original.discard();target.discard();owner.removeAllEffects();if(forced)level.setChunkForced(cx,cz,false);}
        });
    }
}
