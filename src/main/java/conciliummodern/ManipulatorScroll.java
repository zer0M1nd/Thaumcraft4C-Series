package conciliummodern;

public final class ManipulatorScroll {
    private static final net.minecraftforge.network.simple.SimpleChannel NETWORK=net.minecraftforge.network.NetworkRegistry.newSimpleChannel(
        net.minecraft.resources.ResourceLocation.parse("thaumicconcilium:manipulator"),()->"1","1"::equals,"1"::equals);
    private record Scroll(int direction){}
    public static void init(){NETWORK.messageBuilder(Scroll.class,0,net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
        .encoder((packet,buffer)->buffer.writeByte(packet.direction)).decoder(buffer->new Scroll(Integer.signum(buffer.readByte())))
        .consumerMainThread((packet,context)->{var player=context.get().getSender();
            if(player!=null && player.isAlive() && player.isUsingItem() && player.isShiftKeyDown() && packet.direction!=0){
                var stack=player.getUseItem();if(stack.getItem() instanceof tc4revival.content.CastingWandItem && tc4revival.content.WandFocusItem.attached(stack).getItem() instanceof EtherealManipulatorFocus){
                    var tag=player.getPersistentData();long now=player.level().getGameTime();String key="thaumicconcilium:manipulator_scroll_tick";
                    if(!tag.contains(key) || tag.getLong(key)!=now){tag.putLong(key,now);EtherealManipulatorFocus.range(stack,net.minecraft.util.Mth.clamp(EtherealManipulatorFocus.range(stack)+packet.direction,1,16));player.getInventory().setChanged();}
                }
            }context.get().setPacketHandled(true);
        }).add();}
    public static void send(int direction){NETWORK.sendToServer(new Scroll(Integer.signum(direction)));}
    private ManipulatorScroll(){}
}
