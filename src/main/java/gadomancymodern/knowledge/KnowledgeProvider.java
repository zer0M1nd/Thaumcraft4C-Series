package gadomancymodern.knowledge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
/** Implement on a block or block entity to contribute to nearby knowledge books. */
public interface KnowledgeProvider {
    int providedKnowledge(Level level,BlockPos pos);
}
