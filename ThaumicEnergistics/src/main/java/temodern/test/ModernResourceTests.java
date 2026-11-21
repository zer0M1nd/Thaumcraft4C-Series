package temodern.test;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.StorageCells;
import appeng.api.storage.cells.CellState;
import appeng.blockentity.storage.DriveBlockEntity;
import appeng.core.definitions.AEBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
import temodern.TE;
import temodern.essentia.EssentiaKey;
import temodern.storage.EssentiaCell;

@GameTestHolder(TE.ID) @PrefixGameTestTemplate(false)
public class ModernResourceTests {
    @GameTest(template="test_empty") public static void allCellTiersExposeLiveIndicatorStates(GameTestHelper t) {
        for (var entry : TE.CELLS.entrySet()) {
            var item = (EssentiaCell) entry.getValue().get();
            var stack = new ItemStack(item);
            var cell = StorageCells.getCellInventory(stack, null);
            var air = EssentiaKey.parse("aer");
            if (item.tier().creative()) {
                t.assertTrue(cell.getStatus() == CellState.FULL, "Creative indicator stays red");
                continue;
            }
            t.assertTrue(cell.getStatus() == CellState.EMPTY, entry.getKey() + " empty indicator");
            cell.insert(air, 1, Actionable.MODULATE, IActionSource.empty());
            var partial = item.tier().types() == 1 ? CellState.TYPES_FULL : CellState.NOT_EMPTY;
            t.assertTrue(cell.getStatus() == partial, entry.getKey() + " partially used indicator");
            cell.insert(air, item.tier().capacity(), Actionable.MODULATE, IActionSource.empty());
            t.assertTrue(cell.getStatus() == CellState.FULL, entry.getKey() + " full indicator");
            t.assertTrue(StorageCells.getCellInventory(stack.copy(), null).getStatus() == CellState.FULL, "Serialized item retains full status");
            cell.extract(air, item.tier().capacity(), Actionable.MODULATE, IActionSource.empty());
            t.assertTrue(cell.getStatus() == CellState.EMPTY, entry.getKey() + " emptied indicator");
        }
        t.succeed();
    }

    @GameTest(template="test_empty", timeoutTicks=200) public static void aeDriveUsesCellStatesThroughInsertExtractAndRemoval(GameTestHelper t) {
        var pos = new BlockPos(2,2,2);
        t.setBlock(pos, AEBlocks.DRIVE.block());
        t.setBlock(pos.west(), AEBlocks.CREATIVE_ENERGY_CELL.block());
        var drive = (DriveBlockEntity)t.getBlockEntity(pos);
        drive.getInternalInventory().setItemDirect(0, new ItemStack(TE.CELLS.get("1k").get()));
        t.runAfterDelay(100, () -> {
            t.assertTrue(drive.isPowered(), "Drive powers up");
            var cell = drive.getCellInventory(0);
            t.assertTrue(drive.getCellStatus(0) == CellState.EMPTY, "Green LED before insertion");
            var aspects = tc4revival.aspect.TCAspects.catalog().all().stream().limit(12).toList();
            for (var aspect : aspects) cell.insert(EssentiaKey.of(aspect), 1, Actionable.MODULATE, IActionSource.empty());
            t.assertTrue(drive.getCellStatus(0) == CellState.TYPES_FULL, "Orange LED at type limit");
            var air = EssentiaKey.of(aspects.get(0));
            cell.insert(air, 8192, Actionable.MODULATE, IActionSource.empty());
            t.assertTrue(drive.getCellStatus(0) == CellState.FULL, "Red LED at capacity limit");
            cell.extract(air, 8192, Actionable.MODULATE, IActionSource.empty());
            t.assertTrue(drive.getCellStatus(0) == CellState.NOT_EMPTY, "Blue LED after extracting one type");
            drive.getInternalInventory().setItemDirect(0, ItemStack.EMPTY);
            t.assertTrue(drive.getCellItem(0) == null && drive.getCellStatus(0) == CellState.ABSENT, "Removing cell restores empty drive slot");
            t.succeed();
        });
    }
}
