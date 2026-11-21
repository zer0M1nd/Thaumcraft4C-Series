package tc4revival.network;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public record FocusListReply(int request, Map<String, ItemStack> foci) {
    public FocusListReply { foci = Collections.unmodifiableMap(new LinkedHashMap<>(foci)); }
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(request);
        buffer.writeMap(foci, (buf, key) -> buf.writeUtf(key, 128), FriendlyByteBuf::writeItem);
    }
    public static FocusListReply decode(FriendlyByteBuf buffer) {
        return new FocusListReply(buffer.readVarInt(), buffer.readMap(LinkedHashMap::new, buf -> buf.readUtf(128), FriendlyByteBuf::readItem));
    }
}
