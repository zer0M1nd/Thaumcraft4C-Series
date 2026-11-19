package conciliummodern;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.research.ResearchPrerequisites;

/** Optional host edges are added after the base catalog has passed its first, unconditional decode. */
final class ConciliumResearchPrerequisites {
    record Parent(String host,String key,int index) {}
    private static final Map<String,List<Parent>> PARENTS=read();

    static void setup(){
        ResearchPrerequisites.register(ResourceLocation.parse("thaumicconcilium:optional_parents"),
                provider(id->net.minecraftforge.fml.ModList.get().isLoaded(id)));
    }

    static ResearchPrerequisites.Provider provider(Predicate<String> loaded){
        Objects.requireNonNull(loaded);
        return (entry,catalog,current)->{
            var additions=PARENTS.get(entry.key());if(additions==null)return current;
            var hidden=new ArrayList<>(current.hidden());
            int absentBefore=0;
            for(var parent:additions){
                if(!loaded.test(parent.host())){absentBefore++;continue;}
                // An installed host with a changed/missing key is a real compatibility error.
                // Never quietly remove its survival prerequisite just to let startup succeed.
                catalog.require(parent.key());
                if(!hidden.contains(parent.key()))hidden.add(Math.min(parent.index()-absentBefore,hidden.size()),parent.key());
            }
            return new ResearchPrerequisites.Parents(current.visible(),hidden);
        };
    }

    private static Map<String,List<Parent>> read(){
        String path="/conciliummodern/optional_research_parents.json";
        try(var input=Objects.requireNonNull(ConciliumResearchPrerequisites.class.getResourceAsStream(path),path);
                var reader=new InputStreamReader(input,StandardCharsets.UTF_8)){
            var json=JsonParser.parseReader(reader).getAsJsonObject();
            if(json.get("format").getAsInt()!=1)throw new IllegalArgumentException("Unsupported optional research parent format");
            var result=new LinkedHashMap<String,List<Parent>>();
            for(var element:json.getAsJsonArray("entries")){
                var row=element.getAsJsonObject();String key=row.get("research").getAsString();
                if(!key.startsWith("thaumicconcilium:"))throw new IllegalArgumentException("Foreign optional parent target: "+key);
                var parents=new ArrayList<Parent>();var keys=new HashSet<String>();var positions=new HashSet<Integer>();
                for(var value:row.getAsJsonArray("parentsHidden")){
                    var item=value.getAsJsonObject();String host=item.get("host").getAsString(),parent=item.get("key").getAsString();int index=item.get("index").getAsInt();
                    if(!host.matches("[a-z][a-z0-9_]{1,63}")||parent.isBlank()||parent.equals(key)||index<0||!keys.add(parent)||!positions.add(index))
                        throw new IllegalArgumentException("Invalid optional research edge: "+key+" -> "+parent);
                    parents.add(new Parent(host,parent,index));
                }
                parents.sort(Comparator.comparingInt(Parent::index));
                if(parents.isEmpty()||result.putIfAbsent(key,List.copyOf(parents))!=null)throw new IllegalArgumentException("Empty/duplicate optional research target: "+key);
            }
            return Collections.unmodifiableMap(result);
        }catch(IOException error){throw new IllegalStateException("Cannot read optional research parents",error);}
    }

    private ConciliumResearchPrerequisites(){}
}
