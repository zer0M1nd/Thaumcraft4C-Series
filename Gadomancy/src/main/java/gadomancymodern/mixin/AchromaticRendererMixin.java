package gadomancymodern.mixin;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=net.minecraft.client.renderer.entity.LivingEntityRenderer.class,remap=false)
public abstract class AchromaticRendererMixin {
    @ModifyVariable(method={"render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V","m_7392_(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"},at=@At("HEAD"),argsOnly=true,ordinal=0)
    private MultiBufferSource gadomancy$ghostLayers(MultiBufferSource original,LivingEntity entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){return entity instanceof net.minecraft.world.entity.player.Player&&entity.hasEffect(gadomancymodern.aura.GadEffects.ACHROMATIC.get())?gadomancymodern.client.AchromaticRendering.wrap(original):original;}
}
