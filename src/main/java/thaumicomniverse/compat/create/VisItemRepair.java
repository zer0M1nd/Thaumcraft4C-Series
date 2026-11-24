package thaumicomniverse.compat.create;

import java.util.function.ToIntBiFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import tc4revival.aspect.Aspect;
import tc4revival.content.DeviceVisRepair;

public final class VisItemRepair {
    public static boolean needsRepair(ItemStack stack) { return DeviceVisRepair.needsRepair(stack); }
    public static boolean repair(ItemStack stack, Level level, Player player, CompoundTag buffer, ToIntBiFunction<Aspect, Integer> source) {
        return DeviceVisRepair.repair(stack, level, player, buffer, source);
    }
    private VisItemRepair() {}
}
