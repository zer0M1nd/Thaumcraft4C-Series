package thaumcraft.api.crafting;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import tc4revival.content.InfusionLayout;

/** World-side modifiers are evaluated before committing an infusion, including its preview. */
public final class InfusionDeviceModifiers {
    public record Stats(double costMultiplier,double cycleMultiplier,int instability) {
        public static final Stats NONE=new Stats(1,1,0);
        public Stats {
            if(!Double.isFinite(costMultiplier)||costMultiplier<0||!Double.isFinite(cycleMultiplier)||cycleMultiplier<=0)
                throw new IllegalArgumentException("Invalid infusion device modifiers");
        }
    }
    @FunctionalInterface public interface Provider { Stats evaluate(Level level,BlockPos matrix,InfusionLayout layout); }
    private static final Map<ResourceLocation,Provider> PROVIDERS=new LinkedHashMap<>();
    public static synchronized void register(ResourceLocation id,Provider provider){
        if(PROVIDERS.putIfAbsent(Objects.requireNonNull(id),Objects.requireNonNull(provider))!=null)
            throw new IllegalArgumentException("Duplicate infusion device modifier "+id);
    }
    public static Stats evaluate(Level level,BlockPos pos,InfusionLayout layout){
        double cost=1,cycle=1;int instability=0;
        for(var provider:PROVIDERS.values()){
            var stats=provider.evaluate(level,pos,layout);
            cost*=stats.costMultiplier;cycle*=stats.cycleMultiplier;instability=Math.addExact(instability,stats.instability);
        }
        return new Stats(cost,cycle,instability);
    }
    private InfusionDeviceModifiers(){}
}
