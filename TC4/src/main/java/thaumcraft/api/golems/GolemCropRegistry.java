package thaumcraft.api.golems;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.FakePlayer;
import tc4revival.content.GolemWorkHost;

/** First matching rule owns maturity, harvesting and optional order-upgrade planting.
 * Callbacks must not load chunks. Use Context.breakBlock/place for protected world mutations. */
public class GolemCropRegistry {
    public interface Rule {
        boolean matches(BlockState state);
        boolean mature(ServerLevel level,BlockPos pos,BlockState state);
        boolean harvest(Context context);
    }
    public record Context(GolemWorkHost golem,ServerLevel level,FakePlayer player,BlockPos pos,BlockState original,boolean replant) {
        public boolean loaded(BlockPos target){return !level.isOutsideBuildHeight(target)&&level.hasChunkAt(target);}
        public boolean allowed(BlockPos target) {
            return loaded(target)&&level.mayInteract(player,target)&&!net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                new net.minecraftforge.event.level.BlockEvent.BreakEvent(level,target,level.getBlockState(target),player));
        }
        public boolean breakBlock(BlockPos target,boolean drops){return allowed(target)&&level.destroyBlock(target,drops,player);}
        /** Caller consumes a seed only after true. Soil/survival and place protection are checked here. */
        public boolean place(BlockPos target,BlockState state) {
            if(!loaded(target)||!level.getBlockState(target).canBeReplaced()||!state.canSurvive(level,target)||!level.mayInteract(player,target))return false;
            var before=net.minecraftforge.common.util.BlockSnapshot.create(level.dimension(),level,target);
            if(!level.setBlock(target,state,3))return false;
            if(net.minecraftforge.event.ForgeEventFactory.onBlockPlace(player,before,net.minecraft.core.Direction.UP)) {
                before.restore(true,false);return false;
            }
            return true;
        }
    }
    private record Entry(int priority,Rule rule){}
    private static final Map<ResourceLocation,Entry> RULES=new TreeMap<>();
    public static synchronized void register(ResourceLocation id,int priority,Rule rule) {
        if(RULES.putIfAbsent(id,new Entry(priority,Objects.requireNonNull(rule)))!=null)throw new IllegalArgumentException("Duplicate crop rule "+id);
    }
    public static synchronized Optional<Rule> find(BlockState state) {
        return RULES.values().stream().sorted(Comparator.comparingInt(Entry::priority).reversed())
            .map(Entry::rule).filter(rule->rule.matches(state)).findFirst();
    }
}
