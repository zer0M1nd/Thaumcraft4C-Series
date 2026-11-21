package thaumicomniverse.mixin;
import com.simibubi.create.content.equipment.extendoGrip.ExtendoGripRenderHandler;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import thaumicomniverse.compat.create.MagicHand;
@Mixin(value=ExtendoGripRenderHandler.class,remap=false)
public abstract class CreateMagicHandClientMixin {
    @Redirect(method="*",at=@At(value="INVOKE",target="Lcom/tterrag/registrate/util/entry/ItemEntry;isIn(Lnet/minecraft/world/item/ItemStack;)Z"))
    private static boolean omniverse$hand(ItemEntry<?> item,ItemStack stack){return item.isIn(stack)||MagicHand.matches(stack);}
}
