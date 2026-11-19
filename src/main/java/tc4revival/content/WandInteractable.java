package tc4revival.content;

import java.util.Optional;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Base-mod wand callbacks for both blocks and block entities. PASS continues dispatch. */
public interface WandInteractable {
    InteractionResult onWandUseFirst(ItemStack wand, UseOnContext context);
    Optional<ItemStack> onWandUse(Level level, ItemStack wand, Player player);
    void onWandUseTick(ItemStack wand, Player player, int remainingTicks);
    void onWandRelease(ItemStack wand, Level level, Player player, int remainingTicks);
}
