package ttmodern;

import java.util.List;
import com.mojang.datafixers.util.Either;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.client.AspectTooltips;

/** Soul affinity uses the same icon row as the base mod, without pretending to be an essentia jar. */
@Mod.EventBusSubscriber(modid=Tinkerer.ID,value=Dist.CLIENT)
public class SoulAspectTooltip {
    @SubscribeEvent public static void gather(RenderTooltipEvent.GatherComponents event) {
        if(!(event.getItemStack().getItem() instanceof MobEssenceItem soul)||!AspectTooltips.enabled())return;
        var aspect=tc4revival.aspect.TCAspects.catalog().require(soul.aspect());
        event.getTooltipElements().removeIf(e->e.left().map(line->line.getString().equals(thaumcraft.api.aspects.AspectViews.display(aspect).displayName())).orElse(false));
        var row=new AspectTooltips.Row(Component.empty(),List.of(new AspectTooltips.Cell(soul.aspect(),"")));
        event.getTooltipElements().add(Math.min(1,event.getTooltipElements().size()),Either.right(new AspectTooltips.Data(List.of(row),Math.max(24,event.getScreenWidth()/2))));
    }
}
