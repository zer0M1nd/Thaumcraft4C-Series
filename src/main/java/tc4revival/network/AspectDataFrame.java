package tc4revival.network;

import net.minecraft.network.FriendlyByteBuf;
import tc4revival.aspect.sync.AspectDataTransfer;

public record AspectDataFrame(int revision,boolean manifest,int index,int count,byte[] bytes) {
    public void encode(FriendlyByteBuf buffer){buffer.writeVarInt(revision);buffer.writeBoolean(manifest);buffer.writeVarInt(index);buffer.writeVarInt(count);buffer.writeByteArray(bytes);}
    public static AspectDataFrame decode(FriendlyByteBuf buffer){
        int revision=buffer.readVarInt();boolean manifest=buffer.readBoolean();int index=buffer.readVarInt(),count=buffer.readVarInt();
        if(count<1||count>AspectDataTransfer.MAX_COMPRESSED/AspectDataTransfer.CHUNK||index<0||index>=count)throw new IllegalArgumentException("Invalid aspect sync frame");
        return new AspectDataFrame(revision,manifest,index,count,buffer.readByteArray(AspectDataTransfer.CHUNK));
    }
}
