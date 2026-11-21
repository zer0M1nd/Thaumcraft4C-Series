package thaumicexploration;

import java.util.List;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.*;

/** The original hotbar food reservoir; all mutations are server authoritative. */
public class NourishmentTalisman extends Item {
    public NourishmentTalisman() { super(new Properties().stacksTo(1)); }
    @Override public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof Player player) || player.tickCount % 20 != 0) return;
        var tag = stack.getOrCreateTag();
        float food = Math.max(0, Math.min(100, tag.getFloat("food")));
        float saturation = Math.max(0, Math.min(100, tag.getFloat("saturation")));
        // Legacy scans slots 0 through 9, including the first slot above the hotbar.
        for (int i = 0; i < 10; i++) {
            var candidate = player.getInventory().getItem(i);
            if (candidate.isEmpty() || candidate == stack || candidate.is(ExplorationMod.core("zombie_brain"))) continue;
            var nutrition = candidate.getFoodProperties(player);
            if (nutrition == null || nutrition.getNutrition() <= 0 || !nutrition.getEffects().isEmpty() || candidate.getItem() instanceof SuspiciousStewItem || candidate.getItem() instanceof ChorusFruitItem
                    || net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(candidate.getItem()).getPath().contains("mana_bean")) continue;
            if (food + nutrition.getNutrition() >= 100) continue;
            food += nutrition.getNutrition();
            // Keep the old reservoir's 2 * saturation modifier (not vanilla nutrition * modifier).
            saturation = Math.min(100, saturation + 2 * nutrition.getSaturationModifier());
            var remainder=candidate.hasCraftingRemainingItem()?candidate.getCraftingRemainingItem():candidate.getItem() instanceof BowlFoodItem?new ItemStack(Items.BOWL):candidate.is(Items.HONEY_BOTTLE)?new ItemStack(Items.GLASS_BOTTLE):ItemStack.EMPTY;
            candidate.shrink(1);
            if(!remainder.isEmpty()){if(candidate.isEmpty())player.getInventory().setItem(i,remainder);else if(!player.addItem(remainder))player.drop(remainder,false);}
            level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, .5F, 1F);
        }
        var stats = player.getFoodData();
        int fed = Math.min(20 - stats.getFoodLevel(), (int) food);
        if (fed > 0) { stats.setFoodLevel(stats.getFoodLevel() + fed); food -= fed; }
        float supplied = Math.min(Math.max(0, stats.getFoodLevel() - stats.getSaturationLevel()), saturation);
        stats.setSaturation(stats.getSaturationLevel() + supplied);
        tag.putFloat("food", food); tag.putFloat("saturation", saturation - supplied);
    }
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flags) {
        var tag = stack.getTag();
        lines.add(Component.translatable("tooltip.thaumicexploration.nourishment", tag == null ? 0 : (int)tag.getFloat("food"), tag == null ? 0 : (int)tag.getFloat("saturation")));
    }
}
