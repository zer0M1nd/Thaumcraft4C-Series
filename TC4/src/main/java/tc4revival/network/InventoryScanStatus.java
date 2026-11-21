package tc4revival.network;
import net.minecraft.network.FriendlyByteBuf;
/** Display-only acknowledgement: completed/invalid targets must not keep ticking or show progress. */
public record InventoryScanStatus(int menu,int slot,boolean tracking) {
    public void encode(FriendlyByteBuf b){b.writeVarInt(menu);b.writeVarInt(slot);b.writeBoolean(tracking);}
    public static InventoryScanStatus decode(FriendlyByteBuf b){return new InventoryScanStatus(b.readVarInt(),b.readVarInt(),b.readBoolean());}
}
