package gadomancymodern.node;
import java.util.*;
/** Original feeding saturation and aggression arithmetic, independent of world/client state. */
public class GrowingSaturation {
    public final Map<String,Double> saturation=new LinkedHashMap<>();
    public String lastAspect;public int consecutive;public double happiness;public boolean saturated;
    public boolean accepts(String aspect,double random){if(saturated)return false;var stored=saturation.get(aspect);if(stored==null){happiness/=15;return true;}double fraction=stored/110;if(random<=fraction)return false;double increase=1/(1-fraction);if(lastAspect!=null){if(lastAspect.equals(aspect))increase*=Math.log(Math.max(1,consecutive))/6+1;else{increase*=-10;consecutive=0;}}happiness=Math.max(0,happiness+increase);return true;}
    /** Returns whether the original excess-aggression vortex must begin. */
    public boolean fed(String aspect,boolean primal,String source){if(saturated)return false;double multiplier=switch(source){case "wisp"->.8;case "wisp_essence"->1.2;case "aspect_orb"->.7;case "mana_bean","crystal_essence"->1.4;default->1;};if(aspect.equals(lastAspect)){multiplier+=.4;consecutive++;}lastAspect=aspect;saturation.merge(aspect,(primal?1.4:1)*multiplier,Double::sum);saturation.replaceAll((key,value)->key.equals(aspect)?value:Math.max(0,value-1.0/15));boolean explosion=happiness>400;if(explosion)happiness/=10;double average=saturation.values().stream().mapToDouble(Double::doubleValue).average().orElse(0);saturated=average>93.5;return explosion;}
    public boolean zap(int roll){return happiness>roll;}
    public float damage(){double ratio=happiness/400;return ratio>.9?12:ratio>.6?8:ratio>.3?5:2;}
    public net.minecraft.nbt.CompoundTag save(){var tag=new net.minecraft.nbt.CompoundTag();tag.putBoolean("overallSaturated",saturated);tag.putDouble("overallHappiness",happiness);if(lastAspect!=null){tag.putString("lastFedAspect",lastAspect);tag.putInt("lastFedRow",consecutive);}var values=new net.minecraft.nbt.CompoundTag();saturation.forEach(values::putDouble);tag.put("Saturation",values);return tag;}
    public void load(net.minecraft.nbt.CompoundTag tag){saturation.clear();saturated=tag.getBoolean("overallSaturated");happiness=finite(tag.getDouble("overallHappiness"));lastAspect=null;consecutive=0;/* Original and GTNH test lastFed, while their writer does not emit it. Preserve the ordinary reload behavior. */if(tag.contains("lastFed")){lastAspect=tag.getString("lastFedAspect");consecutive=Math.max(0,tag.getInt("lastFedRow"));}var values=tag.getCompound("Saturation");for(var key:values.getAllKeys())saturation.put(key,finite(values.getDouble(key)));}
    protected static double finite(double value){return Double.isFinite(value)?Math.max(0,value):0;}
}
