package arcanearteries;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import vazkii.botania.api.mana.ManaItemHandler;

/** Only entered after Botania's presence is checked; optional API never enters item signatures. */
final class BotaniaBridge {
    static boolean request(ItemStack stack, Player player, boolean consume) {
        return ManaItemHandler.instance().requestManaExact(stack, player, ArteriesConfig.value(ArteriesConfig.MANA_COST), consume);
    }
    private BotaniaBridge() {}
}
