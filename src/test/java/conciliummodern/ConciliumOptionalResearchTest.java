package conciliummodern;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.*;
import tc4revival.research.*;
import thaumcraft.api.research.ResearchPrerequisites;
import static org.junit.jupiter.api.Assertions.*;

/** Real core decoding over real workspace data; no fabricated replacement research definitions. */
class ConciliumOptionalResearchTest {
    private static final Path WORKSPACE=Path.of("../../..");
    private static final String NS="thaumicconcilium:";
    private static final Map<String,String> OPTIONAL=Map.of(
            "thaumichorizons","ThaumicHorizons","automagy","Automagy","gadomancy","Gadomancy",
            "thaumicbases","ThaumicBases","taintedmagic","TaintedMagic");

    private static JsonElement read(Path path)throws Exception{return JsonParser.parseString(Files.readString(path));}
    private static Map<String,JsonElement> resources(String module)throws Exception{
        var result=new LinkedHashMap<String,JsonElement>();Path root=WORKSPACE.resolve("Code/"+module);
        for(String source:List.of("src/generated/resources","src/main/resources")){
            Path dir=root.resolve(source);if(!Files.isDirectory(dir.resolve("data")))continue;
            try(var files=Files.walk(dir.resolve("data"))){
                for(var file:files.filter(p->p.toString().endsWith(".json")).sorted().toList()){
                    String relative=dir.relativize(file).toString().replace('\\','/');
                    if(relative.matches("data/[^/]+/(research_catalog|research_pages|research_page_recipes)/.+\\.json")
                            ||relative.equals("data/thaumcraft/aspect_registry.json")||relative.endsWith("/thaumcraft/aspects.json"))
                        result.put(relative,read(file));
                }
            }
        }
        if(module.equals("Addons/ThaumicBases")){
            // TB deliberately supplies its catalog through a generated PackResources, even with features disabled.
            var dynamic=read(root.resolve("src/main/resources/tbmodern/configured-data.json")).getAsJsonObject();
            for(var entry:dynamic.entrySet())if(entry.getKey().startsWith("research_"))
                result.put("data/thaumicbases/"+entry.getKey(),entry.getValue());
        }
        assertFalse(result.isEmpty(),module);return result;
    }
    private record Fixture(JsonObject json,AspectCatalog aspects){}
    private static Fixture fixture(Set<String> hosts)throws Exception{
        var files=new LinkedHashMap<String,JsonElement>();
        for(String module:List.of("TC4","Addons/ForbiddenMagic","Addons/ThaumicTinkerer","Addons/ThaumicConcilium"))files.putAll(resources(module));
        for(String host:hosts)files.putAll(resources("Addons/"+OPTIONAL.get(host)));
        var json=new JsonObject();for(String key:List.of("categories","pages","recipes"))json.add(key,new JsonArray());
        var aspects=new JsonArray();
        for(var entry:files.entrySet()){
            String path=entry.getKey();var value=entry.getValue();
            if(path.endsWith("aspect_registry.json")||path.endsWith("/thaumcraft/aspects.json")){aspects.addAll(value.getAsJsonArray());continue;}
            String field=path.contains("/research_catalog/")?"categories":path.contains("/research_pages/")?"pages":"recipes";
            json.getAsJsonArray(field).addAll(value.getAsJsonObject().getAsJsonArray(field.equals("categories")?"categories":"entries"));
        }
        return new Fixture(json,AspectDefinitions.decode(aspects));
    }
    private static JsonObject resolve(Fixture fixture,Set<String> hosts){
        var raw=ResearchDefinitions.decode(fixture.json(),fixture.aspects());
        var provider=ConciliumResearchPrerequisites.provider(hosts::contains);var result=fixture.json().deepCopy();
        for(var category:result.getAsJsonArray("categories"))for(var value:category.getAsJsonObject().getAsJsonArray("research")){
            var row=value.getAsJsonObject();var entry=raw.catalog().require(row.get("key").getAsString());
            var parents=provider.resolve(entry,raw.catalog(),new ResearchPrerequisites.Parents(entry.parents(),entry.hiddenParents()));
            var visible=new JsonArray();parents.visible().forEach(visible::add);row.add("parents",visible);
            var hidden=new JsonArray();parents.hidden().forEach(hidden::add);row.add("parentsHidden",hidden);
        }
        return result;
    }
    private static String host(String key){
        if(Set.of("cloneVillager","healingVat","injector","infusionVat","golemPowder").contains(key))return "thaumichorizons";
        if(key.startsWith("TB."))return "thaumicbases";
        String namespace=key.contains(":")?key.substring(0,key.indexOf(':')):"";
        return OPTIONAL.containsKey(namespace)?namespace:null;
    }
    private static String modernKey(String legacy,ResearchCatalog full){
        if(full.find(legacy).isPresent())return legacy;
        if(legacy.equals("GADOMANCY.AURA_CORE"))return "gadomancy:aura_core";
        var candidates=List.of(NS,"forbidden_magic:","thaumic_tinkerer:","taintedmagic:","automagy:").stream()
                .map(prefix->prefix+legacy).filter(key->full.find(key).isPresent()).toList();
        assertEquals(1,candidates.size(),"Unambiguous original research key: "+legacy);return candidates.get(0);
    }
    private static Map<String,List<String>> originalOptionalParents(ResearchCatalog full)throws Exception{
        String source=Files.readString(WORKSPACE.resolve("References/Projects/ThaumicConcilium/upstream/src/main/java/com/ilya3point999k/thaumicconcilium/common/registry/Thaumonomicon.java"));
        source=source.replaceAll("(?s)/\\*.*?\\*/","").replaceAll("//[^\\r\\n]*","");
        var matches=Pattern.compile("new ResearchItem\\(\"([^\"]+)\"").matcher(source);var names=new ArrayList<String>();var starts=new ArrayList<Integer>();
        while(matches.find()){names.add(matches.group(1));starts.add(matches.start());}starts.add(source.length());
        var result=new LinkedHashMap<String,List<String>>();
        for(int i=0;i<names.size();i++){
            var hidden=Pattern.compile("setParentsHidden\\((.*?)\\)",Pattern.DOTALL).matcher(source.substring(starts.get(i),starts.get(i+1)));
            if(!hidden.find())continue;var values=Pattern.compile("\"([^\"]+)\"").matcher(hidden.group(1));var parents=new ArrayList<String>();
            while(values.find())parents.add(modernKey(values.group(1),full));
            if(parents.stream().anyMatch(key->host(key)!=null))result.put(NS+names.get(i),List.copyOf(parents));
        }
        assertEquals(23,result.size());assertEquals(33,result.values().stream().flatMap(List::stream).filter(key->host(key)!=null).count());
        return result;
    }

