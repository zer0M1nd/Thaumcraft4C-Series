package tc4revival.network;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
public record AspectEditorSave(UUID token,int index,int count,byte[] bytes) {
    public static void encode(AspectEditorSave p,FriendlyByteBuf b){b.writeUUID(p.token);b.writeVarInt(p.index);b.writeVarInt(p.count);b.writeByteArray(p.bytes);}
    public static AspectEditorSave decode(FriendlyByteBuf b){return new AspectEditorSave(b.readUUID(),b.readVarInt(),b.readVarInt(),b.readByteArray(24576));}
}
