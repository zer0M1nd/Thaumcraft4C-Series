package tc4revival.network;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import tc4revival.content.ElementalProspecting.Mark;
public record ElementalProspectingPacket(ResourceLocation dimension,BlockPos center,List<Mark> marks) {
    public ElementalProspectingPacket { center=center.immutable();marks=List.copyOf(marks);if(marks.size()>4913)throw new IllegalArgumentException("Oversized prospecting cube"); }
    public void encode(FriendlyByteBuf buffer) { buffer.writeResourceLocation(dimension);buffer.writeBlockPos(center);buffer.writeVarInt(marks.size());for(var mark:marks) { buffer.writeByte(mark.x()).writeByte(mark.y()).writeByte(mark.z());buffer.writeVarInt(mark.value()); } }
    public static ElementalProspectingPacket decode(FriendlyByteBuf buffer) {
        var dimension=buffer.readResourceLocation();var center=buffer.readBlockPos();int count=buffer.readVarInt();
        if(count<0||count>4913)throw new IllegalArgumentException("Invalid prospecting cube size");
        var marks=new ArrayList<Mark>();for(int i=0;i<count;i++)marks.add(new Mark(buffer.readByte(),buffer.readByte(),buffer.readByte(),buffer.readVarInt()));
        if(buffer.isReadable())throw new IllegalArgumentException("Trailing prospecting data");return new ElementalProspectingPacket(dimension,center,marks);
    }
}
