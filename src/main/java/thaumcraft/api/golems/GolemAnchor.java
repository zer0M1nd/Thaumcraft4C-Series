package thaumcraft.api.golems;

import java.util.UUID;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

/** A face in a persistent container frame, independent of its current world position. */
public record GolemAnchor(UUID id, Direction face) {
    public CompoundTag save() {
        var tag=new CompoundTag(); tag.putUUID("Id",id); tag.putByte("Face",(byte)face.get3DDataValue()); return tag;
    }
    public static GolemAnchor load(CompoundTag tag) {
        int face=tag.getByte("Face");
        return tag.hasUUID("Id")&&face>=0&&face<6?new GolemAnchor(tag.getUUID("Id"),Direction.from3DDataValue(face)):null;
    }
}
