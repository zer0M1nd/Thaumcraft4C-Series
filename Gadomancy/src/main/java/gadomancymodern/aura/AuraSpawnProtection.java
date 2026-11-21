package gadomancymodern.aura;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Lux fields expire on the original sixteen-tick cadence and never leak across worlds. */
@Mod.EventBusSubscriber(modid="gadomancy")
public class AuraSpawnProtection {
    protected static final java.util.Map<ServerLevel,java.util.Map<BlockPos,Long>> FIELDS=new java.util.WeakHashMap<>();
    public static void touch(ServerLevel level,BlockPos origin){FIELDS.computeIfAbsent(level,key->new java.util.HashMap<>()).put(origin.immutable(),level.getGameTime());}
    public static boolean protectedAt(ServerLevel level,net.minecraft.world.phys.Vec3 at){var fields=FIELDS.get(level);if(fields==null)return false;fields.values().removeIf(time->time/16!=level.getGameTime()/16);return fields.keySet().stream().anyMatch(pos->at.distanceToSqr(pos.getX(),pos.getY(),pos.getZ())<=4096);}
    @SubscribeEvent public static void spawn(net.minecraftforge.event.entity.living.MobSpawnEvent.PositionCheck event){if(event.getLevel() instanceof ServerLevel level&&event.getEntity().getType().getCategory()==net.minecraft.world.entity.MobCategory.MONSTER&&protectedAt(level,event.getEntity().position()))event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);}
}
