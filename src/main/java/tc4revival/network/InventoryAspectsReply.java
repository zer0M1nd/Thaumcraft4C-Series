package tc4revival.network;
import java.util.*;
import net.minecraft.network.FriendlyByteBuf;
import tc4revival.research.InventoryAspectDisplay.Icon;
public record InventoryAspectsReply(int request,List<Icon> icons) {
    public InventoryAspectsReply { icons=List.copyOf(icons);if(icons.size()>128)throw new IllegalArgumentException("Too many display aspects"); }
    public void encode(FriendlyByteBuf buf) { buf.writeVarInt(request);buf.writeVarInt(icons.size());for(var icon:icons){buf.writeUtf(icon.tag(),64);buf.writeVarInt(icon.amount());} }
    public static InventoryAspectsReply decode(FriendlyByteBuf buf) {
        int request=buf.readVarInt(),count=buf.readVarInt();if(count<0||count>128)throw new IllegalArgumentException("Invalid display aspect count");
        var icons=new ArrayList<Icon>();for(int i=0;i<count;i++){String tag=buf.readUtf(64);int amount=buf.readVarInt();if(amount<0 || (tag.isEmpty()&&amount!=0))throw new IllegalArgumentException("Invalid display amount");icons.add(new Icon(tag,amount));}
        if(buf.isReadable())throw new IllegalArgumentException("Trailing aspect reply data");return new InventoryAspectsReply(request,icons);
    }
}
