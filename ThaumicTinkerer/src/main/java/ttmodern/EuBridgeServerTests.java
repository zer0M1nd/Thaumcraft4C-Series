package ttmodern;

import java.lang.reflect.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class EuBridgeServerTests {
    @GameTest(template="wand_test_empty",timeoutTicks=120) public static void tinkererOptionalEuSourceTransfersAndUnloads(GameTestHelper test) throws Exception { transfer(test,false); }
    @GameTest(template="wand_test_empty",timeoutTicks=120) public static void tinkererOptionalEuConductorTransfersWithLoss(GameTestHelper test) throws Exception { transfer(test,true); }
    private static void transfer(GameTestHelper test,boolean conductor) throws Exception {
        if(!net.minecraftforge.fml.ModList.get().isLoaded("ic2")){test.succeed();return;}
        var api=Class.forName("ic2.api.energy.IEnergyNet");var tile=Class.forName("ic2.api.energy.tile.IEnergyTile");var located=Class.forName("ic2.api.info.ILocatable");
        var source=Class.forName("ic2.api.energy.tile.IEnergySource");var sink=Class.forName("ic2.api.energy.tile.IEnergySink");var wire=Class.forName("ic2.api.energy.tile.IEnergyConductor");
        var network=Class.forName("ic2.api.energy.EnergyNet").getField("instance").get(null);var add=api.getMethod("addTileUnchecked",tile);var remove=api.getMethod("removeTile",tile);var lookup=api.getMethod("getSubTile",Level.class,BlockPos.class);
        var world=test.getLevel();var pos=test.absolutePos(new BlockPos(2,1,2));var remote=pos.east(4);var from=conductor?pos.west():remote;var to=pos.east();
        for(var at:java.util.Set.of(pos,remote,to))world.setBlockAndUpdate(at,Blocks.CHEST.defaultBlockState());if(conductor)world.setBlockAndUpdate(from,Blocks.CHEST.defaultBlockState());
        world.setBlockAndUpdate(pos,Tinkerer.INTERFACE.get().defaultBlockState());var link=(InterfaceEntity)world.getBlockEntity(pos);double[] energy={128,0};var registered=new java.util.ArrayList<Object>();var calls=new java.util.concurrent.ConcurrentHashMap<String,Integer>();
        java.util.function.BiFunction<Class<?>,BlockPos,Object> create=(kind,at)->Proxy.newProxyInstance(tile.getClassLoader(),new Class<?>[]{kind,located},(self,method,args)->{calls.merge(kind.getSimpleName()+"."+method.getName(),1,Integer::sum);return switch(method.getName()){
            case "equals"->self==args[0];case "hashCode"->System.identityHashCode(self);case "toString"->"TT EU transfer "+at;
            case "getWorldObj"->world;case "getPosition"->at;case "getSourceTier","getSinkTier"->1;
            case "getOfferedEnergy"->Math.min(32,energy[0]);case "drawEnergy"->{energy[0]-=(double)args[0];yield null;}
            case "getDemandedEnergy"->1024d-energy[1];case "injectEnergy"->{energy[1]+=(double)args[1];yield 0d;}
            case "acceptsEnergyFrom","emitsEnergyTo"->true;case "getConductionLoss"->2d;
            case "getInsulationEnergyAbsorption","getInsulationBreakdownEnergy","getConductorBreakdownEnergy"->Double.MAX_VALUE;
            case "removeInsulation","removeConductor","onConnectionChange"->null;default->throw new UnsupportedOperationException(method.toString());
        };});
        Runnable cleanup=()->{try{world.removeBlock(pos,false);for(var entity:registered)remove.invoke(network,entity);}catch(ReflectiveOperationException e){throw new RuntimeException(e);}finally{for(var at:java.util.List.of(remote,to,from))world.removeBlock(at,false);}};
        try{for(var entity:java.util.List.of(create.apply(source,from),create.apply(sink,to))){add.invoke(network,entity);registered.add(entity);}if(conductor){var entity=create.apply(wire,remote);add.invoke(network,entity);registered.add(entity);}test.assertTrue(link.link(remote),"Energy interface links remote tile");}
        catch(Throwable failure){cleanup.run();throw failure;}
        test.runAfterDelay(30,()->{try{
            Object locatedProxy=lookup.invoke(network,world,pos);test.assertTrue(locatedProxy!=null,"Transfer interface registered before network calculation");double spent=128-energy[0];test.assertTrue(spent>0&&energy[1]>0&&energy[1]<=spent+.0001,"Actual IC2 network transports energy without duplication: "+spent+" spent / "+energy[1]+" received, connections="+api.getMethod("getAdjacentConnections",tile).invoke(network,locatedProxy)+", calls="+calls);
            test.assertTrue(conductor?spent>energy[1]:Math.abs(spent-energy[1])<.0001,"Only conductors incur their declared network loss");
            Object proxy=lookup.invoke(network,world,pos);test.assertTrue(proxy!=null,"Interface remains a registered network node");link.invalidateCaps();
            if(!conductor){test.assertTrue((double)source.getMethod("getOfferedEnergy").invoke(proxy)==0,"Unload makes retained source handle inert");double before=energy[0];source.getMethod("drawEnergy",double.class).invoke(proxy,16d);test.assertTrue(energy[0]==before,"Retained unloaded handle cannot drain the target");}
            link.reviveCaps();link.refreshDestination();test.runAfterDelay(8,()->{try{test.assertTrue(lookup.invoke(network,world,pos)!=null,"Revived interface rejoins the network");test.succeed();}catch(ReflectiveOperationException e){throw new RuntimeException(e);}finally{cleanup.run();}});
        }catch(Throwable failure){cleanup.run();throw new RuntimeException(failure);}});
    }
    @GameTest(template="wand_test_empty",timeoutTicks=80) public static void tinkererOptionalEuNetworkLinkAndInvalidation(GameTestHelper test) throws Exception {
        if(!net.minecraftforge.fml.ModList.get().isLoaded("ic2")){test.succeed();return;}
        var netApi=Class.forName("ic2.api.energy.IEnergyNet");var tile=Class.forName("ic2.api.energy.tile.IEnergyTile");var sink=Class.forName("ic2.api.energy.tile.IEnergySink");var locatable=Class.forName("ic2.api.info.ILocatable");
        var network=Class.forName("ic2.api.energy.EnergyNet").getField("instance").get(null);var add=netApi.getMethod("addTileUnchecked",tile);var remove=netApi.getMethod("removeTile",tile);var lookup=netApi.getMethod("getSubTile",Level.class,BlockPos.class);
        var world=test.getLevel();var pos=test.absolutePos(new BlockPos(2,1,2));var remote=pos.east(2);var empty=pos.west();double[] received={0};
        world.setBlockAndUpdate(remote,Blocks.CHEST.defaultBlockState());world.setBlockAndUpdate(empty,Blocks.CHEST.defaultBlockState());world.setBlockAndUpdate(pos,Tinkerer.INTERFACE.get().defaultBlockState());var link=(InterfaceEntity)world.getBlockEntity(pos);
        Object destination=Proxy.newProxyInstance(tile.getClassLoader(),new Class<?>[]{sink,locatable},(self,method,args)->switch(method.getName()){
            case "equals"->self==args[0];case "hashCode"->System.identityHashCode(self);case "toString"->"TT test EU sink";
            case "getWorldObj"->world;case "getPosition"->remote;case "getSinkTier"->2;case "getDemandedEnergy"->512d-received[0];
            case "acceptsEnergyFrom"->args[1]==Direction.NORTH;
            case "injectEnergy"->{double accepted=Math.min((double)args[1],512-received[0]);received[0]+=accepted;yield (double)args[1]-accepted;}
            default->null;
        });
        add.invoke(network,destination);test.assertTrue(link.link(remote),"EU target can be linked");
        test.runAfterDelay(8,()->{
            try{
                var forwarded=lookup.invoke(network,world,pos);test.assertTrue(forwarded!=null&&sink.isInstance(forwarded),"Proxy appears in actual IC2 network");
                var inject=sink.getMethod("injectEnergy",Direction.class,double.class,double.class);double unused=(double)inject.invoke(forwarded,Direction.NORTH,96d,32d);test.assertTrue(unused==0&&received[0]==96,"EU injection reaches linked target exactly once");
                var emitter=Class.forName("ic2.api.energy.tile.IEnergyEmitter");var accepts=Class.forName("ic2.api.energy.tile.IEnergyAcceptor").getMethod("acceptsEnergyFrom",emitter,Direction.class);test.assertTrue((boolean)accepts.invoke(forwarded,null,Direction.NORTH)&&!(boolean)accepts.invoke(forwarded,null,Direction.SOUTH),"Same-face acceptance is preserved");
                test.assertTrue(link.link(empty),"Relink to non-EU block succeeds");unused=(double)inject.invoke(forwarded,Direction.NORTH,64d,32d);test.assertTrue(unused==64&&received[0]==96,"Old handle returns all energy unused immediately after relink");
                test.runAfterDelay(8,()->{try{test.assertTrue(lookup.invoke(network,world,pos)==null,"Relink removes stale node from IC2 network");test.succeed();}catch(ReflectiveOperationException e){throw new RuntimeException(e);}finally{cleanup(world,pos,remote,empty,network,remove,destination);}});
            }catch(Throwable error){cleanup(world,pos,remote,empty,network,remove,destination);throw new RuntimeException(error);}
        });
    }
    private static void cleanup(Level world,BlockPos pos,BlockPos remote,BlockPos empty,Object network,Method remove,Object destination){try{remove.invoke(network,destination);}catch(ReflectiveOperationException e){throw new RuntimeException(e);}finally{world.removeBlock(pos,false);world.removeBlock(remote,false);world.removeBlock(empty,false);}}
}
