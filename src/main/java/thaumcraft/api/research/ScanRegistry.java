package thaumcraft.api.research;

import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import tc4revival.research.ScanTarget;
/** Server-side target resolvers run after aiming, before default target resolution. Return empty to fall through. */
public  class ScanRegistry {
    public interface Handler {
        default Optional<ScanTarget> block(ServerLevel level,BlockPos pos) { return Optional.empty(); }
        default Optional<ScanTarget> entity(Entity entity) { return Optional.empty(); }
    }
    private static final Map<ResourceLocation,Handler> HANDLERS=new LinkedHashMap<>();
    public static synchronized void register(ResourceLocation id,Handler handler) {
        if(HANDLERS.putIfAbsent(id,Objects.requireNonNull(handler))!=null)throw new IllegalArgumentException("Duplicate scan handler "+id);
    }
    public static Optional<ScanTarget> block(ServerLevel level,BlockPos pos) {
        for(var handler:HANDLERS.values()) {var result=handler.block(level,pos);if(result.isPresent())return result;}return Optional.empty();
    }
    public static Optional<ScanTarget> entity(Entity entity) {
        for(var handler:HANDLERS.values()) {var result=handler.entity(entity);if(result.isPresent())return result;}return Optional.empty();
    }
}
