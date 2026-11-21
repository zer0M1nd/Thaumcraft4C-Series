package tc4revival.resources;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.resources.IoSupplier;

/** Exposes only conversion outputs, never unrelated or stale files in the cache. */
public  class ExternalResourcePack extends PathPackResources {
    private final Set<String> allowed;

    public ExternalResourcePack(String id, Path root, Set<String> allowed) {
        super(id, root, true);
        this.allowed = Set.copyOf(allowed);
    }

    @Override public IoSupplier<InputStream> getRootResource(String... path) {
        return allowed.contains(String.join("/", path)) ? super.getRootResource(path) : null;
    }

    @Override public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
        return type == PackType.CLIENT_RESOURCES
                && allowed.contains("assets/" + location.getNamespace() + "/" + location.getPath())
                ? super.getResource(type, location) : null;
    }

    @Override public void listResources(PackType type, String namespace, String prefix, ResourceOutput output) {
        if (type != PackType.CLIENT_RESOURCES) return;
        String base = "assets/" + namespace + "/";
        String directory = prefix.isEmpty() ? base : base + prefix + "/";
        allowed.stream().filter(path -> path.startsWith(directory)).sorted().forEach(path -> {
            var location = ResourceLocation.tryParse(namespace + ":" + path.substring(base.length()));
            // Like vanilla packs, ignore invalid entries instead of crashing the entire atlas reload.
            if(location == null)return;
            var resource = getResource(type, location);
            if (resource != null) output.accept(location, resource);
        });
    }

    @Override public Set<String> getNamespaces(PackType type) {
        return type != PackType.CLIENT_RESOURCES ? Set.of() : allowed.stream()
                .filter(path -> path.startsWith("assets/"))
                .map(path -> path.split("/", 3)[1]).collect(Collectors.toUnmodifiableSet());
    }
}
