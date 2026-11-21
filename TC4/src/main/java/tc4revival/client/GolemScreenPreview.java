package tc4revival.client;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import tc4revival.content.GolemEntity;

/** The original golem window used its own perspective camera, independent of the inventory GUI. */
 class GolemScreenPreview {
    static void render(GuiGraphics graphics,GolemEntity golem) {
        var minecraft=Minecraft.getInstance(); var window=minecraft.getWindow();
        graphics.flush();
        var projection=new Matrix4f(RenderSystem.getProjectionMatrix());
        var sorting=RenderSystem.getVertexSorting();
        var modelView=RenderSystem.getModelViewStack();
        var dispatcher=minecraft.getEntityRenderDispatcher();
        var camera=new Quaternionf(dispatcher.cameraOrientation());
        float body=golem.yBodyRot,yaw=golem.getYRot(),pitch=golem.getXRot(),head=golem.yHeadRot,previousHead=golem.yHeadRotO;
        double scale=window.getGuiScale();
        modelView.pushPose();
        try {
            RenderSystem.viewport((int)(((window.getGuiScaledWidth()-320)/2)*scale),
                    (int)(((window.getGuiScaledHeight()-240)/2)*scale),(int)(320*scale),(int)(240*scale));
            RenderSystem.setProjectionMatrix(new Matrix4f().translation(-.34f,.23f,0)
                    .perspective((float)Math.PI/2,1.3333334f,9,80),VertexSorting.DISTANCE_TO_ORIGIN);
            modelView.setIdentity(); RenderSystem.applyModelViewMatrix();
            var pose=new PoseStack(); pose.translate(-1.5,-1,-12); pose.scale(5,5,5);
            Lighting.setupForEntityInInventory();
            dispatcher.overrideCameraOrientation(new Quaternionf());
            golem.yBodyRot=-20; golem.setYRot(0); golem.setXRot(0); golem.yHeadRotO=-5; golem.yHeadRot=-5;
            var buffers=minecraft.renderBuffers().bufferSource();
            dispatcher.getRenderer(golem).render(golem,0,1,pose,buffers,0xF000F0);
            buffers.endBatch();
        } finally {
            golem.yBodyRot=body; golem.setYRot(yaw); golem.setXRot(pitch); golem.yHeadRot=head; golem.yHeadRotO=previousHead;
            dispatcher.overrideCameraOrientation(camera);
            modelView.popPose(); RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(projection,sorting);
            RenderSystem.viewport(0,0,window.getWidth(),window.getHeight());
            Lighting.setupFor3DItems();
        }
    }
    private GolemScreenPreview() {}
}
