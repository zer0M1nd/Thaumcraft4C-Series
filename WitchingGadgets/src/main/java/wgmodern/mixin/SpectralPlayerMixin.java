package wgmodern.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import wgmodern.SpectralBuffers;
@Mixin(value=LivingEntityRenderer.class,remap=false)
public abstract class SpectralPlayerMixin {
    @ModifyVariable(method={"render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V","m_7392_(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"},at=@At("HEAD"),argsOnly=true)
    private MultiBufferSource wg$ghost(MultiBufferSource source,LivingEntity entity,float yaw,float partial,PoseStack pose,MultiBufferSource incoming,int light){return SpectralBuffers.forEntity(source,entity);}
}
