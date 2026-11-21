package tc4revival.aspect.sync;

import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Effective JSON resources only, never a downloadable/executable datapack archive. */
public class AspectDataSet {
    public static final List<String> DIRECTORIES=List.of("item_aspects","item_bonus_aspects","legacy_item_mappings","potion_aspects","recipe_aspect_inputs","crafting_aspect_inputs","aspect_editor","block_scan_targets","block_aspects","entity_aspects","entity_aspect_contributions");
    public static final int MAX_FILES=4096,MAX_BYTES=32*1024*1024;
    private final Map<String,String> files,hashes;
    public AspectDataSet(Map<String,String> input){
        if(input.size()>MAX_FILES)throw new IllegalArgumentException("Too many aspect resources");
        var content=new TreeMap<String,String>();var fingerprints=new TreeMap<String,String>();
        for(var e:input.entrySet()){
            validateKey(e.getKey());String value=JsonParser.parseString(e.getValue()).toString();
            content.put(e.getKey(),value);fingerprints.put(e.getKey(),hash(value));
        }
        files=Collections.unmodifiableMap(content);hashes=Collections.unmodifiableMap(fingerprints);
    }
    public Map<String,String> files(){return files;}
    public Map<String,String> hashes(){return hashes;}
    /** Only a selected network payload has a byte budget; matching local resources do not count. */
    public static long transferBytes(Map<String,String> selected){
        long bytes=0;
        for(var value:selected.values()){
            bytes+=value.getBytes(StandardCharsets.UTF_8).length;
            if(bytes>MAX_BYTES)throw new IllegalArgumentException("Thaumcraft aspect download exceeds 32 MiB");
        }
        return bytes;
    }
    public Map<String,JsonElement> directory(String name){
        var result=new TreeMap<String,JsonElement>();String prefix=name+"/";
        files.forEach((key,value)->{if(key.startsWith(prefix))result.put(key.substring(prefix.length()),JsonParser.parseString(value));});return result;
    }
    public Map<String,String> select(Collection<String> keys){
        var result=new TreeMap<String,String>();for(var key:keys){if(!files.containsKey(key))throw new IllegalArgumentException("Unknown aspect resource");result.put(key,files.get(key));}return result;
    }
    public static List<String> missing(Map<String,String> expected,Map<String,String> local){
        return expected.entrySet().stream().filter(e->!local.containsKey(e.getKey())||!hash(local.get(e.getKey())).equals(e.getValue())).map(Map.Entry::getKey).toList();
    }
    /** The manifest is authoritative: local files absent on the server must not survive merging. */
    public static AspectDataSet merge(Map<String,String> expected,Map<String,String> local,Map<String,String> changed){
        var result=new TreeMap<String,String>();
        for(var e:expected.entrySet()){
            validateKey(e.getKey());var value=changed.getOrDefault(e.getKey(),local.get(e.getKey()));
            if(value==null||!hash(value).equals(e.getValue()))throw new IllegalArgumentException("Aspect resource hash mismatch: "+e.getKey());
            result.put(e.getKey(),value);
        }
        return new AspectDataSet(result);
    }
    public static void validateKey(String key){
        int slash=key.indexOf('/');if(slash<0||!DIRECTORIES.contains(key.substring(0,slash)))throw new IllegalArgumentException("Invalid aspect directory");
        String id=key.substring(slash+1);
        if(!id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))throw new IllegalArgumentException("Invalid aspect resource ID");
        String path=id.substring(id.indexOf(':')+1);
        if(path.startsWith("/")||Arrays.stream(path.split("/",-1)).anyMatch(s->s.isEmpty()||s.equals(".")||s.equals("..")))throw new IllegalArgumentException("Invalid aspect resource path");
    }
    public static String hash(String text){
        try{return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));}
        catch(java.security.NoSuchAlgorithmException error){throw new IllegalStateException(error);}
    }
}
