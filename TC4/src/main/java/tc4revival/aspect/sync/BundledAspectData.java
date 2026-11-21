package tc4revival.aspect.sync;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;

/** Loads installed-mod JSON locally; server-specific replacements stay in memory. */
public class BundledAspectData {
    private static volatile Map<String,String> all;
    /** Installed mod resources are identical on both sides by contract. Never download this baseline. */
    public static synchronized Map<String,String> all(){
        if(all!=null)return all;
        var found=new TreeMap<String,String>();
        for(var file:ModList.get().getModFiles()){
            var data=file.getFile().findResource("data");
            if(!Files.isDirectory(data))continue;
            try(var namespaces=Files.list(data)){
                for(var namespace:namespaces.filter(Files::isDirectory).toList())for(String directory:AspectDataSet.DIRECTORIES){
                    var root=namespace.resolve(directory);if(!Files.isDirectory(root))continue;
                    try(var paths=Files.walk(root)){
                        for(var path:paths.filter(Files::isRegularFile).filter(p->p.toString().endsWith(".json")).sorted().toList()){
                            String relative=root.relativize(path).toString().replace('\\','/');
                            String key=directory+"/"+namespace.getFileName()+":"+relative.substring(0,relative.length()-5);
                            AspectDataSet.validateKey(key);
                            try(var reader=Files.newBufferedReader(path)){found.put(key,JsonParser.parseReader(reader).toString());}
                        }
                    }
                }
            }catch(java.io.IOException error){throw new IllegalStateException("Cannot read bundled aspect definitions: "+file.getFile().getFileName(),error);}
        }
        all=Map.copyOf(found);return all;
    }
    private static final Map<String,String> bundledByHash=new java.util.concurrent.ConcurrentHashMap<>();
    public static Map<String,String> find(Map<String,String> manifest){
        var local=available(manifest);local.entrySet().removeIf(e->!AspectDataSet.hash(e.getValue()).equals(manifest.get(e.getKey())));return local;
    }
    /** Prefer a hash match, but retain a bundled baseline for local prediction before server overrides. */
    public static Map<String,String> available(Map<String,String> manifest){
        var found=new HashMap<String,String>();
        for(var e:manifest.entrySet()){
            AspectDataSet.validateKey(e.getKey());String hit=bundledByHash.get(e.getValue());
            if(hit!=null){found.put(e.getKey(),hit);continue;}
            int slash=e.getKey().indexOf('/');String directory=e.getKey().substring(0,slash);var id=ResourceLocation.parse(e.getKey().substring(slash+1));
            for(var file:ModList.get().getModFiles()){
                var path=file.getFile().findResource("data",id.getNamespace(),directory,id.getPath()+".json");
                if(!Files.isRegularFile(path))continue;
                try(var reader=Files.newBufferedReader(path)){
                    String json=JsonParser.parseReader(reader).toString();
                    found.putIfAbsent(e.getKey(),json);
                    if(AspectDataSet.hash(json).equals(e.getValue())){found.put(e.getKey(),json);bundledByHash.put(e.getValue(),json);break;}
                }catch(java.io.IOException|RuntimeException ignored){/* Server will supply an authoritative replacement. */}
            }
        }
        return found;
    }
}
