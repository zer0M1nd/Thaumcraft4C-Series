package tc4revival.content;

import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class ShockBoundaryServerTests {
    @GameTest(template="wand_test_empty", timeoutTicks=100)
    public static void shockFieldNaturallyDamagesLivingEntity(GameTestHelper test) {
        var level=test.getLevel();var base=test.absolutePos(new BlockPos(3,12,3));
        int cx=base.getX()>>4,cz=base.getZ()>>4;boolean forced=level.setChunkForced(cx,cz,true);
        var floorState=level.getBlockState(base);var fieldState=level.getBlockState(base.above());
        var cow=EntityType.COW.create(level);
        test.runAfterDelay(20,()->{
            level.setBlock(base,Blocks.STONE.defaultBlockState(),2);level.setBlock(base.above(),TCArcane.SHOCK_FIELD.get().defaultBlockState(),2);
            // NoAI also suppresses ordinary mob travel/contact in this Minecraft version.
            cow.moveTo(Vec3.atBottomCenterOf(base.above()));level.addFreshEntity(cow);
        });
        test.runAfterDelay(23,()->{
            try {
                tc4revival.TC4Revival.LOGGER.info("Shock contact diagnostic: health={}, immunity={}, position={}, field={}, tick={}",cow.getHealth(),cow.invulnerableTime,cow.position(),level.getBlockState(base.above()),cow.tickCount);
                test.assertTrue(cow.getHealth()<cow.getMaxHealth()&&cow.getHealth()>=cow.getMaxHealth()-2,"Natural field contact applies real magic damage with vanilla immunity frames");
                test.assertTrue(cow.invulnerableTime>0,"Vanilla living hurt path established immunity timer");
                test.assertTrue(level.getBlockState(base).is(Blocks.STONE),"Air-like field preserves support");
                tc4revival.TC4Revival.LOGGER.info("PASS shockFieldNaturallyDamagesLivingEntity: world ticks, real cow damage and immunity");test.succeed();
            } finally {cow.discard();level.setBlock(base,floorState,2);level.setBlock(base.above(),fieldState,2);if(forced)level.setChunkForced(cx,cz,false);}
        });
    }

    @GameTest(template="wand_test_empty", timeoutTicks=100)
    public static void shockChainRangeUsesIntersectingBoxesRatherThanSphere(GameTestHelper test) {
        var level=test.getLevel();var base=test.absolutePos(new BlockPos(3,12,3));
        var forced=new ArrayList<ChunkPos>();
        for(int x=base.getX()>>4;x<=(base.getX()+10)>>4;x++)for(int z=base.getZ()>>4;z<=(base.getZ()+14)>>4;z++)if(level.setChunkForced(x,z,true))forced.add(new ChunkPos(x,z));
        var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"ShockCube"));player.moveTo(Vec3.atBottomCenterOf(base));player.setYRot(0);player.setXRot(0);
        test.runAfterDelay(20,()->{
            int cases=0;
            try {
                // Diagonal beyond radius eight, center outside but box inside, whole box outside.
                for(var delta:List.of(new Vec3(7.9,0,7.9),new Vec3(8.1,0,0),new Vec3(8.5,0,0))) {
                    var first=EntityType.COW.create(level);var next=EntityType.COW.create(level);
                    try {
                        first.moveTo(player.position().add(0,0,4));next.moveTo(first.position().add(delta));
                        level.addFreshEntity(first);level.addFreshEntity(next);
                        test.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,new AABB(base).inflate(25)).containsAll(List.of(first,next)),"Both fixture entities queryable");
                        var core=new ItemStack(TCItems.SHOCK_FOCUS.get());TCItems.SHOCK_FOCUS.get().applyUpgrade(core,FocusUpgrade.CHAIN_LIGHTNING,3);
                        var wand=TCItems.CASTING_WAND.get().create(WandParts.Rod.WOOD,WandParts.Cap.GOLD,false,true);WandFocusItem.attach(wand,core);player.setItemInHand(InteractionHand.MAIN_HAND,wand);WandCooldowns.clear(player);
                        test.assertTrue(FocusTargeting.pointed(player,0,20,1.1)==first,"Primary target isolated from chain candidates");
                        wand.use(level,player,InteractionHand.MAIN_HAND);TCItems.CASTING_WAND.get().onUseTick(level,player,wand,100);
                        test.assertTrue(first.getHealth()==first.getMaxHealth()-6,"Primary damage");
                        test.assertTrue(next.getHealth()==next.getMaxHealth()-(delta.x<8.5?4:0),"Eight-cube box intersection boundary "+delta);
                        cases++;
                    } finally {first.discard();next.discard();player.stopUsingItem();WandCooldowns.clear(player);}
                }
                tc4revival.TC4Revival.LOGGER.info("PASS shockChainRangeUsesIntersectingBoxesRatherThanSphere: {} diagonal/box edge cases",cases);test.succeed();
            } finally {forced.forEach(c->level.setChunkForced(c.x,c.z,false));}
        });
    }

    @GameTest(template="wand_test_empty", timeoutTicks=100)
    public static void shockFieldBuildLimitsAndCeilingsRespectPlacement(GameTestHelper test) {
        var level=test.getLevel();var base=test.absolutePos(new BlockPos(10,24,10));
        var owner=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"ShockCeiling"));int cases=0;
        for(int area:new int[]{4,6,8})for(int scene=0;scene<4;scene++) {
            int originY=scene==0?level.getMaxBuildHeight()-1:scene==1?level.getMinBuildHeight()+1:base.getY();
            var origin=new BlockPos(base.getX(),originY,base.getZ());
            var ground=scene<2?origin.below():origin.above(area);
            var saved=new HashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
            try {
                // At the world's bottom the superflat fixture has natural dirt above bedrock.
                // Isolate the volume, then restore it instead of mistaking that dirt for an empty ray.
                for(var p:BlockPos.betweenClosed(new BlockPos(base.getX()-8,Math.max(level.getMinBuildHeight(),originY-area),base.getZ()-8),new BlockPos(base.getX()+8,Math.min(level.getMaxBuildHeight()-1,originY+area+1),base.getZ()+8))) {
                    saved.put(p.immutable(),level.getBlockState(p));level.setBlock(p,Blocks.AIR.defaultBlockState(),2);
                }
                for(var p:BlockPos.betweenClosed(ground.offset(-8,0,-8),ground.offset(8,0,8)))level.setBlock(p,Blocks.STONE.defaultBlockState(),2);
                if(scene==3)for(var p:BlockPos.betweenClosed(ground.offset(-8,1,-8),ground.offset(8,1,8)))level.setBlock(p,Blocks.GLASS.defaultBlockState(),2);
                var orb=new ShockOrbEntity(level,owner,area,5);orb.setPos(Vec3.atCenterOf(origin));orb.onHit(new BlockHitResult(orb.position(),Direction.UP,ground,false));
                int fields=0;
                for(var p:BlockPos.betweenClosed(ground.offset(-8,1,-8),ground.offset(8,1,8))) {
                    if(level.getBlockState(p).is(TCArcane.SHOCK_FIELD.get()))fields++;
                    if(scene==3)test.assertTrue(level.getBlockState(p).is(Blocks.GLASS),"Occupied placement space preserved");
                }
                test.assertTrue(scene<2?fields>0:fields==0,"Valid build extremes place fields; solid ceiling or occupied top rejects them: area="+area+" scene="+scene);
                for(var p:BlockPos.betweenClosed(ground.offset(-8,0,-8),ground.offset(8,0,8)))test.assertTrue(level.getBlockState(p).is(Blocks.STONE),"Ground/ceiling preserved");
                cases++;
            } finally {saved.forEach((p,state)->level.setBlock(p,state,2));}
        }
        tc4revival.TC4Revival.LOGGER.info("PASS shockFieldBuildLimitsAndCeilingsRespectPlacement: {} area/build-limit/ceiling scenes",cases);test.succeed();
    }
}
