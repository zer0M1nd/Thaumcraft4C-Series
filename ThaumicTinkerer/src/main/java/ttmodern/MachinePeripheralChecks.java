package ttmodern;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import tc4revival.content.*;

/** Kept separate so the no-CC server never links optional API classes. */
public final class MachinePeripheralChecks {
    public static void transvector(GameTestHelper test) throws Exception {
        var world=test.getLevel();var pos=test.absolutePos(new BlockPos(2,1,2));var first=pos.east();var second=pos.west();
        world.setBlockAndUpdate(pos,Tinkerer.INTERFACE.get().defaultBlockState());
        world.setBlockAndUpdate(first,TCArcane.ARCANE_EAR.get().defaultBlockState());
        world.setBlockAndUpdate(second,TCArcane.ARCANE_EAR.get().defaultBlockState());
        var link=(InterfaceEntity)world.getBlockEntity(pos);test.assertTrue(link.link(first),"Initial remote link succeeds");
        var firstHandle=ComputerIntegration.lookup(link,Direction.NORTH);
        var firstEar=(MachinePeripherals.Ear)firstHandle.orElseThrow(IllegalStateException::new);firstEar.setNote(7);
        test.assertTrue(((ArcaneEarBlockEntity)world.getBlockEntity(first)).note()==7,"Forwarded operation changes remote machine");
        test.assertTrue(link.link(second)&&!firstHandle.isPresent(),"Relinking invalidates previously attached peripheral");
        var secondHandle=ComputerIntegration.lookup(link,Direction.NORTH);var secondEar=(MachinePeripherals.Ear)secondHandle.orElseThrow(IllegalStateException::new);secondEar.setNote(13);
        test.assertTrue(((ArcaneEarBlockEntity)world.getBlockEntity(first)).note()==7&&((ArcaneEarBlockEntity)world.getBlockEntity(second)).note()==13,"New link resolves distinct remote target");
        world.removeBlock(second,false);link.refreshDestination();test.assertTrue(!secondHandle.isPresent()&&!ComputerIntegration.lookup(link,Direction.NORTH).isPresent(),"Missing target detaches remote peripheral");
        world.setBlockAndUpdate(second,TCArcane.ARCANE_EAR.get().defaultBlockState());link.refreshDestination();var replacement=ComputerIntegration.lookup(link,Direction.NORTH);test.assertTrue(replacement.isPresent(),"Target replacement can attach again");
        world.removeBlock(pos,false);test.assertTrue(!replacement.isPresent(),"Removing interface invalidates forwarded handle");world.removeBlock(first,false);world.removeBlock(second,false);test.succeed();
    }
    public static void run(GameTestHelper test) throws Exception {
        var world=test.getLevel();var pos=test.absolutePos(new BlockPos(2,1,2));
        world.setBlockAndUpdate(pos,TCArcane.ESSENTIA_JAR.get().defaultBlockState());var jar=(EssentiaJarBlockEntity)world.getBlockEntity(pos);var aspect=tc4revival.aspect.TCAspects.catalog().find("aer").orElseThrow();jar.label(aspect,Direction.NORTH);
        var handle=MachinePeripherals.lookup(jar);var container=(MachinePeripherals.Container)handle.orElseThrow(IllegalStateException::new);test.assertTrue(container.getAspects().get(1).equals("aer")&&container.getAspectCount("aer")==0,"Empty labeled jar still reports its filter");jar.addToContainer(aspect,12);test.assertTrue(container.getAspectCount("aer")==12&&container.getAspectCount("nonexistent")==0,"Container returns actual count and unknown aspect zero");
        world.setBlockAndUpdate(pos,TCArcane.ESSENTIA_TUBE.get().defaultBlockState());test.assertTrue(!handle.isPresent(),"Replacing the block invalidates the peripheral handle");boolean stale=false;try{container.getAspects();}catch(dan200.computercraft.api.lua.LuaException expected){stale=true;}test.assertTrue(stale,"Previously cached method target rejects removed block");
        var pipe=(MachinePeripherals.Transport)MachinePeripherals.lookup(world.getBlockEntity(pos)).orElseThrow(IllegalStateException::new);test.assertTrue(pipe.getEssentiaType(0)==null,"Empty transport returns Lua nil rather than dereferencing a missing aspect");boolean badSide=false;try{pipe.getSuctionAmount(6);}catch(dan200.computercraft.api.lua.LuaException expected){badSide=true;}test.assertTrue(badSide,"Unknown side is rejected");
        world.setBlockAndUpdate(pos,TCArcane.ARCANE_EAR.get().defaultBlockState());var ear=(MachinePeripherals.Ear)MachinePeripherals.lookup(world.getBlockEntity(pos)).orElseThrow(IllegalStateException::new);ear.setNote(24);test.assertTrue(ear.getNote()==24,"Ear note changes through public pitch operation");ear.setNote(0);test.assertTrue(ear.getNote()==0,"Pitch wraps back to zero");boolean badNote=false;try{ear.setNote(25);}catch(dan200.computercraft.api.lua.LuaException expected){badNote=true;}test.assertTrue(badNote&&ear.getNote()==0,"Invalid pitch leaves the previous state unchanged");world.setBlockAndUpdate(pos,Tinkerer.TABLET.get().defaultBlockState());var tablet=(TabletEntity)world.getBlockEntity(pos);var tabletPeripheral=(MachinePeripherals.Tablet)MachinePeripherals.lookup(tablet).orElseThrow(IllegalStateException::new);tabletPeripheral.setLeftClick(false);tabletPeripheral.setRedstone(true);tabletPeripheral.setRotation(3);test.assertTrue(!tablet.leftClick()&&tablet.redstone()&&tablet.getBlockState().getValue(TabletBlock.FACING)==Direction.EAST,"Tablet writes update authoritative state");test.assertTrue(!tabletPeripheral.trigger(),"Empty tablet cannot be triggered");tablet.items.setStackInSlot(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK));test.assertTrue(tabletPeripheral.trigger()&&!tabletPeripheral.trigger(),"Only idle tablet accepts a single trigger");world.removeBlock(pos,false);test.succeed();
    }
}
