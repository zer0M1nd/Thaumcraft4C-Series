package ttmodern;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.inventory.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.gametest.*;
import tc4revival.content.*;
import tc4revival.aspect.TCAspects;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class TinkererServerTests {
    @GameTest(template="wand_test_empty") public static void tinkererRepairPreviewDoesNotMutateOriginal(GameTestHelper test){
        var original=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE);original.setDamageValue(11);original.setHoverName(net.minecraft.network.chat.Component.literal("Repair sample"));var repaired=TinkersRestoration.repairedCopy(original,8);test.assertTrue(original.getDamageValue()==11&&repaired.getDamageValue()==3,"Preview changes only the copy before payment");test.assertTrue(repaired.getHoverName().getString().equals("Repair sample"),"Repair keeps custom metadata");var finished=TinkersRestoration.repairedCopy(repaired,8);test.assertTrue(finished.getDamageValue()==0&&TinkersRestoration.repairedCopy(finished,8).isEmpty(),"Fully repaired items never request another payment");test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void tinkererQuartzRecipesAndResearch(GameTestHelper test) {
        var recipe=test.getLevel().getRecipeManager().byKey(ResourceLocation.parse("thaumic_tinkerer:dark_quartz")).orElseThrow();
        var menu=new AbstractContainerMenu(null,0){public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p,int s){return ItemStack.EMPTY;} public boolean stillValid(net.minecraft.world.entity.player.Player p){return true;}};
        var grid=new TransientCraftingContainer(menu,3,3);
        for(int slot:new int[]{0,2,6,8})grid.setItem(slot,new ItemStack(Items.QUARTZ));
        grid.setItem(4,new ItemStack(Items.CHARCOAL));
        var craft=(net.minecraft.world.item.crafting.CraftingRecipe)recipe;
        test.assertTrue(craft.matches(grid,test.getLevel())&&craft.assemble(grid,test.getLevel().registryAccess()).getCount()==4,"Original 4 quartz plus charcoal recipe");
        test.assertTrue(tc4revival.research.ResearchCatalogs.catalog().find("thaumic_tinkerer:DARK_QUARTZ").isPresent(),"Addon research loaded");
        test.assertTrue(tc4revival.research.ResearchDefinitions.server().pages().require("thaumic_tinkerer:DARK_QUARTZ").size()==7,"All quartz book pages loaded");
        test.assertTrue(tc4revival.aspect.item.ItemStackAspects.resolve(new ItemStack(Tinkerer.DARK_QUARTZ.get())).orElseThrow().amount(TCAspects.catalog().require("potentia"))==1,"Legacy derived quartz aspect is exposed");
        test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void tinkererRestorerConsumesActualEssentiaAndKeepsItem(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,2,2));
        level.setBlock(pos,Tinkerer.RESTORER.get().defaultBlockState(),3);
        level.setBlock(pos.north(),TCArcane.ESSENTIA_TUBE.get().defaultBlockState(),3);
        var entity=(RestorerEntity)level.getBlockEntity(pos);var tube=(EssentiaTubeBlockEntity)level.getBlockEntity(pos.north());
        var tool=new ItemStack(Items.DIAMOND_PICKAXE);tool.setDamageValue(13);tool.setHoverName(net.minecraft.network.chat.Component.literal("Persistent tool"));entity.insertItem(tool);
        var aspect=TCAspects.catalog().require("instrumentum");test.assertTrue(tube.addEssentia(aspect,1,Direction.SOUTH)==1,"Tube primed");
        test.assertTrue(entity.inventory.extractItem(0,1,false).isEmpty(),"Automation cannot steal unfinished repairs");
        for(int i=0;i<10;i++)entity.tick();
        test.assertTrue(tool.getDamageValue()==5&&tube.essentiaAmount(Direction.SOUTH)==0,"Consumes one actual TOOL for 8 durability");
        test.assertTrue(entity.repairing()&&entity.getUpdateTag().getBoolean("Repairing"),"Successful payment lights synchronized repair runes");
        for(int i=0;i<10;i++)entity.tick();test.assertTrue(!entity.repairing()&&!entity.getUpdateTag().getBoolean("Repairing"),"Starved repair turns runes off without changing the tool");
        var tag=entity.saveWithoutMetadata();var restored=new RestorerEntity(pos,entity.getBlockState());restored.load(tag);
        test.assertTrue(ItemStack.isSameItemSameTags(tool,restored.inventory.getStackInSlot(0)),"Durability and custom NBT survive save");
        tube.addEssentia(aspect,1,Direction.SOUTH);for(int i=0;i<10;i++)entity.tick();
        test.assertTrue(tool.getDamageValue()==0&&entity.inventory.extractItem(0,1,false).getHoverName().getString().equals("Persistent tool"),"Completed item extracts without losing metadata");
        test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void tinkererOriginalJarIdentificationAndConversion(GameTestHelper test) throws Exception {
        var requirements=tc4revival.resources.AddonResourceRequirements.installed().stream().filter(r->r.owner().equals("thaumic_tinkerer")).toList();test.assertTrue(requirements.size()==1,"Tinkerer declares one original resource archive among installed addons");
        var temp=DevelopmentPaths.output(".tooling/resource-tests");java.nio.file.Files.createDirectories(temp);var game=java.nio.file.Files.createTempDirectory(temp,"tc4-addon-resources-");var directory=game.resolve("thaumcraft");java.nio.file.Files.createDirectories(directory);
        var addon=DevelopmentPaths.property("ttmodern.legacyJar");
        java.nio.file.Files.copy(addon,directory.resolve("arbitrary-name.jar"));
        var mapping=tc4revival.resources.OriginalResources.mappings();
        test.assertTrue(tc4revival.resources.OriginalResourceSelection.scan(game,mapping).usable().isEmpty(),"Addon cannot masquerade as base resources");
        var scan=tc4revival.resources.AddonResourceRequirements.scan(game,requirements);test.assertTrue(scan.missing().isEmpty()&&scan.resolved().size()==1,"Addon recognized without relying on filename");
        var prepared=tc4revival.resources.AddonResourceRequirements.prepare(game,scan.resolved().get(0));
        test.assertTrue(java.nio.file.Files.isRegularFile(prepared.directory().resolve("assets/thaumic_tinkerer/textures/item/dark_quartz.png")),"Runtime conversion prepares addon textures");
        var language=java.nio.file.Files.readString(prepared.directory().resolve("assets/thaumic_tinkerer/lang/zh_cn.json"));
        test.assertTrue(language.contains("烟黑石英"),"Original addon language mapped at runtime");
        java.nio.file.Files.copy(DevelopmentPaths.property("ttmodern.baseLegacyJar"),directory.resolve("another-name.jar"));
        test.assertTrue(tc4revival.resources.OriginalResourceSelection.scan(game,mapping).usable().size()==1,"Base selected by its own metadata and resources");
        test.succeed();
    }
}
