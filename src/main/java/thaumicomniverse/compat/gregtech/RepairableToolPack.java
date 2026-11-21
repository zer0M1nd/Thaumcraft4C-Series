package thaumicomniverse.compat.gregtech;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.item.IGTTool;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.registries.ForgeRegistries;
import thaumicomniverse.ThaumicOmniverse;

/** GregTech versions and addons can register different tool types for the same material. */
final class RepairableToolPack extends AbstractPackResources {
    private static final ResourceLocation THAUMIUM = ArcaneGT.id("tags/items/gregtech_tools/thaumium.json");
    private static final ResourceLocation VOID = ArcaneGT.id("tags/items/gregtech_tools/void.json");
    private static final String DESCRIPTION = "Thaumic Omniverse GregTech repairable tools";

    private RepairableToolPack(String id) { super(id, true); }

    static void register(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) return;
        var pack = Pack.readMetaAndCreate("thaumicomniverse_gregtech_tools", Component.literal(DESCRIPTION),
                true, RepairableToolPack::new, PackType.SERVER_DATA, Pack.Position.BOTTOM, PackSource.BUILT_IN);
        if (pack != null) event.addRepositorySource(output -> output.accept(pack));
    }

    private static InputStream tag(Material material) {
        var values = new JsonArray();
        ForgeRegistries.ITEMS.getEntries().stream()
                .filter(entry -> entry.getKey().location().getNamespace().equals(ThaumicOmniverse.MOD_ID))
                .filter(entry -> entry.getValue() instanceof IGTTool tool && tool.getMaterial() == material)
                .map(entry -> entry.getKey().location().toString()).sorted().forEach(values::add);
        var tag = new JsonObject();
        tag.add("values", values);
        return stream(tag.toString());
    }

    private static InputStream stream(String json) {
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }

    @Override public IoSupplier<InputStream> getRootResource(String... path) {
        return path.length == 1 && path[0].equals("pack.mcmeta")
                ? () -> stream("{\"pack\":{\"pack_format\":15,\"description\":\"" + DESCRIPTION + "\"}}") : null;
    }

    @Override public IoSupplier<InputStream> getResource(PackType type, ResourceLocation path) {
        if (type != PackType.SERVER_DATA) return null;
        if (THAUMIUM.equals(path)) return () -> tag(ArcaneMaterials.THAUMIUM);
        if (VOID.equals(path)) return () -> tag(ArcaneMaterials.VOID);
        return null;
    }

    @Override public void listResources(PackType type, String namespace, String prefix, ResourceOutput output) {
        if (type != PackType.SERVER_DATA || !ThaumicOmniverse.MOD_ID.equals(namespace)) return;
        for (var path : List.of(THAUMIUM, VOID)) {
            if (prefix.isEmpty() || path.getPath().startsWith(prefix + "/"))
                output.accept(path, getResource(type, path));
        }
    }

    @Override public Set<String> getNamespaces(PackType type) {
        return type == PackType.SERVER_DATA ? Set.of(ThaumicOmniverse.MOD_ID) : Set.of();
    }

    @Override public void close() {}
}
