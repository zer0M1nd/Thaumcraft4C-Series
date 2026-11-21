package automagy;

import automagy.content.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.*;

@net.minecraftforge.gametest.GameTestHolder("automagy")
@net.minecraftforge.gametest.PrefixGameTestTemplate(false)
public class ScribeInteractionTest {
    @GameTest(template="empty",templateNamespace="automagy")
    public static void accessRequiresLinkAndPowerAndAllowsWand(GameTestHelper h){
        var relative=new BlockPos(1,1,1);h.setBlock(relative,Content.SCRIBE.get());
        var pos=h.absolutePos(relative);var tile=(UnseenScribeBlockEntity)h.getLevel().getBlockEntity(pos);
        var block=(UnseenScribeBlock)Content.SCRIBE.get();
        var player=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"ScribeAccess"));
        player.setPos(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5);
        var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false);
        try {
            player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            var original=player.containerMenu;
            block.use(tile.getBlockState(),h.getLevel(),pos,player,InteractionHand.MAIN_HAND,hit);
            h.assertTrue(player.containerMenu==original,"Unlinked scribe opened a menu");
            var menu=new InventariumMenu(1,player.getInventory(),pos);
            h.assertTrue(!menu.stillValid(player),"Unlinked scribe accepts menu access");
            tile.linkCore(pos.below());
            h.assertTrue(!menu.stillValid(player),"Unpowered scribe accepts menu access");
            var tag=tile.saveWithoutMetadata();tag.putInt("PoweredTicks",20);tile.load(tag);
            h.assertTrue(menu.stillValid(player),"Linked powered scribe rejects menu access");
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(tc4revival.content.TCItems.CASTING_WAND.get()));
            block.use(tile.getBlockState(),h.getLevel(),pos,player,InteractionHand.MAIN_HAND,hit);
            h.assertTrue(player.containerMenu instanceof InventariumMenu,"Wand did not open linked powered scribe");
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Content.SCRIBE_POINTER.get()));
            h.assertTrue(block.use(tile.getBlockState(),h.getLevel(),pos,player,InteractionHand.MAIN_HAND,hit)==InteractionResult.PASS,"Scribe consumed pointer placement");
            tile.load(new CompoundTag());
            h.assertTrue(!menu.stillValid(player),"Menu remains valid after power/link loss");
        } finally {player.closeContainer();player.getInventory().clearContent();}
        h.succeed();
    }
}
