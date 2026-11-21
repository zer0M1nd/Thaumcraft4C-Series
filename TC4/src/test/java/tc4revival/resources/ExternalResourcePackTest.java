package tc4revival.resources;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.world.flag.FeatureFlagSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ExternalResourcePackTest {
    @TempDir Path directory;
    @Test void invalidLegacyCaseDoesNotAbortAtlasEnumeration() throws Exception {
        String valid="assets/automagy/textures/block/blocktally.png";
        String invalid="assets/automagy/textures/block/blockTally.png";
        Files.createDirectories(directory.resolve(valid).getParent());
        Files.writeString(directory.resolve(valid),"texture");
        var seen=new java.util.HashSet<ResourceLocation>();
        try(var pack=new ExternalResourcePack("test",directory,Set.of(valid,invalid))) {
            assertDoesNotThrow(()->pack.listResources(PackType.CLIENT_RESOURCES,"automagy","textures",(id,stream)->seen.add(id)));
            assertEquals(Set.of(ResourceLocation.parse("automagy:textures/block/blocktally.png")),seen);
        }
    }
    private static final String TEXTURE = "assets/thaumcraft/textures/item/example.png";
    private static final String LANGUAGE = "assets/thaumcraft/lang/en_us.json";
    private static final String SOUND = "assets/thaumcraft/sounds/example.ogg";
    private static final Set<String> ALLOWED = Set.of(TEXTURE, LANGUAGE, SOUND);

    private Pack pack(String id, boolean required, boolean fixed, boolean external) throws Exception {
        Path root = directory.resolve(id);
        for (String asset : ALLOWED) {
            Files.createDirectories(root.resolve(asset).getParent());
            Files.writeString(root.resolve(asset), id);
        }
        SharedConstants.tryDetectVersion();
        return Pack.create(id, Component.literal(id), required,
                name -> external ? new ExternalResourcePack(name, root, ALLOWED) : new PathPackResources(name, root, false),
                new Pack.Info(Component.literal(id), 15, FeatureFlagSet.of()), PackType.CLIENT_RESOURCES,
                Pack.Position.BOTTOM, fixed, PackSource.BUILT_IN);
    }

    @Test void realRepositoryOrdersExternalBelowModAdaptationAndUserPacksAcrossReloads() throws Exception {
        var vanilla = pack("vanilla", true, false, false);
        var mod = pack("mod_resources", true, false, false);
        var external = pack("thaumcraft_original_resources", true, true, true);
        var userA = pack("file/user-a", false, false, false);
        var userB = pack("file/user-b", false, false, false);
        var repository = new PackRepository(consumer -> List.of(vanilla, mod, external, userA, userB).forEach(consumer));
        repository.reload();
        repository.setSelected(List.of("vanilla", "mod_resources", "file/user-a", "file/user-b"));
        assertTrue(List.copyOf(repository.getSelectedIds()).indexOf(external.getId())
                < List.copyOf(repository.getSelectedIds()).indexOf(mod.getId()));
        assertWinner(repository, "file/user-b");
        repository.reload();
        assertWinner(repository, "file/user-b");
        repository.setSelected(List.of("vanilla", "mod_resources", "file/user-b", "file/user-a"));
        assertWinner(repository, "file/user-a");
        repository.setSelected(List.of("vanilla", "mod_resources"));
        assertWinner(repository, "mod_resources");
    }

    private void assertWinner(PackRepository repository, String expected) throws Exception {
        var ordered = ExternalResourceOrder.normalize(List.copyOf(repository.getSelectedPacks()));
        assertEquals(List.of("vanilla", "thaumcraft_original_resources", "mod_resources"),
                ordered.subList(0, 3).stream().map(Pack::getId).toList());
        try (var manager = new MultiPackResourceManager(PackType.CLIENT_RESOURCES, ordered.stream().map(Pack::open).toList())) {
            for (String asset : ALLOWED) {
                var location = ResourceLocation.fromNamespaceAndPath("thaumcraft", asset.substring("assets/thaumcraft/".length()));
                try (var input = manager.getResource(location).orElseThrow().open()) {
                    assertEquals(expected, new String(input.readAllBytes(), StandardCharsets.UTF_8), asset);
                }
                assertEquals(expected, manager.getResourceStack(location).get(manager.getResourceStack(location).size() - 1).sourcePackId());
            }
        }
    }

    @Test void cacheExtrasAreNeverExposedEvenThroughListingOrRootLookups() throws Exception {
        var external = pack("external", true, true, true);
        var root = directory.resolve("external");
        Files.writeString(root.resolve("assets/thaumcraft/textures/item/stale.png"), "stale");
        Files.writeString(root.resolve("complete.txt"), "private");
        try (var pack = external.open()) {
            assertNull(pack.getRootResource("complete.txt"));
            assertNull(pack.getResource(PackType.CLIENT_RESOURCES, ResourceLocation.parse("thaumcraft:textures/item/stale.png")));
            var seen = new java.util.HashSet<ResourceLocation>();
            pack.listResources(PackType.CLIENT_RESOURCES, "thaumcraft", "textures", (location, stream) -> seen.add(location));
            assertEquals(Set.of(ResourceLocation.parse("thaumcraft:textures/item/example.png")), seen);
            assertTrue(pack.getNamespaces(PackType.SERVER_DATA).isEmpty());
        }
    }

    @Test void minecraftLanguageLoaderMergesKeysAndReadsUserChangesOnReload() throws Exception {
        var vanilla = pack("vanilla", true, false, false);
        var external = pack("thaumcraft_original_resources", true, true, true);
        var mod = pack("mod_resources", true, false, false);
        var user = pack("user", false, false, false);
        Files.writeString(directory.resolve("vanilla").resolve(LANGUAGE), "{}");
        Files.writeString(directory.resolve(external.getId()).resolve(LANGUAGE),
                "{\"shared\":\"legacy\",\"legacy_only\":\"original book text\"}");
        Files.writeString(directory.resolve(mod.getId()).resolve(LANGUAGE),
                "{\"shared\":\"adapted\",\"own_only\":\"port text\"}");
        var order = ExternalResourceOrder.normalize(List.of(user, external, mod, vanilla));
        for (String replacement : List.of("user first", "user reloaded")) {
            Files.writeString(directory.resolve(user.getId()).resolve(LANGUAGE), "{\"shared\":\"" + replacement + "\"}");
            try (var manager = new MultiPackResourceManager(PackType.CLIENT_RESOURCES, order.stream().map(Pack::open).toList())) {
                var language = net.minecraft.client.resources.language.ClientLanguage.loadFrom(manager, List.of("en_us"), false);
                assertEquals(replacement, language.getOrDefault("shared", "missing"));
                assertEquals("original book text", language.getOrDefault("legacy_only", "missing"));
                assertEquals("port text", language.getOrDefault("own_only", "missing"));
            }
        }
    }
}
