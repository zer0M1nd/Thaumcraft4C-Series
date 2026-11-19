package tc4revival.network;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
public record AspectEditorResult(UUID token,boolean success,String message,String nextName) {
    public static void encode(AspectEditorResult p,FriendlyByteBuf b){b.writeUUID(p.token);b.writeBoolean(p.success);b.writeUtf(p.message,1024);b.writeUtf(p.nextName,64);}
    public static AspectEditorResult decode(FriendlyByteBuf b){return new AspectEditorResult(b.readUUID(),b.readBoolean(),b.readUtf(1024),b.readUtf(64));}
}
