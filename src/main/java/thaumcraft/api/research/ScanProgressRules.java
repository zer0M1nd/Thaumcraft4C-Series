package thaumcraft.api.research;

import java.util.*;
import java.util.function.*;
import tc4revival.aspect.*;
import tc4revival.research.*;
import thaumcraft.api.util.ExtensionRegistry;

/** Server-authoritative scan progression, invoked inside the host's knowledge transaction. */
public final class ScanProgressRules {
    public record Context(AspectCatalog catalog,PlayerKnowledge.ScanKind kind,String identity,PlayerKnowledge.ScanMode mode,AspectAmounts target,int softCap){
        public Context {target=target==null?new AspectAmounts():target.copy();}
        @Override public AspectAmounts target(){return target.copy();}
    }
    public record Tracking(ScanTarget target,PlayerKnowledge knowledge,PlayerKnowledge.ScanMode mode){
        public Tracking {knowledge=knowledge.copy();}
        @Override public PlayerKnowledge knowledge(){return knowledge.copy();}
    }
    private record Rule(Predicate<Context> accepts,BiFunction<PlayerKnowledge,Context,ScanRules.Result> apply) {}
    private static final ExtensionRegistry<Rule> RULES=new ExtensionRegistry<>();
    private static final ExtensionRegistry<Predicate<Tracking>> TRACKING=new ExtensionRegistry<>();
    private ScanProgressRules() {}
    /** Only the selected rule receives the mutable transaction ledger; matching never does. */
    public static void register(String id,int priority,Predicate<Context> accepts,BiFunction<PlayerKnowledge,Context,ScanRules.Result> rule){RULES.register(id,priority,new Rule(accepts,rule));}
    public static void registerTracking(String id,int priority,Predicate<Tracking> rule){TRACKING.register(id,priority,rule);}
    public static Optional<ScanRules.Result> apply(PlayerKnowledge ledger,Context context){
        for(var entry:RULES.entries())if(entry.value().accepts().test(context))return Optional.of(Objects.requireNonNull(entry.value().apply().apply(ledger,context)));
        return Optional.empty();
    }
    public static boolean canTrack(ScanTarget target,PlayerKnowledge knowledge,PlayerKnowledge.ScanMode mode){
        var context=new Tracking(target,knowledge,mode);return TRACKING.entries().stream().anyMatch(e->e.value().test(context));
    }
}
