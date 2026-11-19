package tc4revival.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import org.junit.jupiter.api.Test;
import thaumcraft.api.config.*;
import tc4revival.research.ResearchTableAccess;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class RevivalConfigTest {
    private record Fixture(CoreRevivalFeatures features, ForgeConfigSpec spec) {}
    private Fixture core(RevivalProfile profile) {
        var b = new ForgeConfigSpec.Builder();
        var f = new CoreRevivalFeatures(b); var spec = b.build(); f.bind(spec);
        spec.acceptConfig(CommentedConfig.inMemory()); f.global.set(profile);
        return new Fixture(f, spec);
    }
    @Test void freshProfilesPreserveTheBasicHardestExceptionAndResearchProgression() {
        for (var mode : new RevivalProfile[]{RevivalProfile.EVERYTHING, RevivalProfile.EASIEST, RevivalProfile.HARDEST, RevivalProfile.VANILLA}) {
            var f = core(mode).features;
            var before = ResearchTableAccess.resolve(f, key -> false);
            var after = ResearchTableAccess.resolve(f, key -> true);
            assertEquals(mode != RevivalProfile.VANILLA, before.basic(), mode.toString());
            assertEquals(mode == RevivalProfile.EASIEST, before.search(), mode.toString());
            assertEquals(mode == RevivalProfile.EASIEST, before.related(), mode.toString());
            assertEquals(mode == RevivalProfile.EASIEST, before.connect(), mode.toString());
            assertEquals(mode == RevivalProfile.EVERYTHING || mode == RevivalProfile.EASIEST, after.connect());
            assertFalse(f.orderedInfusion.get(), mode.toString());
        }
    }
    @Test void tutorialsRequireAnExplicitOptInInEveryProfile() {
        for (var mode : List.of(RevivalProfile.EVERYTHING, RevivalProfile.EASIEST, RevivalProfile.HARDEST, RevivalProfile.VANILLA)) {
            var tutorials = core(mode).features.bookTutorials;
            assertFalse(tutorials.get(), mode.toString());
            tutorials.set(true);
            assertTrue(tutorials.get(), mode.toString());
            tutorials.set(FeatureSetting.DEFAULT);
            assertFalse(tutorials.get(), mode.toString());
        }
    }

    @Test void allThreeStagesUnlockOnlyTheirOwnFunctions() {
        var f = core(RevivalProfile.EVERYTHING).features;
        var first = ResearchTableAccess.resolve(f, Set.of(ResearchTableAccess.FIRST)::contains);
        assertTrue(first.search() && first.refill() && first.random());
        assertFalse(first.related()); assertFalse(first.connect());
        var second = ResearchTableAccess.resolve(f, Set.of(ResearchTableAccess.FIRST, ResearchTableAccess.SECOND)::contains);
        assertTrue(second.related()); assertFalse(second.connect());
        assertTrue(ResearchTableAccess.resolve(f, key -> true).connect());
        f.autoRefill.set(false);
        var partial = ResearchTableAccess.resolve(f, key -> true);
        assertFalse(partial.refill()); assertTrue(partial.search() && partial.random());
    }
    @Test void explicitOverridesAreNotMaskedByTheGlobalMode() {
        var f = core(RevivalProfile.VANILLA).features;
        f.autoConnect.set(true);
        assertTrue(ResearchTableAccess.resolve(f, key -> false).connect());
        f.thirdResearch.set(true);
        assertFalse(ResearchTableAccess.resolve(f, key -> false).connect());
        assertTrue(ResearchTableAccess.resolve(f, ResearchTableAccess.THIRD::equals).connect());
        assertFalse(f.stageEnabled(1)); assertFalse(f.stageEnabled(2)); assertTrue(f.stageEnabled(3));
        f.autoConnect.set(false);
        assertFalse(f.stageEnabled(3));
    }
    @Test void typedRelatedQueryCannotBypassStageTwo() {
        var f = core(RevivalProfile.EVERYTHING).features;
        var first = ResearchTableAccess.resolve(f, ResearchTableAccess.FIRST::equals);
        assertTrue(ResearchTableAccess.queryAllowed("aer", first));
        assertFalse(ResearchTableAccess.queryAllowed("~aer", first));
        assertFalse(ResearchTableAccess.queryAllowed("  ~aer", first));
        assertTrue(ResearchTableAccess.queryAllowed("", first));
        f.aspectSearch.set(false); f.secondResearch.set(false);
        var relatedOnly = ResearchTableAccess.resolve(f, key -> false);
        assertFalse(ResearchTableAccess.queryAllowed("aer", relatedOnly));
        assertTrue(ResearchTableAccess.queryAllowed("~aer", relatedOnly));
    }
    @Test void addonFollowsTheProfileButNotCoreIndividualSettings() {
        var core = core(RevivalProfile.VANILLA).features;
        var b = new ForgeConfigSpec.Builder();
        var addon = new RevivalConfig(b, true, core::profile);
        var feature = addon.feature("tools", "工具", "Tools", RevivalConfig.Defaults.BENEFIT);
        var spec = b.build(); addon.bind(spec); spec.acceptConfig(CommentedConfig.inMemory());
        assertEquals(RevivalProfile.FOLLOW_CORE, addon.global.get());
        core.autoConnect.set(true); assertFalse(feature.get());
        core.global.set(RevivalProfile.EVERYTHING); assertTrue(feature.get());
        addon.global.set(RevivalProfile.HARDEST); assertFalse(feature.get());
        core.global.set(RevivalProfile.EASIEST); assertFalse(feature.get());
        feature.set(true); assertTrue(feature.get());
    }
    @Test void everyDefaultAndOverrideCombinationResolvesWithoutOverridingExplicitValues() {
        for (var profile : RevivalProfile.values()) for (var setting : FeatureSetting.values()) {
            for (var defaults : new RevivalConfig.Defaults[]{RevivalConfig.Defaults.BENEFIT, RevivalConfig.Defaults.CHALLENGE, RevivalConfig.Defaults.CONVENIENCE}) {
                boolean expected = setting == FeatureSetting.DEFAULT ? defaults.in(profile) : setting == FeatureSetting.TRUE;
                assertEquals(expected, setting.resolve(defaults.in(profile)));
            }
        }
    }
    @Test void dependentClientDefaultFollowsGuidanceButExplicitChoiceWins() {
        var core=core(RevivalProfile.VANILLA).features;
        assertTrue(core.listeningGuidance.get());
        var b=new ForgeConfigSpec.Builder();var client=new RevivalConfig(b,core::profile);
        var option=client.feature("jei.research_queries","跟随指引","Follow guidance",()->core.listeningGuidance.get());
        var spec=b.build();client.bind(spec);spec.acceptConfig(CommentedConfig.inMemory());
        assertTrue(option.get());core.listeningGuidance.set(false);assertFalse(option.get());
        option.set(true);assertTrue(option.get());core.listeningGuidance.set(true);option.set(false);assertFalse(option.get());
    }
}
