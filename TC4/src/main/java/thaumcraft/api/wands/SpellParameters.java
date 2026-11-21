package thaumcraft.api.wands;

import java.util.*;
import net.minecraft.world.item.ItemStack;
import tc4revival.aspect.Aspect;
import thaumcraft.api.util.ExtensionRegistry;

/** One deterministic effect provider per parameter; costs are linear and independent of effects. */
public final class SpellParameters {
    @FunctionalInterface public interface Provider {OptionalDouble value(String channel,double base,WandCasting.Context context);}
    private static final ExtensionRegistry<Provider> EFFECTS=new ExtensionRegistry<>();
    private SpellParameters() {}
    public static void register(String id,int priority,Provider provider){EFFECTS.register(id,priority,provider);}
    public static double scale(String channel,double base){
        if(!Double.isFinite(base)||base<0)throw new IllegalArgumentException("Invalid spell parameter "+channel);
        var context=WandCasting.current();if(context==null)return base;
        double result=base*context.effect();
        for(var entry:EFFECTS.entries()){var value=entry.value().value(channel,base,context);if(value.isPresent()){result=value.getAsDouble();break;}}
        if(!Double.isFinite(result)||result<0)throw new IllegalArgumentException("Invalid spell effect "+channel);return result;
    }
    public static double limited(String channel,double base,double maximum){return Math.min(maximum,scale(channel,base));}
    public static int bounded(String channel,int base,int maximum){return Math.max(base,(int)Math.min(maximum,Math.ceil(scale(channel,base))));}
    public static int duration(String channel,int ticks){double speed=scale(channel,1);if(speed<=0)throw new IllegalArgumentException("Non-positive spell speed");return (int)Math.max(1,Math.min(Integer.MAX_VALUE,Math.ceil(ticks/speed)));}
    public static int cost(int base,double strength){
        if(base<0||!Double.isFinite(strength)||strength<1)throw new IllegalArgumentException("Invalid spell cost");
        double value=Math.ceil(base*strength);if(value>Integer.MAX_VALUE)throw new IllegalArgumentException("Spell cost overflow");return (int)value;
    }
    private static final class PlannedCosts extends AbstractMap<Aspect,Integer> {
        final UUID operation;final Map<Aspect,Integer> raw,values;
        PlannedCosts(UUID operation,Map<Aspect,Integer> raw,Map<Aspect,Integer> values){this.operation=operation;this.raw=Map.copyOf(raw);this.values=Collections.unmodifiableMap(values);}
        @Override public Set<Entry<Aspect,Integer>> entrySet(){return values.entrySet();}
    }
    public static Map<Aspect,Integer> costs(ItemStack wand,Map<Aspect,Integer> base){
        var context=WandCasting.current(wand);if(context==null)return base;
        if(base instanceof PlannedCosts planned&&planned.operation.equals(context.operation()))return base;
        if(base instanceof PlannedCosts planned)base=planned.raw;
        var values=new LinkedHashMap<Aspect,Integer>();base.forEach((aspect,amount)->values.put(aspect,cost(amount,context.strength())));
        return new PlannedCosts(context.operation(),base,values);
    }
}
