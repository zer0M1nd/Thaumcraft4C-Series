package automagy.content;

import automagy.logic.SignalGraph;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import java.util.*;

/** Server-only connected-component solving, with virtual vanilla wires to exclude feedback as an external source. */
public final class RedcrystalNetwork {
    private static final ThreadLocal<Boolean> SAMPLING = ThreadLocal.withInitial(() -> false);
    private static final int MAX_NODES = 16384;
    public static boolean sampling() { return SAMPLING.get(); }
    private record Contact(BlockPos target, Direction out, Direction in, int cap) {}
    private static boolean node(ServerLevel level, BlockPos pos) {
        return level.hasChunkAt(pos) && (level.getBlockState(pos).getBlock() instanceof RedcrystalBlock || level.getBlockState(pos).is(Blocks.REDSTONE_WIRE));
    }
    private static List<Contact> contacts(ServerLevel level, BlockPos pos) {
        var result = new ArrayList<Contact>();
        Direction face = level.getBlockEntity(pos) instanceof RedcrystalBlockEntity be ? be.face() : Direction.UP;
        for (Direction d : Direction.values()) {
            BlockPos next = pos.relative(d);
            if (node(level, next)) result.add(new Contact(next, d, d.getOpposite(), 32767));
            // Strong power through one solid block is capped to vanilla redstone strength.
            if (level.hasChunkAt(next) && level.getBlockState(next).isRedstoneConductor(level,next))
                for (Direction edge : Direction.values()) {
                    BlockPos far = next.relative(edge);
                    if (!far.equals(pos) && node(level,far)) result.add(new Contact(far,d,edge.getOpposite(),15));
                }
            if (d.getAxis() == face.getAxis()) continue;
            // Stair steps along a mounted surface, with an open path over the intervening edge.
            if (!level.hasChunkAt(next)) continue;
            if (level.getBlockState(next).isRedstoneConductor(level,next)) {
                BlockPos raised = next.relative(face), clearance=pos.relative(face);
                if (node(level, raised) && !level.getBlockState(clearance).isRedstoneConductor(level,clearance))
                    result.add(new Contact(raised, d, d.getOpposite(),32767));
            } else {
                BlockPos lowered = next.relative(face.getOpposite());
                if (node(level, lowered)) result.add(new Contact(lowered,d,d.getOpposite(),32767));
            }
        }
        if (level.getBlockEntity(pos) instanceof RedcrystalBlockEntity be && be.kind().equals("res")) {
            for (int distance=1; distance<=15; distance++) {
                BlockPos target = pos.relative(face,distance);
                if (!level.hasChunkAt(target)) break;
                if (level.getBlockEntity(target) instanceof RedcrystalBlockEntity remote && remote.kind().equals("res")) {
                    if (remote.face()==face.getOpposite()) result.add(new Contact(target,face,face.getOpposite(),32767));
                    if (remote.face().getAxis()==face.getAxis()) break;
                }
            }
        }
        return result;
    }
    private static boolean accepts(ServerLevel level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof RedcrystalBlockEntity be ? be.input(direction)
                : true;
    }
    private static boolean sends(ServerLevel level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof RedcrystalBlockEntity be ? be.output(direction)
                : direction!=Direction.UP;
    }
    private static int independentSignal(ServerLevel level, BlockPos pos, Direction toward) {
        if (!level.hasChunkAt(pos) || node(level,pos)) return 0;
        var state = level.getBlockState(pos);
        int strength = level.getBlockEntity(pos) instanceof automagy.api.AmplifiedSignal enhanced
                ? enhanced.amplifiedSignal(toward.getOpposite()) : Math.min(15,state.getSignal(level,pos,toward));
        if (state.isRedstoneConductor(level,pos)) for (Direction d : Direction.values()) {
            BlockPos source=pos.relative(d);
            if (level.hasChunkAt(source) && !node(level,source))
                strength=Math.max(strength,level.getBlockState(source).getDirectSignal(level,source,d));
        }
        return Math.max(0,Math.min(32767,strength));
    }
    private record GraphContact(GlobalPos target,Direction out,Direction in,int cap,boolean wireless) {}
    private static List<GraphContact> graphContacts(ServerLevel level,BlockPos pos){
        var result=new ArrayList<GraphContact>();
        var tile=level.getBlockEntity(pos);
        for(var edge:contacts(level,pos)){
            boolean wireless=tile instanceof RedcrystalBlockEntity be&&be.kind().equals("res")&&edge.out==be.face()
                &&level.getBlockEntity(edge.target) instanceof RedcrystalBlockEntity other&&other.kind().equals("res")&&other.face()==be.face().getOpposite();
            result.add(new GraphContact(GlobalPos.of(level.dimension(),edge.target),edge.out,edge.in,edge.cap,wireless));
        }
        if(tile instanceof RedcrystalBlockEntity be&&be.kind().equals("merc"))for(var remote:MirrorSignalLinks.contacts(level,be))result.add(new GraphContact(remote,null,null,32767,true));
        return result;
    }
    public static void update(ServerLevel origin,BlockPos start){
        if(sampling())return;
        var component=new LinkedHashMap<GlobalPos,List<GraphContact>>();var seen=new HashSet<GlobalPos>();var queue=new ArrayDeque<GlobalPos>();
        var initial=GlobalPos.of(origin.dimension(),start);queue.add(initial);seen.add(initial);
        while(!queue.isEmpty()&&component.size()<MAX_NODES){
            var key=queue.removeFirst();var world=origin.getServer().getLevel(key.dimension());if(world==null||!node(world,key.pos()))continue;
            var contacts=graphContacts(world,key.pos());component.put(key,contacts);
            for(var edge:contacts)if(seen.add(edge.target))queue.addLast(edge.target);
        }
        var graph=new SignalGraph<GlobalPos>();var sources=new HashMap<GlobalPos,Integer>();SAMPLING.set(true);
        try{
            for(var entry:component.entrySet()){
                var key=entry.getKey();var level=origin.getServer().getLevel(key.dimension());var pos=key.pos();int source=0;
                for(Direction direction:Direction.values())if(accepts(level,pos,direction)||level.getBlockState(pos).is(Blocks.REDSTONE_WIRE))source=Math.max(source,independentSignal(level,pos.relative(direction),direction));
                sources.put(key,source);
                if(level.getBlockEntity(pos) instanceof RedcrystalBlockEntity be)graph.node(key,source,value->SignalGraph.transform(be.kind(),be.setting,value));
                else graph.node(key,source,value->Math.min(15,value));
                for(var edge:entry.getValue()){
                    if(!component.containsKey(edge.target))continue;
                    var remote=origin.getServer().getLevel(edge.target.dimension());
                    if(edge.wireless||sends(level,pos,edge.out)&&accepts(remote,edge.target.pos(),edge.in))graph.edge(key,edge.target,1,edge.cap);
                }
            }
        }finally{SAMPLING.remove();}
        var changed=new ArrayList<GlobalPos>();
        var solved=graph.solve();
        for(var entry:solved.entrySet()){
            var level=origin.getServer().getLevel(entry.getKey().dimension());
            if(level.getBlockEntity(entry.getKey().pos()) instanceof RedcrystalBlockEntity be){
                int wired=sources.getOrDefault(entry.getKey(),0);
                if(be.kind().equals("merc"))for(var contact:component.getOrDefault(entry.getKey(),List.of()))if(!contact.wireless&&contact.in!=null){
                    var remote=origin.getServer().getLevel(contact.target.dimension());
                    if(remote!=null&&accepts(level,be.getBlockPos(),contact.out)&&sends(remote,contact.target.pos(),contact.in))wired=Math.max(wired,Math.min(contact.cap,solved.getOrDefault(contact.target,0))-1);
                }
                boolean mirrorInput=be.kind().equals("merc")&&entry.getValue()>wired;
                if(be.power!=entry.getValue()||be.mirrorInput!=mirrorInput){
                    be.mirrorInput=mirrorInput;
                    be.power=entry.getValue();be.changed();level.setBlock(be.getBlockPos(),be.getBlockState().setValue(RedcrystalBlock.POWER,Math.min(15,be.power)),2);changed.add(entry.getKey());
                }be.solvedAt=level.getGameTime();
            }
        }
        for(var key:changed){var level=origin.getServer().getLevel(key.dimension());var pos=key.pos();Block block=level.getBlockState(pos).getBlock();level.updateNeighborsAt(pos,block);for(Direction direction:Direction.values())level.updateNeighborsAt(pos.relative(direction),block);}
    }
    private RedcrystalNetwork() {}
}
