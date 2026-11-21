package thaumcraft.api.research;

import java.util.UUID;
import java.util.function.Consumer;
import thaumcraft.api.util.ExtensionRegistry;

/** Read-only diagnostics. Observer failures are isolated from book authorization and networking. */
public final class ResearchBookLifecycle {
    public enum Stage { SERVER_ATTEMPT, SESSION_CREATED, SYNC_SENT, PACKET_RECEIVED, SCREEN_CREATED, REJECTED, FAILED, CLOSED }
    public record Observation(Stage stage,UUID player,UUID session,String detail) {}
    private static final ExtensionRegistry<Consumer<Observation>> OBSERVERS=new ExtensionRegistry<>();
    private ResearchBookLifecycle() {}
    public static void registerObserver(String id,int priority,Consumer<Observation> observer){OBSERVERS.register(id,priority,observer);}
    public static void observe(Observation event){
        for(var entry:OBSERVERS.entries())try{entry.value().accept(event);}catch(RuntimeException error){
            org.slf4j.LoggerFactory.getLogger(ResearchBookLifecycle.class).warn("Book observer failed: "+entry.id(),error);
        }
    }
    public static void observe(Stage stage,UUID player,UUID session,String detail){observe(new Observation(stage,player,session,detail));}
}
