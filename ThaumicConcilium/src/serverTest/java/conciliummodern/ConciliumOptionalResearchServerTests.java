package conciliummodern;

import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.TCAspects;
import tc4revival.research.*;
import thaumcraft.api.research.ResearchCategories;

/** Checks the final server snapshot, including common-setup registration and materialization. */
@GameTestHolder("thaumicconcilium") @PrefixGameTestTemplate(false)
public final class ConciliumOptionalResearchServerTests {
    private static final String NS="thaumicconcilium:";
    private static boolean loaded(String mod){return ModList.get().isLoaded(mod);}

    @GameTest(template="empty") public static void loadedCatalogRetainsAllNodesWithoutDanglingParentsOrMirrors(GameTestHelper test){
        var snapshot=ResearchDefinitions.server();test.assertTrue(snapshot!=null,"Server research snapshot was published");
        var catalog=snapshot.catalog();
        test.assertTrue(catalog.all().stream().filter(entry->entry.key().startsWith(NS)).count()==82,"All 82 Concilium research identities survive optional-host gating");
        for(var entry:catalog.all()){
            for(var parent:entry.parents())test.assertTrue(catalog.find(parent).isPresent(),entry.key()+" visible parent "+parent);
            for(var parent:entry.hiddenParents())test.assertTrue(catalog.find(parent).isPresent(),entry.key()+" hidden parent "+parent);
            for(var parent:entry.parentsAny())test.assertTrue(catalog.find(parent).isPresent(),entry.key()+" alternative parent "+parent);
            if(entry.mirror())test.assertTrue(!catalog.original(entry.key()).mirror(),entry.key()+" resolves to its real gameplay identity");
        }
        test.succeed();
    }

    @GameTest(template="empty") public static void optionalHiddenParentsRetainOriginalKeysAndRelativeOrder(GameTestHelper test){
        var catalog=ResearchDefinitions.server().catalog();
        for(var expected:originalHiddenParents().entrySet()){
            var actual=catalog.require(NS+expected.getKey()).hiddenParents();
            test.assertTrue(actual.equals(expected.getValue()),expected.getKey()+" hidden parents: expected "+expected.getValue()+", actual "+actual);
            test.assertTrue(new HashSet<>(actual).size()==actual.size(),expected.getKey()+" has no duplicated host edges");
        }
        test.succeed();
    }

    @GameTest(template="empty") public static void registeredHostGatesRejectSavedCompletionForUnavailableContent(GameTestHelper test){
        var catalog=ResearchDefinitions.server().catalog();var rules=ResearchCatalogs.rules();
        for(var gate:expectedGates().entrySet()){
            String key=NS+gate.getKey();boolean expected=gate.getValue();var entry=catalog.require(key);
            test.assertTrue(ResearchCategories.enabled(key)==expected,key+" common-setup host gate");
            test.assertTrue(ResearchCatalogs.enabled(entry)==expected,key+" published server enablement");
            var saved=new PlayerKnowledge(TCAspects.catalog());saved.rememberResearch(key);
            test.assertTrue(rules.completed(saved,key)==expected,key+" old completed marker cannot enable absent integration");
            test.assertTrue(rules.prerequisites(saved,key)==expected,key+" old completed marker observes the same availability gate");
        }
        test.succeed();
    }

    @GameTest(template="empty") public static void loadedHostPrerequisitesCannotBeSkippedDuringResearchUnlock(GameTestHelper test){
        var catalog=ResearchDefinitions.server().catalog();var rules=ResearchCatalogs.rules();int checked=0;
        // Include ordinary base content so the minimal-host run also exercises real prerequisite checks.
        var targets=new ArrayList<>(originalHiddenParents().keySet());targets.add("THAUMATURGES");
        for(String name:targets){
            var entry=catalog.require(NS+name);if(!ResearchCatalogs.enabled(entry))continue;
            var required=new LinkedHashSet<String>();
            for(String parent:entry.parents())required.add(catalog.original(parent).key());
            for(String parent:entry.hiddenParents())required.add(catalog.original(parent).key());
            var alternatives=entry.parentsAny().stream().map(key->catalog.original(key).key()).toList();
            var complete=new PlayerKnowledge(TCAspects.catalog());required.forEach(complete::rememberResearch);alternatives.forEach(complete::rememberResearch);
            test.assertTrue(rules.prerequisites(complete,entry.key()),name+" accepts every actual prerequisite marker");
            for(String missing:required){
                var incomplete=new PlayerKnowledge(TCAspects.catalog());
                required.stream().filter(key->!key.equals(missing)).forEach(incomplete::rememberResearch);
                alternatives.stream().filter(key->!key.equals(missing)).forEach(incomplete::rememberResearch);
                test.assertTrue(!rules.prerequisites(incomplete,entry.key()),name+" still requires "+missing);checked++;
            }
        }
        test.assertTrue(checked>0,"At least one real prerequisite was checked in this host profile");test.succeed();
    }

