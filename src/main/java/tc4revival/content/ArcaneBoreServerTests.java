package tc4revival.content;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class ArcaneBoreServerTests {
    @GameTest(template="wand_test_empty")
    public static void boreOnlyMountsAboveOrBelowItsBase(GameTestHelper test){
        var center=test.absolutePos(new BlockPos(3,4,3));
        test.getLevel().setBlock(center,TCArcane.ARCANE_BORE_BASE.get().defaultBlockState(),3);
        for(var face:Direction.values()){
            var state=TCArcane.ARCANE_BORE.get().defaultBlockState().setValue(ArcaneBoreBlock.MOUNT,face);
            test.assertTrue(state.canSurvive(test.getLevel(),center.relative(face))==(face.getAxis()==Direction.Axis.Y),"Only top/bottom mounting survives: "+face);
        }
        test.getLevel().setBlock(center,Blocks.OAK_PLANKS.defaultBlockState(),3);
        test.assertTrue(!TCArcane.ARCANE_BORE.get().defaultBlockState().canSurvive(test.getLevel(),center.above()),"Planks do not replace a bore base");
        test.succeed();
    }
    @GameTest(template="wand_test_empty",timeoutTicks=160)
    public static void boreActuallyMinesAndRoutesDropsToBaseChest(GameTestHelper test) {
        var level=test.getLevel(); var pos=test.absolutePos(new BlockPos(2,4,2)); var base=pos.below();
        level.setBlock(base,TCArcane.ARCANE_BORE_BASE.get().defaultBlockState().setValue(ArcaneBoreBlock.FACING,Direction.WEST),3);
        level.setBlock(base.west(),Blocks.CHEST.defaultBlockState(),3);
        level.setBlock(pos,TCArcane.ARCANE_BORE.get().defaultBlockState().setValue(ArcaneBoreBlock.FACING,Direction.EAST),3);
        var bore=(ArcaneBoreBlockEntity)level.getBlockEntity(pos);
        bore.setItem(0,new ItemStack(TCItems.EXCAVATION_FOCUS.get())); bore.setItem(1,new ItemStack(Items.DIAMOND_PICKAXE));
        bore.work().orient(true);
        for (int y=-2;y<=2;y++) for (int z=-2;z<=2;z++) level.setBlock(pos.offset(4,y,z),Blocks.STONE.defaultBlockState(),3);
        level.setBlock(base.south(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);
        test.runAfterDelay(100,()->{
            var chest=(net.minecraft.world.Container)level.getBlockEntity(base.west());
            test.assertTrue(java.util.stream.IntStream.range(0,chest.getContainerSize()).anyMatch(slot->chest.getItem(slot).is(Items.COBBLESTONE)),"Actual server ticks mine stone and route its drops to the base chest");
            test.assertTrue(bore.pick().getDamageValue()>0,"Mining consumes the installed pick's durability");
            level.setBlock(base.south(),Blocks.AIR.defaultBlockState(),3); int damage=bore.pick().getDamageValue();
            test.runAfterDelay(20,()->{ test.assertTrue(bore.pick().getDamageValue()==damage,"Removing redstone pauses mining"); pass(test,"real-mining-and-output"); });
        });
    }
    @GameTest(template="wand_test_empty")
    public static void boreInventoryEquipmentAndStorageRules(GameTestHelper test) {
        var level=test.getLevel(); var pos=test.absolutePos(new BlockPos(2,4,2));
        level.setBlock(pos.below(),TCArcane.ARCANE_BORE_BASE.get().defaultBlockState(),3);
        level.setBlock(pos,TCArcane.ARCANE_BORE.get().defaultBlockState(),3);
        var bore=(ArcaneBoreBlockEntity)level.getBlockEntity(pos); var pick=new ItemStack(TCItems.ELEMENTAL_PICKAXE.get()); pick.enchant(Enchantments.BLOCK_EFFICIENCY,3);
        bore.setItem(0,new ItemStack(TCItems.EXCAVATION_FOCUS.get())); bore.setItem(1,pick);
        var equipment=ArcaneBoreEquipment.inspect(bore.focus(),bore.pick());
        test.assertTrue(equipment.dowsing() && equipment.speed()==3 && equipment.width()==5 && equipment.canDig(pick),"Elemental pick enables clusters and combines with the excavation focus");
        pick.setDamageValue(pick.getMaxDamage()-1);
        test.assertTrue(ArcaneBoreEquipment.inspect(bore.focus(),pick).canDig(pick),"The final durability point is usable");
        var copy=new ArcaneBoreBlockEntity(pos,bore.getBlockState()); copy.load(bore.saveWithoutMetadata());
        test.assertTrue(ItemStack.isSameItemSameTags(copy.pick(),pick) && copy.pick().getDamageValue()==pick.getDamageValue(),"Pick damage and enchantments survive storage");
        var player=bore.worker(); player.setPos(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5);
        var menu=new ArcaneBoreMenu(1,player.getInventory(),bore);
        test.assertTrue(menu.slots.size()==38 && !menu.getSlot(0).mayPlace(new ItemStack(Items.STICK)) && !menu.getSlot(1).mayPlace(new ItemStack(Items.DIAMOND_AXE)),"GUI slots restrict core and pick classes");
        test.assertTrue(!bore.canPlaceItem(0,new ItemStack(Items.STICK)),"Automation follows the same equipment restrictions as the menu");
        level.setBlock(pos.below(),Blocks.AIR.defaultBlockState(),3);
        test.assertTrue(level.isEmptyBlock(pos),"Removing the base removes the unsupported bore");
        pass(test,"inventory-equipment-storage");
    }
    private static void pass(GameTestHelper test,String name) { com.mojang.logging.LogUtils.getLogger().info("Arcane bore system test passed: {}",name); test.succeed(); }
}
