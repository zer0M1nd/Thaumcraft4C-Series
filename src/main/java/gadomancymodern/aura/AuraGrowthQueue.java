package gadomancymodern.aura;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Old updateTick performed growth; in modern Minecraft scheduledTick and randomTick are distinct. */
@Mod.EventBusSubscriber(modid="gadomancy")
public class AuraGrowthQueue {
    public record Pending(BlockPos pos,Block block,long due){}
    protected static final java.util.Map<ServerLevel,java.util.Map<BlockPos,Pending>> QUEUES=new java.util.WeakHashMap<>();
    public static void schedule(ServerLevel level,BlockPos pos,int delay){var queue=QUEUES.computeIfAbsent(level,key->new java.util.HashMap<>());var update=new Pending(pos.immutable(),level.getBlockState(pos).getBlock(),level.getGameTime()+delay);queue.merge(pos.immutable(),update,(old,next)->old.due<=next.due?old:next);}
    @SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.LevelTickEvent event){if(event.phase!=net.minecraftforge.event.TickEvent.Phase.END||!(event.level instanceof ServerLevel level))return;var queue=QUEUES.get(level);if(queue==null)return;var ready=new java.util.ArrayList<Pending>();queue.values().removeIf(update->{if(update.due<=level.getGameTime()){ready.add(update);return true;}return false;});for(var update:ready)if(level.hasChunkAt(update.pos)){var state=level.getBlockState(update.pos);if(state.is(update.block)&&state.isRandomlyTicking())state.randomTick(level,update.pos,level.random);}}
}
