package tc4revival.network;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
public record AspectEditorOpen(UUID token,String defaultName) {
    public static void encode(AspectEditorOpen p,FriendlyByteBuf b){b.writeUUID(p.token);b.writeUtf(p.defaultName,64);}
    public static AspectEditorOpen decode(FriendlyByteBuf b){return new AspectEditorOpen(b.readUUID(),b.readUtf(64));}
}
