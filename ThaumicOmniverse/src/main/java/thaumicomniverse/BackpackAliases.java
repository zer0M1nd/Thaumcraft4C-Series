package thaumicomniverse;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.*;

/** Registry aliases also resolve stacks nested inside backpack upgrade NBT. */
final class BackpackAliases {
    private static final List<String> RETURNED = List.of("arcane_crafting_upgrade", "energized_node_upgrade");

    static void register(RegisterEvent event) {
        if (!event.getRegistryKey().equals(ForgeRegistries.Keys.ITEMS)
                || !net.minecraftforge.fml.ModList.get().isLoaded("sophisticatedbackpacks")) return;
        if (ForgeRegistries.ITEMS instanceof ForgeRegistry<Item> registry)
            for (String path : RETURNED) registry.addAlias(id("thaumicfutures", path), id("thaumicomniverse", path));
    }

    static void missing(MissingMappingsEvent event) {
        for (var mapping : event.getMappings(ForgeRegistries.Keys.ITEMS, "thaumicfutures")) {
            String path = mapping.getKey().getPath();
            if (RETURNED.contains(path) && ForgeRegistries.ITEMS.containsKey(id("thaumicomniverse", path)))
                mapping.remap(ForgeRegistries.ITEMS.getValue(id("thaumicomniverse", path)));
        }
    }

    private static ResourceLocation id(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }
}
