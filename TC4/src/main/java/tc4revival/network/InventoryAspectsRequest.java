package tc4revival.network;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;
import tc4revival.research.*;
/** Client-supplied stacks are queries only, including creative virtual slots; never inventory mutations. */
public record InventoryAspectsRequest(int request,ItemStack stack) {
    private static final java.util.Map<ServerPlayer,Integer> LAST=new java.util.WeakHashMap<>();
    public void encode(FriendlyByteBuf buf) { buf.writeVarInt(request);buf.writeItem(stack); }
    public static InventoryAspectsRequest decode(FriendlyByteBuf buf) {
        var packet=new InventoryAspectsRequest(buf.readVarInt(),buf.readItem());
        if(buf.isReadable())throw new IllegalArgumentException("Trailing aspect request data");return packet;
    }
    public void handle(ServerPlayer player) {
        if(player==null)return;
        int now=player.server.getTickCount();var last=LAST.get(player);if(last!=null && now-last<4)return;LAST.put(player,now);
        // Keep a forged recursive inventory tag from causing unbounded work in a display-only query.
        var icons=stack.hasTag() && stack.getTag().sizeInBytes()>262144?java.util.List.<InventoryAspectDisplay.Icon>of():
                InventoryAspectDisplay.resolve(stack,KnowledgeSavedData.get(player.server).snapshot(player.getUUID()).orElse(null));
        if(stack.isEmpty()&&player.containerMenu==player.inventoryMenu&&player.containerMenu.getCarried().is(tc4revival.content.TCItems.THAUMOMETER.get()))
            icons=InventoryAspectDisplay.resolveSelf(player,KnowledgeSavedData.get(player.server).snapshot(player.getUUID()).orElse(null));
        TCNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new InventoryAspectsReply(request,icons));
    }
}
