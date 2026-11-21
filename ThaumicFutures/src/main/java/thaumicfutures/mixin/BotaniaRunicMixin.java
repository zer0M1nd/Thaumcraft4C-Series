package thaumicfutures.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import tc4revival.content.RunicGear;

/** Legacy baubles support runic augmentation without any free initial shield. */
@Pseudo
@Mixin(targets = "vazkii.botania.common.item.equipment.bauble.BaubleItem", remap = false)
public abstract class BotaniaRunicMixin implements RunicGear {
    @Override public boolean runicEnabled(net.minecraft.world.item.ItemStack stack) {
        return thaumicfutures.FutureFeatures.Group.BOTANIA.enabled();
    }
}
