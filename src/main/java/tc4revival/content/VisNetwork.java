package tc4revival.content;

import java.lang.ref.WeakReference;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.aspect.Aspect;
import thaumcraft.api.visnet.VisEndpoint;

/** One graph for block entities and addon parts. World ownership is weak and no query loads chunks. */
@Mod.EventBusSubscriber(modid="thaumcraft")
public class VisNetwork {
    private static class Graph {
        final Map<VisEndpoint.Key,WeakReference<VisEndpoint>> nodes=new LinkedHashMap<>();
        final Map<VisEndpoint.Key,VisEndpoint.Key> parents=new LinkedHashMap<>();
        VisEndpoint get(VisEndpoint.Key key){var ref=nodes.get(key);return ref==null?null:ref.get();}
    }
    private static final Map<Level,Graph> GRAPHS=new WeakHashMap<>();
    private VisNetwork(){}
    @SubscribeEvent public static void unload(LevelEvent.Unload event){if(event.getLevel() instanceof Level level)GRAPHS.remove(level);}
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END || event.getServer().getTickCount()%40!=0)return;
        for(var level:event.getServer().getAllLevels()) {
            var graph=GRAPHS.get(level);if(graph==null)continue;
            clean(graph);
            for(var ref:List.copyOf(graph.nodes.values())){var node=ref.get();if(live(node))connect(graph,node);}
        }
    }
    public static boolean live(VisEndpoint node){
        return node!=null && node.level()!=null && !node.level().isClientSide && node.level().hasChunkAt(node.key().position()) && node.valid();
    }
    private static void requireThread(VisEndpoint node){
        if(node.level()==null || node.level().isClientSide || node.level().getServer()==null || !node.level().getServer().isSameThread())
            throw new IllegalStateException("Vis endpoint changes require the owning server thread");
    }
    public static void register(VisEndpoint node){
        requireThread(node);if(!live(node))return;
        if(node.range()<1 || node.range()>128 || node.attunement()< -1 || node.attunement()>5)throw new IllegalArgumentException("Invalid vis endpoint range/color");
        var graph=GRAPHS.computeIfAbsent(node.level(),unused->new Graph());
        var previous=graph.get(node.key());
        if(previous!=null && previous!=node && live(previous))throw new IllegalArgumentException("Duplicate live vis endpoint "+node.key());
        if(previous!=node){detachTree(graph,node.key());graph.nodes.put(node.key(),new WeakReference<>(node));}
        clean(graph);connect(graph,node);
    }
    public static void unregister(VisEndpoint node){
        if(node.level()==null || node.level().isClientSide)return;
        requireThread(node);var graph=GRAPHS.get(node.level());
        if(graph==null || graph.get(node.key())!=node)return;
        detachTree(graph,node.key());graph.nodes.remove(node.key());
    }
    public static void refresh(VisEndpoint node){
        requireThread(node);var graph=GRAPHS.get(node.level());
        if(graph!=null && graph.get(node.key())==node)detachTree(graph,node.key());
    }
    private static void detachTree(Graph graph,VisEndpoint.Key key){
        var pending=new ArrayDeque<VisEndpoint.Key>();var seen=new HashSet<VisEndpoint.Key>();pending.add(key);
        while(!pending.isEmpty()){
            var current=pending.remove();if(!seen.add(current))continue;
            for(var edge:List.copyOf(graph.parents.entrySet()))if(edge.getValue().equals(current))pending.add(edge.getKey());
            if(graph.parents.remove(current)!=null){var node=graph.get(current);if(node!=null)node.parentChanged(null);}
        }
    }
    private static void clean(Graph graph){
        for(var key:List.copyOf(graph.nodes.keySet()))if(!live(graph.get(key))){detachTree(graph,key);graph.nodes.remove(key);}
        for(var edge:List.copyOf(graph.parents.entrySet())){
            var child=graph.get(edge.getKey());var parent=graph.get(edge.getValue());
            if(!edgeValid(child,parent))detachTree(graph,edge.getKey());
        }
    }
    private static boolean edgeValid(VisEndpoint child,VisEndpoint parent){
        if(!live(child)||!live(parent)||!parent.relay()||child.level()!=parent.level()
                ||!child.connectsTo(parent)||!parent.connectsTo(child))return false;
        double distance=child.point().distanceToSqr(parent.point());
        return !child.key().equals(parent.key()) && distance<=(double)child.range()*child.range()
            && (child.attunement()==-1||parent.attunement()==-1||child.attunement()==parent.attunement()) && visible(child,parent);
    }
    private static VisEndpoint root(Graph graph,VisEndpoint node){
        var seen=new HashSet<VisEndpoint.Key>();
        while(live(node)&&seen.add(node.key())){
            if(node.source())return node;
            var parent=graph.get(graph.parents.get(node.key()));if(!edgeValid(node,parent))return null;node=parent;
        }
        return null;
    }
    private static void connect(Graph graph,VisEndpoint child){
        if(child.source()||root(graph,child)!=null)return;
        VisEndpoint best=null;double score=Double.MAX_VALUE;
        for(var ref:graph.nodes.values()){
            var candidate=ref.get();if(!live(candidate)||candidate==child||root(graph,candidate)==null||!edgeValid(child,candidate))continue;
            double distance=child.point().distanceToSqr(candidate.point());
            double value=candidate.source()?distance-child.range()*2:distance;
            if(value<score){score=value;best=candidate;}
        }
        if(best!=null){graph.parents.put(child.key(),best.key());child.parentChanged(best);}
    }
    public static VisEndpoint parent(VisEndpoint child){
        var graph=GRAPHS.get(child.level());if(graph==null)return null;
        var parent=graph.get(graph.parents.get(child.key()));return edgeValid(child,parent)?parent:null;
    }
    public static VisEndpoint root(VisEndpoint node){var graph=GRAPHS.get(node.level());return graph==null?null:root(graph,node);}
    public static int consume(VisEndpoint leaf,Aspect aspect,int requested){
        if(!live(leaf)||requested<=0||!aspect.isPrimal())return 0;
        requireThread(leaf);var graph=GRAPHS.get(leaf.level());if(graph==null)return 0;
        var source=root(graph,leaf);if(source==null)return 0;
        int amount=Math.max(0,Math.min(requested,source.drain(aspect,requested)));
        if(amount>0){var seen=new HashSet<VisEndpoint.Key>();for(var node=leaf;node!=null&&node!=source&&seen.add(node.key());node=parent(node))node.consumed(aspect,amount);}
        return amount;
    }
    public static int drain(Level level,BlockPos consumer,Aspect aspect,int requested){
        if(level.isClientSide || !aspect.isPrimal() || requested<=0 || !level.hasChunkAt(consumer))return 0;
        var graph=GRAPHS.get(level);if(graph==null)return 0;clean(graph);
        var closest=new LinkedHashMap<VisEndpoint.Key,VisEndpoint>();var scores=new HashMap<VisEndpoint.Key,Double>();
        for(var ref:graph.nodes.values()){
            var candidate=ref.get();if(!live(candidate)||!candidate.relay())continue;var root=root(graph,candidate);if(root==null)continue;
            double distance=Vec3.atCenterOf(consumer).distanceToSqr(candidate.point());
            if(distance<=0 || distance>(double)candidate.range()*candidate.range() || distance>=scores.getOrDefault(root.key(),Double.MAX_VALUE))continue;
            scores.put(root.key(),distance);closest.put(root.key(),candidate);
        }
        int remaining=requested;
        for(var candidate:closest.values()){
            int amount=consume(candidate,aspect,remaining);remaining-=amount;
            if(amount>0)candidate.consumerDrain(consumer,aspect,amount);
            if(remaining==0)break;
        }
        return requested-remaining;
    }
    static boolean visible(VisEndpoint source,VisEndpoint target){
        if(!live(source)||!live(target)||source.level()!=target.level())return false;
        var from=source.point();var to=target.point();
        return BlockGetter.traverseBlocks(from,to,source.level(),(world,pos)->{
            if(pos.equals(source.key().position())||pos.equals(target.key().position()))return null;
            if(!world.hasChunkAt(pos))return Boolean.FALSE;
            var shape=world.getBlockState(pos).getCollisionShape(world,pos,CollisionContext.empty());
            return !shape.isEmpty()&&shape.clip(from,to,pos)!=null?Boolean.FALSE:null;
        },ignored->Boolean.TRUE);
    }
}
