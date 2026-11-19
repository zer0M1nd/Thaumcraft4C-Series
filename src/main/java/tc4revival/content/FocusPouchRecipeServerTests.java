package tc4revival.content;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.aspect.TCAspects;
import tc4revival.research.KnowledgeSavedData;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class FocusPouchRecipeServerTests {
    @GameTest(template="wand_test_empty")
    public static void pouchCraftConsumesBeltLeatherGoldAndThreePrimals(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,2,2));level.setBlock(pos,TCArcane.BLOCK.get().defaultBlockState(),2);var table=(ArcaneWorkbenchBlockEntity)level.getBlockEntity(pos);
        var player=new FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"PouchRecipe"));player.setGameMode(GameType.SURVIVAL);player.moveTo(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,0,0);
        var belt=ForgeRegistries.ITEMS.getValue(ResourceLocation.parse("thaumcraft:mundane_belt"));
        for(int i=0;i<9;i++)table.setItem(i,new ItemStack(i==1?Items.GOLD_INGOT:i==4?belt:Items.LEATHER));
        var wand=TCItems.CASTING_WAND.get().create(WandParts.Rod.WOOD,WandParts.Cap.GOLD,false,true);var before=wand.copy();table.setItem(10,wand);var menu=new ArcaneWorkbenchMenu(1,player.getInventory(),table);player.containerMenu=menu;
        try {
            test.assertTrue(menu.getSlot(0).getItem().isEmpty(),"Pouch recipe is locked without its research");KnowledgeSavedData.get(player.server).update(player.getUUID(),bound->0,k->{k.rememberResearch("FOCUSPOUCH");return null;});menu.refresh();
            test.assertTrue(menu.getSlot(0).getItem().is(TCItems.FOCUS_POUCH.get()),"Actual legacy ingredients match loaded recipe");menu.clicked(0,0,ClickType.PICKUP,player);
            var result=menu.getCarried();test.assertTrue(result.is(TCItems.FOCUS_POUCH.get())&&result.getCount()==1&&FocusPouchItem.contents(result).stream().allMatch(ItemStack::isEmpty),"One empty usable pouch is crafted");
            for(int slot=0;slot<9;slot++)test.assertTrue(table.getItem(slot).isEmpty(),"Seven leather, one gold and belt are consumed");
            for(var aspect:TCAspects.catalog().primals()){int cost=switch(aspect.tag()){case "terra","ordo","perditio"->1000;default->0;};test.assertTrue(TCItems.CASTING_WAND.get().vis(before,aspect)-TCItems.CASTING_WAND.get().vis(wand,aspect)==cost,"Exact three-primal ten-vis debit");}
            tc4revival.TC4Revival.LOGGER.info("PASS pouchCraftConsumesBeltLeatherGoldAndThreePrimals: loaded research recipe, exact materials, empty output and vis debit");test.succeed();
        }finally{menu.setCarried(ItemStack.EMPTY);player.containerMenu=player.inventoryMenu;table.clearContent();level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);}
    }
}
