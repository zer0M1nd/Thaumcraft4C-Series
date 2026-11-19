package tbmodern.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Optional upgrade behavior. Register on both sides during common setup; callbacks run in registration order. */
public interface RevolverBehavior {
    default float damageStart(LivingEntity target, Player shooter, ItemStack gun, int level, float damage) {return damage;}
    default float damageEnd(LivingEntity target, Player shooter, ItemStack gun, int level, float damage) {return damage;}
    /** Invoked server-side only after an accepted hit. Return false to let the projectile continue. */
    default boolean afterHit(LivingEntity target, Player shooter, ItemStack gun, int level, float damage) {return true;}
    default boolean passesBlocks(Player shooter, ItemStack gun, int level) {return false;}
    /** Positive values: shots per essentia; -1: two essentia for one shot, matching the primal upgrade. */
    default int shotsPerEssentia(ItemStack gun, int level, int shots) {return shots;}
    default double speedMultiplier(ItemStack gun, int level, double multiplier) {return multiplier;}
    /** Client rendering only. Part names: handle, cylinder, metal, barrel, trigger; null keeps the previous texture. */
    default ResourceLocation texture(ItemStack gun, int level, String part) {return null;}
}
