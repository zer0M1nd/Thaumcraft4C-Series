package thaumcraft.api.resources;

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;

/** Register during addon construction: initial pack discovery precedes the Forge gameplay bus starting. */
public final class ExternalResourceConverters {
    private static final Map<ResourceLocation,Consumer<Map<String,byte[]>>> CONVERTERS=new ConcurrentSkipListMap<>();
    public static void register(ResourceLocation id,Consumer<Map<String,byte[]>> converter) {
        if(CONVERTERS.putIfAbsent(java.util.Objects.requireNonNull(id),java.util.Objects.requireNonNull(converter))!=null)
            throw new IllegalArgumentException("Duplicate external resource converter: "+id);
    }
    public static void prepare(String owner,Map<String,byte[]> resources) {
        CONVERTERS.forEach((id,converter)->{if(id.getNamespace().equals(owner))converter.accept(resources);});
    }
    private ExternalResourceConverters(){}
}
