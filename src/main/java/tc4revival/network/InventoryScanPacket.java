package tc4revival.network;
import net.minecraft.network.FriendlyByteBuf;
public record InventoryScanPacket(int menu,int slot) {
    public void encode(FriendlyByteBuf b){b.writeVarInt(menu);b.writeVarInt(slot);}
    public static InventoryScanPacket decode(FriendlyByteBuf b){return new InventoryScanPacket(b.readVarInt(),b.readVarInt());}
    public void handle(net.minecraft.server.level.ServerPlayer p){tc4revival.research.InventoryScanning.request(p,menu,slot);}
}
