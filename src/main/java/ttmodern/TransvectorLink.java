package ttmodern;
import net.minecraft.core.BlockPos;
/** Shared two-click binding contract, distinct from remote inventory access. */
public interface TransvectorLink {boolean link(BlockPos target);}
