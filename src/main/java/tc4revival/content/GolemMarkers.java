package tc4revival.content;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;

/** Ordered, face-sensitive marker storage shared by the bell and golem transport tasks. */
public  class GolemMarkers {
    private final List<GolemMarker> entries=new ArrayList<>();
    public List<GolemMarker> inDimension(ResourceLocation dimension) {
        return entries.stream().filter(marker->marker.dimension().equals(dimension)).toList();
    }
    public void replace(Collection<GolemMarker> markers) { entries.clear(); entries.addAll(markers); }
    public List<Direction> faces(BlockPos pos,ResourceLocation dimension,byte color) {
        var markers=inDimension(dimension); var result=new ArrayList<Direction>();
        for (var face:Direction.values()) {
            var query=new GolemMarker(pos,dimension,face,color);
            if (markers.stream().anyMatch(query::matches)) result.add(face);
        }
        return List.copyOf(result);
    }
    public ListTag save() { var list=new ListTag(); entries.forEach(marker->list.add(marker.save())); return list; }
    public void load(ListTag list) {
        entries.clear(); for (Tag entry:list) if (entry instanceof CompoundTag tag) GolemMarker.load(tag).ifPresent(entries::add);
    }
}
