package tc4revival.content;

import java.util.function.ToIntBiFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.TCAspects;
import tc4revival.aspect.item.ItemAspects;

/** Partial cV payments stay in the device when its inventory changes. */
public class DeviceVisRepair {
    private DeviceVisRepair() {}

    private static AspectAmounts costs(ItemStack stack, int tier) {
        var key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        var base = ItemAspects.baseSession().base(key.toString()).orElse(null);
        return base == null ? new AspectAmounts() : VisRepair.costs(TCAspects.catalog(), base, tier, stack.getDamageValue());
    }

    public static boolean needsRepair(ItemStack stack) {
        int tier = stack.getEnchantmentLevel(TCEnchantments.REPAIR.get());
        return tier > 0 && stack.isDamaged() && !costs(stack, tier).isEmpty();
    }

    public static boolean repair(ItemStack stack, Level level, Player player, CompoundTag credit,
                                 ToIntBiFunction<Aspect, Integer> source) {
        if (!(level instanceof ServerLevel server) || level.getGameTime() % 40 != 0 || !stack.isDamaged()) return false;
        int tier = stack.getEnchantmentLevel(TCEnchantments.REPAIR.get());
        if (tier <= 0) return false;
        var costs = costs(stack, tier).view();
        if (costs.isEmpty()) return false;
        boolean paid = true;
        for (var cost : costs.entrySet()) {
            String key = cost.getKey().tag();
            int stored = Math.max(0, credit.getInt(key));
            if (stored < cost.getValue()) {
                stored += source.applyAsInt(cost.getKey(), cost.getValue() - stored);
                credit.putInt(key, stored);
            }
            paid &= stored >= cost.getValue();
        }
        if (!paid || stack.getItem() instanceof RepairableItem item &&
                !item.beforeVisRepair(stack, player == null ? FakePlayerFactory.getMinecraft(server) : player, tier)) return false;
        costs.forEach((aspect, cost) -> credit.putInt(aspect.tag(), credit.getInt(aspect.tag()) - cost));
        stack.setDamageValue(Math.max(0, stack.getDamageValue() - tier));
        return true;
    }
}
