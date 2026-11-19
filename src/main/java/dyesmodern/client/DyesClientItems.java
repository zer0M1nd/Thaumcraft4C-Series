package dyesmodern.client;

import dyesmodern.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.client.ClientKnowledge;

@Mod.EventBusSubscriber(modid="thaumicdyes",value=Dist.CLIENT)
public final class DyesClientItems {
    @SubscribeEvent public static void tooltip(ItemTooltipEvent event) {
        if(!Minecraft.getInstance().isSameThread()||event.getEntity()==null)return;
        var stack=event.getItemStack();
        if(!(stack.getItem() instanceof DyesArmorItem item)||item.upgradeSlots()!=1||DyesArmorItem.upgrade(stack,0)!=0)return;
        if(ClientKnowledge.snapshot().map(k->k.researchMarkers().contains("thaumicdyes:TD.RUNICARMORUPGRADES")).orElse(false))
            event.getToolTip().add(Math.min(1,event.getToolTip().size()),Component.translatable("item.runic.upgrade.0").withStyle(ChatFormatting.DARK_GRAY));
    }
    @Mod.EventBusSubscriber(modid="thaumicdyes",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Colors {
        @SubscribeEvent public static void register(RegisterColorHandlersEvent.Item event) {
            Item[] items=DyesItems.ALL.values().stream().map(value->value.get()).filter(item->item instanceof DyesArmorItem armor&&armor.dyeable()).toArray(Item[]::new);
            event.register((stack,tint)->tint==0?((DyesArmorItem)stack.getItem()).getColor(stack):-1,items);
        }
    }
    private DyesClientItems(){}
}
