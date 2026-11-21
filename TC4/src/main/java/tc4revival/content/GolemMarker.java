package tc4revival.content;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/** A marker identifies a face; only the query's wildcard color matches every saved color. */
public record GolemMarker(BlockPos pos,ResourceLocation dimension,Direction face,byte color,thaumcraft.api.golems.GolemAnchor anchor) {
    public GolemMarker(BlockPos pos,ResourceLocation dimension,Direction face,byte color){this(pos,dimension,face,color,null);}
    public GolemMarker {
        pos=Objects.requireNonNull(pos).immutable(); Objects.requireNonNull(dimension); Objects.requireNonNull(face);
        if (color < -1 || color > 15) throw new IllegalArgumentException("Invalid golem marker color");
    }
    public boolean matches(GolemMarker stored) {
        return pos.equals(stored.pos) && dimension.equals(stored.dimension) && face==stored.face && (color==-1 || color==stored.color);
    }
    public CompoundTag save() {
        var tag=new CompoundTag(); tag.putInt("x",pos.getX()); tag.putInt("y",pos.getY()); tag.putInt("z",pos.getZ());
        if(anchor!=null)tag.put("Anchor",anchor.save());
        tag.putString("dimension",dimension.toString()); tag.putByte("side",(byte)face.get3DDataValue()); tag.putByte("color",color); return tag;
    }
    public static java.util.Optional<GolemMarker> load(CompoundTag tag) {
        var dimension=ResourceLocation.tryParse(tag.getString("dimension")); int side=tag.getByte("side"),color=tag.getByte("color");
        if (dimension==null || side<0 || side>5 || color < -1 || color>15) return java.util.Optional.empty();
        return java.util.Optional.of(new GolemMarker(new BlockPos(tag.getInt("x"),tag.getInt("y"),tag.getInt("z")),dimension,Direction.from3DDataValue(side),(byte)color,thaumcraft.api.golems.GolemAnchor.load(tag.getCompound("Anchor"))));
    }
}
