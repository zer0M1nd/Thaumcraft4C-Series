package tc4revival.content;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** The old TileOwned ward checks the pearl's spawn location, regardless of access rights. */
@Mod.EventBusSubscriber(modid="thaumcraft")
public  class OwnedDevicePearlProtection {
    private OwnedDevicePearlProtection() {}
    @SubscribeEvent public static void joined(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof ThrownEnderpearl pearl)) return;
        var origin=pearl.blockPosition();
        for (var pos:BlockPos.betweenClosed(origin.offset(-5,-5,-5),origin.offset(5,5,5))) {
            if (!(level.getBlockEntity(pos) instanceof OwnedDeviceBlockEntity)) continue;
            if (pearl.getOwner() instanceof Player player) player.displayClientMessage(
                    Component.translatable("thaumcraft.ward.pearl_blocked")
                            .withStyle(ChatFormatting.DARK_PURPLE,ChatFormatting.ITALIC),false);
            // Modern entity managers must not receive an entity already marked removed.
            event.setCanceled(true);
            pearl.discard();
            return;
        }
    }
}
