package temodern.test;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.GenericStack;
import appeng.api.util.AEColor;
import appeng.blockentity.networking.CableBusBlockEntity;
import appeng.blockentity.storage.DriveBlockEntity;
import appeng.core.definitions.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraftforge.gametest.*;
import tc4revival.content.*;
import temodern.TE;
import temodern.essentia.*;
import temodern.machine.Machines;
import thaumcraft.api.aspects.EssentiaStorageApi;

@GameTestHolder(TE.ID) @PrefixGameTestTemplate(false)
public class ReservoirRegressionTests {
    @GameTest(template="test_empty")
    public static void pickaxeHarvestsInfusionProvider(GameTestHelper test) {
        var pos=new BlockPos(2,2,2);var level=test.getLevel();
        var player=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(level);
        var oldMode=player.gameMode.getGameModeForPlayer();var oldHand=player.getMainHandItem();
        try {
            player.setGameMode(GameType.SURVIVAL);player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_PICKAXE));
            for(var block:Machines.ALL.values())test.assertTrue(player.hasCorrectToolForDrops(block.get().defaultBlockState()),"Pickaxe harvests "+block.getId());
            test.setBlock(pos,Machines.ALL.get("infusion_provider").get());
            var state=test.getBlockState(pos);
            test.assertTrue(player.getMainHandItem().getDestroySpeed(state)>1,"Pickaxe receives its proper mining speed");
            test.assertTrue(player.gameMode.destroyBlock(test.absolutePos(pos)),"Survival mining destroys provider");
            var drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(test.absolutePos(pos)).inflate(1));
            test.assertTrue(drops.stream().mapToInt(e->e.getItem().is(Machines.ALL.get("infusion_provider").get().asItem())?e.getItem().getCount():0).sum()==1,"Survival mining drops one provider");
        } finally { player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,oldHand);player.setGameMode(oldMode); }
        test.succeed();
    }
    @GameTest(template="test_empty")
    public static void reservoirSimulatesSharedCapacityAndAllFaces(GameTestHelper test) {
        var pos=new BlockPos(2,2,2);test.setBlock(pos,TCArcane.ESSENTIA_RESERVOIR.get());
        var reservoir=(EssentiaReservoirBlockEntity)test.getBlockEntity(pos);
        var air=EssentiaKey.parse("aer");var fire=EssentiaKey.parse("ignis");
        for(var side:Direction.values()) {
            var bus=new EssentiaBus(test.getLevel(),test.absolutePos(pos),side);
            var handle=EssentiaStorageApi.atInventory(test.getLevel(),test.absolutePos(pos));
            var storage=new EssentiaStorage(handle,false,()->{});
            test.assertTrue(bus.push(air,200,Actionable.SIMULATE)==200 && reservoir.amount()==0,"Nonmutating insertion from "+side);
            test.assertTrue(bus.push(air,200,Actionable.MODULATE)==200,"All-face insertion from "+side);
            test.assertTrue(bus.push(fire,100,Actionable.SIMULATE)==56 && reservoir.amount()==200,"Types share 256-unit capacity");
            test.assertTrue(bus.push(fire,100,Actionable.MODULATE)==56 && bus.push(air,1,Actionable.MODULATE)==0,"Overflow stays with sender");
            test.assertTrue(storage.extract(air,300,Actionable.SIMULATE,IActionSource.empty())==200 && reservoir.amount()==256,"Nonmutating extraction");
            test.assertTrue(storage.extract(air,300,Actionable.MODULATE,IActionSource.empty())==200,"Exact first aspect extraction");
            test.assertTrue(storage.extract(fire,300,Actionable.MODULATE,IActionSource.empty())==56 && reservoir.amount()==0,"Exact second aspect extraction");
        }
        test.assertTrue(reservoir.connectable(Direction.DOWN)&&!reservoir.connectable(Direction.UP),"Pipe orientation is preserved");
        var stale=EssentiaStorageApi.atInventory(test.getLevel(),test.absolutePos(pos));test.getLevel().removeBlock(test.absolutePos(pos),false);
        test.assertTrue(stale.insert(air.aspect(),1,false)==0 && stale.contents().isEmpty(),"Removed reservoir handle cannot duplicate resources");
        test.succeed();
    }
    @GameTest(template="test_empty",timeoutTicks=1200)
    public static void realBusesTransferReservoirsThroughNonPipeFaces(GameTestHelper test) {
        var drivePos=new BlockPos(2,3,2);test.setBlock(drivePos,AEBlocks.DRIVE.block());test.setBlock(drivePos.west(),AEBlocks.CREATIVE_ENERGY_CELL.block());
        var drive=(DriveBlockEntity)test.getBlockEntity(drivePos);drive.getInternalInventory().setItemDirect(0,new ItemStack(TE.CELLS.get("1k").get()));
        var cablePos=drivePos.east();test.setBlock(cablePos,AEBlocks.CABLE_BUS.block());
        var cable=(CableBusBlockEntity)test.getBlockEntity(cablePos);cable.addPart(AEParts.GLASS_CABLE.item(AEColor.TRANSPARENT),null,null);
        var importer=cable.addPart(AEParts.IMPORT_BUS.asItem(),Direction.UP,null);
        var exporter=cable.addPart(AEParts.EXPORT_BUS.asItem(),Direction.DOWN,null);var air=EssentiaKey.parse("aer");exporter.getConfig().setStack(0,new GenericStack(air,1));
        test.setBlock(cablePos.above(),TCArcane.ESSENTIA_RESERVOIR.get());test.setBlock(cablePos.below(),TCArcane.ESSENTIA_RESERVOIR.get());
        var source=(EssentiaReservoirBlockEntity)test.getBlockEntity(cablePos.above());var dest=(EssentiaReservoirBlockEntity)test.getBlockEntity(cablePos.below());
        // These are NOT the pipe-facing sides. Ordinary AE buses must still see the whole inventory.
        source.face(Direction.NORTH);dest.face(Direction.NORTH);source.addToContainer(air.aspect(),32);
        test.succeedWhen(()->{
            test.assertTrue(dest.amount()==32,"Powered AE import/export delivers all 32 essentia through non-pipe faces");
            test.assertTrue(source.amount()==0 && drive.getMainNode().getGrid().getStorageService().getInventory().getAvailableStacks().get(air)==0,"No missing or duplicated essentia");
        });
    }
    @GameTest(template="test_empty",timeoutTicks=200)
    public static void realStorageBusMountsReservoir(GameTestHelper test) {
        var p=new BlockPos(2,3,2);test.setBlock(p,AEBlocks.CABLE_BUS.block());test.setBlock(p.west(),AEBlocks.CREATIVE_ENERGY_CELL.block());
        var cable=(CableBusBlockEntity)test.getBlockEntity(p);cable.addPart(AEParts.GLASS_CABLE.item(AEColor.TRANSPARENT),null,null);var bus=cable.addPart(AEParts.STORAGE_BUS.asItem(),Direction.DOWN,null);
        test.setBlock(p.below(),TCArcane.ESSENTIA_RESERVOIR.get());var reservoir=(EssentiaReservoirBlockEntity)test.getBlockEntity(p.below());reservoir.face(Direction.NORTH);
        test.runAfterDelay(100,()->{
            var storage=bus.getGridNode().getGrid().getStorageService().getInventory();var air=EssentiaKey.parse("aer");var source=IActionSource.empty();
            test.assertTrue(storage.insert(air,300,Actionable.MODULATE,source)==256 && reservoir.amount()==256,"Storage bus mounts real reservoir without cells");
            test.assertTrue(storage.extract(air,300,Actionable.MODULATE,source)==256 && reservoir.amount()==0,"Storage bus drains exact physical contents");test.succeed();
        });
    }
}
