package ttmodern;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class AcquisitionServerTests {
    @GameTest(template="wand_test_empty")
    public static void tinkererExportRecipeAspects(GameTestHelper test) throws Exception {
        var data=new AspectExport().export();
        java.nio.file.Files.writeString(DevelopmentPaths.output("scripts/derived-aspects.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(data));
        test.assertTrue(data.getAsJsonArray("entries").size()>=12,"All registered recipe outputs receive aspects");test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void tinkererRecipeAspectsMatchPublishedData(GameTestHelper test) throws Exception {
        var data=new AspectExport().export();
        for(var element:data.getAsJsonArray("entries")) {
            var row=element.getAsJsonObject();String id=row.getAsJsonArray("items").get(0).getAsString();
            var stack=new net.minecraft.world.item.ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.parse(id)));
            var actual=tc4revival.aspect.item.ItemStackAspects.resolve(stack).orElseThrow();
            var expected=new java.util.TreeMap<String,Integer>();
            row.getAsJsonArray("aspects").forEach(a->expected.put(a.getAsJsonObject().get("aspect").getAsString(),a.getAsJsonObject().get("amount").getAsInt()));
            var direct=new java.util.TreeMap<String,Integer>();
            tc4revival.aspect.item.ItemAspects.direct(stack).orElseThrow().view().forEach((a,n)->direct.put(a.tag(),n));
            test.assertTrue(expected.equals(direct),"Raw recipe aspects differ for "+id);
            var amounts=new tc4revival.aspect.AspectAmounts();expected.forEach((tag,n)->amounts.add(tc4revival.aspect.TCAspects.catalog().require(tag),n));
            expected.clear();new tc4revival.aspect.item.StackAspectBonuses(tc4revival.aspect.TCAspects.catalog()).cull(amounts).view().forEach((a,n)->expected.put(a.tag(),n));
            var found=new java.util.TreeMap<String,Integer>();actual.view().forEach((a,n)->found.put(a.tag(),n));
            test.assertTrue(expected.equals(found),"Published aspects differ from recipe for "+id+": "+found+" expected "+expected);
        }
        test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void tinkererArcaneAcquisitionPaysAndConsumes(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new net.minecraft.core.BlockPos(2,2,2));
        level.setBlock(pos,tc4revival.content.TCArcane.BLOCK.get().defaultBlockState(),3);
        var table=(tc4revival.content.ArcaneWorkbenchBlockEntity)level.getBlockEntity(pos);
        var player=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"TinkererCraft"));player.moveTo(pos.getCenter());
        try {
            table.setItem(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE));
            table.setItem(1,new net.minecraft.world.item.ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.parse("thaumcraft:thaumium_ingot"))));
            table.setItem(2,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE));
            var wand=tc4revival.content.TCItems.CASTING_WAND.get().create(tc4revival.content.WandParts.Rod.SILVERWOOD,tc4revival.content.WandParts.Cap.GOLD,false,true);table.setItem(10,wand);
            var menu=new tc4revival.content.ArcaneWorkbenchMenu(1,player.getInventory(),table);player.containerMenu=menu;
            test.assertTrue(menu.getSlot(0).getItem().isEmpty(),"Funnel requires research");
            thaumcraft.api.ThaumcraftApiHelper.completeResearch(player,"thaumic_tinkerer:FUNNEL");menu.refresh();
            menu.clicked(0,0,net.minecraft.world.inventory.ClickType.PICKUP,player);
            test.assertTrue(menu.getCarried().is(Tinkerer.FUNNEL.get().asItem()),"Actual arcane output produced");
            for(int slot=0;slot<3;slot++)test.assertTrue(table.getItem(slot).isEmpty(),"Ingredient consumed");
            for(String tag:new String[]{"ordo","perditio"})test.assertTrue(tc4revival.content.TCItems.CASTING_WAND.get().vis(wand,tc4revival.aspect.TCAspects.catalog().require(tag))==9900,"Exact one-vis debit");
            test.succeed();
        }finally{player.containerMenu=player.inventoryMenu;level.removeBlock(pos,false);}
    }
}
