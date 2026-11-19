package thaumcraft.api.aspects;

import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import tc4revival.content.EssentiaSource;

/** Individual moving reservoirs, with their current world-space mouths. Never cache their positions. */
public class MobileEssentiaSources {
    public record Located(Vec3 position, EssentiaSource source) {
        public Located { Objects.requireNonNull(position); Objects.requireNonNull(source); }
    }
    @FunctionalInterface public interface Provider { Collection<Located> find(Level level, AABB bounds); }
    private static final Map<ResourceLocation,Provider> PROVIDERS = new TreeMap<>();
    public static synchronized void register(ResourceLocation id, Provider provider) {
        if (PROVIDERS.putIfAbsent(Objects.requireNonNull(id),Objects.requireNonNull(provider)) != null)
            throw new IllegalArgumentException("Duplicate moving essentia provider " + id);
    }
    public static List<Located> find(Level level, AABB bounds) {
        var found = new ArrayList<Located>();
        for (var provider : PROVIDERS.values())
            for (var source : provider.find(level,bounds)) if (bounds.contains(source.position())) found.add(source);
        return List.copyOf(found);
    }
    private MobileEssentiaSources() {}
}
