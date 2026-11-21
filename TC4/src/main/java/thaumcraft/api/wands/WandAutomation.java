package thaumcraft.api.wands;

import java.util.UUID;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;

/** Thread-local, nestable read identity for one wand interaction. Mutable player data is never redirected. */
public class WandAutomation {
    public static final String KEY = "thaumcraft:automation_core";
    private record Identity(UUID actor, UUID owner) {}
    private static final ThreadLocal<Identity> ACTIVE = new ThreadLocal<>();
    public static boolean installed(ItemStack wand) { return wand.hasTag() && wand.getTag().contains(KEY); }
    public static ItemStack core(ItemStack wand) {
        return installed(wand) ? ItemStack.of(wand.getTag().getCompound(KEY)) : ItemStack.EMPTY;
    }
    public static boolean allowed(ItemStack wand, Player player) {
        if (!(player instanceof FakePlayer) || !installed(wand)) return true;
        var core = core(wand);
        return player instanceof FakePlayer && core.getItem() instanceof AutomationCore item && item.enabled(core) && item.owner(core) != null;
    }
    public static UUID readIdentity(UUID actor) {
        var current = ACTIVE.get(); return current != null && current.actor().equals(actor) ? current.owner() : actor;
    }
    public static Scope open(ItemStack wand, Player player) {
        var previous = ACTIVE.get();
        var core = core(wand);
        if (player instanceof FakePlayer && core.getItem() instanceof AutomationCore item && item.enabled(core) && item.owner(core) != null)
            ACTIVE.set(new Identity(player.getUUID(), item.owner(core)));
        else ACTIVE.remove();
        return new Scope(previous);
    }
    public static class Scope implements AutoCloseable {
        private final Identity previous;
        private boolean closed;
        private Scope(Identity previous) { this.previous = previous; }
        @Override public void close() {
            if (closed) return; closed = true;
            if (previous == null) ACTIVE.remove(); else ACTIVE.set(previous);
        }
    }
}
