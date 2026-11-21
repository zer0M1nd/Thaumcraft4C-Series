package gadomancymodern.aura;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

/** Open extension point: a single aura may affect blocks, entities, or both. */
public class AuraEffect {
    public int interval(){return 8;}
    public double range(){return 8;}
    public boolean accepts(Entity entity){return false;}
    public void entity(ServerLevel level,BlockPos origin,Entity entity){}
    public int blockCount(RandomSource random){return 0;}
    public void block(ServerLevel level,BlockPos origin,BlockPos chosen){}
}
