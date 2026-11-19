package tc4revival.data;

import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DefinitionAliasTest {
    @Test void exactLegacyDeclarationAllowsMigrationWithoutOpeningTheWholeNamespace() {
        var aliases = Set.of("thaumicomniverse:restored_rod");
        assertDoesNotThrow(() -> AddonDefinitions.validateNamespace("thaumicfutures", "thaumicfutures:new_rod", aliases));
        assertDoesNotThrow(() -> AddonDefinitions.validateNamespace("thaumicfutures", "thaumicomniverse:restored_rod", aliases));
        assertThrows(IllegalArgumentException.class, () -> AddonDefinitions.validateNamespace("thaumicfutures", "thaumicomniverse:other_rod", aliases));
    }
    @Test void declarationCannotClaimCoreOrVanillaDefinitions() {
        for (var id : Set.of("thaumcraft:greatwood", "minecraft:stick", "INVALID ID"))
            assertThrows(IllegalArgumentException.class, () -> AddonDefinitions.validateNamespace("thaumicfutures", id, Set.of(id)));
    }
}
