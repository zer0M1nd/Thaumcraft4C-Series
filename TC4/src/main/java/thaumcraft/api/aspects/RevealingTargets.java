package thaumcraft.api.aspects;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.UnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import tc4revival.content.RevealedAspectContainer;

/** Optional client projections for containers carried by entities. No client classes are loaded on a server. */
public class RevealingTargets {
    public record Target(Vec3 hit, BlockPos local, Direction face, boolean above,
                         RevealedAspectContainer container, UnaryOperator<Vec3> toWorld) {}
    @FunctionalInterface public interface Provider {
        Target find(Player player, Vec3 eye, Vec3 end, float partial);
    }
    private static final Map<ResourceLocation, Provider> PROVIDERS = new LinkedHashMap<>();
    public static void register(ResourceLocation id, Provider provider) { PROVIDERS.put(id, provider); }
    public static Target find(Player player, Vec3 eye, Vec3 end, float partial) {
        Target nearest = null;
        double distance = eye.distanceToSqr(end);
        for (var provider : PROVIDERS.values()) {
            var target = provider.find(player, eye, end, partial);
            if (target != null && eye.distanceToSqr(target.hit()) < distance) {
                nearest = target;
                distance = eye.distanceToSqr(target.hit());
            }
        }
        return nearest;
    }
}
