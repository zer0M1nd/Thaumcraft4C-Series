package thaumicomniverse.compat.ponder;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;
import tc4revival.aspect.AspectAmounts;
import tc4revival.client.*;
import tc4revival.content.*;

/** Local virtual-world adapter: normal world renderers and resource ownership are preserved. */
public final class PonderNodeRenderers extends RenderType {
    private static final ResourceLocation NODES=ResourceLocation.parse("thaumcraft:textures/misc/nodes.png");
    private static final ResourceLocation RING=ResourceLocation.parse("thaumcraft:textures/items/lightningringv.png");
    private static final ResourceLocation PORTAL=ResourceLocation.parse("thaumcraft:textures/misc/eldritch_portal.png");
    private static final ResourceLocation BEAM=ResourceLocation.parse("thaumcraft:textures/misc/beam1.png");
    // The GUI transform uses screen-space distances. World entity shaders apply fog to those distances.
    private record Sprite(ResourceLocation texture,boolean translucent,boolean insideGlass) {}
    private static final java.util.function.Function<Sprite,RenderType> SPRITES=Util.memoize(sprite->create(
        "omniverse_ponder_node",DefaultVertexFormat.POSITION_COLOR_TEX,VertexFormat.Mode.QUADS,256,false,sprite.translucent(),
        CompositeState.builder().setShaderState(POSITION_COLOR_TEX_SHADER)
            .setTextureState(new TextureStateShard(sprite.texture(),true,false)).setCullState(NO_CULL)
            .setTransparencyState(sprite.translucent()?TRANSLUCENT_TRANSPARENCY:LIGHTNING_TRANSPARENCY)
            // Ponder's glass model has already written depth when its contents are drawn.
            .setDepthTestState(sprite.insideGlass()?NO_DEPTH_TEST:LEQUAL_DEPTH_TEST)
            .setWriteMaskState(sprite.insideGlass()?COLOR_WRITE:COLOR_DEPTH_WRITE).createCompositeState(false)));
    public static void draw(AspectAmounts aura,AuraNodeType type,boolean energized,PoseStack pose,MultiBufferSource buffers,float scale) {
        aura=thaumcraft.api.client.ClientAspectViews.project(aura,thaumcraft.api.aspects.AspectViews.Purpose.NODE);
        // A virtual scene has neither the player's viewing distance nor the real-world deferred render pass.
        // Remove the scene's rotation from the billboard while retaining its position and scale.
        boolean insideGlass=scale<1;
        pose.pushPose();pose.translate(.5,insideGlass?.4:.5,.5);
        var matrix=pose.last().pose();
        var origin=matrix.transformPosition(new Vector3f());
        var size=matrix.getScale(new Vector3f());
        matrix.identity().translation(origin).scale(size.x*scale,size.y*scale,size.z*scale);
        pose.last().normal().identity();
        long time=System.nanoTime()/40_000_000;int frame=(int)(time%32),index=0;
        for(var aspect:aura.aspects()) {
            float amount=NodeVisualSize.amount(aura.amount(aspect));
            float radius=.2f+(.5f+.15f*(float)Math.sin(time/(14d+index)))*amount/50f;
            pose.pushPose();pose.mulPose(Axis.ZP.rotationDegrees((time*(index+1))%360));
            quad(pose,buffers,NODES,frame/32f,0,(frame+1)/32f,1/32f,radius,
                aspect.color(),.55f/Math.max(1,aura.size()/2f),aspect.blendFactor()==771,insideGlass);pose.popPose();index++;
        }
        int row=switch(type){case NORMAL->1;case DARK->2;case HUNGRY->3;case PURE->4;case TAINTED->5;case UNSTABLE->6;};
        quad(pose,buffers,NODES,frame/32f,row/32f,(frame+1)/32f,(row+1)/32f,.4f,0xffffff,.65f,type==AuraNodeType.DARK||type==AuraNodeType.TAINTED,insideGlass);
        if(energized)quad(pose,buffers,RING,(time%16)/16f,0,(time%16+1)/16f,1,.34f,0xffffff,.8f,false,insideGlass);
        pose.popPose();
    }
    public static void drawPortal(EldritchPortalBlockEntity portal,float partial,PoseStack pose,MultiBufferSource buffers) {
        float width=Math.max(0,Math.min(5,portal.opening()+partial))/5f;
        float height=Math.max(0,Math.min(30,portal.opening()+partial))/30f;
        pose.pushPose();pose.translate(.5,.5,.5);
        var matrix=pose.last().pose();
        var origin=matrix.transformPosition(new Vector3f());
        var size=matrix.getScale(new Vector3f());
        matrix.identity().translation(origin).scale(size.x*width,size.y*height,size.z);
        pose.last().normal().identity();
        int frame=(int)((System.nanoTime()/50_000_000L)%16);
        quad(pose,buffers,PORTAL,frame/16f,0,(frame+1)/16f,1,1,0xffffff,1,true,false);
        pose.popPose();
    }
    public static void beam(net.minecraft.world.phys.Vec3 start,net.minecraft.world.phys.Vec3 end,PoseStack pose,MultiBufferSource buffers){
        var delta=end.subtract(start);if(delta.lengthSqr()<1e-8)return;
        var axis=delta.normalize();var side=axis.cross(Math.abs(axis.y)>.99?new net.minecraft.world.phys.Vec3(1,0,0):new net.minecraft.world.phys.Vec3(0,1,0)).normalize();
        var type=SPRITES.apply(new Sprite(BEAM,false,false));
        var out=buffers instanceof SuperRenderTypeBuffer ponder?ponder.getLateBuffer(type):buffers.getBuffer(type);
        float time=(System.nanoTime()/50_000_000L%1000)/10f;
        for(var ribbon:java.util.List.of(side,axis.cross(side))){
            var offset=ribbon.scale(.07);var points=new net.minecraft.world.phys.Vec3[]{start.subtract(offset),end.subtract(offset),end.add(offset),start.add(offset)};
            for(int i=0;i<4;i++){var v=points[i];out.vertex(pose.last().pose(),(float)v.x,(float)v.y,(float)v.z)
                .color(.7f,.8f,1f,.45f).uv(i<2?0:1,time+(i==1||i==2?(float)delta.length():0)).endVertex();}
        }
    }
    private static void quad(PoseStack pose,MultiBufferSource buffers,ResourceLocation texture,float u1,float v1,float u2,float v2,float radius,int rgb,float alpha,boolean translucent,boolean insideGlass) {
        var type=SPRITES.apply(new Sprite(texture,translucent,insideGlass));
        // World sections submit BEs before their solid geometry. Keep the sprite in the late phase,
        // with depth writes so a later buffer switch cannot let the platform paint over it.
        var out=buffers instanceof SuperRenderTypeBuffer ponder?ponder.getLateBuffer(type):buffers.getBuffer(type);
        var m=pose.last().pose();
        float r=(rgb>>16&255)/255f,g=(rgb>>8&255)/255f,b=(rgb&255)/255f;
        out.vertex(m,-radius,-radius,0).color(r,g,b,alpha).uv(u2,v2).endVertex();
        out.vertex(m,-radius,radius,0).color(r,g,b,alpha).uv(u2,v1).endVertex();
        out.vertex(m,radius,radius,0).color(r,g,b,alpha).uv(u1,v1).endVertex();
        out.vertex(m,radius,-radius,0).color(r,g,b,alpha).uv(u1,v2).endVertex();
    }
    private PonderNodeRenderers() {super("unused",DefaultVertexFormat.POSITION_COLOR_TEX,VertexFormat.Mode.QUADS,256,false,false,()->{},()->{});}
}
