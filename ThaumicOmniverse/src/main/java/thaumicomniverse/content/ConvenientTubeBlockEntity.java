package thaumicomniverse.content;

import tc4revival.content.*;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.aspect.*;
import thaumcraft.api.aspects.EssentiaStorageApi;

/** Active, bounded, chunk-safe routing. A servo moves one unit per ten ticks. */
public class ConvenientTubeBlockEntity extends EssentiaTubeBlockEntity {
    private int servos;
    // Retain a failed third-party insertion even if the source refuses rollback.
    private AspectAmounts pending=new AspectAmounts();
    public ConvenientTubeBlockEntity(BlockPos pos,BlockState state){super(ModernAlchemy.CONVENIENT_TUBE_ENTITY.get(),pos,state);}
    public boolean servo(Direction side){return (servos&(1<<side.ordinal()))!=0;}
    public void setServo(Direction side,boolean value){if(value)servos|=1<<side.ordinal();else servos&=~(1<<side.ordinal());changed();}
    public boolean storageFace(Direction side){
        var entity=adjacent(side);
        return entity!=null&&!(entity instanceof EssentiaTubeBlockEntity)&&(!(entity instanceof EssentiaTransport t)||t.connectable(side.getOpposite()))&&EssentiaStorageApi.supports(entity,side.getOpposite());
    }
    @Override public boolean hasTransport(Direction side){return adjacent(side) instanceof ConvenientTubeBlockEntity||storageFace(side);}
    /** Active routing and legacy suction are separate networks. Both endpoints must agree. */
    @Override public boolean connectable(Direction side){
        return super.connectable(side)&&!(adjacent(side) instanceof EssentiaTubeBlockEntity tube&&!(tube instanceof ConvenientTubeBlockEntity));
    }
    @Override public EssentiaTransport neighbor(Direction side){
        if(adjacent(side) instanceof ConvenientTubeBlockEntity other)return other.connectable(side.getOpposite())?other:null;
        return storageFace(side)?adjacent(side) instanceof EssentiaTransport t?t:this:null; // Rendering only.
    }
    @Override public boolean canInput(Direction side){return false;}
    @Override public boolean canOutput(Direction side){return false;}
    @Override public int suction(Direction side){return 0;}
    @Override public Aspect suctionType(Direction side){return null;}
    @Override public int minimumSuction(){return 0;}
    @Override public Aspect essentiaType(Direction side){return null;}
    @Override public int essentiaAmount(Direction side){return 0;}
    @Override public int takeEssentia(Aspect a,int n,Direction side){return 0;}
    @Override public int addEssentia(Aspect a,int n,Direction side){return 0;}
    private boolean allows(Aspect aspect){return flow().filter()==null||flow().filter().equals(aspect);}
    private record Node(ConvenientTubeBlockEntity tube,int distance){}
    private record Destination(BlockPos pos,Direction face,int distance){}
    private List<Destination> destinations(Aspect aspect,BlockPos source){
        if(!allows(aspect))return List.of();
        var queue=new PriorityQueue<Node>(Comparator.comparingInt(Node::distance).thenComparing(n->n.tube.getBlockPos().asLong()));
        var distances=new HashMap<BlockPos,Integer>();var targets=new HashMap<String,Destination>();
        distances.put(worldPosition,0);queue.add(new Node(this,0));int visited=0;
        while(!queue.isEmpty()&&visited++<4096){
            var node=queue.remove();var tube=node.tube();
            if(node.distance()!=distances.get(tube.getBlockPos()))continue;
            for(var side:Direction.values()){
                if(!tube.flow().open(side))continue;
                var pos=tube.getBlockPos().relative(side);
                if(tube.adjacent(side) instanceof ConvenientTubeBlockEntity next){
                    if(!next.flow().open(side.getOpposite())||!next.allows(aspect))continue;
                    int distance=node.distance()+(next.flow().kind()==EssentiaTubeFlow.Kind.RESTRICTED?1000:1);
                    if(distance<distances.getOrDefault(pos,Integer.MAX_VALUE)){distances.put(pos,distance);queue.add(new Node(next,distance));}
                }else if(!pos.equals(source)&&!tube.servo(side)&&tube.storageFace(side)){
                    var dest=new Destination(pos,side.getOpposite(),node.distance()+1);
                    String key=pos.asLong()+":"+side.getOpposite();
                    if(!targets.containsKey(key)||targets.get(key).distance()>dest.distance())targets.put(key,dest);
                }
            }
        }
        return targets.values().stream().sorted(Comparator.comparingInt(Destination::distance).thenComparing(d->d.pos().asLong()).thenComparing(d->d.face().ordinal())).toList();
    }
    @Override public void tick(){
        if(!(level instanceof ServerLevel world)||Math.floorMod(world.getGameTime()+worldPosition.asLong(),10)!=0)return;
        transferCycle();
    }
    void transferCycle(){
        if(!thaumicomniverse.RevivalFeatures.Group.TUBES.enabled())return;
        if(!(level instanceof ServerLevel world))return;
        for(var aspect:List.copyOf(pending.aspects())){
            for(var dest:destinations(aspect,worldPosition)){
                var storage=EssentiaStorageApi.at(world,dest.pos(),dest.face());
                int amount=storage.insert(aspect,Math.min(1,pending.amount(aspect)),false);
                if(amount>0){pending.remove(aspect,amount);changed();break;}
            }
        }
        if(!pending.aspects().isEmpty())return;
        for(var side:Direction.values()){
            if(!servo(side)||!flow().open(side)||!storageFace(side))continue;
            var sourcePos=worldPosition.relative(side);var source=EssentiaStorageApi.at(world,sourcePos,side.getOpposite());
            boolean moved=false;
            for(var aspect:source.contents().aspects()){
                if(!allows(aspect))continue;
                for(var dest:destinations(aspect,sourcePos)){
                    var target=EssentiaStorageApi.at(world,dest.pos(),dest.face());
                    if(target.insert(aspect,1,true)==0)continue;
                    var transfer=EssentiaStorageApi.transfer(source,target,aspect,1);
                    if(transfer.remainder()>0){pending.add(aspect,transfer.remainder());changed();}
                    if(transfer.inserted()>0||transfer.remainder()>0){moved=true;break;}
                }
                if(moved)break;
            }
        }
    }
    @Override public InteractionResult onWandUseFirst(ItemStack stack,UseOnContext context){
        var player=context.getPlayer();
        if(player!=null&&player.isShiftKeyDown()){
            int part=EssentiaTubeBlock.tracePart(this,player);
            if(part>=0&&part<6&&servo(Direction.from3DDataValue(part))){
                if(!level.mayInteract(player,worldPosition)||!player.mayUseItemAt(worldPosition,context.getClickedFace(),stack))return InteractionResult.FAIL;
                if(!level.isClientSide){setServo(Direction.from3DDataValue(part),false);net.minecraft.world.level.block.Block.popResource(level,worldPosition,new ItemStack(ModernAlchemy.ESSENTIA_SERVO.get()));}
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return super.onWandUseFirst(stack,context);
    }
    public void dropAttachments(){
        if(level==null||level.isClientSide)return;
        int previous=servos;servos=0; // Clear before callbacks so removal cannot drop an attachment twice.
        for(var side:Direction.values())if((previous&(1<<side.ordinal()))!=0)
            net.minecraft.world.level.block.Block.popResource(level,worldPosition,new ItemStack(ModernAlchemy.ESSENTIA_SERVO.get()));
        if(previous!=0)changed();
    }
    public void refreshAttachments(){
        if(level==null||level.isClientSide)return;
        for(var side:Direction.values())if(servo(side)&&level.hasChunkAt(worldPosition.relative(side))&&!storageFace(side)){
            setServo(side,false);
            net.minecraft.world.level.block.Block.popResource(level,worldPosition,new ItemStack(ModernAlchemy.ESSENTIA_SERVO.get()));
        }
        changed();
    }
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.putInt("Servos",servos);var saved=new CompoundTag();pending.view().forEach((a,n)->saved.putInt(a.tag(),n));tag.put("Pending",saved);}
    @Override public void load(CompoundTag tag){super.load(tag);servos=tag.getInt("Servos")&63;pending=new AspectAmounts();var saved=tag.getCompound("Pending");for(String key:saved.getAllKeys())if(saved.getInt(key)>0)TCAspects.catalog().find(key).ifPresent(a->pending.add(a,saved.getInt(key)));}
    @Override public CompoundTag getUpdateTag(){var tag=super.getUpdateTag();tag.putInt("Servos",servos);return tag;}
}
