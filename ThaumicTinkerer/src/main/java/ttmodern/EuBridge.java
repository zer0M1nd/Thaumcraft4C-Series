package ttmodern;

import java.lang.reflect.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** IC2's public locatable energy-tile API, loaded only when IC2 is installed. */
final class EuBridge {
    private static final org.slf4j.Logger LOG=com.mojang.logging.LogUtils.getLogger();
    private static Api api;private static boolean attempted,warned;
    private final InterfaceEntity link;
    private volatile Object registered,target,network;private volatile BlockEntity targetBlock;
    EuBridge(InterfaceEntity link){this.link=link;}
    private record Api(Field instance,Class<?> tile,Class<?> locatable,List<Class<?>> kinds,Method lookup,Method add,Method remove){}
    private static Api api(){
        if(!attempted){attempted=true;try{
            var tile=Class.forName("ic2.api.energy.tile.IEnergyTile");var net=Class.forName("ic2.api.energy.IEnergyNet");
            api=new Api(Class.forName("ic2.api.energy.EnergyNet").getField("instance"),tile,Class.forName("ic2.api.info.ILocatable"),List.of(Class.forName("ic2.api.energy.tile.IEnergySink"),Class.forName("ic2.api.energy.tile.IEnergySource"),Class.forName("ic2.api.energy.tile.IEnergyConductor")),net.getMethod("getSubTile",Level.class,BlockPos.class),net.getMethod("addTileUnchecked",tile),net.getMethod("removeTile",tile));
        }catch(ReflectiveOperationException|LinkageError failure){warn(failure);}}
        return api;
    }
    private static void warn(Throwable error){if(!warned){warned=true;LOG.warn("IC2 public energy API is unavailable; transvector EU forwarding is disabled",error);}}
    void refresh(){
        var world=link.getLevel();if(world==null||world.isClientSide)return;
        var api=api();if(api==null)return;
        try{
            var net=api.instance().get(null);var block=link.destination();var candidate=net==null||block==null?null:api.lookup().invoke(net,world,block.getBlockPos());
            if(candidate==registered)candidate=null;
            if(candidate==target&&block==targetBlock&&net==network)return;
            detach();if(candidate==null)return;
            var interfaces=new ArrayList<Class<?>>();interfaces.add(api.locatable());for(var kind:api.kinds())if(kind.isInstance(candidate))interfaces.add(kind);if(interfaces.size()==1)return;
            final Object resolved=candidate;final BlockEntity resolvedBlock=block;
            Object proxy=Proxy.newProxyInstance(api.tile().getClassLoader(),interfaces.toArray(Class<?>[]::new),(self,method,args)->{
                if(method.getDeclaringClass()==Object.class)return switch(method.getName()){case "equals"->self==args[0];case "hashCode"->System.identityHashCode(self);default->"TT EU interface at "+link.getBlockPos();};
                if(method.getName().equals("getWorldObj"))return world;
                if(method.getName().equals("getPosition"))return link.getBlockPos();
                if(self!=registered||resolvedBlock!=targetBlock||resolved!=target)return inactive(method,args);
                // IC2 builds connectivity on worker threads. Level#getBlockEntity deliberately returns
                // null off-thread; consulting the world there would make every proxy appear disconnected.
                // Main-thread operations validate against the world; graph workers use the volatile
                // registration snapshot, invalidated by relinking, capability unload and server ticks.
                if(world.getServer().isSameThread()&&(link.destination()!=resolvedBlock||api.lookup().invoke(network,world,resolvedBlock.getBlockPos())!=resolved))return inactive(method,args);
                try{return method.invoke(resolved,args);}catch(InvocationTargetException failure){throw failure.getCause();}
            });
            network=net;target=resolved;targetBlock=block;registered=proxy;
            try{api.add().invoke(net,proxy);}catch(ReflectiveOperationException failure){detach();throw failure;}
        }catch(ReflectiveOperationException|LinkageError failure){detach();warn(failure);}
    }
    private static Object inactive(Method method,Object[] args){
        if(method.getName().equals("injectEnergy"))return args[1]; // Return all EU unused, never swallow energy.
        var type=method.getReturnType();if(type==boolean.class)return false;if(type==int.class)return 0;if(type==double.class)return method.getName().equals("getConductionLoss")||method.getName().contains("Breakdown")||method.getName().contains("Absorption")?Double.MAX_VALUE:0d;return null;
    }
    void detach(){
        var previous=registered;var previousNetwork=network;registered=null;target=null;targetBlock=null;network=null;
        if(previous!=null&&api!=null)try{api.remove().invoke(previousNetwork,previous);}catch(ReflectiveOperationException|LinkageError failure){warn(failure);}
    }
    Object registeredTile(){return registered;}
}
