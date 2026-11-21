package thaumicomniverse.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.render.BlockEntityRenderHelper;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumicomniverse.compat.create.MovingEssentiaRendering;

@Mixin(value=BlockEntityRenderHelper.class,remap=false)
public abstract class CreateEssentiaShellMixin {
    @org.spongepowered.asm.mixin.injection.Group(name="omniverse$shell",min=1,max=1)
    @Inject(require=0,method="renderBlockEntities(Lnet/minecraft/world/level/Level;Lcom/simibubi/create/foundation/virtualWorld/VirtualRenderWorld;Ljava/lang/Iterable;Lcom/mojang/blaze3d/vertex/PoseStack;Lorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;F)V",at=@At("TAIL"))
    private static void omniverse$renderVesselShells(Level realLevel,VirtualRenderWorld renderLevel,Iterable<BlockEntity> tiles,PoseStack pose,Matrix4f lightTransform,MultiBufferSource buffers,float partial,CallbackInfo ci) {
        omniverse$render(realLevel,renderLevel,tiles,pose,lightTransform,buffers);
    }
    @org.spongepowered.asm.mixin.injection.Group(name="omniverse$shell",min=1,max=1)
    @Inject(require=0,method="renderBlockEntities(Ljava/util/List;Ljava/util/BitSet;Ljava/util/BitSet;Lcom/simibubi/create/foundation/virtualWorld/VirtualRenderWorld;Lnet/minecraft/world/level/Level;Lcom/mojang/blaze3d/vertex/PoseStack;Lorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;F)V",at=@At("TAIL"))
    private static void omniverse$renderModern(java.util.List<BlockEntity> tiles,java.util.BitSet visible,java.util.BitSet errors,VirtualRenderWorld renderLevel,Level realLevel,PoseStack pose,Matrix4f lightTransform,MultiBufferSource buffers,float partial,CallbackInfo ci) {
        omniverse$render(realLevel,renderLevel,tiles,pose,lightTransform,buffers);
    }
    @org.spongepowered.asm.mixin.Unique
    private static void omniverse$render(Level realLevel,VirtualRenderWorld renderLevel,Iterable<BlockEntity> tiles,PoseStack pose,Matrix4f lightTransform,MultiBufferSource buffers) {
        if(renderLevel==null||!VisualizationManager.supportsVisualization(realLevel))return;
        for(var tile:tiles) {
            if(!MovingEssentiaRendering.inlineBody(tile))continue;
            var pos=tile.getBlockPos();
            var lightPos=pos;
            if(lightTransform!=null) {
                var transformed=new Vector4f(pos.getX()+.5f,pos.getY()+.5f,pos.getZ()+.5f,1).mul(lightTransform);
                lightPos=BlockPos.containing(transformed.x,transformed.y,transformed.z);
            }
            pose.pushPose();
            pose.translate(pos.getX(),pos.getY(),pos.getZ());
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(tile.getBlockState(),pose,buffers,LevelRenderer.getLightColor(realLevel,lightPos),OverlayTexture.NO_OVERLAY,tile.getModelData(),null);
            pose.popPose();
        }
    }
}
