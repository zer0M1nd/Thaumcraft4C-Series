package thmodern.lens;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
public final class LensNetwork {
    public static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(net.minecraft.resources.ResourceLocation.parse("thaumichorizons:lenses"),()->"1","1"::equals,"1"::equals);
    public record Change(String key){}
    public record View(net.minecraft.network.chat.Component name,java.util.Map<String,Integer> aspects,int progress){public View {aspects=java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(aspects));if(aspects.size()>128)throw new IllegalArgumentException("Too many lens aspects");}}
    public static void register(){
        CHANNEL.messageBuilder(Change.class,0,NetworkDirection.PLAY_TO_SERVER).encoder((p,b)->b.writeUtf(p.key(),32)).decoder(b->new Change(b.readUtf(32))).consumerMainThread((p,c)->{var player=c.get().getSender();if(player!=null)LensEquipment.change(player,p.key());}).add();
        CHANNEL.messageBuilder(View.class,1,NetworkDirection.PLAY_TO_CLIENT).encoder((p,b)->{b.writeComponent(p.name());b.writeVarInt(p.progress());b.writeVarInt(p.aspects().size());p.aspects().forEach((key,n)->{b.writeUtf(key,64);b.writeVarInt(n);});}).decoder(b->{var name=b.readComponent();int progress=b.readVarInt(),count=b.readVarInt();if(count<0||count>128)throw new IllegalArgumentException("Invalid lens view");var aspects=new java.util.LinkedHashMap<String,Integer>();for(int i=0;i<count;i++)aspects.put(b.readUtf(64),b.readVarInt());return new View(name,aspects,progress);}).consumerMainThread((p,c)->net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->thmodern.client.ScholarLensOverlay.receive(p))).add();
    }
    public static void view(net.minecraft.server.level.ServerPlayer player,View view){if(player.server.getPlayerList().getPlayer(player.getUUID())==player)CHANNEL.send(PacketDistributor.PLAYER.with(()->player),view);}
}
