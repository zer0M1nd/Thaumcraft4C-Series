package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.aspect.Aspect;
import thaumcraft.api.visnet.VisEndpoint;

/** Native block entity adapter into the same graph used by non-block-entity addon endpoints. */
public abstract class VisNetworkNodeBlockEntity extends BlockEntity implements VisEndpoint {
    protected int networkTicks;
    private boolean refresh=true;
    protected VisNetworkNodeBlockEntity(BlockEntityType<?> type,BlockPos position,BlockState state){super(type,position,state);}
    public abstract int range();
    public abstract boolean source();
    public byte attunement(){return -1;}
    @Override public Level level(){return level;}
    @Override public Key key(){return new Key(worldPosition,ResourceLocation.parse("thaumcraft:block"));}
    @Override public boolean valid(){return !isRemoved()&&level!=null&&level.hasChunkAt(worldPosition)&&level.getBlockEntity(worldPosition)==this;}
    public VisEndpoint networkParent(){return VisNetwork.parent(this);}
    public VisEndpoint networkRoot(){return VisNetwork.root(this);}
    /** Compatibility view: use networkParent/networkRoot when the endpoint may be an addon part. */
    public VisNetworkNodeBlockEntity parent(){return networkParent() instanceof VisNetworkNodeBlockEntity node?node:null;}
    public VisNetworkNodeBlockEntity root(){return networkRoot() instanceof VisNetworkNodeBlockEntity node?node:null;}
    public int consumeVis(Aspect aspect,int requested){return VisNetwork.consume(this,aspect,requested);}
    @Override public int drain(Aspect aspect,int requested){return drainSource(aspect,requested);}
    @Override public void consumed(Aspect aspect,int amount){onVisConsumed(aspect);}
    @Override public void consumerDrain(BlockPos consumer,Aspect aspect,int amount){onConsumerDrain(consumer,aspect,amount);}
    @Override public void parentChanged(VisEndpoint parent){
        onParentChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);
    }
    protected int drainSource(Aspect aspect,int requested){return 0;}
    protected void onVisConsumed(Aspect aspect){}
    protected void onConsumerDrain(BlockPos consumer,Aspect aspect,int amount){}
    protected void onParentChanged(){}
    public void tickNetwork(){
        if(level==null||level.isClientSide)return;
        boolean interval=networkTicks++%40==0;
        if(interval||refresh){refresh=false;VisNetwork.register(this);}
    }
    public void refreshNetwork(){if(level!=null&&!level.isClientSide)VisNetwork.refresh(this);refresh=true;}
    @Override public void setRemoved(){VisNetwork.unregister(this);super.setRemoved();}
    @Override public void onChunkUnloaded(){VisNetwork.unregister(this);super.onChunkUnloaded();}
    @Override public void clearRemoved(){super.clearRemoved();refresh=true;}
}
