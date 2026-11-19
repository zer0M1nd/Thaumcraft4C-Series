package temodern.essentia;

import java.util.function.Consumer;
import java.util.function.Supplier;
import appeng.api.behaviors.ContainerItemStrategy;
import appeng.api.config.Actionable;
import appeng.api.stacks.GenericStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import tc4revival.aspect.AspectAmounts;
import tc4revival.content.*;

/** Native phials are indivisible eight-unit containers; jars retain their labels and never void overflow. */
public final class ContainerStrategy implements ContainerItemStrategy<EssentiaKey, ContainerStrategy.Context> {
    private final EssentiaAmpoule.Strategy ampoules = new EssentiaAmpoule.Strategy();
    public record Context(Player player, Supplier<ItemStack> get, Consumer<ItemStack> set) {
        void replaceOne(ItemStack replacement) {
            var original = get.get();
            if (original.getCount() == 1) set.accept(replacement);
            else { original.shrink(1); player.getInventory().placeItemBackInInventory(replacement); }
            player.getInventory().setChanged(); player.containerMenu.broadcastChanges();
        }
    }
    private boolean accepts(ItemStack stack) { return stack.getItem() instanceof EssentiaAmpoule || stack.getItem() instanceof EssentiaPhialItem || stack.getItem() instanceof EssentiaJarItem; }
    @Override public GenericStack getContainedStack(ItemStack stack) {
        if (stack.getItem() instanceof EssentiaAmpoule) return EssentiaAmpoule.contents(stack);
        if (!accepts(stack) || !(stack.getItem() instanceof StoredAspectItem item)) return null;
        var stored = item.storedAspects(stack);
        if (stored.size() != 1) return null;
        var aspect = stored.aspects().get(0);
        return stored.amount(aspect) > 0 ? new GenericStack(EssentiaKey.of(aspect), stored.amount(aspect)) : null;
    }
    @Override public Context findCarriedContext(Player player, AbstractContainerMenu menu) {
        return accepts(menu.getCarried()) ? new Context(player, menu::getCarried, menu::setCarried) : null;
    }
    @Override public Context findPlayerSlotContext(Player player, int slot) {
        return accepts(player.getInventory().getItem(slot)) ? new Context(player, () -> player.getInventory().getItem(slot), s -> player.getInventory().setItem(slot, s)) : null;
    }
    @Override public long extract(Context context, EssentiaKey key, long amount, Actionable mode) {
        var stack = context.get.get();
        if (stack.getItem() instanceof EssentiaAmpoule) return ampoules.extract(stack, key, amount, mode);
        var content = getContainedStack(stack);
        if (content == null || !content.what().equals(key) || amount <= 0) return 0;
        boolean phial = stack.getItem() instanceof EssentiaPhialItem;
        long taken = phial ? amount >= 8 && content.amount() == 8 ? 8 : 0 : Math.min(amount, content.amount());
        if (taken > 0 && mode == Actionable.MODULATE) context.replaceOne(phial ? new ItemStack(TCItems.EMPTY_PHIAL.get()) : jar(stack, key, (int) (content.amount() - taken)));
        return taken;
    }
    @Override public long insert(Context context, EssentiaKey key, long amount, Actionable mode) {
        if (amount <= 0 || key.aspect() == null) return 0;
        var stack = context.get.get();
        if (stack.getItem() instanceof EssentiaAmpoule) return ampoules.insert(stack, key, amount, mode);
        var content = getContainedStack(stack);
        if (content != null && !content.what().equals(key)) return 0;
        boolean phial = stack.getItem() instanceof EssentiaPhialItem;
        if (phial && !stack.is(TCItems.EMPTY_PHIAL.get())) return 0;
        if (stack.hasTag() && stack.getTag().contains("AspectFilter") && !stack.getTag().getString("AspectFilter").equals(key.tag())) return 0;
        long stored = content == null ? 0 : content.amount();
        long accepted = phial ? amount >= 8 ? 8 : 0 : Math.min(amount, Math.max(0, 64 - stored));
        if (accepted > 0 && mode == Actionable.MODULATE) context.replaceOne(phial ? EssentiaPhialItem.filled(key.aspect()) : jar(stack, key, (int) (stored + accepted)));
        return accepted;
    }
    private ItemStack jar(ItemStack original, EssentiaKey key, int amount) {
        boolean voidJar = original.is(TCItems.VOID_JAR.get()) || original.is(TCItems.FILLED_VOID_JAR.get());
        var result = new ItemStack(voidJar ? (amount==0?TCItems.VOID_JAR.get():TCItems.FILLED_VOID_JAR.get())
                : (amount==0?TCItems.ESSENTIA_JAR.get():TCItems.FILLED_ESSENTIA_JAR.get()));
        if (original.hasTag()) result.setTag(original.getTag().copy());
        ((StoredAspectItem) result.getItem()).setStoredAspects(result, amount == 0 ? new AspectAmounts() : new AspectAmounts().add(key.aspect(), amount));
        if(amount==0)result.removeTagKey("Aspects");
        if(result.hasTag()&&result.getTag().isEmpty())result.setTag(null);
        return result;
    }
    @Override public GenericStack getExtractableContent(Context context) { return getContainedStack(context.get.get()); }
    @Override public void playFillSound(Player player, EssentiaKey key) { player.playSound(net.minecraft.sounds.SoundEvents.GENERIC_SWIM, .25f, 1); }
    @Override public void playEmptySound(Player player, EssentiaKey key) { playFillSound(player, key); }
}
