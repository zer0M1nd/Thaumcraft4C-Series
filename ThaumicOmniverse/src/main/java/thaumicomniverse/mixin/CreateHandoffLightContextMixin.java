package thaumicomniverse.mixin;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.render.ContraptionEntityRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumicomniverse.compat.create.ContraptionHandoffLight;

@Mixin(value=ContraptionEntityRenderer.class, remap=false)
public abstract class CreateHandoffLightContextMixin {
    @Inject(method="render(Lcom/simibubi/create/content/contraptions/AbstractContraptionEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at=@At("HEAD"))
    private void omniverse$begin(AbstractContraptionEntity entity, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo ci) {
        ContraptionHandoffLight.begin(entity);
    }
    @Inject(method="render(Lcom/simibubi/create/content/contraptions/AbstractContraptionEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at=@At("RETURN"))
    private void omniverse$end(AbstractContraptionEntity entity, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo ci) {
        ContraptionHandoffLight.end();
    }
}
