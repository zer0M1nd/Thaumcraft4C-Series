package thaumicfutures.mixin;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = {
    "vazkii.botania.common.item.equipment.armor.manasteel.ManasteelArmorItem",
    "vazkii.botania.common.item.equipment.armor.terrasteel.TerrasteelArmorItem",
    "vazkii.botania.common.item.equipment.armor.elementium.ElementiumArmorItem"
}, remap = false)
public abstract class BotaniaArmorSetMixin {
    @Inject(method = "hasArmorSetItem", at = @At("HEAD"), cancellable = true, remap = false)
    private void omniverse$helmetSet(Player player, EquipmentSlot slot, CallbackInfoReturnable<Boolean> cir) {
        if (!thaumicfutures.FutureFeatures.Group.BOTANIA.enabled()) return;
        if (player == null || slot != EquipmentSlot.HEAD) return;
        var stack = player.getItemBySlot(slot);
        var key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (key != null && key.getNamespace().equals("thaumicfutures")
                && stack.getItem() instanceof thaumcraft.api.IGoggles && stack.getItem() instanceof ArmorItem helmet
                && helmet.getMaterial() == ((ArmorItem) (Object) this).getMaterial()) cir.setReturnValue(true);
    }
}
