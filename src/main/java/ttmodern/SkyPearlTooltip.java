package ttmodern;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid=Tinkerer.ID,value=net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class SkyPearlTooltip {
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void tooltip(net.minecraftforge.event.entity.player.ItemTooltipEvent event){
        var pearl=event.getItemStack();var player=event.getEntity();if(player==null||!pearl.is(Tinkerer.SKY_PEARL.get())||!SkyPearlItem.bound(pearl))return;
        if(player.level().dimension().location().equals(SkyPearlItem.dimension(pearl))){double distance=Math.sqrt(player.distanceToSqr(SkyPearlItem.position(pearl).getCenter()));event.getToolTip().add(net.minecraft.network.chat.Component.literal(String.format(java.util.Locale.ROOT,"%.2f m",Math.ceil(distance*100)/100)));}
        else event.getToolTip().add(net.minecraft.network.chat.Component.translatable("ttmisc.differentDim"));
    }
}
