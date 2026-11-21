package thaumicomniverse.compat.create;

import java.util.*;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.core.BlockPos;
import org.joml.Vector4f;

/** Preserve actual recently sampled light during the short entity/terrain handoff. */
public final class ContraptionHandoffLight {
    private static final Map<AbstractContraptionEntity, Samples> CACHE = new WeakHashMap<>();
    private static Samples active;
    private static boolean frozen;
    private static final class Samples {
        Map<Long,Integer> current = new HashMap<>(), previous = new HashMap<>();
    }
    public static void begin(AbstractContraptionEntity entity) {
        active = null;
        if (!ContraptionRenderHandoff.enabled()) { CACHE.clear(); return; }
        active = CACHE.computeIfAbsent(entity, ignored -> new Samples());
        frozen = ContraptionRenderHandoff.frozen(entity);
        if (!frozen) {
            var reuse = active.previous;
            active.previous = active.current;
            active.current = reuse;
            reuse.clear();
        }
    }
    public static int sample(Vector4f position, int light) {
        if (active == null) return light;
        long key = BlockPos.containing(position.x(), position.y(), position.z()).asLong();
        if (frozen) {
            // Never invent full-bright lighting or borrow light from nearby rooms.
            // Keep each channel at least as bright as this exact sample location
            // was immediately before opaque terrain replaced the moving model.
            light = max(light, active.current.getOrDefault(key, 0));
            return max(light, active.previous.getOrDefault(key, 0));
        }
        if (active.current.size() < 65536) active.current.put(key, light);
        return light;
    }
    private static int max(int a, int b) {
        return Math.max(a & 0xffff, b & 0xffff) | (Math.max(a >>> 16, b >>> 16) << 16);
    }
    public static void end() { active = null; }
    private ContraptionHandoffLight() {}
}
