package tc4revival.content;

import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class WardingSettingsServerTests {
    private static ItemStack wand(FakePlayer player,ItemStack core) {
        var wand=TCItems.CASTING_WAND.get().create(WandParts.Rod.GREATWOOD,WandParts.Cap.GOLD,false,true);WandFocusItem.attach(wand,core);player.setItemInHand(InteractionHand.MAIN_HAND,wand);return wand;
    }
    @GameTest(template="wand_test_empty",timeoutTicks=100)
    public static void wardingActualWandRayCastHonorsPositionDelayAndExpires(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(4,20,4));var previous=level.getBlockState(pos);
        var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"WardRay"));var item=TCItems.CASTING_WAND.get();
        var stack=wand(player,new ItemStack(TCItems.WARDING_FOCUS.get()));
        player.moveTo(pos.getX()+.5,pos.getY()+.5-player.getEyeHeight(),pos.getZ()-2,0,0);
        try {
            level.setBlock(pos,Blocks.STONE.defaultBlockState(),2);item.use(level,player,InteractionHand.MAIN_HAND);
            test.assertTrue(level.getBlockEntity(pos) instanceof WardedBlockEntity,"Real wand ray hits and protects source");var ward=level.getBlockEntity(pos);var paid=stack.copy();long start=System.currentTimeMillis();
            item.use(level,player,InteractionHand.MAIN_HAND);
            test.assertTrue(level.getBlockEntity(pos)==ward&&ItemStack.matches(stack,paid),"Immediate same-position cast cannot undo protection or debit again");
            Runnable[] verify=new Runnable[1];verify[0]=()->{
                // Server catch-up may run 20 ticks in under 500ms; wait on the actual legacy clock.
                if(System.currentTimeMillis()-start<500){test.runAfterDelay(1,()->verify[0].run());return;}
                try {
                    item.use(level,player,InteractionHand.MAIN_HAND);
                    test.assertTrue(level.getBlockState(pos).is(Blocks.STONE)&&level.getBlockEntity(pos)==null&&ItemStack.matches(stack,paid),"Same actual ray restores freely after real delay");
                    tc4revival.TC4Revival.LOGGER.info("PASS wardingActualWandRayCastHonorsPositionDelayAndExpires: actual wand use/ray, immediate suppression, natural 500ms expiry and free restoration");test.succeed();
                } finally {level.setBlock(pos,previous,2);WandCooldowns.clear(player);}
            };test.runAfterDelay(1,verify[0]);
        } catch(Throwable failure){level.setBlock(pos,previous,2);WandCooldowns.clear(player);throw failure;}
    }

    @GameTest(template="wand_test_empty")
    public static void wardingUpgradeRangesAndAreaPacketCycleMatchLegacy(GameTestHelper test) {
        var focus=TCItems.WARDING_FOCUS.get();var player=new FakePlayer(test.getLevel(),new GameProfile(UUID.randomUUID(),"WardSettings"));
        var plain=new ItemStack(focus);var untouched=wand(player,plain);var before=untouched.copy();new tc4revival.network.WandAreaPacket(InteractionHand.MAIN_HAND).handle(player);
        test.assertTrue(ItemStack.matches(untouched,before),"No architect means G cannot mutate wand");
        test.assertTrue(!focus.canApplyUpgrade(plain,player,FocusUpgrade.ENLARGE,3),"Enlarge requires architect");
        for(int enlarged=0;enlarged<=3;enlarged++) {
            var core=new ItemStack(focus);focus.applyUpgrade(core,FocusUpgrade.ARCHITECT,2);for(int i=0;i<enlarged;i++)focus.applyUpgrade(core,FocusUpgrade.ENLARGE,3+i);
            var stack=wand(player,core);int maximum=3+enlarged;
            test.assertTrue(focus.maximumArea(core)==maximum&&focus.canApplyUpgrade(core,player,FocusUpgrade.ENLARGE,3),"Architect with up to three enlargements");
            for(char axis:new char[]{'x','y','z'})test.assertTrue(focus.area(stack,axis)==maximum,"Missing area defaults to maximum");
            var packet=new tc4revival.network.WandAreaPacket(InteractionHand.MAIN_HAND);
            for(int dimension=0;dimension<4;dimension++) {
                player.setShiftKeyDown(false);
                for(char axis:new char[]{'x','y','z'})stack.getOrCreateTag().putInt("area"+axis,maximum);
                test.assertTrue(focus.dimension(stack)==dimension,"Shift G cycles all/x/z/y");packet.handle(player);
                for(char axis:new char[]{'x','y','z'}) {boolean changed=dimension==0||dimension==1&&axis=='x'||dimension==2&&axis=='z'||dimension==3&&axis=='y';test.assertTrue(focus.area(stack,axis)==(changed?0:maximum),"Only selected radius wraps maximum to zero");}
                player.setShiftKeyDown(true);packet.handle(player);
            }
            player.setShiftKeyDown(false);test.assertTrue(focus.dimension(stack)==0,"Dimension wraps to all");
            stack.getOrCreateTag().putInt("areax",Integer.MAX_VALUE);stack.getOrCreateTag().putInt("areay",Integer.MIN_VALUE);stack.getOrCreateTag().putInt("aread",99);
            test.assertTrue(focus.area(stack,'x')==maximum&&focus.area(stack,'y')==0&&focus.dimension(stack)==3,"Malformed ranges clamp safely");
        }
        tc4revival.TC4Revival.LOGGER.info("PASS wardingUpgradeRangesAndAreaPacketCycleMatchLegacy: four enlargement levels, actual area packet all/x/z/y cycling, no-architect guard and bounds");test.succeed();
    }
}
