package thaumcraft.api.golems;

import java.util.Optional;
import tc4revival.content.GolemWorkHost;

/** Scoped server-thread identity for blocks used by a golem's fake player. Never persisted on the player. */
public class GolemInteraction implements AutoCloseable {
    private static final ThreadLocal<GolemWorkHost> CURRENT=new ThreadLocal<>();
    private final GolemWorkHost previous;
    private GolemInteraction(GolemWorkHost host){previous=CURRENT.get();CURRENT.set(java.util.Objects.requireNonNull(host));}
    public static GolemInteraction enter(GolemWorkHost host){return new GolemInteraction(host);}
    public static Optional<GolemWorkHost> current(){return Optional.ofNullable(CURRENT.get());}
    @Override public void close(){if(previous==null)CURRENT.remove();else CURRENT.set(previous);}
}
