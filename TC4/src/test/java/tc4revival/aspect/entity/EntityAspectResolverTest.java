package tc4revival.aspect.entity;

import com.google.gson.JsonParser;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.AspectCatalog;
import tc4revival.aspect.LegacyAspect;
import tc4revival.data.LegacyEntityDefinitions;
import static org.junit.jupiter.api.Assertions.*;

class EntityAspectResolverTest {
    private final AspectCatalog catalog = new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList());
    private final EntityAspectResolver resolver = new EntityAspectResolver(LegacyEntityDefinitions.rules(), catalog);

    private EntityFacts ordinary(String type) { return new EntityFacts("minecraft:" + type, null, false, false, false, false, true); }

    @Test void clueNamesExcludeHashQualifiersAndPlayerIdentities() {
        var powered = new EntityFacts("minecraft:creeper", null, false, false, true, true, false);
        assertEquals("Creeper", resolver.legacyName(powered).orElseThrow());
        assertEquals("Enderman", resolver.legacyName(ordinary("enderman")).orElseThrow());
        assertTrue(resolver.legacyName(new EntityFacts("minecraft:player", "TC4Tester", false, false, false, false, false)).isEmpty());
        assertTrue(resolver.legacyName(ordinary("warden")).isEmpty());
    }

    @Test void vanillaDefinitionsIncludeRenamedAndSplitOldEntityTypes() {
        assertEquals(48, resolver.ruleCount());
        for (String type : List.of("horse", "donkey", "mule", "skeleton_horse", "zombie_horse")) {
            var horse = resolver.resolve(ordinary(type)).orElseThrow();
            assertEquals("EntityHorse", horse.legacyIdentity());
            assertEquals(4, horse.aspects().amount(catalog.require("bestia")));
            assertEquals(6, horse.aspects().total());
        }
        assertEquals(resolver.resolve(ordinary("ocelot")).orElseThrow().scanHash(),
                resolver.resolve(ordinary("cat")).orElseThrow().scanHash());
        assertEquals("PigZombie", resolver.resolve(ordinary("zombified_piglin")).orElseThrow().legacyIdentity());
    }

    @Test void poweredCreeperOverridesBaseAndKeepsBothOldIdentityQualifiers() {
        var ordinary = resolver.resolve(ordinary("creeper")).orElseThrow();
        assertEquals(4, ordinary.aspects().total());
        var powered = resolver.resolve(new EntityFacts("minecraft:creeper", null, false, false, true, true, false)).orElseThrow();
        assertEquals(9, powered.aspects().total());
        assertEquals(3, powered.aspects().amount(catalog.require("potentia")));
        assertEquals("Creeperpowered1FLASHINGPOWERED", powered.legacyIdentity());
    }

    @Test void witherSkeletonAndBabyZombieVillagerRetainOldVariantIdentities() {
        var skeleton = resolver.resolve(ordinary("wither_skeleton")).orElseThrow();
        assertEquals("SkeletonSkeletonType1", skeleton.legacyIdentity());
        assertEquals(4, skeleton.aspects().amount(catalog.require("exanimis")));
        assertEquals(2, skeleton.aspects().amount(catalog.require("ignis")));
        var villager = resolver.resolve(new EntityFacts("minecraft:zombie_villager", null, true, true, false, false, false)).orElseThrow();
        assertEquals("ZombieCHILDVILLAGER", villager.legacyIdentity());
        assertEquals(4, villager.aspects().total());
    }

    @Test void childChangesScanIdentityButDoesNotScaleAspectAmounts() {
        var adult = resolver.resolve(ordinary("pig")).orElseThrow();
        var baby = resolver.resolve(new EntityFacts("minecraft:pig", null, true, false, false, false, false)).orElseThrow();
        assertEquals("PigCHILD", baby.legacyIdentity());
        assertNotEquals(adult.scanHash(), baby.scanHash());
        assertEquals(adult.aspects().view(), baby.aspects().view());
    }

    @Test void newEntitiesAndNewBoatVariantsRemainUnassigned() {
        for (String type : List.of("phantom", "warden", "rabbit", "armor_stand", "glow_squid", "chest_boat")) {
            assertTrue(resolver.resolve(ordinary(type)).isEmpty(), type);
        }
        assertEquals(4, resolver.resolve(ordinary("boat")).orElseThrow().aspects().total());
        assertTrue(resolver.resolve(new EntityFacts("minecraft:boat", null, false, false, false, false, false)).isEmpty());
    }

    @Test void specialPlayerNamesAndDeterministicDrawsMatchTheOldRules() {
        var azanor = resolver.resolve(new EntityFacts("minecraft:player", "AzAnOr", false, false, false, false, false)).orElseThrow();
        assertEquals(20, azanor.aspects().amount(catalog.require("alienis")));
        assertEquals(4, azanor.aspects().amount(catalog.require("humanus")));
        var player = new EntityFacts("minecraft:player", "TC4Tester", false, false, false, false, false);
        var first = resolver.resolve(player).orElseThrow();
        var second = resolver.resolve(player).orElseThrow();
        assertEquals(16, first.aspects().total());
        assertEquals(first.aspects().view(), second.aspects().view());
        assertEquals("player_TC4Tester", first.legacyIdentity());
        first.aspects().remove(catalog.require("humanus"));
        assertEquals(16, resolver.resolve(player).orElseThrow().aspects().total());
    }

    @Test void generatedJsonRetainsEveryOrderedRuleAndQuantity() {
        var original = LegacyEntityDefinitions.rules();
        assertEquals(original, EntityAspectJson.decode(EntityAspectJson.encode(original)));
    }

    @Test void malformedOrAmbiguousDataCannotBecomeAnActiveTable() {
        var one = LegacyEntityDefinitions.rules().get(0);
        assertThrows(IllegalArgumentException.class, () -> new EntityAspectResolver(List.of(one, one), catalog));
        var invalid = new EntityAspectRule("minecraft:pig", "Pig", EntityAspectRule.Variant.ANY, "",
                List.of(new EntityAspectRule.Quantity("missing", 2)));
        assertThrows(IllegalArgumentException.class, () -> new EntityAspectResolver(List.of(invalid), catalog));
        var json = EntityAspectJson.encode(List.of(one));
        json.getAsJsonArray("entries").get(0).getAsJsonObject().getAsJsonArray("aspects").get(0).getAsJsonObject().addProperty("amount", 1.5);
        assertThrows(ArithmeticException.class, () -> EntityAspectJson.decode(json));
        assertThrows(IllegalArgumentException.class, () -> EntityAspectJson.decode(JsonParser.parseString("{\"format\":\"1\",\"entries\":[]}")));
    }
}
