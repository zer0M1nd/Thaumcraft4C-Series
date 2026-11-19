package tc4revival.content;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
/** Client-observed nodes expire before visibility checks. */
public  class SinisterNodeMemory {
    private record Location(ResourceLocation dimension,BlockPos pos) {}
    private final java.util.Map<Location,Long> seen=new java.util.HashMap<>();
    public void observe(ResourceLocation dimension,BlockPos pos,long now) { seen.put(new Location(dimension,pos.immutable()),now); }
    public int size() { return seen.size(); }
    public boolean active(ResourceLocation dimension,Vec3 eye,Vec3 look,long now) {
        seen.entrySet().removeIf(entry->entry.getValue()<now-10000);boolean result=false;
        for(var entry:seen.entrySet()) {
            if(entry.getKey().dimension().equals(dimension)&&inCone(eye,look,entry.getKey().pos().getCenter())) { result=true;break; }
        }
        return result;
    }
    public static boolean inCone(Vec3 eye,Vec3 look,Vec3 target) {
        var difference=target.subtract(eye);double distance=difference.length(),axisLength=look.length()*256;
        if(distance==0||axisLength==0)return false;
        double projection=difference.dot(look.scale(256))/axisLength;
        return projection/distance>Math.cos(.66f/2)&&projection<axisLength;
    }
}
