package thaumicfutures.autocaster.client;

import java.io.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import tc4revival.client.GroupedObjMesh;
import tc4revival.content.WandFocusItem;
import thaumicfutures.autocaster.AutocasterEntity;

/** Unmodified TC5 OBJ groups and UVs, with the original independent base/head rotations. */
public class AutocasterRenderer extends EntityRenderer<AutocasterEntity> {
    private static final ResourceLocation MODEL=ResourceLocation.parse("thaumicfutures:models/autocaster.obj");
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumicfutures:textures/entity/autocaster.png");
    private static GroupedObjMesh mesh;
    public AutocasterRenderer(EntityRendererProvider.Context context) { super(context);shadowRadius=.5f; }
    public static void reload() { mesh=null; }
    @Override public ResourceLocation getTextureLocation(AutocasterEntity turret) { return TEXTURE; }
    @Override public void render(AutocasterEntity turret,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light) {
        if(mesh==null)try(var input=Minecraft.getInstance().getResourceManager().getResourceOrThrow(MODEL).open()) {
            mesh=GroupedObjMesh.read(new InputStreamReader(input,java.nio.charset.StandardCharsets.UTF_8));
        } catch(IOException error) { throw new IllegalStateException("Cannot load original TC5 autocaster model",error); }
        int overlay=OverlayTexture.pack(0,turret.hurtTime>0 || turret.deathTime>0);
        pose.pushPose();pose.translate(0,.5,0);
        switch(turret.facing()) {
            case DOWN->pose.mulPose(Axis.XP.rotationDegrees(180));
            case NORTH->pose.mulPose(Axis.XP.rotationDegrees(-90));
            case SOUTH->pose.mulPose(Axis.XP.rotationDegrees(90));
            case WEST->pose.mulPose(Axis.ZP.rotationDegrees(90));
            case EAST->pose.mulPose(Axis.ZP.rotationDegrees(-90));
            default->{}
        }
        pose.translate(0,-.5,0);draw("base",pose,buffers,light,overlay,0xffffff,1);pose.popPose();
        pose.pushPose();pose.translate(0,.5,0);
        if(turret.hurtTime>0)pose.translate(Math.sin((turret.tickCount+partial)*7)*turret.hurtTime/500,0,0);
        pose.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partial,turret.yHeadRotO,turret.yHeadRot)));
        pose.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partial,turret.xRotO,turret.getXRot())));
        draw("ball",pose,buffers,light,overlay,0xffffff,1);
        var focus=turret.focus();if(focus.getItem() instanceof WandFocusItem item)draw("focus",pose,buffers,light,overlay,item.color(focus),.9f);
        pose.popPose();super.render(turret,yaw,partial,pose,buffers,light);
    }
    private static void draw(String group,PoseStack pose,MultiBufferSource buffers,int light,int overlay,int color,float alpha) {
        var vertices=mesh.groups().get(group);if(vertices==null)return;
        var out=buffers.getBuffer(RenderType.entityTranslucent(TEXTURE));var frame=pose.last();
        for(var v:vertices)out.vertex(frame.pose(),v.x(),v.y(),v.z()).color((color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,alpha)
                .uv(v.u(),v.v()).overlayCoords(overlay).uv2(light).normal(frame.normal(),v.nx(),v.ny(),v.nz()).endVertex();
    }
}
