package tbmodern.api;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Called once on the server after a complete pipe use. The pipe consumes one dose afterwards. */
public interface Tobacco {
    void smoke(ServerPlayer player, ItemStack dose, ItemStack pipe, boolean silverwood);
}
