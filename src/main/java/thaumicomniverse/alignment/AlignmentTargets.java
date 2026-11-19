package thaumicomniverse.alignment;

import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public final class AlignmentTargets extends SavedData {
    private final Map<String,AlignmentMath.Summary> targets=new TreeMap<>();
    public static AlignmentTargets get(ServerLevel level) {return level.getServer().overworld().getDataStorage().computeIfAbsent(AlignmentTargets::load,AlignmentTargets::new,"thaumicomniverse_alignment_targets");}
    public AlignmentMath.Summary resolve(String key,AlignmentMath.Summary generated) {return targets.computeIfAbsent(key,k->{setDirty();return generated;});}
    public static AlignmentTargets load(CompoundTag tag) {
        var saved=new AlignmentTargets();for(var key:tag.getAllKeys()){var entry=tag.getCompound(key);var vector=new TreeMap<String,Long>();var raw=entry.getCompound("vector");for(var aspect:raw.getAllKeys())vector.put(aspect,raw.getLong(aspect));
            if(entry.getLong("count")<1||entry.getInt("kinds")<1)throw new IllegalArgumentException("Corrupt alignment target "+key);saved.targets.put(key,new AlignmentMath.Summary(vector,entry.getLong("count"),entry.getInt("kinds")));}return saved;
    }
    @Override public CompoundTag save(CompoundTag tag) {targets.forEach((key,summary)->{var entry=new CompoundTag();var vector=new CompoundTag();summary.vector().forEach(vector::putLong);entry.put("vector",vector);entry.putLong("count",summary.count());entry.putInt("kinds",summary.kinds());tag.put(key,entry);});return tag;}
}
