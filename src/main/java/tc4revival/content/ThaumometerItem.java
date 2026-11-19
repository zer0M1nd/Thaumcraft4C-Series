package tc4revival.content;

import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import tc4revival.client.ThaumometerRenderer;
import tc4revival.research.ThaumometerUse;

/** Vanilla use packets carry intent only; the server owns the target and elapsed scan time. */
public  class ThaumometerItem extends Item {
    public ThaumometerItem() { super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON)); }
    @Override public void appendHoverText(ItemStack stack,Level level,java.util.List<net.minecraft.network.chat.Component> lines,net.minecraft.world.item.TooltipFlag flag) {
        lines.add(net.minecraft.network.chat.Component.translatable("thaumcraft.thaumometer.ignore_fluids").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
    @Override public int getUseDuration(ItemStack stack) { return tc4revival.research.ScanProgress.USE_DURATION; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.NONE; }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        if (player instanceof ServerPlayer serverPlayer) ThaumometerUse.begin(serverPlayer, hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }
    @Override public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (entity instanceof ServerPlayer player) ThaumometerUse.tick(player, stack, remaining);
        // TC4 ends the local use at count 5. Do not keep the raised scanner waiting for a server round trip.
        else if(level.isClientSide && remaining<=tc4revival.research.ScanProgress.COMPLETE_REMAINING)entity.stopUsingItem();
    }
    @Override public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remaining) {
        if (entity instanceof ServerPlayer player) ThaumometerUse.cancel(player);
    }
    @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player) ThaumometerUse.cancel(player);
        return stack;
    }
    @Override public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return ThaumometerRenderer.instance();
            }
        });
    }
}
