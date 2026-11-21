package automagy.content;
import automagy.logic.*;
import java.util.*;
import net.minecraft.nbt.*;
/** Named tags, including unpaid costs, avoid restarting/refunding cycles on chunk load. */
final class NetherProgressData {
 static CompoundTag cycle(NethermindCycle cycle){var s=cycle.snapshot();var tag=new CompoundTag();tag.putString("Rune",s.rune());tag.putBoolean("DoubleBlaze",s.doubleBlaze());tag.putInt("Remaining",s.remaining());tag.putInt("Power",s.power());tag.putInt("Age",s.age());tag.putBoolean("InstabilityChecked",s.instabilityChecked());tag.put("Needed",amounts(s.needed()));return tag;}
 static void cycle(NethermindCycle cycle,CompoundTag tag){cycle.restore(new NethermindCycle.Snapshot(tag.getString("Rune"),tag.getBoolean("DoubleBlaze"),tag.getInt("Remaining"),tag.getInt("Power"),tag.getInt("Age"),tag.getBoolean("InstabilityChecked"),amounts(tag.getCompound("Needed"))));}
 static CompoundTag bait(NetherBait bait){var s=bait.snapshot();var tag=new CompoundTag();tag.putString("Active",s.active());tag.putInt("Reward",s.reward());tag.put("Uses",amounts(s.uses()));tag.put("Needed",amounts(s.needed()));return tag;}
 static void bait(NetherBait bait,CompoundTag tag){bait.restore(new NetherBait.Snapshot(tag.getString("Active"),tag.getInt("Reward"),amounts(tag.getCompound("Uses")),amounts(tag.getCompound("Needed"))));}
 private static CompoundTag amounts(Map<String,Integer> values){var tag=new CompoundTag();values.forEach(tag::putInt);return tag;}
 private static Map<String,Integer> amounts(CompoundTag tag){var values=new LinkedHashMap<String,Integer>();for(String id:tag.getAllKeys())if(tag.contains(id,Tag.TAG_INT)&&tag.getInt(id)>0)values.put(id,tag.getInt(id));return values;}
 private NetherProgressData(){}
}
