package automagy.content;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

public final class FilterItem extends Item {
    public final boolean blacklist;
    public FilterItem(boolean blacklist) { super(new Properties().stacksTo(1)); this.blacklist = blacklist; }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        int slot = hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : 40;
        if (player instanceof ServerPlayer server) NetworkHooks.openScreen(server,
                new SimpleMenuProvider((id, inv, p) -> new FilterMenu(id, inv, slot), stack.getHoverName()),
                buf -> { buf.writeVarInt(slot); buf.writeUtf(ItemFilter.load(stack).name(), ItemFilter.MAX_NAME); });
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
