package automagy;

import automagy.content.Content;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;

@net.minecraftforge.gametest.GameTestHolder("automagy")
@net.minecraftforge.gametest.PrefixGameTestTemplate(false)
public class ScribeMawHarvestTest {
    @GameTest(template="empty",templateNamespace="automagy")
    public static void legacyToolsAccelerateWithoutRequiringTool(GameTestHelper h) {
        var axe = new ItemStack(Items.WOODEN_AXE);
        var pick = new ItemStack(Items.WOODEN_PICKAXE);
        var scribe = Content.SCRIBE.get().defaultBlockState();
        h.assertTrue(axe.getDestroySpeed(scribe) > pick.getDestroySpeed(scribe), "Scribe lacks axe speed bonus");
        h.assertTrue(scribe.getDestroySpeed(h.getLevel(), h.absolutePos(new BlockPos(1,1,1))) == 2.5f, "Scribe hardness differs from legacy");
        h.assertTrue(!scribe.requiresCorrectToolForDrops(), "Wood scribe unexpectedly requires a tool");
        for (var block : new net.minecraft.world.level.block.Block[]{Content.HUNGRY_MAW.get(), Content.FINICAL_MAW.get(), Content.REQUISITION_TOME.get()}) {
            var state = block.defaultBlockState();
            h.assertTrue(pick.getDestroySpeed(state) > axe.getDestroySpeed(state), "Maw lacks pickaxe speed bonus");
            h.assertTrue(!state.requiresCorrectToolForDrops(), "NiceStone must remain hand-harvestable");
        }
        var tome=Content.REQUISITION_TOME.get().defaultBlockState();
        h.assertTrue(tome.getDestroySpeed(h.getLevel(),BlockPos.ZERO)==3,"Tome hardness differs from legacy");
        h.assertTrue(tome.getBlock().getExplosionResistance()==10,"Tome blast resistance differs from legacy");
        h.assertTrue(tome.propagatesSkylightDown(h.getLevel(),BlockPos.ZERO),"Tome blocks skylight");
        h.succeed();
    }
}
