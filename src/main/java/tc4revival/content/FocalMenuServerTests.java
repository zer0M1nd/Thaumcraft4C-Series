package tc4revival.content;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class FocalMenuServerTests {
    @GameTest(template="wand_test_empty")
    public static void focalMenuTransfersAndRejectsInvalidContext(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,2,2));
        level.setBlock(pos,TCArcane.FOCAL_BLOCK.get().defaultBlockState(),2);
        var tile=(FocalManipulatorBlockEntity)level.getBlockEntity(pos);
        var player=new FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"FocalMenu"));
        player.setGameMode(GameType.SURVIVAL);player.moveTo(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,0,0);
        player.experienceLevel=40;player.experienceProgress=0;player.totalExperience=FocalUpgradePlan.experiencePoints(40);
        var menu=new FocalManipulatorMenu(1,player.getInventory(),tile);
        try {
            var focus=new ItemStack(TCItems.EXCAVATION_FOCUS.get());focus.getOrCreateTag().putString("audit","preserved");
            player.getInventory().setItem(9,focus.copy());
            test.assertTrue(!menu.quickMoveStack(player,1).isEmpty()&&player.getInventory().getItem(9).isEmpty()&&ItemStack.matches(tile.getItem(0),focus),"Inventory focus moves into sole focus slot preserving NBT");
            test.assertTrue(!menu.clickMenuButton(player,FocusUpgrade.FRUGAL.id)&&player.experienceLevel==40,"Inactive menu cannot start transaction");
            player.containerMenu=menu;
            test.assertTrue(!menu.clickMenuButton(player,-1)&&!menu.clickMenuButton(player,FocusUpgrade.SILK_TOUCH.id)&&player.experienceLevel==40,"Negative button and wrong-rank option rejected without charge");
            player.moveTo(pos.getX()+8.51,pos.getY()+.5,pos.getZ()+.5,0,0);
            test.assertTrue(!menu.clickMenuButton(player,FocusUpgrade.FRUGAL.id)&&menu.quickMoveStack(player,0).isEmpty(),"Out-of-range menu rejects start and transfer");
            player.moveTo(pos.getX()+8.5,pos.getY()+.5,pos.getZ()+.5,0,0);
            test.assertTrue(menu.clickMenuButton(player,FocusUpgrade.FRUGAL.id)&&player.totalExperience==FocalUpgradePlan.experiencePoints(40)-280,"Exact eight-block boundary accepts upgrade with fixed XP-point debit");
            test.assertTrue(menu.size()==800&&menu.remaining().total()==800&&menu.rank()==1&&menu.upgrade()==FocusUpgrade.FRUGAL.id,"Menu data exposes real active cost and selection");
            for(int i=0;i<36;i++)player.getInventory().setItem(i,new ItemStack(Items.COBBLESTONE,64));
            test.assertTrue(menu.quickMoveStack(player,0).isEmpty()&&tile.size()==800,"Full inventory cannot remove or cancel pending core");
            player.getInventory().setItem(9,ItemStack.EMPTY);
            test.assertTrue(!menu.quickMoveStack(player,0).isEmpty()&&tile.isEmpty()&&tile.size()==0&&"preserved".equals(player.getInventory().getItem(9).getTag().getString("audit")),"Successful shift removal preserves core and cancels upgrade");
            player.getInventory().clearContent();player.getInventory().setItem(9,new ItemStack(Items.DIRT,3));
            test.assertTrue(!menu.quickMoveStack(player,1).isEmpty()&&player.getInventory().getItem(0).getCount()==3&&tile.isEmpty(),"Nonfocus main inventory moves to hotbar, not focus slot");
            test.assertTrue(!menu.quickMoveStack(player,28).isEmpty()&&player.getInventory().getItem(9).getCount()==3,"Nonfocus hotbar moves back to main inventory");
            test.assertTrue(menu.quickMoveStack(player,-1).isEmpty()&&menu.quickMoveStack(player,37).isEmpty(),"Invalid slot indices are rejected");
            tile.clearContent();level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
            test.assertTrue(!menu.clickMenuButton(player,FocusUpgrade.FRUGAL.id),"Removed block invalidates old menu");
            tc4revival.TC4Revival.LOGGER.info("PASS focalMenuTransfersAndRejectsInvalidContext: active menu, rank, range, transfer conservation, cancellation and menu data");test.succeed();
        } finally {player.containerMenu=player.inventoryMenu;tile.clearContent();level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);}
    }

    @GameTest(template="wand_test_empty")
    public static void focalMalformedPaymentCannotApplyUpgrade(GameTestHelper test) {
        var pos=test.absolutePos(new BlockPos(2,2,2));var state=TCArcane.FOCAL_BLOCK.get().defaultBlockState();
        var base=new CompoundTag();base.put("Focus",new ItemStack(TCItems.EXCAVATION_FOCUS.get()).save(new CompoundTag()));
        base.putInt("size",800);base.putInt("rank",1);base.putInt("upgrade",FocusUpgrade.FRUGAL.id);
        tc4revival.aspect.AspectNbt.write(base,FocalManipulatorBlockEntity.cost(FocusUpgrade.FRUGAL,1));
        for(int variant=0;variant<12;variant++){
            var tag=base.copy();
            switch(variant){
                case 0 -> tag.putInt("size",-1);
                case 1 -> tag.putInt("size",799);
                case 2 -> tag.putInt("rank",0);
                case 3 -> tag.putInt("rank",6);
                case 4 -> tag.putInt("upgrade",999);
                case 5 -> tag.putString("Aspects","invalid");
                case 6 -> tag.getList("Aspects",Tag.TAG_COMPOUND).getCompound(0).putString("key","unknown");
                case 7 -> tag.getList("Aspects",Tag.TAG_COMPOUND).getCompound(0).putInt("amount",-1);
                case 8 -> tag.getList("Aspects",Tag.TAG_COMPOUND).getCompound(0).putInt("amount",201);
                case 9 -> tag.getList("Aspects",Tag.TAG_COMPOUND).getCompound(0).putString("key","fames");
                case 10 -> tag.getList("Aspects",Tag.TAG_COMPOUND).getCompound(0).putString("amount","invalid");
                case 11 -> {var focus=new ItemStack(TCItems.EXCAVATION_FOCUS.get());TCItems.EXCAVATION_FOCUS.get().applyUpgrade(focus,FocusUpgrade.POTENCY,1);tag.put("Focus",focus.save(new CompoundTag()));}
            }
            var tile=new FocalManipulatorBlockEntity(pos,state);tile.load(tag);
            test.assertTrue(tile.size()==0&&tile.remaining().isEmpty()&&tile.upgrade()==-1,"Malformed payment cancelled variant "+variant);
            test.assertTrue(tile.getItem(0).save(new CompoundTag()).equals(tag.getCompound("Focus")),"Cancellation preserves existing focus without adding an upgrade");
        }
        tc4revival.TC4Revival.LOGGER.info("PASS focalMalformedPaymentCannotApplyUpgrade: 12 malformed cost/rank/aspect/focus cases");test.succeed();
    }
}
