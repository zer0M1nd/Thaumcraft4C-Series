package gadomancymodern.automation;

import gadomancymodern.Gadomancy;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

public class RemoteJarEntity extends StickyJarBlockEntity {
    protected UUID network;
    protected boolean sticky;
    protected int networkTicks;
    protected boolean registeredToNetwork;
    protected static final Map<MinecraftServer,Map<UUID,Network>> NETWORKS=new WeakHashMap<>();
    public RemoteJarEntity(BlockPos pos,BlockState state){super(Gadomancy.REMOTE_JAR_BE.get(),pos,state);}
    public UUID network(){return network;}
    public void network(UUID id){if(level!=null&&!level.isClientSide&&network!=null&&!network.equals(id)){var groups=NETWORKS.get(level.getServer());if(groups!=null&&groups.get(network)!=null)groups.get(network).remove(this);}network=id;registeredToNetwork=false;changed();}
    public boolean sticky(){return sticky;}
    @Override public boolean voidJar(){return false;}
    @Override public ItemStack asItem(){var stack=new ItemStack(Gadomancy.REMOTE_JAR_ITEM.get());var base=super.asItem();if(base.hasTag())stack.setTag(base.getTag().copy());var tag=stack.getOrCreateTag();if(!sticky)tag.remove("isStickyJar");if(network!=null)tag.putUUID("networkId",network);if(tag.isEmpty())stack.setTag(null);return stack;}
    @Override public void readItem(ItemStack stack){super.readItem(stack);var tag=stack.getTag();sticky=tag!=null&&tag.getBoolean("isStickyJar");if(!sticky)support=net.minecraft.core.Direction.DOWN;network(tag!=null&&tag.hasUUID("networkId")?tag.getUUID("networkId"):null);}
    @Override public void tick(){
        super.tick();if(level==null||level.isClientSide)return;
        if(networkTicks%3==0&&network!=null&&(!registeredToNetwork||amount()<capacity())){
            networkTicks=0;var group=NETWORKS.computeIfAbsent(level.getServer(),server->new HashMap<>()).computeIfAbsent(network,id->new Network());group.join(this);registeredToNetwork=true;group.update(level.getServer().overworld().getGameTime());
        }networkTicks++;
    }
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);if(network!=null)tag.putUUID("networkId",network);tag.putBoolean("isStickyJar",sticky);}
    @Override public void load(CompoundTag tag){super.load(tag);network=tag.hasUUID("networkId")?tag.getUUID("networkId"):null;sticky=tag.getBoolean("isStickyJar");registeredToNetwork=false;}
    /** Weak references avoid carrying stopped dimensions/worlds into another server session. */
    public static class Network {
        protected long nextTime;
        protected final List<java.lang.ref.WeakReference<RemoteJarEntity>> jars=new ArrayList<>();
        public void join(RemoteJarEntity jar){if(jars.stream().noneMatch(ref->ref.get()==jar))jars.add(new java.lang.ref.WeakReference<>(jar));}
        public void remove(RemoteJarEntity jar){jars.removeIf(ref->ref.get()==null||ref.get()==jar);}
        public void update(long time){
            if(time<=nextTime)return;
            jars.removeIf(ref->!valid(ref.get()));RemoteJarEntity high=null,low=null;
            for(var ref:jars){var jar=ref.get();if(jar!=null&&(high==null||high.amount()<=jar.amount()))high=jar;}
            if(high!=null)for(var ref:jars){var jar=ref.get();if(jar==null||jar==high||!jar.accepts(high.aspect())
                    ||jar.amount()>0&&!java.util.Objects.equals(jar.aspect(),high.aspect()))continue;
                if(low==null||low.amount()>jar.amount())low=jar;}
            if(high!=null&&low!=null&&high!=low&&low.amount()+1<high.amount()&&low.addToContainer(high.aspect(),1)==0)high.takeFromContainer(high.aspect(),1);
            nextTime=time+3;
        }
        protected static boolean valid(RemoteJarEntity jar){return jar!=null&&jar.getLevel()!=null&&!jar.isRemoved()&&jar.getLevel().hasChunkAt(jar.worldPosition)&&jar.getLevel().getBlockEntity(jar.worldPosition)==jar;}
    }
}
