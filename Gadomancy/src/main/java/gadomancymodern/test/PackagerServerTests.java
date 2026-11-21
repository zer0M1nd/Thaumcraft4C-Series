package gadomancymodern.test;

import gadomancymodern.Gadomancy;
import gadomancymodern.automation.*;
import gadomancymodern.item.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.nbt.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("gadomancy") @PrefixGameTestTemplate(false)
public class PackagerServerTests {
    @GameTest(templateNamespace="gadomancy",template="test_empty")
    public static void configuredGlassRecipeAndPrerequisite(GameTestHelper test){
        var level=test.getLevel();var recipe=(gadomancymodern.automation.PackagerRecipe)level.getRecipeManager().byKey(new net.minecraft.resources.ResourceLocation("gadomancy","arcane_packager")).orElseThrow();
        var menu=new net.minecraft.world.inventory.AbstractContainerMenu(null,0){public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p,int slot){return ItemStack.EMPTY;}public boolean stillValid(net.minecraft.world.entity.player.Player p){return true;}};
        var grid=new net.minecraft.world.inventory.TransientCraftingContainer(menu,3,3);String[] items={"thaumcraft:greatwood_planks","minecraft:piston","thaumcraft:greatwood_planks","thaumcraft:warded_glass","gadomancy:aura_core_aer","thaumcraft:warded_glass","thaumcraft:essentia_jar","thaumcraft:arcane_workbench","thaumcraft:essentia_jar"};
        for(int i=0;i<9;i++)grid.setItem(i,new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation(items[i]))));
        boolean original=tc4revival.config.TCConfig.SERVER.wardedStone.get();
        try(var temporary=new TemporaryCoreConfig()){
            tc4revival.config.TCConfig.SERVER.wardedStone.set(true);test.assertTrue(recipe.matches(grid,level),"Default recipe needs warded glass");
            tc4revival.config.TCConfig.SERVER.wardedStone.set(false);test.assertTrue(!recipe.matches(grid,level),"Disabled warded recipe no longer accepts warded glass");grid.setItem(3,new ItemStack(net.minecraft.world.item.Items.GLASS));grid.setItem(5,new ItemStack(net.minecraft.world.item.Items.GLASS));
            test.assertTrue(recipe.matches(grid,level),"Disabled protection uses ordinary glass");test.assertTrue(recipe.displayDefinition().getAsJsonObject("key").getAsJsonObject("G").get("item").getAsString().equals("minecraft:glass"),"Display and real recipe agree");
            var materialized=thaumcraft.api.research.ResearchPrerequisites.materialize(tc4revival.research.ResearchDefinitions.server().json());var catalog=tc4revival.research.ResearchDefinitions.decode(materialized).catalog();
            test.assertTrue(!catalog.require("gadomancy:arcane_packager").parents().contains("WARDEDARCANA"),"Disabled parent does not block the research");
        }test.succeed();
    }
    @GameTest(templateNamespace="gadomancy",template="test_empty")
    public static void packagingConsumesInputsExactlyOnce(GameTestHelper test){
        var pos=test.absolutePos(new BlockPos(2,2,2));test.getLevel().setBlockAndUpdate(pos,Gadomancy.PACKAGER.get().defaultBlockState());
        var machine=(PackagerBlockEntity)test.getLevel().getBlockEntity(pos);
        ItemStack named=new ItemStack(Items.DIAMOND,17);named.setHoverName(net.minecraft.network.chat.Component.literal("preserved"));
        machine.setItem(0,named.copy());machine.setItem(1,new ItemStack(Items.STONE,64));machine.setItem(9,new ItemStack(Items.LEATHER,2));machine.setItem(10,new ItemStack(Items.STRING,3));
        test.assertTrue(machine.pack(),"Valid leather and string package");
        test.assertTrue(machine.getItem(0).isEmpty()&&machine.getItem(1).isEmpty(),"Input emptied after commit");
        test.assertTrue(machine.getItem(9).getCount()==1&&machine.getItem(10).getCount()==2,"Consume one of each material");
        var contents=PackageCodec.decode(machine.getItem(11).getTag().getByteArray("items")).orElseThrow();
        test.assertTrue(contents.stream().anyMatch(i->ItemStack.isSameItemSameTags(i,named)&&i.getCount()==17),"Count and NBT retained");
        test.assertTrue(!machine.pack(),"Occupied output cannot duplicate contents");
        machine.setItem(11,ItemStack.EMPTY);machine.setItem(0,new ItemStack(Items.EMERALD,4));machine.useEssentia=true;machine.disguise=true;
        machine.addEssentia(tc4revival.aspect.TCAspects.catalog().require("pannus"),8,Direction.UP);
        test.assertTrue(machine.pack()&&machine.cloth==4,"Alternative payment consumes exactly four cloth");
        test.assertTrue(machine.getItem(9).getCount()==1&&machine.getItem(10).getCount()==2,"Essentia mode preserves physical wrappers");
        test.assertTrue(machine.getItem(11).is(Gadomancy.DISGUISED_PACKAGE.get()),"Disguise selects fake loot bag");test.succeed();
    }
    @GameTest(templateNamespace="gadomancy",template="test_empty")
    public static void packageNestingAndMalformedData(GameTestHelper test){
        var item=(ArcanePackageItem)Gadomancy.PACKAGE.get();ItemStack first=new ItemStack(item),second=new ItemStack(item),third=new ItemStack(item);
        test.assertTrue(item.setContents(first,java.util.List.of(new ItemStack(Items.DIAMOND,2))),"First layer");
        test.assertTrue(item.setContents(second,java.util.List.of(first)),"One nested package allowed like legacy");
        test.assertTrue(!item.setContents(third,java.util.List.of(second)),"Third nesting layer rejected");
        test.assertTrue(PackageCodec.decode(new byte[]{1,2,3,4}).isEmpty(),"Corrupt compressed NBT safely rejected");
        test.assertTrue(PackageCodec.decode(new byte[PackageCodec.COMPRESSED_LIMIT+1]).isEmpty(),"Oversized data safely rejected");test.succeed();
    }
    @GameTest(templateNamespace="gadomancy",template="test_empty",timeoutTicks=100)
    public static void packagerTimingAndPersistence(GameTestHelper test){
        var pos=test.absolutePos(new BlockPos(2,2,2));test.getLevel().setBlockAndUpdate(pos,Gadomancy.PACKAGER.get().defaultBlockState());
        var machine=(PackagerBlockEntity)test.getLevel().getBlockEntity(pos);machine.setItem(0,new ItemStack(Items.GOLD_INGOT,5));machine.setItem(9,new ItemStack(Items.LEATHER));machine.setItem(10,new ItemStack(Items.STRING));machine.autoStart=true;
        test.runAfterDelay(60,()->{test.assertTrue(machine.getItem(11).is(Gadomancy.PACKAGE.get()),"Automatic machine finishes its cycle");
            var restored=new PackagerBlockEntity(pos,machine.getBlockState());restored.load(machine.saveWithoutMetadata());
            test.assertTrue(restored.autoStart&&ItemStack.isSameItemSameTags(restored.getItem(11),machine.getItem(11)),"Settings and package persist");test.succeed();});
    }
}
