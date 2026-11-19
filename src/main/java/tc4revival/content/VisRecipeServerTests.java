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
public  class VisRecipeServerTests {
    @GameTest(template="wand_test_empty")
    public static void relayAndChargerAcquisitionUseLegacyMaterialsAndVis(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,2,2));level.setBlock(pos,TCArcane.BLOCK.get().defaultBlockState(),2);var table=(ArcaneWorkbenchBlockEntity)level.getBlockEntity(pos);
        String[][] patterns={{"","minecraft:iron_ingot","","minecraft:iron_ingot","thaumcraft:balanced_shard","minecraft:iron_ingot","","minecraft:iron_ingot",""},{"","thaumcraft:vis_relay","","thaumcraft:wand_rod_greatwood","","thaumcraft:wand_rod_greatwood","minecraft:iron_ingot","","minecraft:iron_ingot"}};
        try {
            for(int recipe=0;recipe<2;recipe++){
                final String research=recipe==0?"VISPOWER":"VISCHARGERELAY";
                var player=new FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"VisRecipe"));player.setGameMode(GameType.SURVIVAL);player.moveTo(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,0,0);
                for(int slot=0;slot<9;slot++){var id=patterns[recipe][slot];table.setItem(slot,id.isEmpty()?ItemStack.EMPTY:new ItemStack(ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(id))));}
                var wand=TCItems.CASTING_WAND.get().create(WandParts.Rod.SILVERWOOD,WandParts.Cap.GOLD,false,true);table.setItem(10,wand);var before=wand.copy();
                var menu=new ArcaneWorkbenchMenu(1,player.getInventory(),table);player.containerMenu=menu;
                test.assertTrue(menu.getSlot(0).getItem().isEmpty(),"Specific recipe is locked without "+research);
                KnowledgeSavedData.get(player.server).update(player.getUUID(),bound->0,k->{k.rememberResearch(research);return null;});menu.refresh();
                var expected=recipe==0?TCArcane.VIS_RELAY.get().asItem():TCArcane.WORKBENCH_CHARGER.get().asItem();int count=recipe==0?2:1;
                test.assertTrue(menu.getSlot(0).getItem().is(expected)&&menu.getSlot(0).getItem().getCount()==count,"Loaded recipe has legacy output count");
                menu.clicked(0,0,ClickType.PICKUP,player);
                test.assertTrue(menu.getCarried().is(expected)&&menu.getCarried().getCount()==count,"Actual output transaction succeeds");
                for(int slot=0;slot<9;slot++)test.assertTrue(table.getItem(slot).isEmpty(),"Recipe consumes its exact ingredient cells");
                for(var aspect:TCAspects.catalog().primals()){
                    int cost=(aspect.tag().equals("ignis")||aspect.tag().equals("ordo")||recipe==1&&aspect.tag().equals("aer"))?(recipe==0?800:1600):0;
                    test.assertTrue(TCItems.CASTING_WAND.get().vis(before,aspect)-TCItems.CASTING_WAND.get().vis(wand,aspect)==cost,"Specific recipe exact debit for "+aspect.tag());
                }
                menu.setCarried(ItemStack.EMPTY);player.containerMenu=player.inventoryMenu;table.clearContent();
            }
            tc4revival.TC4Revival.LOGGER.info("PASS relayAndChargerAcquisitionUseLegacyMaterialsAndVis: two loaded recipes, research gates, exact ingredients, outputs and primal debits");test.succeed();
        }finally{table.clearContent();level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);}
    }
}
