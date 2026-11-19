package ttmodern;

import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class KamiUtilityServerTests {
    @GameTest(template="wand_test_empty") public static void tinkererProtoclayPreservesInventoryAndModes(GameTestHelper test){
        var player=new net.minecraftforge.common.util.FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"TTClay"));var inventory=player.getInventory();inventory.selected=0;
        var pick=new ItemStack(Tinkerer.AWAKENED_PICK.get());pick.getOrCreateTag().putInt("ToolMode",2);var shovel=new ItemStack(Tinkerer.AWAKENED_SHOVEL.get());shovel.setHoverName(net.minecraft.network.chat.Component.literal("Named shovel"));shovel.enchant(net.minecraft.world.item.enchantment.Enchantments.BLOCK_FORTUNE,3);
        inventory.setItem(0,pick);inventory.setItem(20,shovel);test.assertTrue(!ProtoclayItem.select(player,Blocks.DIRT.defaultBlockState()),"No clay means no automatic switch");inventory.setItem(30,new ItemStack(Tinkerer.PROTOCLAY.get()));
        test.assertTrue(ProtoclayItem.select(player,Blocks.DIRT.defaultBlockState()),"Dirt selects the specialized shovel rather than the overlapping pickaxe");test.assertTrue(inventory.getItem(0)==shovel&&inventory.getItem(20)==pick&&pick.getTag().getInt("ToolMode")==2&&shovel.hasCustomHoverName()&&shovel.isEnchanted(),"Exchange preserves exact stacks and NBT");
        test.assertTrue(ProtoclayItem.select(player,Blocks.STONE.defaultBlockState())&&inventory.getItem(0)==pick,"Stone selects the existing pickaxe");
        test.assertTrue(!ProtoclayItem.select(player,Blocks.OAK_LOG.defaultBlockState())&&inventory.getItem(0)==pick,"Missing axe leaves inventory and the original action alone");test.succeed();
    }
    @GameTest(template="wand_test_empty") public static void tinkererCatAmuletCanDefuseIgnitedCreeper(GameTestHelper test){
        var creeper=net.minecraft.world.entity.EntityType.CREEPER.create(test.getLevel());creeper.ignite();test.assertTrue(creeper.isIgnited(),"Precondition: explicitly ignited creeper");CatAmuletItem.calm(creeper);test.assertTrue(!creeper.isIgnited()&&creeper.isAlive(),"Defusing clears manual ignition without deleting or replacing the entity");test.succeed();
    }
}
