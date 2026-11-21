package tc4revival.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import tc4revival.content.GolemEntity;

/** Core extrusion and upgrade squares retain the original chest coordinates. */
public  class GolemBadges {
    private static final String[] UPGRADES={"air","earth","fire","water","order","entropy"};
    public static void render(GolemEntity entity,PoseStack pose,MultiBufferSource buffers,int light) {
        var config=entity.configuration();
        if (config.core()!=null) {
            pose.pushPose(); pose.mulPose(Axis.XP.rotationDegrees(180)); pose.translate(.0875,-.96,.15+(config.decorated('P')?.03:0));
            pose.scale(.175f,.175f,.175f); pose.mulPose(Axis.YP.rotationDegrees(180));
            var texture=ResourceLocation.parse("thaumcraft:item/golem_core_"+config.core().name().toLowerCase(java.util.Locale.ROOT));
            plate(pose,animatedItem(buffers,texture),light,.2f,16);
            pose.popPose();
        }
        var upgrades=config.upgrades();
        for (int index=0;index<upgrades.length;index++) {
            pose.pushPose(); pose.mulPose(Axis.XP.rotationDegrees(180));
            pose.translate(-.05-.08*(upgrades.length-1)/2+.08*index,-1.106,.099); pose.scale(.1f,.1f,.1f);
            int visible=config.displayedUpgrade(index);
            var texture=ResourceLocation.parse("thaumcraft:item/golem_upgrade_"+(visible<0?"empty":UPGRADES[visible]));
            var out=animatedItem(buffers,texture);
            vertex(out,pose,0,0,0,1,1,light,0,0,1); vertex(out,pose,1,0,0,0,1,light,0,0,1);
            vertex(out,pose,1,1,0,0,0,light,0,0,1); vertex(out,pose,0,1,0,1,0,light,0,0,1);
            pose.popPose();
        }
    }
    private static VertexConsumer animatedItem(MultiBufferSource buffers,ResourceLocation texture) {
        var atlas=net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS;
        var sprite=net.minecraft.client.Minecraft.getInstance().getTextureAtlas(atlas).apply(texture);
        // Atlas animation uploads one frame; sampling the source PNG displays its whole strip.
        return sprite.wrap(buffers.getBuffer(RenderType.entityCutoutNoCull(atlas)));
    }
    static void plate(PoseStack pose,VertexConsumer out,int light,float depth,int pixels) {
        quad(out,pose,light,0,0,0,1,1,0,1,1,0,0,0,0,1);
        quad(out,pose,light,0,0,-depth,1,1,-depth,1,1,0,0,0,0,-1);
        for (int index=0;index<pixels;index++) {
            float low=index/(float)pixels,high=(index+1f)/pixels,uv=(index+.5f)/pixels;
            quad(out,pose,light,low,0,-depth,low,1,0,1-uv,1,1-uv,0,-1,0,0);
            quad(out,pose,light,high,0,-depth,high,1,0,1-uv,1,1-uv,0,1,0,0);
            quad(out,pose,light,0,low,-depth,1,low,0,1,1-uv,0,1-uv,0,-1,0);
            quad(out,pose,light,0,high,-depth,1,high,0,1,1-uv,0,1-uv,0,1,0);
        }
    }
    private static void quad(VertexConsumer out,PoseStack pose,int light,float x0,float y0,float z0,float x1,float y1,float z1,float u0,float v0,float u1,float v1,float nx,float ny,float nz) {
        vertex(out,pose,x0,y0,z0,u0,v0,light,nx,ny,nz);
        if (nx!=0) {
            vertex(out,pose,x0,y1,z0,u0,v1,light,nx,ny,nz); vertex(out,pose,x1,y1,z1,u1,v1,light,nx,ny,nz); vertex(out,pose,x1,y0,z1,u1,v0,light,nx,ny,nz);
        } else {
            vertex(out,pose,x1,y0,z0,u1,v0,light,nx,ny,nz); vertex(out,pose,x1,y1,z1,u1,v1,light,nx,ny,nz); vertex(out,pose,x0,y1,z1,u0,v1,light,nx,ny,nz);
        }
    }
    private static void vertex(VertexConsumer out,PoseStack pose,float x,float y,float z,float u,float v,int light,float nx,float ny,float nz) {
        out.vertex(pose.last().pose(),x,y,z).color(255,255,255,255).uv(u,v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.last().normal(),nx,ny,nz).endVertex();
    }
    private GolemBadges() {}
}
