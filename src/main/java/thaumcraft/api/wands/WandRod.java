package thaumcraft.api.wands;

import java.util.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
/** Rod metadata lives in JSON. Register behavior only when the default regeneration is insufficient. */
public  class WandRod {
    @FunctionalInterface public interface Update { void tick(ItemStack wand,Player player); }
    private static final Map<String,Update> UPDATES=new LinkedHashMap<>();
    public static tc4revival.content.WandParts.Rod get(String id) { return tc4revival.content.WandParts.Rod.require(id); }
    public static synchronized void registerUpdate(String id,Update callback) {
        get(id);if(UPDATES.putIfAbsent(id,Objects.requireNonNull(callback))!=null)throw new IllegalArgumentException("Duplicate rod callback "+id);
    }
    public static void tick(ItemStack wand,Player player) { var callback=UPDATES.get(((tc4revival.content.CastingWandItem)wand.getItem()).rod(wand).tag);if(callback!=null)callback.tick(wand,player); }
}
