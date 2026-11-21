package thaumcraft.api.aspects;

import com.google.gson.JsonObject;
import java.util.*;
import java.util.function.UnaryOperator;
import net.minecraft.world.item.ItemStack;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.item.ItemAspectAccess;
import thaumcraft.api.util.ExtensionRegistry;

/** Reload transforms and per-stack resolution with explicit pass/handled-empty semantics. */
public final class ItemAspectProviders {
    public record Query(ItemStack stack,ItemAspectAccess access,boolean scanning) {
        public Query {stack=stack.copy();Objects.requireNonNull(access);}
        @Override public ItemStack stack(){return stack.copy();}
    }
    public record Resolution(boolean handled,Optional<AspectAmounts> aspects) {
        public Resolution {aspects=aspects.map(AspectAmounts::copy);}
        @Override public Optional<AspectAmounts> aspects(){return aspects.map(AspectAmounts::copy);}
        public static Resolution pass(){return new Resolution(false,Optional.empty());}
        public static Resolution handled(Optional<AspectAmounts> aspects){return new Resolution(true,aspects);}
    }
    @FunctionalInterface public interface Provider {Resolution resolve(Query query);}
    private static final ExtensionRegistry<Provider> STACKS=new ExtensionRegistry<>();
    private static final ExtensionRegistry<UnaryOperator<JsonObject>> RULES=new ExtensionRegistry<>();
    private static final ThreadLocal<Set<String>> ACTIVE=ThreadLocal.withInitial(HashSet::new);
    private ItemAspectProviders() {}
    public static void register(String id,int priority,Provider provider){STACKS.register(id,priority,provider);}
    public static void registerRuleTransform(String id,int priority,UnaryOperator<JsonObject> transform){RULES.register(id,priority,transform);}
    public static JsonObject prepareRules(JsonObject source){
        JsonObject result=source.deepCopy();
        for(var entry:RULES.entries())result=Objects.requireNonNull(entry.value().apply(result.deepCopy())).deepCopy();
        return result;
    }
    public static Resolution resolve(Query query){
        var active=ACTIVE.get();
        try {
            for(var entry:STACKS.entries())if(active.add(entry.id())){
                try {var result=Objects.requireNonNull(entry.value().resolve(query));if(result.handled())return result;}
                finally {active.remove(entry.id());}
            }
            return Resolution.pass();
        } finally {if(active.isEmpty())ACTIVE.remove();}
    }
}
