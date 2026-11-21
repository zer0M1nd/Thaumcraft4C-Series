package thaumcraft.api.wands;

import java.util.*;
import net.minecraft.world.item.ItemStack;
import tc4revival.content.FocusUpgrade;
/** JSON supplies identity/cost/artwork/ranks; named code callbacks supply behavior. Register in common setup enqueueWork. */
public  class FocusUpgradeType {
    public interface Behavior {
        default void applied(ItemStack focus,int rank) {}
        default boolean canApply(ItemStack focus,net.minecraft.world.entity.player.Player player,int rank) { return true; }
        default double costMultiplier(ItemStack focus,net.minecraft.world.entity.player.Player player,tc4revival.aspect.Aspect aspect,int level) { return 1; }
        default void tick(ItemStack wand,ItemStack focus,net.minecraft.world.entity.player.Player player,int level) {}
    }
    private static final Map<String,Behavior> BEHAVIORS=new LinkedHashMap<>();
    private FocusUpgradeType() {}
    public static FocusUpgrade get(String key) { return FocusUpgrade.require(key); }
    public static synchronized void registerBehavior(String key,Behavior behavior) {
        FocusUpgrade.require(key);
        if(BEHAVIORS.putIfAbsent(key,Objects.requireNonNull(behavior))!=null)throw new IllegalArgumentException("Duplicate upgrade behavior "+key);
    }
    public static void applied(FocusUpgrade upgrade,ItemStack focus,int rank) { var behavior=BEHAVIORS.get(upgrade.key);if(behavior!=null)behavior.applied(focus,rank); }
    public static boolean canApply(FocusUpgrade upgrade,ItemStack focus,net.minecraft.world.entity.player.Player player,int rank) { var behavior=BEHAVIORS.get(upgrade.key);return behavior==null||behavior.canApply(focus,player,rank); }
    public static double costMultiplier(ItemStack focus,net.minecraft.world.entity.player.Player player,tc4revival.aspect.Aspect aspect) {
        if(!(focus.getItem() instanceof tc4revival.content.WandFocusItem item))return 1;
        double total=1;
        for(var entry:BEHAVIORS.entrySet()) {
            int level=item.upgradeLevel(focus,get(entry.getKey()).id);
            if(level>0) {double value=entry.getValue().costMultiplier(focus,player,aspect,level);if(!Double.isFinite(value)||value<0)throw new IllegalArgumentException("Invalid upgrade vis multiplier "+entry.getKey());total*=value;}
        }
        return total;
    }
    public static void tick(ItemStack wand,net.minecraft.world.entity.player.Player player) {
        if(BEHAVIORS.isEmpty())return;
        var focus=tc4revival.content.WandFocusItem.attached(wand);
        if(!(focus.getItem() instanceof tc4revival.content.WandFocusItem item))return;
        BEHAVIORS.forEach((key,behavior)->{int level=item.upgradeLevel(focus,get(key).id);if(level>0)behavior.tick(wand,focus,player,level);});
        // attached() returns a copy; persist callback changes back into the wand.
        tc4revival.content.WandFocusItem.attach(wand,focus);
    }
}
