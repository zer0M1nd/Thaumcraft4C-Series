package tc4revival.content;

import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class FocalInteractionServerTests {
    @GameTest(template="wand_test_empty")
    public static void focalMissingResearchConsumesInteractionAndReportsReason(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,2,2));var state=TCArcane.FOCAL_BLOCK.get().defaultBlockState();
        level.setBlock(pos,state,2);var messages=new java.util.ArrayList<Component>();
        var player=new FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"FocalFeedback")){
            @Override public void sendSystemMessage(Component message){messages.add(message.copy());}
        };
        player.moveTo(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5,0,0);
        var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false);
        try {
            for(var hand:InteractionHand.values()){
                player.setItemInHand(hand,new ItemStack(Items.DIRT,4));
                var before=player.getItemInHand(hand).copy();int count=messages.size();
                var result=state.use(level,player,hand,hit);
                test.assertTrue(result==InteractionResult.CONSUME&&messages.size()==count+1,"Missing research consumes interaction and emits one reason for each hand");
                var message=messages.get(count);
                test.assertTrue(message.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents text&&text.getKey().equals("tc.researchmissing")
                    &&message.getStyle().getColor().equals(net.minecraft.network.chat.TextColor.fromLegacyFormat(net.minecraft.ChatFormatting.RED)),"Exact legacy translation key and red styling");
                test.assertTrue(player.containerMenu==player.inventoryMenu&&ItemStack.matches(before,player.getItemInHand(hand)),"No menu opens or hand item changes");
                player.setShiftKeyDown(true);count=messages.size();
                test.assertTrue(state.use(level,player,hand,hit)==InteractionResult.PASS&&messages.size()==count,"Sneaking retains legacy bypass without warning");
                player.setShiftKeyDown(false);
            }
            tc4revival.TC4Revival.LOGGER.info("PASS focalMissingResearchConsumesInteractionAndReportsReason: both hands, translated red reason, consumed interaction and sneaking bypass");test.succeed();
        }finally{level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);}
    }
}
