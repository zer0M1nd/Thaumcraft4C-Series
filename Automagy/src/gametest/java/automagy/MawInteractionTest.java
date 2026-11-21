package automagy;

import automagy.content.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

@net.minecraftforge.gametest.GameTestHolder("automagy")
@net.minecraftforge.gametest.PrefixGameTestTemplate(false)
public class MawInteractionTest {
    @GameTest(template="empty",templateNamespace="automagy")
    public static void activationEditsRatherThanInsertingPaper(GameTestHelper h) {
        var relative=new BlockPos(1,2,1);
        h.setBlock(relative.below(),Blocks.CHEST);
        var block=(MawBlock)Content.FINICAL_MAW.get();
        h.setBlock(relative,block.defaultBlockState().setValue(MawBlock.FACING,Direction.UP));
        var pos=h.absolutePos(relative);
        var tile=(MawBlockEntity)h.getLevel().getBlockEntity(pos);
        var player=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"MawInteraction"));
        player.setPos(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5);
        var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false);
        try {
            for(var held:java.util.List.of(ItemStack.EMPTY,new ItemStack(Items.PAPER),new ItemStack(Content.WHITE_FILTER.get()))) {
                player.setItemInHand(InteractionHand.MAIN_HAND,held);
                h.assertTrue(block.use(block.defaultBlockState(),h.getLevel(),pos,player,InteractionHand.MAIN_HAND,hit)==InteractionResult.PASS,
                        "Empty hands and unpopulated filters must not activate the maw");
            }
            for(var item:java.util.List.of(Content.WHITE_FILTER.get(),Content.BLACK_FILTER.get())) {
                var paper=new ItemStack(item);var filter=new ItemFilter();filter.name("iron");
                paper.getOrCreateTag().put("Filter",filter.save());var before=paper.copy();
                player.setItemInHand(InteractionHand.MAIN_HAND,paper);
                h.assertTrue(block.use(block.defaultBlockState(),h.getLevel(),pos,player,InteractionHand.MAIN_HAND,hit).consumesAction(),
                        "Populated filter must activate the editor");
                h.assertTrue(ItemStack.matches(before,player.getMainHandItem()),"Activation consumed or changed the held filter");
                h.assertTrue(tile.stored(0).isEmpty(),"Activation inserted paper instead of opening the editor");
            }
        } finally {player.getInventory().clearContent();}
        h.succeed();
    }
}
