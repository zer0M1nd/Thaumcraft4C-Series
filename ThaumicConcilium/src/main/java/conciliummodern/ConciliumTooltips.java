package conciliummodern;

import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=ThaumicConcilium.ID,value=Dist.CLIENT)
public final class ConciliumTooltips {
    private static final java.util.Map<String,String> HINTS=java.util.Map.of(
            "REFLECTION_HINT","reflection","THICK_TAINT_HINT","thicktaint","WINDINGS_HINT","windings","DEST_CRYSTAL_HINT","destcrystal",
            "VISCONDUCTOR_HINT","visconductor","POSITIVE_BURST_HINT","positiveburst","IMPULSE_HINT","impulse");
    private static boolean hasFourHints(net.minecraft.world.entity.player.Player player) {
        if(player==null)return false;
        var remaining=new java.util.HashSet<>(java.util.List.of("thaumicconcilium:REFLECTIONFOCI","thaumicconcilium:BOTTLEOFTHICKTAINT","thaumicconcilium:RUNICWINDINGS","thaumicconcilium:DESTCRYSTAL"));
        tc4revival.client.ClientKnowledge.snapshot().ifPresent(knowledge->remaining.removeAll(knowledge.researchMarkers()));
        for(int slot=0;slot<player.getInventory().getContainerSize();slot++){
            var stack=player.getInventory().getItem(slot);
            if((stack.is(tc4revival.content.TCItems.RESEARCH_NOTES.get()) || stack.is(tc4revival.content.TCItems.DISCOVERY.get())) && stack.hasTag())remaining.remove(stack.getTag().getString("key"));
        }
        return remaining.isEmpty();
    }
    @SubscribeEvent public static void tooltip(ItemTooltipEvent event) {
        if(NetherMembraneRecipe.coated(event.getItemStack()))event.getToolTip().add(Component.translatable("tc.tooltip.nether_membrane"));
        var stack=event.getItemStack();
        if((stack.is(tc4revival.content.TCItems.RESEARCH_NOTES.get()) || stack.is(tc4revival.content.TCItems.DISCOVERY.get())) && stack.hasTag()) {
            HINTS.forEach((tag,hint)->{if(stack.getTag().contains(tag))event.getToolTip().add(Component.translatable("tc.tooltip.hint."+hint));});
            if(java.util.List.of("REFLECTION_HINT","THICK_TAINT_HINT","WINDINGS_HINT","DEST_CRYSTAL_HINT").stream().anyMatch(stack.getTag()::contains)
                    && hasFourHints(event.getEntity()))event.getToolTip().add(Component.translatable("tc.tooltip.hint.mad_complete"));
        }
        if(stack.getItem() instanceof tc4revival.content.CastingWandItem && stack.hasTag()) {
            String name=stack.getTag().getString(WandXylographyRecipe.TAG);
            if(!name.isBlank())event.getToolTip().add(Component.translatable("tc.tooltip.xylography").append(" ").append(Component.literal(name)));
        }
    }
}
