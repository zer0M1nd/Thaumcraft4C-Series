package emtmodern.test;
import emtmodern.ElectroMagicTools;
import emtmodern.energy.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
@GameTestHolder("emt") @PrefixGameTestTemplate(false)
public class ProcessorServerTests {
    @GameTest(templateNamespace="emt",template="test_empty")
    public static void exactProcessingBonusAndNoPowerRequirement(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,3,3));level.setBlockAndUpdate(pos,ElectroMagicTools.PROCESSOR.get().defaultBlockState());var machine=(EtherealProcessorEntity)level.getBlockEntity(pos);int prior=ProcessorConfig.BONUS.get();
        try{ProcessorConfig.BONUS.set(1);machine.setItem(0,new ItemStack(Items.IRON_ORE,4));machine.cookTime=ProcessorConfig.SPEED.get()-1;EtherealProcessorEntity.tick(level,pos,machine.getBlockState(),machine);test.assertTrue(machine.getItem(0).getCount()==3&&machine.getItem(2).is(Items.IRON_INGOT)&&machine.getItem(2).getCount()==2,"Default furnace recipe doubles with no fuel or EU");test.assertTrue(machine.getItem(1).is(tc4revival.content.TCItems.nugget(tc4revival.content.NuggetType.THAUMIUM)),"First bonus is thaumium nugget");machine.smelt();test.assertTrue(machine.getItem(1).is(ElectroMagicTools.CONTENT.get("legacy_tin_nugget").get())&&machine.getItem(1).getCount()==2,"Original second bonus metadata switches to tin, preserved explicitly");machine.setItem(0,new ItemStack(Items.IRON_ORE));machine.setItem(1,ItemStack.EMPTY);machine.smelt();test.assertTrue(machine.getItem(1).isEmpty(),"No bonus on last input is original behavior");}
        finally{ProcessorConfig.BONUS.set(prior);}
        for(var item:java.util.List.of(Items.SAND,Items.POTATO,Items.COD,Items.IRON_HELMET,Items.IRON_AXE,Items.COBBLESTONE))test.assertTrue(EtherealProcessorEntity.blocked(new ItemStack(item)),"Original anti-loop/food/tool filter "+item);
        machine.setItem(0,new ItemStack(Items.GOLD_ORE));machine.setItem(2,new ItemStack(Items.GOLD_INGOT,63));machine.cookTime=12;EtherealProcessorEntity.tick(level,pos,machine.getBlockState(),machine);test.assertTrue(machine.cookTime==0,"63-count output stops and resets progress");test.succeed();
    }
    @GameTest(templateNamespace="emt",template="test_empty")
    public static void inventoryAutomationGuiAndWrenchRemainOriginal(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,3,3));level.setBlockAndUpdate(pos,ElectroMagicTools.PROCESSOR.get().defaultBlockState());var machine=(EtherealProcessorEntity)level.getBlockEntity(pos);machine.setItem(0,new ItemStack(Items.GOLD_ORE,5));machine.cookTime=123;var saved=machine.saveWithoutMetadata();var restored=new EtherealProcessorEntity(pos,machine.getBlockState());restored.load(saved);test.assertTrue(restored.cookTime==123&&restored.getItem(0).getCount()==5,"Inventory and work survive save");
        var top=machine.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER,Direction.UP).orElseThrow(IllegalStateException::new);var side=machine.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER,Direction.EAST).orElseThrow(IllegalStateException::new);var bottom=machine.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER,Direction.DOWN).orElseThrow(IllegalStateException::new);test.assertTrue(top.getSlots()==1&&bottom.getSlots()==2&&side.getSlots()==1,"Original top input, bottom two outputs, sides bonus only");test.assertTrue(side.insertItem(0,new ItemStack(Items.GOLD_ORE),false).getCount()==1,"Side output cannot accept input");
        var player=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(level);var menu=new EtherealProcessorMenu(1,player.getInventory(),machine);test.assertTrue(menu.slots.size()==39&&menu.slots.get(0).x==56&&menu.slots.get(0).y==26&&menu.slots.get(1).x==116&&menu.slots.get(2).y==53,"Old three machine slots and36 inventory slots");test.assertTrue(ElectroMagicTools.PROCESSOR.get().getWrenchDrops(level,pos,machine.getBlockState(),machine,player,0).get(0).is(ElectroMagicTools.PROCESSOR.get().asItem()),"Wrench always returns machine");test.assertTrue(level.getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.parse("emt:ethereal_processor")).isPresent(),"Original infusion recipe present");test.succeed();
    }
}
