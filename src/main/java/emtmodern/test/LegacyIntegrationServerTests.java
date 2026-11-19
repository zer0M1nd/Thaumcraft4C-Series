package emtmodern.test;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;
import thaumcraft.api.aspects.Aspect;

@GameTestHolder("emt") @PrefixGameTestTemplate(false)
public class LegacyIntegrationServerTests {
    @GameTest(templateNamespace="emt",template="test_empty")
    public static void originalOutputCommandGivesReadableCompleteBookToNonOperator(GameTestHelper test){
        var player=GogglesServerTests.player(test);var dispatcher=test.getLevel().getServer().getCommands().getDispatcher();var source=player.createCommandSourceStack().withPermission(0);
        try{test.assertTrue(dispatcher.execute("emt_outputs",source)==1,"Legacy command remains available without operator permission");}catch(com.mojang.brigadier.exceptions.CommandSyntaxException failure){throw new IllegalStateException(failure);}
        var book=player.getInventory().items.stream().filter(s->s.is(net.minecraft.world.item.Items.WRITTEN_BOOK)).findFirst().orElseThrow();var tag=book.getTag();test.assertTrue(net.minecraft.world.item.WrittenBookItem.makeSureTagIsValid(tag)&&tag.getString("author").equals("Tombenpotter"),"Modern written book contains valid author/title/pages");
        var pages=tag.getList("pages",net.minecraft.nbt.Tag.TAG_STRING);var all=new StringBuilder();for(int i=0;i<pages.size();i++){var page=net.minecraft.network.chat.Component.Serializer.fromJson(pages.getString(i));test.assertTrue(page!=null,"Every page is a valid component");all.append(page.getString());}
        for(var aspect:tc4revival.aspect.TCAspects.catalog().all())test.assertTrue(all.toString().contains(aspect.tag()+": "+emtmodern.energy.EmtEnergyConfig.essentiaOutput(aspect)),"Book includes live output for "+aspect.tag());test.succeed();
    }
    @GameTest(templateNamespace="emt",template="test_empty")
    public static void originalIndustrialCraftDirectAspectsLoadFromData(GameTestHelper test){
        String[] ids={"generator","water_generator","solar_generator","wind_generator","batbox","cesu","mfe","mfsu","iron_furnace","electric_furnace","macerator","extractor","compressor","iron_fence","reinforced_glass"};
        for(String id:ids){var item=ForgeRegistries.ITEMS.getValue(new ResourceLocation("ic2",id));test.assertTrue(item!=null,"Referenced IC2 item exists: "+id);var aspects=thaumcraft.api.ThaumcraftApiHelper.getObjectAspects(new ItemStack(item));test.assertTrue(aspects.size()>0,"Original IC2 object tag is queryable: "+id);}
        var mfsu=thaumcraft.api.ThaumcraftApiHelper.getObjectAspects(new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("ic2","mfsu"))));test.assertTrue(mfsu.getAmount(Aspect.ENERGY)==7&&mfsu.getAmount(Aspect.METAL)==2&&mfsu.getAmount(Aspect.MECHANISM)==1,"MFSU uses original explicit quantities, not derived crafting sums");
        var glass=thaumcraft.api.ThaumcraftApiHelper.getObjectAspects(new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("ic2","reinforced_glass"))));test.assertTrue(glass.getAmount(Aspect.COLD)==2&&glass.getAmount(Aspect.METAL)==2,"Reinforced glass uses original direct aspects");test.succeed();
    }
}
