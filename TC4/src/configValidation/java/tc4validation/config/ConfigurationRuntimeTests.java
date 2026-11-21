package tc4validation.config;

import com.google.gson.*;
import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import tc4revival.config.TCConfig;
import tc4revival.research.*;
import thaumcraft.api.config.*;
import thaumcraft.api.research.*;

/** Runs only in the isolated configuration-validation payload. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public class ConfigurationRuntimeTests {
    static final String[] ADDONS={"automagy","emtmodern","fmmodern","gadomancymodern","nodalmechanics",
        "tmmodern","tbmodern","temodern","thmodern","timodern","thaumicomniverse","ttmodern","wtmodern","wgmodern"};
    static List<RevivalConfig> configs() {
        var list=new ArrayList<RevivalConfig>();list.add(TCConfig.SERVER.revival);
        for(String name:ADDONS)try{list.add((RevivalConfig)Class.forName(name+".RevivalFeatures").getField("CONFIG").get(null));}
        catch(ClassNotFoundException absent){/* Optional-addon absence snapshots; the full-suite test still requires exactly 15. */}
        catch(ReflectiveOperationException error){throw new IllegalStateException(name,error);}
        return list;
    }
    static class Restore implements AutoCloseable {
        final Map<RevivalConfig,RevivalProfile> profiles=new LinkedHashMap<>();
        final Map<RevivalConfig.Option,FeatureSetting> options=new LinkedHashMap<>();
        Restore(){for(var config:configs()){profiles.put(config,config.global.get());config.options().values().forEach(o->options.put(o,o.configured()));}}
        public void close(){options.forEach(RevivalConfig.Option::set);profiles.forEach((c,p)->c.global.set(p));}
    }
    @GameTest(template="wand_test_empty",templateNamespace="thaumcraft",timeoutTicks=100,batch="configuration")
    public static void actualForgeConfigsInheritAndAllowIndependentOverrides(GameTestHelper h) {
        try(var restore=new Restore()) {
            var configs=configs();h.assertTrue(configs.size()==15,"Did not inspect all 15 installed modules");
            for(var config:configs)h.assertTrue(config.loaded(),"A SERVER config is not loaded");
            for(var mode:List.of(RevivalProfile.EVERYTHING,RevivalProfile.VANILLA,RevivalProfile.EASIEST,RevivalProfile.HARDEST)){
                TCConfig.SERVER.revival.global.set(mode);
                for(int i=1;i<configs.size();i++){
                    var addon=configs.get(i);addon.global.set(RevivalProfile.FOLLOW_CORE);
                    h.assertTrue(addon.profile()==mode,"Inheritance failed for "+ADDONS[i-1]+" in "+mode);
                    addon.global.set(RevivalProfile.EASIEST);
                    h.assertTrue(addon.profile()==RevivalProfile.EASIEST,"Independent profile lost for "+ADDONS[i-1]);
                    addon.global.set(RevivalProfile.FOLLOW_CORE);
                }
            }
            for(var config:configs)for(var option:config.options().values()){
                config.global.set(RevivalProfile.VANILLA);option.set(FeatureSetting.TRUE);
                h.assertTrue(option.get(),"Explicit TRUE ignored by "+option.path());
                config.global.set(RevivalProfile.EVERYTHING);option.set(FeatureSetting.FALSE);
                h.assertTrue(!option.get(),"Explicit FALSE ignored by "+option.path());
            }
        }
        h.succeed();
    }
    @GameTest(template="wand_test_empty",templateNamespace="thaumcraft",timeoutTicks=100,batch="configuration")
    public static void fourModesEnforceResearchTableProgressAndManualTilde(GameTestHelper h) {
        try(var restore=new Restore()) {
            var f=TCConfig.SERVER.revival;f.options().values().forEach(o->o.set(FeatureSetting.DEFAULT));
            String[] keys={ResearchTableAccess.FIRST,ResearchTableAccess.SECOND,ResearchTableAccess.THIRD};
            for(var mode:List.of(RevivalProfile.EVERYTHING,RevivalProfile.EASIEST,RevivalProfile.HARDEST,RevivalProfile.VANILLA)) {
                f.global.set(mode);
                for(int learned=0;learned<=3;learned++) {
                    var known=Set.of(Arrays.copyOf(keys,learned));
                    var a=ResearchTableAccess.resolve(f,known::contains);
                    boolean easy=mode==RevivalProfile.EASIEST,progress=mode==RevivalProfile.EVERYTHING;
                    h.assertTrue(a.basic()==(mode!=RevivalProfile.VANILLA),"Hardest basic-shortcut exception or Vanilla failed");
                    h.assertTrue(a.search()==(easy||progress&&learned>=1)&&a.refill()==a.search()&&a.random()==a.search(),"R1 permissions "+mode+"/"+learned);
                    h.assertTrue(a.related()==(easy||progress&&learned>=2),"R2 permissions "+mode+"/"+learned);
                    h.assertTrue(a.connect()==(easy||progress&&learned>=3),"R3 permissions "+mode+"/"+learned);
                    h.assertTrue(ResearchTableAccess.queryAllowed("  ~aer",a)==a.related(),"Typed tilde bypassed R2");
                    h.assertTrue(ResearchTableAccess.queryAllowed("aer",a)==a.search(),"Normal search ignored R1");
                    h.assertTrue(ResearchTableAccess.queryAllowed("",a),"Empty query must remain legal");
                }
            }
        }
        h.succeed();
    }
    @GameTest(template="wand_test_empty",templateNamespace="thaumcraft",timeoutTicks=200,batch="configuration")
    public static void realCombinedCatalogSupportsAllSixtyFourStageGateCombinations(GameTestHelper h) {
        try(var restore=new Restore()) {
            // Exercise the same unmaterialized merged input used on reload. A published
            // Vanilla snapshot has already removed disabled-stage edges and is not input.
            JsonObject raw;
            try {
                var field=ResearchDefinitions.class.getDeclaredField("rawServer");field.setAccessible(true);
                raw=((JsonObject)field.get(null)).deepCopy();
            } catch(ReflectiveOperationException error) { throw new IllegalStateException(error); }
            var f=TCConfig.SERVER.revival;
            String[] keys={ResearchTableAccess.FIRST,ResearchTableAccess.SECOND,ResearchTableAccess.THIRD};
            h.assertTrue(ResearchDefinitions.server().catalog().all().size()>500,"Combined addon catalog missing");
            for(int mask=0;mask<64;mask++){
                f.global.set(RevivalProfile.VANILLA);f.autoRefill.set(false);f.randomResearch.set(false);
                f.aspectSearch.set((mask&1)!=0);f.relatedSearch.set((mask&2)!=0);f.autoConnect.set((mask&4)!=0);
                f.firstResearch.set((mask&8)!=0);f.secondResearch.set((mask&16)!=0);f.thirdResearch.set((mask&32)!=0);
                var resolved=ResearchPrerequisites.materialize(raw);
                var snapshot=ResearchDefinitions.decode(resolved);
                var disabled=resolved.getAsJsonArray("disabledResearch").asList().stream().map(JsonElement::getAsString).toList();
                for(int stage=1;stage<=3;stage++)h.assertTrue(disabled.contains(keys[stage-1])!=f.stageEnabled(stage),"Stage visibility mask="+mask);
                h.assertTrue(snapshot.catalog().require(keys[1]).parents().contains(keys[0])==f.stageEnabled(1),"R2 has stale R1 edge mask="+mask);
                h.assertTrue(snapshot.catalog().require(keys[2]).parents().contains(keys[1])==f.stageEnabled(2),"R3 has stale R2 edge mask="+mask);
                h.assertTrue(snapshot.catalog().require(keys[1]).parents().contains("JARBRAIN"),"Lost brain gate");
                h.assertTrue(snapshot.catalog().require(keys[2]).parents().contains("PRIMPEARL"),"Lost pearl gate");
                for(String key:keys)if(!disabled.contains(key))for(String parent:snapshot.catalog().require(key).parents())
                    h.assertTrue(!disabled.contains(parent),"Active research has a disabled parent: "+key+"/"+parent);
            }
        }
        h.succeed();
    }
}
