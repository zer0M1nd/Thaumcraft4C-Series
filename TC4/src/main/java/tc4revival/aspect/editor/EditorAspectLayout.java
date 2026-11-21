package tc4revival.aspect.editor;
import java.util.*;
import tc4revival.aspect.*;
public class EditorAspectLayout {
    private static final Map<AspectCatalog,Map<String,Integer>> TIERS=Collections.synchronizedMap(new WeakHashMap<>());
    public static int tier(Aspect a,AspectCatalog catalog){
        return TIERS.computeIfAbsent(catalog,c->new TierIndex(c).calculate()).get(a.tag());
    }
    /** Cyclic addon aspects occupy one display tier; the component DAG retains legacy depths. */
    private static final class TierIndex {
        private final AspectCatalog catalog;
        private final Map<String,Integer> indices=new HashMap<>(),low=new HashMap<>(),groups=new HashMap<>();
        private final List<List<Aspect>> members=new ArrayList<>();
        private final Deque<Aspect> stack=new ArrayDeque<>();
        private final Set<String> active=new HashSet<>();
        private final Map<Integer,Integer> depths=new HashMap<>();
        TierIndex(AspectCatalog catalog){this.catalog=catalog;}
        Map<String,Integer> calculate(){
            for(var a:catalog.all())if(!indices.containsKey(a.tag()))visit(a);
            var result=new HashMap<String,Integer>();
            for(var a:catalog.all())result.put(a.tag(),depth(groups.get(a.tag())));
            return Map.copyOf(result);
        }
        private void visit(Aspect a){
            String tag=a.tag();int index=indices.size();indices.put(tag,index);low.put(tag,index);
            stack.push(a);active.add(tag);
            for(String component:a.components()){
                var child=catalog.require(component);String key=child.tag();
                if(!indices.containsKey(key)){visit(child);low.put(tag,Math.min(low.get(tag),low.get(key)));}
                else if(active.contains(key))low.put(tag,Math.min(low.get(tag),indices.get(key)));
            }
            if(low.get(tag).equals(indices.get(tag))){
                var group=new ArrayList<Aspect>();Aspect child;
                do{child=stack.pop();active.remove(child.tag());groups.put(child.tag(),members.size());group.add(child);}while(!child.tag().equals(tag));
                members.add(group);
            }
        }
        private int depth(int group){
            if(depths.containsKey(group))return depths.get(group);
            int result=members.get(group).stream().allMatch(Aspect::isPrimal)?0:1;
            for(var a:members.get(group))for(String component:a.components()){
                int child=groups.get(catalog.require(component).tag());
                if(child!=group)result=Math.max(result,1+depth(child));
            }
            depths.put(group,result);return result;
        }
    }
    /** mode: native, reverse native, tier, reverse tier, grouped tier, reverse grouped tier. */
    public static List<List<Aspect>> rows(AspectCatalog catalog,int mode,int columns){
        if(columns<1||mode<0||mode>5)throw new IllegalArgumentException();
        var all=new ArrayList<>(catalog.all());
        if(mode>=2)all.sort(Comparator.comparingInt(a->tier(a,catalog)));
        if(mode%2==1)Collections.reverse(all);
        var result=new ArrayList<List<Aspect>>();var row=new ArrayList<Aspect>();int previous=-1;
        for(var a:all){int t=tier(a,catalog);
            if(!row.isEmpty()&&(row.size()==columns||(mode>=4&&t!=previous))){result.add(List.copyOf(row));row.clear();}
            row.add(a);previous=t;
        }
        if(!row.isEmpty())result.add(List.copyOf(row));return result;
    }
    public static int digit(int key){
        if(key>=48&&key<=57)return key-48;
        if(key>=320&&key<=329)return key-320;
        return switch(key){case 81->4;case 87->5;case 69->6;case 65->7;case 83->8;case 68->9;case 90->0;default->-1;};
    }
    public static int append(int value,int digit){return Math.min(EditorDefinitions.MAX_AMOUNT,value*10+digit);}
}
