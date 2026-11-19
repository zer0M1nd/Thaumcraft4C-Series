package thaumcraft.api.visnet;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import tc4revival.aspect.Aspect;

/** A live, server-owned network endpoint; it need not be a block entity.
 * Host retains the endpoint object; register on load and unregister on removal/unload.
 * Endpoint ID is stable for a face/part, unique at its block position within a dimension. */
public interface VisEndpoint {
    record Key(BlockPos position, ResourceLocation id) { public Key {position=position.immutable();java.util.Objects.requireNonNull(id);} }
    Level level();
    Key key();
    boolean valid();
    int range();
    boolean source();
    /** Whether other consumers may draw through this endpoint. Receivers can opt out. */
    default boolean relay(){return true;}
    default byte attunement(){return -1;}
    default Vec3 point(){return Vec3.atCenterOf(key().position());}
    /** Whether this endpoint's physical ports permit an edge to the other endpoint. */
    default boolean connectsTo(VisEndpoint other){return true;}
    /** Source callback: remove and return 0..requested centivis. Never called for simulation. */
    default int drain(Aspect aspect,int requested){return 0;}
    default void consumed(Aspect aspect,int amount){}
    default void consumerDrain(BlockPos consumer,Aspect aspect,int amount){}
    /** Parent may be null. Hosts synchronize their own part renderer using this callback. */
    default void parentChanged(VisEndpoint parent){}
}