    /** Fixed upstream lists, deliberately independent of the product sidecar/provider. */
    private static Map<String,List<String>> originalHiddenParents(){
        var expected=new LinkedHashMap<String,List<String>>();
        expected.put("PROTOBODY",loaded("thaumichorizons")?List.of("cloneVillager"):List.of());
        expected.put("RIFTGEM",loaded("gadomancy")?List.of("gadomancy:aura_core","WARDEDARCANA"):List.of("WARDEDARCANA"));
        expected.put("BUFFGOLEM",loaded("thaumichorizons")?List.of("golemPowder","GOLEMTHAUMIUM","GOLEMFETTER"):List.of("GOLEMTHAUMIUM","GOLEMFETTER"));
        var warpwood=new ArrayList<String>();if(loaded("thaumicbases"))warpwood.add("TB.Bracelet.Iron");
        if(loaded("taintedmagic"))warpwood.addAll(List.of("taintedmagic:ROD_warpwood","taintedmagic:CAP_shadowmetal"));
        expected.put("WARPWOODBRACELET",List.copyOf(warpwood));
        var squid=new ArrayList<String>();if(loaded("thaumichorizons"))squid.add("infusionVat");
        if(loaded("thaumicbases"))squid.addAll(List.of("TB.Tobacco.Eldritch","TB.Tobacco.Wispy","TB.Tobacco.Angry","TB.Tobacco.Mining","TB.Tobacco.Wisdom","TB.Tobacco.Tainted","TB.Tobacco.Saturating","TB.Tobacco.Sanity"));
        expected.put("DOPESQUID",List.copyOf(squid));
        var witchwood=new ArrayList<String>();if(loaded("thaumicbases"))witchwood.add("TB.Bracelet.Iron");
        witchwood.addAll(List.of(NS+"ROD_witchwood",NS+"CAP_vinteum"));expected.put("WITCHWOODBRACELET",List.copyOf(witchwood));
        return expected;
    }

    private static Map<String,Boolean> expectedGates(){
        boolean horizons=loaded("thaumichorizons"),gadomancy=loaded("gadomancy"),bases=loaded("thaumicbases"),tainted=loaded("taintedmagic");
        var gates=new LinkedHashMap<String,Boolean>();
        for(String key:List.of("PROTOBODY","SOLIDVOID"))gates.put(key,tainted&&horizons);
        for(String key:List.of("TCHORIZONS","BRAINLITTERING","BUFFGOLEM"))gates.put(key,horizons);
        gates.put("REDPOWEREDMIND",loaded("automagy")&&horizons);
        gates.put("DRAINAGESYRINGE",horizons&&loaded("botania")&&loaded("bloodmagic"));
        for(String key:List.of("TCGADOMANCY","RIFTGEM","TERRACAST","VALETCORE"))gates.put(key,gadomancy);
        gates.put("ASSISTANCECORE",gadomancy&&horizons);gates.put("TCBASES",bases);
        for(String key:List.of("TCTAINTEDMAGIC","PONTIFEXROBE","PONTIFEXHAMMER","MATERIALPEELER","FLESHCRUCIBLE"))gates.put(key,tainted);
        gates.put("DOPESQUID",horizons&&bases&&RemainingMobs.tobaccoEnabled());return gates;
    }

    private ConciliumOptionalResearchServerTests(){}
}
