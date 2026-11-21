package tc4revival.content;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** The old labyrinth cancels survival flight every second. */
@Mod.EventBusSubscriber(modid="thaumcraft")
public  class OuterLandsRestrictions {
    private OuterLandsRestrictions() {}
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase!=TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || player.tickCount%20!=0 || player.getAbilities().instabuild || player.isSpectator()
                || !player.level().dimension().equals(TCEldritch.OUTER_LANDS) || !player.getAbilities().flying) return;
        player.getAbilities().flying=false; player.onUpdateAbilities();
        player.sendSystemMessage(Component.translatable("tc.break.fly").withStyle(ChatFormatting.ITALIC,ChatFormatting.GRAY));
    }
}
