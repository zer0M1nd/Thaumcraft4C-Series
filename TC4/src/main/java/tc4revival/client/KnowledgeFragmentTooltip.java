package tc4revival.client;

import java.util.Comparator;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.TC4Revival;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.AspectNbt;
import tc4revival.aspect.TCAspects;
import tc4revival.content.TCItems;

/** ItemResource's optional stored-aspect names, separate from scan-derived tooltips. */
@Mod.EventBusSubscriber(modid = TC4Revival.MOD_ID, value = Dist.CLIENT)
public  class KnowledgeFragmentTooltip {
    private KnowledgeFragmentTooltip() {}
    @SubscribeEvent public static void tooltip(ItemTooltipEvent event) {
        if(AspectTooltips.enabled())return;
        var stack = event.getItemStack();
        if (!(stack.getItem() instanceof tc4revival.content.StoredAspectItem) || !stack.hasTag() || event.getEntity() == null) return;
        var knowledge = ClientKnowledge.displaySnapshot();
        var stored = AspectNbt.read(stack.getTag(), TCAspects.catalog()).amounts();
        var names = stored.view().keySet().stream()
                .sorted(Comparator.comparing(Aspect::tag))
                .map(aspect -> knowledge.filter(state -> state.knows(aspect)).isPresent()
                        ? Component.literal(thaumcraft.api.client.ClientAspectViews.view(aspect).displayName() + (stack.getItem() instanceof tc4revival.content.ManaBeanItem || stack.getItem() instanceof tc4revival.content.WispEssenceItem || stack.getItem() instanceof tc4revival.content.EssenceCrystalItem || stack.getItem() instanceof tc4revival.content.EssentiaPhialItem || stack.getItem() instanceof tc4revival.content.EssentiaJarItem ? " x" + stored.amount(aspect) : "")) : Component.translatable("tc.aspect.unknown"))
                .toList();
        event.getToolTip().addAll(Math.min(1, event.getToolTip().size()), names);
    }
}