    @Test void allThirtyTwoHostSubsetsDecodeInitiallyAndRetainExactlyTheAvailableOriginalPrerequisites()throws Exception{
        var all=fixture(OPTIONAL.keySet());var originals=originalOptionalParents(ResearchDefinitions.decode(all.json(),all.aspects()).catalog());
        var hosts=OPTIONAL.keySet().stream().sorted().toList();
        for(int mask=0;mask<(1<<hosts.size());mask++){
            var selected=new HashSet<String>();for(int i=0;i<hosts.size();i++)if((mask&(1<<i))!=0)selected.add(hosts.get(i));
            var input=fixture(selected);var before=input.json().deepCopy();
            // This is the actual startup decoder which previously threw Unknown research cloneVillager.
            var initial=ResearchDefinitions.decode(input.json(),input.aspects());
            assertEquals(82,initial.catalog().all().stream().filter(r->r.key().startsWith(NS)).count(),selected.toString());
            var materialized=resolve(input,selected);var ready=ResearchDefinitions.decode(materialized,input.aspects());
            for(var original:originals.entrySet()){
                var expected=original.getValue().stream().filter(key->host(key)==null||selected.contains(host(key))).toList();
                assertEquals(expected,ready.catalog().require(original.getKey()).hiddenParents(),selected+" / "+original.getKey());
            }
            assertEquals(before,input.json(),"Resolving one host set must not alter a later reload's raw definition");
            assertEquals(materialized,resolve(new Fixture(materialized,input.aspects()),selected),"Reload does not duplicate optional edges");
        }
    }

    @Test void installedHostWithMissingRealResearchIsRejectedInsteadOfWeakeningTheGate()throws Exception{
        var input=fixture(Set.of());var catalog=ResearchDefinitions.decode(input.json(),input.aspects()).catalog();
        var target=catalog.require(NS+"PROTOBODY");
        var failure=assertThrows(IllegalArgumentException.class,()->ConciliumResearchPrerequisites.provider("thaumichorizons"::equals)
                .resolve(target,catalog,new ResearchPrerequisites.Parents(target.parents(),target.hiddenParents())));
        assertTrue(failure.getMessage().contains("cloneVillager"),failure.getMessage());
        assertEquals(List.of(NS+"FLESHCRUCIBLE"),target.parents());assertTrue(target.hiddenParents().isEmpty());
    }
}
