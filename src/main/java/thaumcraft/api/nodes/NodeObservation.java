package thaumcraft.api.nodes;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import tc4revival.content.AuraNodeBlockEntity;
import thaumcraft.api.util.ExtensionRegistry;

/** Snapshots of loaded nodes, without retaining a node or requesting any additional chunks. */
public final class NodeObservation {
    public record State(ServerLevel level,BlockPos position,String identity,int lock,int rechargeWait,long gameTime,CompoundTag saved,CompoundTag live) {
        public State {position=position.immutable();saved=saved.copy();live=live.copy();}
        @Override public CompoundTag saved(){return saved.copy();}
        @Override public CompoundTag live(){return live.copy();}
    }
    private record Observer(int interval,Consumer<State> observer) {}
    private static final ExtensionRegistry<Observer> OBSERVERS=new ExtensionRegistry<>();
    private NodeObservation() {}
    public static void observeLoadedNodes(String id,int intervalTicks,Consumer<State> observer){
        if(intervalTicks<1)throw new IllegalArgumentException("Positive node observation interval required");
        OBSERVERS.register(id,0,new Observer(intervalTicks,observer));
    }
    public static State snapshot(AuraNodeBlockEntity node){
        if(!(node.getLevel() instanceof ServerLevel level)||!level.getServer().isSameThread())throw new IllegalStateException("Server node snapshot required");
        return new State(level,node.getBlockPos(),node.nodeId(),node.lockState(),node.rechargeWaitTicks(),level.getGameTime(),node.saveWithoutMetadata(),node.getUpdateTag());
    }
    public static void afterTick(AuraNodeBlockEntity node){
        if(!(node.getLevel() instanceof ServerLevel level)||node.isRemoved())return;
        State snapshot=null;
        for(var entry:OBSERVERS.entries())if(level.getGameTime()%entry.value().interval()==0){
            if(snapshot==null)snapshot=snapshot(node);
            try{entry.value().observer().accept(snapshot);}catch(RuntimeException failure){
                org.slf4j.LoggerFactory.getLogger(NodeObservation.class).warn("Node observer failed: "+entry.id(),failure);
            }
        }
    }
}
