package tc4revival.content;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import tc4revival.research.ResearchBookOpening;

/** Both legacy variants use server-owned book sessions; the cheat configuration is checked there. */
public  class ThaumonomiconItem extends Item {
    private final boolean cheat;
    public ThaumonomiconItem(boolean cheat) { super(new Properties().stacksTo(1)); this.cheat = cheat; }
    public boolean cheat() { return cheat; }
    @Override public Rarity getRarity(ItemStack stack) { return cheat ? Rarity.EPIC : Rarity.UNCOMMON; }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer)
            ResearchBookOpening.open(serverPlayer, hand, stack -> stack.getItem() instanceof ThaumonomiconItem,
                    stack -> stack.getItem() instanceof ThaumonomiconItem book && book.cheat());
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        if (cheat) tooltip.add(Component.translatable("item.thaumcraft.thaumonomicon.cheat"));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
