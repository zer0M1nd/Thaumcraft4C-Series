package tc4revival.content;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.TCAspects;
import tc4revival.research.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class ThaumometerLifecycleServerTests {
    @GameTest(template="wand_test_empty",timeoutTicks=50)
    public static void thaumometerInvalidContextsCannotResumeWithoutNewUse(GameTestHelper test) {
        var level=test.getLevel();var target=test.absolutePos(new BlockPos(2,3,2));
        level.setBlock(target,Blocks.DIRT.defaultBlockState(),2);
        var players=new ArrayList<FakePlayer>();var stacks=new ArrayList<ItemStack>();var before=new ArrayList<net.minecraft.nbt.CompoundTag>();
        String[] cases={"zero-health","spectator","logout-event","changed-hand","stopped-use"};
        var data=KnowledgeSavedData.get(level.getServer());
        for(var name:cases) {
            var player=new FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"ScanLifecycle"));
            player.setGameMode(GameType.SURVIVAL);player.setPos(target.getX()+.5,target.getY()-1,target.getZ()+3.5);player.setYRot(180);player.setXRot(0);
            data.ensure(player.getUUID(),bound->0);
            var selected=ScanTargetSelector.resolve(player).orElseThrow();
            test.assertTrue(selected.complete(data.snapshot(player.getUUID()).orElseThrow(),TCAspects.catalog(),PlayerKnowledge.ScanMode.THAUMOMETER,100).succeeded(),"Real ray target is independently rewardable: "+name);
            var stack=new ItemStack(TCItems.THAUMOMETER.get());player.setItemInHand(InteractionHand.MAIN_HAND,stack);
            players.add(player);stacks.add(stack);
        }
        test.runAtTickTime(1,()->{
            for(int i=0;i<players.size();i++) {
                var player=players.get(i);stacks.get(i).getItem().use(level,player,InteractionHand.MAIN_HAND);
                before.add(KnowledgeNbt.write(data.snapshot(player.getUUID()).orElseThrow()));
            }
        });
        for(int elapsed=1;elapsed<=22;elapsed++) {
            int tick=elapsed;
            test.runAtTickTime(elapsed+1,()->{
                for(int i=0;i<players.size();i++) {
                    var player=players.get(i);var stack=stacks.get(i);
                    if(tick==10) switch(i) {
                        case 0 -> player.setHealth(0);
                        case 1 -> player.setGameMode(GameType.SPECTATOR);
                        case 2 -> MinecraftForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(player));
                        case 3 -> {player.stopUsingItem();player.setItemInHand(InteractionHand.OFF_HAND,stack);player.startUsingItem(InteractionHand.OFF_HAND);}
                        case 4 -> player.stopUsingItem();
                    }
                    // Leave the invalid context across a real END tick to exercise registered cleanup.
                    if(tick==11) {
                        player.setHealth(player.getMaxHealth());player.setGameMode(GameType.SURVIVAL);player.stopUsingItem();
                        player.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);player.setItemInHand(InteractionHand.MAIN_HAND,stack);player.startUsingItem(InteractionHand.MAIN_HAND);
                    }
                    if(tick!=10)stack.getItem().onUseTick(level,player,stack,25-tick);
                    test.assertTrue(before.get(i).equals(KnowledgeNbt.write(data.snapshot(player.getUUID()).orElseThrow())),"Invalidated use cannot reward after context restored: "+cases[i]+" tick "+tick);
                }
                if(tick==22) {
                    for(var player:players){ThaumometerUse.cancel(player);player.stopUsingItem();}
                    level.setBlock(target,Blocks.AIR.defaultBlockState(),2);
                    tc4revival.TC4Revival.LOGGER.info("PASS thaumometerInvalidContextsCannotResumeWithoutNewUse: five actual ray/item contexts and registered END/logout cleanup");test.succeed();
                }
            });
        }
    }
}
