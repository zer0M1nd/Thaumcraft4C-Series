package tc4revival.content;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.config.TCConfig;
import tc4revival.network.*;
import tc4revival.research.*;

/** The eight standalone TC4 item warp registrations, independent of worn gear and research warp. */
@Mod.EventBusSubscriber(modid="thaumcraft")
public  class CraftingWarp {
    private CraftingWarp() {}
    public static int amount(ItemStack output) {
        if(output.isEmpty()) return 0;
        var addon=thaumcraft.api.ThaumcraftApi.craftingWarp(output);if(addon.isPresent())return addon.getAsInt();
        if(output.is(TCItems.SINISTER_STONE.get())) return SinisterStoneItem.legacyMetadata(output)==0?1:0;
        return output.is(TCItems.HELLBAT_FOCUS.get()) || output.is(TCItems.BRAIN_JAR.get())
                || output.is(TCItems.LIQUID_DEATH_BUCKET.get()) || output.is(TCItems.TAINT_BOTTLE.get())
                || output.is(TCItems.golem(GolemMaterial.FLESH)) || output.is(TCItems.PRIMAL_FOCUS.get())
                || output.is(TCItems.wandComponent("staff_rod_primal")) ? 1 : 0;
    }
    @SubscribeEvent public static void crafted(PlayerEvent.ItemCraftedEvent event) {
        if(!(event.getEntity() instanceof ServerPlayer player) || TCConfig.SERVER.disableWarp.get()) return;
        int gain=amount(event.getCrafting()); if(gain==0) return;
        // Old ItemCraftedEvent awards once, even for a multi-item result or a creative player.
        KnowledgeSavedData.get(player.server).update(player.getUUID(),player.getRandom()::nextInt,knowledge->{
            knowledge.changeWarp(PlayerKnowledge.WarpKind.STICKY,gain);
            knowledge.setWarpCounter(knowledge.totalWarp()); return null;
        });
        TCNetwork.send(player,new WarpGainPacket(PlayerKnowledge.WarpKind.STICKY,gain));
    }
}
