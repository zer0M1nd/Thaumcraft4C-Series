package tc4revival.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import org.junit.jupiter.api.Test;
import thaumcraft.api.config.*;
import static org.junit.jupiter.api.Assertions.*;

class ConfigMigrationTest {
    @Test void siblingImportPreservesWorldSettingsAndNeverOverwritesConfiguredTarget(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) throws Exception {
        var old = dir.resolve("omniverse-server.toml");
        var original = "[revival_features]\nrestored = \"FALSE\"\n";
        java.nio.file.Files.writeString(old, original);
        var builder = new ForgeConfigSpec.Builder();
        var restored = builder.defineEnum("revival_features.restored", FeatureSetting.DEFAULT);
        var migration = new MigratingConfigSpec(builder.build()).fromSibling(old.getFileName().toString());
        try (var target = com.electronwill.nightconfig.core.file.CommentedFileConfig.builder(dir.resolve("futures-server.toml")).build()) {
            assertTrue(migration.needsMigration(target));
            migration.correct(target); migration.acceptConfig(target);
            assertEquals(FeatureSetting.FALSE, restored.get());
            assertEquals(original, java.nio.file.Files.readString(old));
            target.set("revival_features.restored", "TRUE");
            migration.acceptConfig(target);
            assertEquals(FeatureSetting.TRUE, restored.get());
            assertEquals(0, migration.migrate(target));
        }
        migration.acceptConfig(null);
    }
    @Test void siblingImportDoesNotReadIntoNetworkConfigsOrAllowDirectoryTraversal() {
        var spec = new ForgeConfigSpec.Builder().build();
        var migration = new MigratingConfigSpec(spec).fromSibling("old.toml");
        assertFalse(migration.needsMigration(CommentedConfig.inMemory()));
        for (var name : java.util.List.of("", ".", "..", "../old.toml", "folder/old.toml"))
            assertThrows(IllegalArgumentException.class, () -> migration.fromSibling(name), name);
    }
    @Test void restoredOptionDistinguishesFreshExistingAndExplicitWorlds() {
        var builder = new ForgeConfigSpec.Builder();
        var option = builder.define("enabled", false);
        var prior = builder.defineInRange("oldCapacity", 50, 0, 100);
        var spec = builder.build();
        var migration = new MigratingConfigSpec(spec).existingDefault("enabled", true, "oldCapacity");
        var fresh = CommentedConfig.inMemory();
        migration.correct(fresh); migration.acceptConfig(fresh);
        assertFalse(option.get());
        var old = CommentedConfig.inMemory(); old.set("oldCapacity", 75);
        assertFalse(migration.isCorrect(old));
        migration.correct(old); migration.acceptConfig(old);
        assertTrue(option.get()); assertEquals(75, prior.get());
        assertEquals(0, migration.migrate(old));
        var explicit = CommentedConfig.inMemory(); explicit.set("enabled", false); explicit.set("oldCapacity", 75);
        migration.correct(explicit); migration.acceptConfig(explicit);
        assertFalse(option.get()); assertEquals(75, prior.get());
        migration.acceptConfig(null);
    }
    @Test void commonFileImportsOnlyIntoAnEmptyWorldConfig(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) throws Exception {
        var file = dir.resolve("old.toml");
        java.nio.file.Files.writeString(file, "[crafting]\nextended_cells = false\n");
        var b = new ForgeConfigSpec.Builder();
        var value = b.defineEnum("revival_features.extended_cells", FeatureSetting.DEFAULT);
        var migration = new MigratingConfigSpec(b.build()).fromFile(() -> file)
                .booleanOption("crafting.extended_cells", "revival_features.extended_cells");
        var fresh = CommentedConfig.inMemory(); migration.acceptConfig(fresh);
        assertEquals(FeatureSetting.FALSE, value.get());
        assertEquals("[crafting]\nextended_cells = false\n", java.nio.file.Files.readString(file));
        var configured = CommentedConfig.inMemory(); configured.set("revival_features.extended_cells", "TRUE");
        migration.acceptConfig(configured); assertEquals(FeatureSetting.TRUE, value.get());
        assertEquals(0, migration.migrate(configured)); migration.acceptConfig(null);
    }
    @Test void migrationRunsBeforeForgeDropsUnknownKeysAndFansOutOldSwitch() {
        var b = new ForgeConfigSpec.Builder();
        var a = b.defineEnum("revival_features.a", FeatureSetting.DEFAULT);
        var c = b.defineEnum("revival_features.c", FeatureSetting.DEFAULT);
        var spec = b.build();
        var migration = new MigratingConfigSpec(spec).booleanOption("old", "revival_features.a").booleanOption("old", "revival_features.c");
        var data = CommentedConfig.inMemory(); data.set("old", false);
        assertFalse(migration.isCorrect(data));
        migration.correct(data); migration.acceptConfig(data);
        assertEquals(FeatureSetting.FALSE, a.get()); assertEquals(FeatureSetting.FALSE, c.get());
        assertFalse(data.contains("old")); assertEquals(0, migration.migrate(data));
        migration.acceptConfig(null);
        assertFalse(spec.isLoaded());
    }
    @Test void explicitNewKeyWinsAndInvalidLegacyValuesUseForgeValidation() {
        var b = new ForgeConfigSpec.Builder();
        var value = b.defineEnum("new", FeatureSetting.DEFAULT); var spec = b.build();
        var migration = new MigratingConfigSpec(spec).booleanOption("old", "new");
        var data = CommentedConfig.inMemory(); data.set("old", false); data.set("new", "TRUE");
        migration.acceptConfig(data); assertEquals(FeatureSetting.TRUE, value.get());
        var invalid = CommentedConfig.inMemory(); invalid.set("old", "invalid");
        migration.acceptConfig(invalid); assertEquals(FeatureSetting.DEFAULT, value.get());
        var fresh = CommentedConfig.inMemory(); migration.acceptConfig(fresh);
        assertEquals(FeatureSetting.DEFAULT, value.get()); assertFalse(fresh.contains("old"));
    }
}
