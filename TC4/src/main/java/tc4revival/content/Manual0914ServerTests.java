package tc4revival.content;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class Manual0914ServerTests {
    @GameTest(template="wand_test_empty")
    public static void boreItemRejectsInvalidInitialSupport(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(4,4,4));
        var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"BorePlacement"));
        player.moveTo(pos.getX()+.5,pos.getY()+1,pos.getZ()+2);
        for(var support:java.util.List.of(Blocks.STONE,Blocks.OAK_PLANKS,TCArcane.ARCANE_BORE_BASE.get(),
                TCArcane.woodwork("greatwood_planks"),TCArcane.woodwork("silverwood_planks"))) {
            level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);level.setBlock(pos.below(),support.defaultBlockState(),3);
            var stack=new ItemStack(TCArcane.ARCANE_BORE.get());player.setItemInHand(InteractionHand.MAIN_HAND,stack);
            var hit=new BlockHitResult(Vec3.atBottomCenterOf(pos),Direction.UP,pos.below(),false);
            var result=((BlockItem)stack.getItem()).place(new BlockPlaceContext(player,InteractionHand.MAIN_HAND,stack,hit));
            // User's 0921_1 requirement supersedes the original wooden-device loophole.
            boolean allowed=support==TCArcane.ARCANE_BORE_BASE.get();
            test.assertTrue(level.getBlockState(pos).is(TCArcane.ARCANE_BORE.get())==allowed,"Initial placement support: "+support);
            test.assertTrue(result.consumesAction()==allowed,"Placement result agrees with actual world");
            if(!allowed)test.assertTrue(stack.getCount()==1,"Rejected placement keeps the item");
        }
        for(var face:Direction.values()) {
            level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);
            var supportPos=pos.relative(face.getOpposite());
            level.setBlock(supportPos,TCArcane.ARCANE_BORE_BASE.get().defaultBlockState(),3);
            var stack=new ItemStack(TCArcane.ARCANE_BORE.get());player.setItemInHand(InteractionHand.MAIN_HAND,stack);
            var hit=new BlockHitResult(Vec3.atCenterOf(supportPos).add(face.getStepX()*.5,face.getStepY()*.5,face.getStepZ()*.5),face,supportPos,false);
            var result=((BlockItem)stack.getItem()).place(new BlockPlaceContext(player,InteractionHand.MAIN_HAND,stack,hit));
            boolean allowed=face==Direction.UP||face==Direction.DOWN;
            test.assertTrue(level.getBlockState(pos).is(TCArcane.ARCANE_BORE.get())==allowed&&result.consumesAction()==allowed,"0921_1 base top/bottom only: "+face);
            if(!allowed)test.assertTrue(stack.getCount()==1,"Rejected side mount keeps the item");
            level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);level.setBlock(supportPos,Blocks.AIR.defaultBlockState(),3);
        }
        test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void eldritchFieldSlowsAndWeakensOrdinaryEntities(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,3,3));
        var field=TCEldritch.FIELD.get();level.setBlock(pos,field.defaultBlockState(),3);
        var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"FieldContact"));
        player.moveTo(pos.getCenter());player.setDeltaMovement(1,.2,1);
        field.entityInside(field.defaultBlockState(),level,pos,player);
        test.assertTrue(player.hasEffect(MobEffects.WEAKNESS)&&player.getEffect(MobEffects.WEAKNESS).getAmplifier()==1,"Field gives weakness II");
        test.assertTrue(Math.abs(player.getDeltaMovement().x-.66)<.0001&&Math.abs(player.getDeltaMovement().y-.2)<.0001,"Field slows horizontal movement only");
        var warden=TCEntities.ELDRITCH_WARDEN.get().create(level);warden.setDeltaMovement(1,0,1);
        field.entityInside(field.defaultBlockState(),level,pos,warden);
        test.assertTrue(!warden.hasEffect(MobEffects.WEAKNESS)&&warden.getDeltaMovement().x==1,"Eldritch mobs are immune");
        test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void auraNodeUsesItsCustomBreakSound(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,3,3));
        var state=TCArcane.AURA_NODE.get().defaultBlockState();level.setBlock(pos,state,3);
        var sound=state.getSoundType(level,pos,null);
        test.assertTrue(sound.getBreakSound()==TCSounds.CRAFT_FAIL.get(),"Node destruction selects craftfail, not stone");
        test.assertTrue(sound.getHitSound()==net.minecraft.sounds.SoundEvents.EMPTY,"Stone mining sounds are suppressed");
        test.succeed();
    }
}
