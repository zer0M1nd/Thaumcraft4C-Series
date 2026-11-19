package ttmodern;

import java.util.*;
import java.util.function.IntUnaryOperator;

/** Bounded primal traits keep growth denominators positive and breeding data safe to persist. */
public final class CropTraits {
    public static final List<String> PRIMALS=List.of("aer","ignis","aqua","terra","ordo","perditio");
    private CropTraits() {}
    public static int growthBound(int water) { int bounded=Math.max(0,Math.min(50,water));return (2510-bounded*bounded)*6; }
    public static int exchangeBound(int air) { int bounded=Math.max(0,Math.min(50,air));return (2550-bounded*bounded)*10; }
    public static void cap(Map<String,Integer> values,int maximum,IntUnaryOperator random) {
        values.entrySet().removeIf(e->e.getValue()==null||e.getValue()<=0);
        values.replaceAll((key,value)->Math.min(maximum,value));
        int total=values.values().stream().mapToInt(Integer::intValue).sum();
        while(total-->maximum) { var keys=new ArrayList<>(values.keySet());var key=keys.get(random.applyAsInt(keys.size()));int amount=values.get(key)-1;if(amount==0)values.remove(key);else values.put(key,amount); }
    }
    public static int texture(String aspect) { int index=PRIMALS.indexOf(aspect);return index<0?6:index; }
}
