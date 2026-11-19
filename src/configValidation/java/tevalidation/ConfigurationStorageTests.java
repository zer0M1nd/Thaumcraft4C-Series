package tevalidation;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.*;
import temodern.*;
import temodern.essentia.EssentiaKey;
import temodern.storage.EssentiaCell;
import thaumcraft.api.config.FeatureSetting;

/** Isolated payload; exercises the installed AE2 StorageCell implementation. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public class ConfigurationStorageTests {
    @GameTest(template="wand_test_empty",templateNamespace="thaumcraft",timeoutTicks=100,batch="configuration")
    public static void disabledExtendedCellsRetainAndReleaseStoredEssentia(GameTestHelper h) {
        var a=RevivalFeatures.CELLS_256K.configured();var b=RevivalFeatures.EXTENDED_CELLS.configured();
        try {
            var key=EssentiaKey.parse("aer");var source=IActionSource.empty();
            for(String tier:new String[]{"256k","1024k","4096k","16384k","quantum","singularity"}) {
                RevivalFeatures.CELLS_256K.set(true);RevivalFeatures.EXTENDED_CELLS.set(true);
                var stack=new ItemStack(TE.CELLS.get(tier).get());
                var cell=new EssentiaCell.Inventory(stack,(EssentiaCell)stack.getItem(),null);
                h.assertTrue(cell.insert(key,64,Actionable.MODULATE,source)==64,"Could not seed "+tier);
                var saved=stack.save(new net.minecraft.nbt.CompoundTag());
                RevivalFeatures.CELLS_256K.set(false);
                h.assertTrue(!RecipeConfig.itemEnabled("essentia_cell_"+tier),"Disabled cell remains in content list "+tier);
                h.assertTrue(cell.insert(key,4,Actionable.MODULATE,source)==0,"Disabled cell accepts new contents "+tier);
                h.assertTrue(saved.equals(stack.save(new net.minecraft.nbt.CompoundTag())),"Disabling mutated existing NBT "+tier);
                var loaded=ItemStack.of(saved.copy());var reloaded=new EssentiaCell.Inventory(loaded,(EssentiaCell)loaded.getItem(),null);
                h.assertTrue(reloaded.extract(key,20,Actionable.SIMULATE,source)==20&&reloaded.total()==64,"Simulation lost contents "+tier);
                h.assertTrue(reloaded.extract(key,20,Actionable.MODULATE,source)==20&&reloaded.total()==44,"Stored contents cannot be recovered "+tier);
                RevivalFeatures.CELLS_256K.set(true);
                h.assertTrue(reloaded.insert(key,20,Actionable.MODULATE,source)==20&&reloaded.total()==64,"Re-enable cannot resume storage "+tier);
            }
            RevivalFeatures.CELLS_256K.set(false);RevivalFeatures.EXTENDED_CELLS.set(true);
            h.assertTrue(!RevivalFeatures.extended(),"Explicit child bypassed disabled parent");
            h.assertTrue(RecipeConfig.itemEnabled("essentia_cell_64k"),"Legacy storage was disabled with additions");
        } finally {RevivalFeatures.CELLS_256K.set(a);RevivalFeatures.EXTENDED_CELLS.set(b);}
        h.succeed();
    }
}
