package tc4revival.research;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.AspectCatalog;
import tc4revival.aspect.LegacyAspect;
import tc4revival.research.ResearchDefinition.Flag;
import static org.junit.jupiter.api.Assertions.*;

class ResearchRulesTest {
    private final AspectCatalog aspects = new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList());
    private JsonArray evidence() {
        try (var reader = new InputStreamReader(java.util.Objects.requireNonNull(getClass().getResourceAsStream("/research-catalog.json")), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonArray();
        } catch (java.io.IOException failure) { throw new AssertionError(failure); }
    }
    private ResearchCatalog catalog() { return ResearchCatalog.decode(evidence(), aspects); }
    private PlayerKnowledge player() { return new PlayerKnowledge(aspects); }
    private ResearchRules.ItemClue wool() { return new ResearchRules.ItemClue(stack -> stack.key().item().equals("minecraft:wool")); }

    @Test void automaticResearchIsPersistedOnceWithoutCostsOrClueRemoval() {
        var catalog = catalog();
        var progression = new ResearchProgression(catalog, entry -> true);
        var player = player();
        player.rememberResearch("@THAUMIUM");
        player.setWarpCounter(73);
        var pools = Map.copyOf(player.knownAspects());
        var completions = progression.unlockAutomatic(player, false);
        assertEquals(List.of("WARP", "RESEARCH", "ENCHANT", "NODES", "ORE", "ASPECTS", "KNOWFRAG",
                "THAUMONOMICON", "PLANTS", "PECH", "BASICTHAUMATURGY", "CAP_iron", "ROD_wood", "CRUCIBLE",
                "PHIAL", "TABLE", "BASICARTIFACE", "THAUMOMETER", "RESTABLE", "GRATE", "ARCTABLE", "ARCANESTONE"),
                completions.stream().map(ResearchProgression.Completion::key).toList());
        assertEquals(23, player.researchMarkers().size());
        assertTrue(player.researchMarkers().contains("@THAUMIUM"));
        assertEquals(pools, player.knownAspects());
        assertEquals(73, player.warpCounter());
        assertTrue(progression.unlockAutomatic(player, false).isEmpty());
    }

    @Test void automaticResearchIgnoresDisabledParentsAndDoesNotUnlockSiblings() {
        var progression = new ResearchProgression(catalog(), entry -> !entry.key().equals("TABLE") && !entry.key().equals("RESEARCH"));
        var player = player();
        progression.unlockAutomatic(player, false);
        assertFalse(player.researchMarkers().contains("TABLE"));
        assertFalse(player.researchMarkers().contains("RESEARCH"));
        assertTrue(player.researchMarkers().containsAll(List.of("RESTABLE", "ARCTABLE", "KNOWFRAG", "THAUMONOMICON")));
        assertFalse(player.researchMarkers().contains("COREGATHER"));
    }

    @Test void researchWarpUsesCeilingPermanentFloorStickyAndRefreshesTotalCounterOnlyOnce() {
        var progression = new ResearchProgression(catalog(), entry -> true);
        var player = player();
        player.setWarp(PlayerKnowledge.WarpKind.PERMANENT, 4);
        player.setWarp(PlayerKnowledge.WarpKind.STICKY, 5);
        player.setWarp(PlayerKnowledge.WarpKind.TEMPORARY, 6);
        var keys = List.of("RESEARCHER2", "FOCUSHELLBAT", "CRIMSON", "ADVANCEDGOLEM", "OCULUS");
        int[] values = {1, 2, 3, 5, 6};
        for (int i = 0; i < keys.size(); i++) {
            var result = progression.completeTrusted(player, keys.get(i), false).orElseThrow();
            assertEquals((values[i] + 1) / 2, result.permanentWarp());
            assertEquals(values[i] / 2, result.stickyWarp());
            assertEquals(player.totalWarp(), player.warpCounter());
        }
        assertEquals(14, player.warp(PlayerKnowledge.WarpKind.PERMANENT));
        assertEquals(12, player.warp(PlayerKnowledge.WarpKind.STICKY));
        assertEquals(6, player.warp(PlayerKnowledge.WarpKind.TEMPORARY));
        player.setWarpCounter(2);
        assertTrue(progression.completeTrusted(player, "OCULUS", false).isEmpty());
        assertEquals(2, player.warpCounter());
        assertEquals(32, player.totalWarp());
    }

    @Test void wussModeCluesAndUnknownTrustedMarkersDoNotAwardWarpOrResetCounter() {
        var progression = new ResearchProgression(catalog(), entry -> true);
        var player = player();
        player.setWarpCounter(29);
        for (String key : List.of("@FOCUSHELLBAT", "UNKNOWN_TRUSTED")) {
            assertEquals(new ResearchProgression.Completion(key, 0, 0), progression.completeTrusted(player, key, false).orElseThrow());
        }
        progression.completeTrusted(player, "FOCUSHELLBAT", true);
        assertEquals(0, player.totalWarp());
        assertEquals(29, player.warpCounter());
        assertTrue(progression.completeTrusted(player, "FOCUSHELLBAT", false).isEmpty());
        assertEquals(0, player.totalWarp());
    }

    @Test void eligibleCompletionChecksParentsAndRetainsClue() {
        var progression = new ResearchProgression(catalog(), entry -> true);
        var player = player();
        player.rememberResearch("@FOCUSHELLBAT");
        assertTrue(progression.completeEligible(player, "FOCUSHELLBAT", false).isEmpty());
        player.rememberResearch("FOCUSFIRE");
        player.rememberResearch("INFUSION");
        assertEquals(List.of("FOCUSHELLBAT"), progression.completeEligible(player, "FOCUSHELLBAT", false).stream()
                .map(ResearchProgression.Completion::key).toList());
        assertTrue(player.researchMarkers().contains("@FOCUSHELLBAT"));
        assertEquals(2, player.totalWarp());
        assertTrue(progression.completeEligible(player, "FOCUSHELLBAT", false).isEmpty());
        assertTrue(progression.completeEligible(player, "missing", false).isEmpty());
        assertTrue(new ResearchProgression(catalog(), entry -> false).completeEligible(player, "WARP", false).isEmpty());
    }

    @Test void siblingsUseUpdatedKnowledgeAndListedOrderWithoutRecursiveTraversal() {
        var original = new ResearchProgression(catalog(), entry -> true);
        var player = player();
        player.rememberResearch("HUNGRYCHEST");
        assertEquals(List.of("GOLEMSTRAW", "COREGATHER", "GOLEMBELL"), original.completeEligible(player, "GOLEMSTRAW", false)
                .stream().map(ResearchProgression.Completion::key).toList());
        var data = evidence();
        for (var category : data) for (var value : category.getAsJsonObject().getAsJsonArray("research")) {
            var entry = value.getAsJsonObject();
            switch (entry.get("key").getAsString()) {
                case "COREGATHER" -> entry.add("siblings", JsonParser.parseString("[\"GOLEMWOOD\"]"));
                case "GOLEMWOOD" -> entry.add("parents", new JsonArray());
                case "GOLEMBELL" -> entry.add("parents", JsonParser.parseString("[\"COREGATHER\"]"));
                default -> { }
            }
        }
        var altered = ResearchCatalog.decode(data, aspects);
        var second = player();
        second.rememberResearch("HUNGRYCHEST");
        assertEquals(List.of("GOLEMSTRAW", "COREGATHER", "GOLEMBELL"),
                new ResearchProgression(altered, entry -> true).completeEligible(second, "GOLEMSTRAW", false)
                        .stream().map(ResearchProgression.Completion::key).toList());
        assertFalse(second.researchMarkers().contains("GOLEMWOOD"));
        var filtered = player();
        filtered.rememberResearch("HUNGRYCHEST");
        assertEquals(List.of("GOLEMSTRAW"), new ResearchProgression(altered, entry -> !entry.key().equals("COREGATHER"))
                .completeEligible(filtered, "GOLEMSTRAW", false).stream().map(ResearchProgression.Completion::key).toList());
    }

    @Test void runtimeBaselineRetainsAllCategoriesAndObservedResearchMetadata() {
        var catalog = catalog();
        assertEquals(List.of("BASICS", "THAUMATURGY", "ALCHEMY", "ARTIFICE", "GOLEMANCY", "ELDRITCH"),
                catalog.categories().stream().map(ResearchCatalog.Category::key).toList());
        assertEquals(List.of(19, 41, 29, 50, 39, 16), catalog.categories().stream().map(entry -> entry.research().size()).toList());
        assertEquals(194, catalog.all().size());
        assertEquals("WARP", catalog.all().get(0).key());
        var focus = catalog.require("FOCUSHELLBAT");
        assertEquals(List.of("FOCUSFIRE", "INFUSION"), focus.hiddenParents());
        assertEquals(2, focus.warp());
        assertTrue(focus.has(Flag.HIDDEN));
        assertEquals(List.of("ignis"), focus.triggers().aspects().orElseThrow());
        var brain = catalog.require("JARBRAIN").triggers().items().orElseThrow().get(0);
        assertEquals(3, brain.key().metadata());
        assertEquals(Optional.of("quicksilver"), brain.ore());
        assertThrows(UnsupportedOperationException.class, () -> focus.aspects().clear());
    }

    @Test void clueDoesNotRequireParentsAndOnlyRemembersOneMarker() {
        var rules = new ResearchRules(catalog());
        var player = player();
        assertFalse(rules.prerequisites(player, "MIRROR"));
        var enderman = new ResearchRules.EntityClue("Enderman");
        assertEquals(List.of("MIRROR"), rules.clueCandidates(player, enderman, Map.of()));
        assertEquals(Optional.of("MIRROR"), rules.createClue(player, enderman, Map.of(), bound -> { assertEquals(1, bound); return 0; }));
        assertEquals(java.util.Set.of("@MIRROR"), player.researchMarkers());
        assertFalse(rules.completed(player, "MIRROR"));
        assertTrue(rules.completed(player, "@MIRROR"));
        assertTrue(rules.createClue(player, enderman, Map.of(), bound -> fail("No random draw without candidates")).isEmpty());
    }

    @Test void actualPositiveRewardIsRequiredForAspectTriggersButNotItemTriggers() {
        var rules = new ResearchRules(catalog());
        var player = player();
        var irrelevant = new ResearchRules.EntityClue("Cow");
        assertEquals(List.of("THAUMIUM"), rules.clueCandidates(player, irrelevant, Map.of("metallum", 1)));
        assertTrue(rules.clueCandidates(player, irrelevant, Map.of("metallum", 0, "ignis", -1)).isEmpty());
        var bone = new ResearchRules.ItemClue(stack -> stack.key().item().equals("minecraft:bone"));
        assertEquals(List.of("BONEBOW"), rules.clueCandidates(player, bone, Map.of()));
        assertTrue(rules.clueCandidates(player, null, Map.of("metallum", 10)).isEmpty());
    }

    @Test void savedItemObservationsRecoverCluesBeforeParentsWithoutRewardsOrCompletion() {
        var rules=new ResearchRules(catalog());
        var player=player();
        var pools=Map.copyOf(player.knownAspects());
        var markers=java.util.Set.copyOf(player.researchMarkers());
        assertFalse(rules.prerequisites(player,"PRIMPEARL"));
        assertEquals(List.of("PRIMPEARL"),rules.restoreItemClues(player,s->s.key().item().equals("Thaumcraft:ItemEldritchObject") && s.key().metadata()==3));
        assertTrue(player.researchMarkers().contains("@PRIMPEARL"));
        assertFalse(rules.completed(player,"PRIMPEARL"));
        assertEquals(pools,player.knownAspects());
        assertEquals(markers.size()+1,player.researchMarkers().size());
        for(var parent:catalog().require("PRIMPEARL").parents())player.rememberResearch(parent);
        for(var parent:catalog().require("PRIMPEARL").hiddenParents())player.rememberResearch(parent);
        assertTrue(rules.prerequisites(player,"PRIMPEARL"));
        assertTrue(rules.completed(player,"@PRIMPEARL"));
        assertTrue(rules.restoreItemClues(player,s->true).stream().noneMatch("PRIMPEARL"::equals));
    }

    @Test void restorationCoversAllExplicitSharedTriggersAndRespectsDisabledResearch() {
        var rules=new ResearchRules(catalog(),entry->!entry.key().equals("TINYFEZ"));
        var player=player();
        var restored=rules.restoreItemClues(player,s->s.key().item().equals("minecraft:wool"));
        assertEquals(List.of("TINYBOWTIE","TINYHAT","TINYGLASSES"),restored);
        assertFalse(player.researchMarkers().contains("@TINYFEZ"));
        assertTrue(rules.restoreItemClues(player,s->s.key().item().equals("minecraft:wool")).isEmpty());
        assertEquals(List.of("TINYFEZ"),new ResearchRules(catalog()).restoreItemClues(player,s->s.key().item().equals("minecraft:wool")));
    }

    @Test void overlappingTriggersDoNotWeightRandomChoiceTwice() {
        var rules = new ResearchRules(catalog());
        var player = player();
        var candidates = rules.clueCandidates(player, wool(), Map.of("pannus", 3));
        assertEquals(List.of("TINYBOWTIE", "TINYHAT", "TINYFEZ", "TINYGLASSES"), candidates);
        assertEquals(Optional.of("TINYFEZ"), rules.createClue(player, wool(), Map.of("pannus", 3), bound -> { assertEquals(4, bound); return 2; }));
        player.rememberResearch("TINYHAT");
        assertEquals(List.of("TINYBOWTIE", "TINYGLASSES"), rules.clueCandidates(player, wool(), Map.of("pannus", 3)));
    }

    @Test void hiddenSearchChecksParentsAndExcludesAlreadyKnownClues() {
        var rules = new ResearchRules(catalog());
        var player = player();
        assertFalse(rules.hiddenCandidates(player).contains("FOCUSHELLBAT"));
        player.rememberResearch("FOCUSFIRE");
        assertFalse(rules.hiddenCandidates(player).contains("FOCUSHELLBAT"));
        player.rememberResearch("INFUSION");
        assertTrue(rules.hiddenCandidates(player).contains("FOCUSHELLBAT"));
        player.rememberResearch("@FOCUSHELLBAT");
        assertFalse(rules.hiddenCandidates(player).contains("FOCUSHELLBAT"));
        assertFalse(rules.hiddenCandidates(player).contains("BATHSALTS")); // null trigger arrays
        assertEquals(rules.findHidden(player, 100), rules.findHidden(player, 149));
        player.rememberResearch("FOCUSHELLBAT");
        assertFalse(rules.hiddenCandidates(player).contains("FOCUSHELLBAT"));
    }

    @Test void presentEmptyTriggersRemainDifferentFromAbsentTriggers() {
        var json = evidence();
        for (var category : json) for (var research : category.getAsJsonObject().getAsJsonArray("research")) {
            var value = research.getAsJsonObject();
            if (value.get("key").getAsString().equals("BATHSALTS")) value.add("itemTriggers", new JsonArray());
        }
        var rules = new ResearchRules(ResearchCatalog.decode(json, aspects));
        assertTrue(rules.hiddenCandidates(player()).contains("BATHSALTS"));
        assertFalse(rules.clueCandidates(player(), wool(), Map.of()).contains("BATHSALTS"));
    }

    @Test void completionRequiresCatalogForResearchButAcceptsUnknownClueMarkers() {
        var rules = new ResearchRules(catalog());
        var player = player();
        player.rememberResearch("MISSING");
        player.rememberResearch("@MISSING");
        assertFalse(rules.completed(player, "MISSING"));
        assertTrue(rules.completed(player, "@MISSING"));
        assertFalse(rules.prerequisites(player, "MISSING"));
        assertFalse(rules.completed(player, "CAP_iron")); // auto-unlock is not an implicit completion test
    }

    @Test void noteSelectionFollowsDifficultyAndBothKindsOfParents() {
        var rules = new ResearchRules(catalog());
        var player = player();
        catalog().all().forEach(entry -> { entry.parents().forEach(player::rememberResearch); entry.hiddenParents().forEach(player::rememberResearch); });
        player.forgetResearch("FOCUSSHOCK");
        assertFalse(rules.matchingCandidates(player, "potentia", 0).contains("FOCUSSHOCK"));
        assertTrue(rules.matchingCandidates(player, "potentia", 1).contains("FOCUSSHOCK"));
        assertTrue(rules.matchingCandidates(player, "potentia", -1).isEmpty());
        player.forgetResearch("FOCUSFIRE");
        assertFalse(rules.matchingCandidates(player, "potentia", 1).contains("FOCUSSHOCK"));
    }

    @Test void configFilteringDisablesSelectionAndCompletionWithoutDeletingStoredMarkers() {
        var rules = new ResearchRules(catalog(), entry -> !entry.key().equals("MIRROR"));
        var player = player();
        player.rememberResearch("MIRROR");
        assertFalse(rules.completed(player, "MIRROR"));
        assertTrue(player.researchMarkers().contains("MIRROR"));
        assertTrue(rules.clueCandidates(player, new ResearchRules.EntityClue("Enderman"), Map.of()).isEmpty());
    }

    @Test void corruptCatalogFailsBeforeItCanReplaceValidDefinitions() {
        var json = evidence();
        var category = json.get(0).getAsJsonObject();
        category.getAsJsonArray("research").add(category.getAsJsonArray("research").get(0).deepCopy());
        assertThrows(IllegalArgumentException.class, () -> ResearchCatalog.decode(json, aspects));
        var unknownAspect = evidence();
        unknownAspect.get(0).getAsJsonObject().getAsJsonArray("research").get(0).getAsJsonObject().getAsJsonObject("aspects").addProperty("unreal", 1);
        assertThrows(IllegalArgumentException.class, () -> ResearchCatalog.decode(unknownAspect, aspects));
    }

    @Test void generatedResourceRetainsEveryMetadataValueAndAspectIterationOrder() throws Exception {
        try (var reader = new InputStreamReader(java.util.Objects.requireNonNull(getClass()
                .getResourceAsStream("/data/thaumcraft/research_catalog/legacy.json")), StandardCharsets.UTF_8)) {
            var root = JsonParser.parseReader(reader).getAsJsonObject();
            assertEquals(1, root.get("format").getAsInt());
            var generated = ResearchCatalog.decode(root.getAsJsonArray("categories"), aspects);
            assertEquals(catalog().all().size() - 1, generated.all().size());
            assertTrue(generated.find("ENDLESSRESEARCH").isEmpty());
            var expectedData = evidence();
            for (var category : expectedData) {
                for (var research : category.getAsJsonObject().getAsJsonArray("research")) {
                    var definition = research.getAsJsonObject();
                    if (definition.get("key").getAsString().equals("INFUSIONENCHANTMENT")) {
                        assertEquals(27, definition.get("pageCount").getAsInt());
                        definition.addProperty("pageCount", 44); // Seventeen requested modern enchantment pages.
                    }
                    if (definition.get("key").getAsString().equals("ENTROPICPROCESSING")) {
                        assertEquals(3, definition.get("pageCount").getAsInt());
                        definition.addProperty("pageCount", 7); // Copper explanation and three recipe groups (F46).
                    }
                }
            }
            for (var entry : ResearchCatalog.decode(expectedData, aspects).all()) {
                if (entry.key().equals("BANNERS")) continue;
                assertEquals(entry, generated.require(entry.key()));
                assertEquals(List.copyOf(entry.aspects().keySet()), List.copyOf(generated.require(entry.key()).aspects().keySet()), entry.key());
            }
        }
    }

    private ScanTarget scanTarget(String id, String tag, int amount, ResearchRules.Clue clue) {
        return new ScanTarget(new ScanTarget.Entity(java.util.UUID.randomUUID()), PlayerKnowledge.ScanKind.OBJECT,
                Optional.of(id), new tc4revival.aspect.AspectAmounts().add(aspects.require(tag), amount), Optional.ofNullable(clue));
    }

    @Test void knownTargetCanRevealNewExplicitClueOnceWithoutAnyRepeatReward() {
        var player = player();
        var target = scanTarget("old-bone", "metallum", 4,
                new ResearchRules.ItemClue(stack -> stack.key().item().equals("minecraft:bone")));
        player.rememberScan(PlayerKnowledge.ScanKind.OBJECT, "@old-bone");
        var pools = Map.copyOf(player.knownAspects());
        var rules = new ResearchRules(catalog());
        assertFalse(target.canTrack(player, PlayerKnowledge.ScanMode.THAUMOMETER));
        assertTrue(target.canTrack(player, PlayerKnowledge.ScanMode.THAUMOMETER, rules));
        var timer = new ScanProgress(20, rules);
        assertEquals(ScanProgress.State.TRACKING, timer.begin(0, "overworld", target, player));
        for (int tick = 1; tick < 20; tick++) assertEquals(ScanProgress.State.TRACKING, timer.observe(tick, "overworld", target, player));
        assertEquals(ScanProgress.State.COMPLETED, timer.observe(20, "overworld", target, player));
        var result = complete(target, player);
        assertEquals(Optional.of("BONEBOW"), result.clue());
        assertTrue(result.scan().succeeded());
        assertTrue(result.scan().rewards().isEmpty());
        assertEquals(pools, player.knownAspects());
        assertFalse(target.canTrack(player, PlayerKnowledge.ScanMode.THAUMOMETER, rules));
        assertEquals(ScanRules.Status.ALREADY_SCANNED, complete(target, player).scan().status());
        assertEquals(pools, player.knownAspects());
    }

    @Test void repeatScanCannotCreateAspectTriggersOrDisabledResearch() {
        var player = player();
        var target = scanTarget("old-iron", "metallum", 4, new ResearchRules.ItemClue(stack -> false));
        player.rememberScan(PlayerKnowledge.ScanKind.OBJECT, "@old-iron");
        assertFalse(target.canTrack(player, PlayerKnowledge.ScanMode.THAUMOMETER, new ResearchRules(catalog())));
        assertTrue(complete(target, player).clue().isEmpty());
        var bone = scanTarget("old-iron", "metallum", 4,
                new ResearchRules.ItemClue(stack -> stack.key().item().equals("minecraft:bone")));
        assertFalse(bone.canTrack(player, PlayerKnowledge.ScanMode.THAUMOMETER, new ResearchRules(catalog(), entry -> false)));
    }
    private ScanTarget.Completion complete(ScanTarget target, PlayerKnowledge player) {
        return target.completeWithClue(player, aspects, PlayerKnowledge.ScanMode.THAUMOMETER, 100,
                new ResearchRules(catalog()), bound -> 0);
    }

    @Test void successfulScanCommitsScanRewardsAndOnlyTheClueMarker() {
        var player = player();
        player.discover(aspects.require("vitreus"));
        var target = scanTarget("iron", "metallum", 4, new ResearchRules.ItemClue(stack -> false));
        var result = complete(target, player);
        assertTrue(result.scan().succeeded());
        assertEquals(Optional.of("THAUMIUM"), result.clue());
        assertEquals(6, player.pool(aspects.require("metallum")));
        assertEquals(java.util.Set.of("@THAUMIUM"), player.researchMarkers());
        assertTrue(player.scanned(PlayerKnowledge.ScanKind.OBJECT, "iron"));
        var repeat = complete(target, player);
        assertEquals(ScanRules.Status.ALREADY_SCANNED, repeat.scan().status());
        assertTrue(repeat.clue().isEmpty());
        assertEquals(6, player.pool(aspects.require("metallum")));
    }

    @Test void rejectedScanCannotGrantAnOtherwiseMatchingItemClue() {
        var player = player();
        var target = scanTarget("bone", "metallum", 4, new ResearchRules.ItemClue(stack -> stack.key().item().equals("minecraft:bone")));
        assertEquals(ScanRules.Status.MISSING_PARENT, complete(target, player).scan().status());
        assertTrue(player.researchMarkers().isEmpty());
        assertFalse(player.scanned(PlayerKnowledge.ScanKind.OBJECT, "bone"));
    }

    @Test void zeroActualRewardSkipsAspectTriggerButStillAllowsItemTrigger() {
        var player = player();
        player.discover(aspects.require("vitreus"));
        player.discover(aspects.require("metallum"));
        var ordinary = scanTarget("zero", "metallum", 0, new ResearchRules.ItemClue(stack -> false));
        assertTrue(complete(ordinary, player).clue().isEmpty());
        var bone = scanTarget("bone", "metallum", 0, new ResearchRules.ItemClue(stack -> stack.key().item().equals("minecraft:bone")));
        assertEquals(Optional.of("BONEBOW"), complete(bone, player).clue());
        assertEquals(0, player.pool(aspects.require("metallum")));
    }

    @Test void discoveryBonusParticipatesInClueRewardsEvenForZeroBaseAmount() {
        var player = player();
        player.discover(aspects.require("vitreus"));
        var target = scanTarget("zero-new", "metallum", 0, new ResearchRules.ItemClue(stack -> false));
        var result = complete(target, player);
        assertEquals(2, result.scan().rewards().get(0).gained());
        assertEquals(Optional.of("THAUMIUM"), result.clue());
    }

    @Test void targetsWithoutClueSourceStillRewardTheirScan() {
        var player = player();
        var result = complete(scanTarget("node-or-player", "ignis", 4, null), player);
        assertTrue(result.scan().succeeded());
        assertTrue(result.clue().isEmpty());
        assertEquals(4, player.pool(aspects.require("ignis")));
    }
}
