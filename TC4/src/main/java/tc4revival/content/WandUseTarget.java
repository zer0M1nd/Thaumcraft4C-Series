package tc4revival.content;

import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/** Legacy coordinate keys plus a dimension guard against stale cross-world callbacks. */
public record WandUseTarget(int x, int y, int z, String dimension) {
    public void write(CompoundTag tag) {
        tag.putInt("IIUX", x); tag.putInt("IIUY", y); tag.putInt("IIUZ", z); tag.putString("tc4_use_dimension", dimension);
    }
    public static Optional<WandUseTarget> read(CompoundTag tag, String currentDimension) {
        if (tag == null || !tag.contains("IIUX", Tag.TAG_INT) || !tag.contains("IIUY", Tag.TAG_INT) || !tag.contains("IIUZ", Tag.TAG_INT)
                || !tag.contains("tc4_use_dimension", Tag.TAG_STRING) || !tag.getString("tc4_use_dimension").equals(currentDimension))
            return Optional.empty();
        return Optional.of(new WandUseTarget(tag.getInt("IIUX"), tag.getInt("IIUY"), tag.getInt("IIUZ"), currentDimension));
    }
    public static void clear(CompoundTag tag) {
        if (tag == null) return;
        tag.remove("IIUX"); tag.remove("IIUY"); tag.remove("IIUZ"); tag.remove("tc4_use_dimension");
    }
}
