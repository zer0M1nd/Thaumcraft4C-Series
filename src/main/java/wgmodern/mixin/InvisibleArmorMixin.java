package wgmodern.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.*;
import wgmodern.WGMagicClient;
@Mixin(value=HumanoidArmorLayer.class,remap=false)
public abstract class InvisibleArmorMixin {
    @Inject(method={"renderArmorPiece","m_117118_"},at=@At("HEAD"),cancellable=true)
    private void wg$hide(PoseStack pose,MultiBufferSource buffers,LivingEntity wearer,EquipmentSlot slot,int light,HumanoidModel<?> model,CallbackInfo ci){if(WGMagicClient.hidden(wearer.getItemBySlot(slot),wearer))ci.cancel();}
}
