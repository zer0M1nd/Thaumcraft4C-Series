package thaumcraft.api.golems;

import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Optional render projection. No client-only classes enter the server API. */
public class GolemLocationVisuals {
    public record Frame(Vec3 center,Vec3 normal,Vec3 up){}
    @FunctionalInterface public interface Provider { Frame resolve(Level level,CompoundTag view,float partial); }
    private static final Map<ResourceLocation,Provider> PROVIDERS=new HashMap<>();
    public static void register(ResourceLocation id,Provider provider){PROVIDERS.put(id,provider);}
    public static Frame resolve(Level level,CompoundTag view,float partial){
        var provider=PROVIDERS.get(ResourceLocation.tryParse(view.getString("Provider")));return provider==null?null:provider.resolve(level,view,partial);
    }
}
