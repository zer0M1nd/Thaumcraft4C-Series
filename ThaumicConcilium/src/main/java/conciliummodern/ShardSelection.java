package conciliummodern;

import net.minecraft.world.InteractionHand;
import net.minecraftforge.network.*;

public final class ShardSelection {
    private static final net.minecraftforge.network.simple.SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(
            net.minecraft.resources.ResourceLocation.parse("thaumicconcilium:shard_mill"),()->"1","1"::equals,"1"::equals);
    private record Select(InteractionHand hand,int type){}
    public static void init(){CHANNEL.messageBuilder(Select.class,0,NetworkDirection.PLAY_TO_SERVER)
            .encoder((p,b)->b.writeEnum(p.hand).writeByte(p.type)).decoder(b->new Select(b.readEnum(InteractionHand.class),b.readUnsignedByte()))
            .consumerMainThread((p,c)->{var player=c.get().getSender();if(player!=null && player.isAlive() && !player.isSpectator()){
                var stack=player.getItemInHand(p.hand);if(stack.getItem() instanceof ShardMill mill)mill.select(stack,player,p.type);
            }c.get().setPacketHandled(true);}).add();}
    public static void send(InteractionHand hand,PowderType type){CHANNEL.sendToServer(new Select(hand,type.ordinal()));}
    private ShardSelection(){}
}
