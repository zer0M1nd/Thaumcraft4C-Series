package tc4revival.content;

import net.minecraft.core.Direction;
import tc4revival.aspect.Aspect;

/** Directional essentia flow is separate from remote reservoir access. */
public interface EssentiaTransport {
    default boolean renderExtendedTube() { return false; }
    boolean connectable(Direction face);
    boolean canInput(Direction face);
    boolean canOutput(Direction face);
    Aspect suctionType(Direction face);
    int suction(Direction face);
    int minimumSuction();
    /** A null face queries stored contents without selecting a neighboring connection. */
    Aspect essentiaType(Direction face);
    /** Supports the same null-face contents query as essentiaType. */
    int essentiaAmount(Direction face);
    int takeEssentia(Aspect aspect,int amount,Direction face);
    int addEssentia(Aspect aspect,int amount,Direction face);
}
