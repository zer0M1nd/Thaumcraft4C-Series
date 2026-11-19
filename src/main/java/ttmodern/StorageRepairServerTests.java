package ttmodern;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class StorageRepairServerTests {
    @GameTest(template="wand_test_empty") public static void tinkererOptionalEnderStorageBindingAndInventory(GameTestHelper test) throws Exception {
        if(!EnderStorageBridge.available()){test.succeed();return;}
        var world=test.getLevel();var pos=test.absolutePos(new BlockPos(2,1,2));
        var block=ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse("enderstorage:ender_chest"));
        test.assertTrue(block!=null&&block!=net.minecraft.world.level.block.Blocks.AIR,"Actual EnderStorage chest registered");
        world.setBlockAndUpdate(pos,block.defaultBlockState());var chest=world.getBlockEntity(pos);
        var player=new net.minecraftforge.common.util.FakePlayer(world,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"TTStorage"));player.moveTo(pos.getCenter());
        var focus=new ItemStack(Tinkerer.ENDER_CHEST_FOCUS.get());var frequencyType=Class.forName("codechicken.enderstorage.api.Frequency");
        try {
            Object frequency=chest.getClass().getMethod("getFrequency").invoke(chest);
            Object isolated=frequencyType.getMethod("fromString",String.class,String.class,String.class,java.util.UUID.class,net.minecraft.network.chat.Component.class).invoke(null,"red","blue","green",player.getUUID(),player.getName());
            frequencyType.getMethod("set",frequencyType).invoke(frequency,isolated);
            test.assertTrue(EnderStorageBridge.bind(focus,chest,player)&&EnderChestFocus.bound(focus),"Focus binds a real optional chest");
            Object stored=frequencyType.getMethod("readFromStack",ItemStack.class).invoke(null,focus);
            var serialize=frequencyType.getMethod("writeToNBT",CompoundTag.class);
            test.assertTrue(serialize.invoke(frequency,new CompoundTag()).equals(serialize.invoke(stored,new CompoundTag())),"Frequency and ownership survive focus binding");
            var inventory=(net.minecraft.world.Container)chest.getClass().getMethod("getStorage").invoke(chest);
            inventory.setItem(0,new ItemStack(Items.DIAMOND,7));
            test.assertTrue(EnderStorageBridge.open(focus,player),"Actual optional inventory opens through public API");
            test.assertTrue(player.containerMenu.getSlot(0).getItem().is(Items.DIAMOND)&&player.containerMenu.getSlot(0).getItem().getCount()==7,"Opened inventory shares the chest's stored items");
            player.containerMenu.getSlot(0).remove(2);test.assertTrue(inventory.getItem(0).getCount()==5,"Focus and block use one storage, without duplication");inventory.clearContent();
            test.succeed();
        } finally {player.closeContainer();world.removeBlock(pos,false);}
    }
    @GameTest(template="wand_test_empty") public static void tinkererOptionalTinkersRepairPreservesToolData(GameTestHelper test) throws Exception {
        if(!net.minecraftforge.fml.ModList.get().isLoaded("tconstruct")){test.succeed();return;}
        var item=ForgeRegistries.ITEMS.getValue(ResourceLocation.parse("tconstruct:pickaxe"));
        var toolType=Class.forName("slimeknights.tconstruct.library.tools.nbt.ToolStack");var materialType=Class.forName("slimeknights.tconstruct.library.tools.nbt.MaterialNBT");
        var materials=new ListTag();for(int i=0;i<3;i++)materials.add(StringTag.valueOf("tconstruct:iron"));
        Object materialData=materialType.getMethod("readFromNBT",Tag.class).invoke(null,materials);
        Object definition=item.getClass().getMethod("getToolDefinition").invoke(item);
        Object tool=toolType.getMethod("createTool",Item.class,Class.forName("slimeknights.tconstruct.library.tools.definition.ToolDefinition"),materialType).invoke(null,item,definition,materialData);
        toolType.getMethod("setDamage",int.class).invoke(tool,53);var original=(ItemStack)toolType.getMethod("createStack").invoke(tool);original.setHoverName(net.minecraft.network.chat.Component.literal("Tinkers repair sample"));
        boolean previous=TinkererConfig.REPAIR_TINKERS.get();
        try {
            TinkererConfig.REPAIR_TINKERS.set(false);test.assertTrue(!TinkersRestoration.accepts(original),"Optional repairing remains disabled by default");
            TinkererConfig.REPAIR_TINKERS.set(true);test.assertTrue(TinkersRestoration.accepts(original),"Configured adapter accepts actual modifiable tools");
            var repaired=TinkersRestoration.repairedCopy(original,8);
            test.assertTrue(original.getDamageValue()==53&&repaired.getDamageValue()==45,"Public tool API repairs a copy by the paid amount");
            test.assertTrue(repaired.getHoverName().equals(original.getHoverName())&&repaired.getTag().get("tic_materials").equals(original.getTag().get("tic_materials")),"Materials and custom name survive repair");test.succeed();
        } finally {TinkererConfig.REPAIR_TINKERS.set(previous);}
    }
    private StorageRepairServerTests(){}
}
