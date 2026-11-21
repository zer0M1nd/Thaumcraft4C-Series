package tc4revival.research;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.AspectCatalog;
import tc4revival.aspect.LegacyAspect;
import tc4revival.research.PlayerKnowledge.ScanKind;
import tc4revival.research.PlayerKnowledge.ScanMode;
import tc4revival.research.PlayerKnowledge.WarpKind;
import static org.junit.jupiter.api.Assertions.*;

class KnowledgeTest {
    @Test void clearScansPreservesOtherKnowledgeAndAllowsBothModesAgain() {
        var player = PlayerKnowledge.firstLogin(catalog, bound -> 2);
        player.setPool(aspect("vitreus"), (short) 31);
        player.rememberResearch("RESEARCHER1");
        player.setWarp(WarpKind.PERMANENT, 7);
        player.setWarpCounter(12);
        for (var kind : ScanKind.values()) {
            player.rememberScan(kind, "@normal");
            player.rememberScan(kind, "#enhanced");
        }
        var expected = KnowledgeNbt.write(player);
        for (var kind : ScanKind.values()) expected.put("scans_" + kind.name().toLowerCase(java.util.Locale.ROOT), new net.minecraft.nbt.ListTag());
        assertEquals(6, player.clearScans());
        assertEquals(expected, KnowledgeNbt.write(player));
        var loaded = KnowledgeNbt.read(KnowledgeNbt.write(player), catalog).knowledge();
        for (var kind : ScanKind.values()) for (var mode : ScanMode.values()) {
            assertTrue(loaded.canScan(kind, "normal", mode));
            assertTrue(loaded.canScan(kind, "enhanced", mode));
        }
        assertEquals(0, loaded.clearScans());
    }
    private final AspectCatalog catalog = new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList());
    private Aspect aspect(String tag) { return catalog.require(tag); }

    @Test void firstLoginRollsSixPoolsOfFifteenToNineteenInOriginalOrder() {
        var calls = new AtomicInteger();
        var player = PlayerKnowledge.firstLogin(catalog, bound -> { assertEquals(5, bound); return calls.getAndIncrement() % 5; });
        assertEquals(6, calls.get());
        assertEquals(15, player.pool(aspect("aer")));
        assertEquals(16, player.pool(aspect("terra")));
        assertEquals(17, player.pool(aspect("ignis")));
        assertEquals(List.of("aer", "ignis", "terra", "aqua", "ordo", "perditio"),
                player.knownAspects().keySet().stream().map(Aspect::tag).toList());
        assertEquals(6, player.knownAspects().size());
    }

    @Test void parentsMustBeDiscoveredEvenWhenTheChildWasGrantedSeparately() {
        var player = new PlayerKnowledge(catalog);
        assertTrue(player.knowsParents(aspect("vitreus")));
        assertFalse(player.knowsParents(aspect("metallum")));
        player.discover(aspect("metallum"));
        assertFalse(player.knowsParents(aspect("metallum")));
        assertTrue(player.discover(aspect("vitreus")));
        assertFalse(player.discover(aspect("vitreus")));
        assertTrue(player.knowsParents(aspect("metallum")));
    }

    @Test void insufficientDebitRejectsAtomicallyWithoutChangingBalance() {
        var player = new PlayerKnowledge(catalog);
        player.setPool(aspect("aer"), (short) 2);
        assertFalse(player.changePool(aspect("aer"), (short) -3));
        assertEquals(2, player.pool(aspect("aer")));
        assertTrue(player.changePool(aspect("aer"), (short) -2));
        assertEquals(0, player.pool(aspect("aer")));
        assertFalse(player.changePool(aspect("aer"), (short) -1));
        assertTrue(player.knows(aspect("aer")));
    }

    @Test void integerBalancesSurvivePersistenceAndShortCompatibilitySaturates() {
        var player = new PlayerKnowledge(catalog);
        player.setPool(aspect("aer"), Short.MAX_VALUE);
        player.changePool(aspect("aer"), (short) 1);
        assertEquals(32768, player.knownAspects().get(aspect("aer")));
        assertEquals(Short.MAX_VALUE, player.pool(aspect("aer")));
        assertEquals(32768, player.poolAmount(aspect("aer")));
        var reloaded = KnowledgeNbt.read(KnowledgeNbt.write(player), catalog).knowledge();
        assertEquals(Short.MAX_VALUE, reloaded.pool(aspect("aer")));
        assertEquals(32768, reloaded.poolAmount(aspect("aer")));
        assertEquals(32768, reloaded.knownAspects().get(aspect("aer")));
        assertTrue(reloaded.changePool(aspect("aer"), Integer.MAX_VALUE));
        assertEquals(Integer.MAX_VALUE, reloaded.poolAmount(aspect("aer")));
        assertFalse(reloaded.changePool(aspect("aer"), 1));
    }

    @Test void enhancedScanReplacesOrdinaryScanAndBlocksFurtherAttempts() {
        var player = new PlayerKnowledge(catalog);
        player.rememberScan(ScanKind.OBJECT, "@item");
        assertFalse(player.canScan(ScanKind.OBJECT, "item", ScanMode.THAUMOMETER));
        assertTrue(player.canScan(ScanKind.OBJECT, "item", ScanMode.ENHANCED));
        player.rememberScan(ScanKind.OBJECT, "#item");
        assertEquals(List.of("#item"), List.copyOf(player.scannedKeys(ScanKind.OBJECT)));
        assertFalse(player.canScan(ScanKind.OBJECT, "item", ScanMode.ENHANCED));
        assertFalse(player.canScan(ScanKind.OBJECT, "item", ScanMode.THAUMOMETER));
        assertTrue(player.canScan(ScanKind.ENTITY, "item", ScanMode.THAUMOMETER));
    }

    @Test void wipeClearsWarpCounterAndAllProgressButKeepsZeroPrimals() {
        var player = PlayerKnowledge.firstLogin(catalog, bound -> 4);
        player.rememberResearch("TEST");
        player.discover(aspect("vitreus"));
        player.rememberScan(ScanKind.ENTITY, "@mob");
        player.setWarp(WarpKind.PERMANENT, 10);
        player.setWarpCounter(80);
        player.wipe();
        assertEquals(0, player.warpCounter());
        assertEquals(0, player.totalWarp());
        assertEquals(6, player.knownAspects().size());
        assertTrue(player.knownAspects().values().stream().allMatch(value -> value == 0));
        assertTrue(player.researchMarkers().isEmpty());
        assertTrue(player.scannedKeys(ScanKind.ENTITY).isEmpty());
    }

    @Test void nbtRetainsAllKnowledgeCategoriesAndRepairsUnknownAspects() {
        var player = new PlayerKnowledge(catalog);
        player.setPool(aspect("vitreus"), (short) 7);
        player.rememberResearch("@TEST");
        for (ScanKind kind : ScanKind.values()) player.rememberScan(kind, "#" + kind.name());
        player.setWarp(WarpKind.PERMANENT, 5);
        player.setWarp(WarpKind.STICKY, 7);
        player.setWarp(WarpKind.TEMPORARY, 9);
        player.setWarpCounter(21);
        var data = KnowledgeNbt.write(player);
        var unknown = new CompoundTag();
        unknown.putString("key", "unknown");
        unknown.putShort("amount", (short) 8);
        data.getList("aspects", Tag.TAG_COMPOUND).add(unknown);
        var result = KnowledgeNbt.read(data, catalog);
        assertEquals(1, result.problems().size());
        var loaded = result.knowledge();
        assertEquals(player.knownAspects(), loaded.knownAspects());
        assertEquals(player.researchMarkers(), loaded.researchMarkers());
        for (ScanKind kind : ScanKind.values()) assertEquals(player.scannedKeys(kind), loaded.scannedKeys(kind));
        assertEquals(21, loaded.totalWarp());
        assertEquals(21, loaded.warpCounter());
    }

    @Test void worldDataKeepsPlayersSeparateAndRollsBackFailedUpdates() {
        var world = new KnowledgeSavedData(catalog);
        UUID first = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID second = UUID.fromString("22222222-2222-2222-2222-222222222222");
        world.ensure(first, bound -> 0);
        world.ensure(second, bound -> 4);
        world.ensure(first, bound -> { fail("Existing players must not reroll initial pools"); return 0; });
        assertTrue(world.isDirty());
        assertThrows(IllegalArgumentException.class, () -> world.update(first, bound -> 0, copy -> {
            copy.setPool(aspect("aer"), (short) 80);
            throw new IllegalArgumentException("reject");
        }));
        var snapshot = world.snapshot(first).orElseThrow();
        snapshot.setPool(aspect("aer"), (short) 30);
        assertEquals(15, world.snapshot(first).orElseThrow().pool(aspect("aer")));
        var reloaded = KnowledgeSavedData.load(world.save(new CompoundTag()), catalog);
        assertEquals(15, reloaded.snapshot(first).orElseThrow().pool(aspect("aer")));
        assertEquals(19, reloaded.snapshot(second).orElseThrow().pool(aspect("aer")));
    }

    @Test void unsupportedWorldContainerIsPreservedInsteadOfReplacedWithEmptyKnowledge() {
        var original = new CompoundTag();
        original.putInt("version", 999);
        original.putString("future_data", "retain");
        var loaded = KnowledgeSavedData.load(original, catalog);
        assertFalse(loaded.isDirty());
        assertThrows(IllegalStateException.class, () -> loaded.ensure(UUID.randomUUID(), bound -> 0));
        assertEquals(original, loaded.save(new CompoundTag()));
    }
}
