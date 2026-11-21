package gadomancymodern.knowledge;
import java.util.*;
/** Original work budget and bookshelf chance, independent of world/rendering. */
public class KnowledgeWork {
    public static Map<String,Integer> initial(Map<String,Integer> aspects){var work=new LinkedHashMap<String,Integer>();aspects.forEach((aspect,amount)->work.put(aspect,(int)Math.min(Integer.MAX_VALUE,Math.max(10L,amount*4L))));return work;}
    public static int chance(int knowledge){return (int)Math.min(Integer.MAX_VALUE,Math.max(0L,200L-knowledge)+100);}
    public static void progress(Map<String,Integer> work,int index){if(work.isEmpty())return;String aspect=new ArrayList<>(work.keySet()).get(Math.floorMod(index,work.size()));int amount=work.get(aspect)-1;if(amount<=0)work.remove(aspect);else work.put(aspect,amount);}
}
