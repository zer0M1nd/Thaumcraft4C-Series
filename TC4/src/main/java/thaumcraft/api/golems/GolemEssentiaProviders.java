package thaumcraft.api.golems;

import java.util.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumcraft.api.aspects.IEssentiaStorage;

/** Physical golem access only. These providers are never exported through an essentia storage bus. */
public final class GolemEssentiaProviders {
    @FunctionalInterface public interface Provider { IEssentiaStorage find(BlockEntity entity,Direction face); }
    private static final SortedMap<ResourceLocation,Provider> PROVIDERS=new TreeMap<>();
    private GolemEssentiaProviders() {}
    public static synchronized void register(ResourceLocation id,Provider provider) {
        if(PROVIDERS.putIfAbsent(Objects.requireNonNull(id),Objects.requireNonNull(provider))!=null)
            throw new IllegalArgumentException("Duplicate golem essentia provider "+id);
    }
    public static IEssentiaStorage find(BlockEntity entity,Direction face) {
        if(entity==null||entity.isRemoved())return null;
        for(var provider:PROVIDERS.values()){var found=provider.find(entity,face);if(found!=null)return found;}
        return null;
    }
}
