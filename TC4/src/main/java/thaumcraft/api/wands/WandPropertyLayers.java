package thaumcraft.api.wands;

import java.util.*;
import thaumcraft.api.util.ExtensionRegistry;

/** Explicit integration scopes and deterministic highest-priority replacement values. */
public final class WandPropertyLayers {
    // Sceptres use 150 centivis for each declared vis, the largest built-in multiplier.
    public static final int MAX_CAPACITY=Integer.MAX_VALUE/150;
    @FunctionalInterface public interface CapacityLayer { OptionalInt capacity(String rod,int base); }
    private record Layer(Set<String> targets,CapacityLayer provider) {}
    private static final ExtensionRegistry<Layer> CAPACITY=new ExtensionRegistry<>();
    private WandPropertyLayers() {}
    public static void registerCapacity(String id,int priority,Set<String> targets,CapacityLayer provider){
        if(targets.isEmpty())throw new IllegalArgumentException("Empty wand property integration scope");
        CAPACITY.register(id,priority,new Layer(Set.copyOf(targets),Objects.requireNonNull(provider)));
    }
    public static int checkedCapacity(int value){
        if(value<1||value>MAX_CAPACITY)throw new IllegalArgumentException("Capacity exceeds centivis storage: "+value);
        return value;
    }
    /** Called while materializing the server snapshot, never in a client render/query callback. */
    public static int resolveCapacity(String rod,int base){
        checkedCapacity(base);
        for(var entry:CAPACITY.entries())if(entry.value().targets().contains(rod)){
            var value=Objects.requireNonNull(entry.value().provider().capacity(rod,base));
            if(value.isPresent())return checkedCapacity(value.getAsInt());
        }
        return base;
    }
}
