package automagy.logic;
import java.util.*;
/** Text config parser. Empty lists explicitly disable the corresponding essentia cost. */
public final class EssentiaCostSetting {
    private EssentiaCostSetting() {}
    public static Optional<Map.Entry<String,Integer>> entry(String text) {
        if(text==null)return Optional.empty();
        String[] parts=text.trim().split("=",-1);
        if(parts.length!=2||!parts[0].trim().matches("[a-z][a-z0-9_]*"))return Optional.empty();
        try{int amount=Integer.parseInt(parts[1].trim());return amount>=0&&amount<=65535?Optional.of(Map.entry(parts[0].trim(),amount)):Optional.empty();}
        catch(NumberFormatException error){return Optional.empty();}
    }
    public static Map<String,Integer> parse(List<? extends String> values) {
        var result=new LinkedHashMap<String,Integer>();
        for(String value:values){var entry=entry(value).orElseThrow(()->new IllegalArgumentException("Invalid aspect cost: "+value));if(entry.getValue()==0)result.remove(entry.getKey());else result.put(entry.getKey(),entry.getValue());}
        return Collections.unmodifiableMap(result);
    }
}
