package temodern.essentia;

import java.util.List;
import appeng.api.behaviors.ContainerItemStrategy;
import appeng.api.config.Actionable;
import appeng.api.stacks.GenericStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import thaumcraft.api.aspects.EssentiaStorageApi;
import temodern.TE;

/** Actual recoverable contents, also usable with the normal AE terminal's container interaction. */
public final class EssentiaAmpoule extends Item {
    public EssentiaAmpoule() { super(new Properties().stacksTo(1)); }
    public static ItemStack filled(EssentiaKey key, long amount) {
        var stack = new ItemStack(TE.AMPOULE.get());
        write(stack, key, amount); return stack;
    }
    public static GenericStack contents(ItemStack stack) {
        if (!(stack.getItem() instanceof EssentiaAmpoule) || !stack.hasTag()) return null;
        var key = EssentiaKey.parse(stack.getTag().getString("aspect"));
        long amount = stack.getTag().getLong("amount");
        return key == null || amount <= 0 ? null : new GenericStack(key, amount);
    }
    private static void write(ItemStack stack, EssentiaKey key, long amount) {
        if (amount <= 0) { stack.removeTagKey("aspect"); stack.removeTagKey("amount"); }
        else { stack.getOrCreateTag().putString("aspect", key.tag()); stack.getOrCreateTag().putLong("amount", amount); }
    }
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flags) {
        if(stack.hasTag()&&!thaumcraft.api.client.ClientAspectViews.visible(stack.getTag().getString("aspect"))){
            tooltip.add(thaumcraft.api.client.ClientAspectViews.unknownContents(java.util.List.of(stack.getTag().getString("aspect"))));return;
        }
        var content = contents(stack);
        if (content != null) tooltip.add(Component.literal(content.amount() + " ").append(content.what().getDisplayName()));
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        var player=context.getPlayer();
        if(player!=null && (!context.getLevel().mayInteract(player,context.getClickedPos())
                || !player.mayUseItemAt(context.getClickedPos(),context.getClickedFace(),context.getItemInHand())))return InteractionResult.FAIL;
        if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        var storage = EssentiaStorageApi.at(level, context.getClickedPos(), context.getClickedFace());
        var stack = context.getItemInHand();
        var content = contents(stack);
        if (content != null) {
            var key = (EssentiaKey) content.what();
            int accepted = storage.insert(key.aspect(), (int) Math.min(Integer.MAX_VALUE, content.amount()), false);
            write(stack, key, content.amount() - accepted);
            return accepted > 0 ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        for (var aspect : storage.contents().aspects()) {
            int taken = storage.extract(aspect, 64, false);
            if (taken > 0) { write(stack, EssentiaKey.of(aspect), taken); return InteractionResult.SUCCESS; }
        }
        return InteractionResult.PASS;
    }
    public static final class Strategy implements ContainerItemStrategy<EssentiaKey, ItemStack> {
        @Override public GenericStack getContainedStack(ItemStack stack) { return contents(stack); }
        @Override public ItemStack findCarriedContext(Player player, AbstractContainerMenu menu) {
            return menu.getCarried().getItem() instanceof EssentiaAmpoule ? menu.getCarried() : null;
        }
        @Override public ItemStack findPlayerSlotContext(Player player, int slot) {
            var stack = player.getInventory().getItem(slot);
            return stack.getItem() instanceof EssentiaAmpoule ? stack : null;
        }
        @Override public long extract(ItemStack stack, EssentiaKey key, long amount, Actionable mode) {
            var content = contents(stack);
            if (amount <= 0 || content == null || !content.what().equals(key)) return 0;
            long taken = Math.min(amount, content.amount());
            if (mode == Actionable.MODULATE) write(stack, key, content.amount() - taken);
            return taken;
        }
        @Override public long insert(ItemStack stack, EssentiaKey key, long amount, Actionable mode) {
            if (amount <= 0) return 0;
            var content = contents(stack);
            if (content != null && !content.what().equals(key)) return 0;
            long stored = content == null ? 0 : content.amount();
            // Recovery drops may exceed the normal capacity; they can always be emptied.
            long accepted = Math.min(amount, Math.max(0, 64 - stored));
            if (accepted > 0 && mode == Actionable.MODULATE) write(stack, key, stored + accepted);
            return accepted;
        }
        @Override public void playFillSound(Player player, EssentiaKey key) {}
        @Override public void playEmptySound(Player player, EssentiaKey key) {}
        @Override public GenericStack getExtractableContent(ItemStack stack) { return contents(stack); }
    }
}
