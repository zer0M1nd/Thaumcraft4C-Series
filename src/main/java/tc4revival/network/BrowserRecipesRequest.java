package tc4revival.network;
import java.util.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;
import tc4revival.research.*;
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="thaumcraft")
public record BrowserRecipesRequest(int request,boolean catalog,List<ItemStack> stacks) {
    private static final Map<ServerPlayer,Integer> LAST_CATALOG=new WeakHashMap<>(),LAST_ITEMS=new WeakHashMap<>();
    private record Job(int request,boolean catalog,BrowserRecipes.Catalog work){}
    private static final LinkedHashMap<ServerPlayer,Job> JOBS=new LinkedHashMap<>();
    public BrowserRecipesRequest {if(stacks.size()>32)throw new IllegalArgumentException("Too many query stacks");stacks=List.copyOf(stacks);}
    public void encode(FriendlyByteBuf b){b.writeVarInt(request);b.writeBoolean(catalog);b.writeVarInt(stacks.size());stacks.forEach(b::writeItem);}
    public static BrowserRecipesRequest decode(FriendlyByteBuf b){int id=b.readVarInt();boolean catalog=b.readBoolean();int n=b.readVarInt();if(n<0||n>32)throw new IllegalArgumentException("Too many query stacks");var stacks=new ArrayList<ItemStack>();for(int i=0;i<n;i++)stacks.add(b.readItem());return new BrowserRecipesRequest(id,catalog,stacks);}
    public void handle(ServerPlayer p){
        if(p==null)return;var times=catalog?LAST_CATALOG:LAST_ITEMS;int now=p.server.getTickCount();var lastTime=times.get(p);
        if(lastTime!=null&&now-lastTime<(catalog?40:2))return;times.put(p,now);
        var active=JOBS.get(p);
        if(active!=null&&active.request()==request&&active.catalog()==catalog)return; // Retry must not restart work.
        JOBS.put(p,new Job(request,catalog,catalog?BrowserRecipes.catalog(p):BrowserRecipes.items(p,stacks)));
    }
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.ServerTickEvent event){
        if(event.phase!=net.minecraftforge.event.TickEvent.Phase.END||JOBS.isEmpty())return;
        // One round-robin slice globally per tick, rather than a budget multiplied by connected players.
        var iterator=JOBS.entrySet().iterator();var next=iterator.next();var p=next.getKey();var job=next.getValue();iterator.remove();
        if(p.hasDisconnected()||p.isRemoved())return;
        var rows=job.work().drain(4096,2_000_000L);boolean done=job.work().done();
        for(var row:rows)send(p,new BrowserRecipesPacket(job.request(),job.catalog(),false,List.of(row)));
        if(done||p.server.getTickCount()%20==0)send(p,new BrowserRecipesPacket(job.request(),job.catalog(),done,List.of()));
        if(!done)JOBS.put(p,job);
    }
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void stop(net.minecraftforge.event.server.ServerStoppedEvent event){JOBS.clear();LAST_CATALOG.clear();LAST_ITEMS.clear();}
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void logout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event){if(event.getEntity() instanceof ServerPlayer p)JOBS.remove(p);}
    private static void send(ServerPlayer p,BrowserRecipesPacket packet){TCNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(()->p),packet);}
}
