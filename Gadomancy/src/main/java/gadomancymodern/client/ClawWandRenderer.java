package gadomancymodern.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import tc4revival.content.*;

/** Gadomancy's ModelWandPart(7,4): deliberately no lower cap, 7px rod and only four rune rows. */
public class ClawWandRenderer {
    protected final ModelPart rod,cap,focus;
    public ClawWandRenderer(){var mesh=new MeshDefinition();var root=mesh.getRoot();root.addOrReplaceChild("rod",CubeListBuilder.create().texOffs(0,8).addBox(-1,-1,-1,2,7,2),PartPose.offset(0,2,0));root.addOrReplaceChild("cap",CubeListBuilder.create().texOffs(0,0).addBox(-1,-1,-1,2,2,2),PartPose.ZERO);root.addOrReplaceChild("focus",CubeListBuilder.create().texOffs(0,0).addBox(-3,-6,-3,6,6,6),PartPose.ZERO);var baked=LayerDefinition.create(mesh,32,32).bakeRoot();rod=baked.getChild("rod");cap=baked.getChild("cap");focus=baked.getChild("focus");}
    public void render(ItemStack stack,float ticks,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var wand=(CastingWandItem)stack.getItem();boolean staff=wand.isStaff(stack);var part=wand.rod(stack);pose.pushPose();if(staff)pose.translate(0,.2,0);
        pose.pushPose();if(staff){pose.translate(0,-.1,0);pose.scale(1.2f,2,1.2f);}rod.render(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(ResourceLocation.parse(part.texture()))),part.glowing()?205:light,overlay);pose.popPose();
        pose.pushPose();pose.scale(staff?1.3f:1.2f,staff?1.1f:1,staff?1.3f:1.2f);var out=buffers.getBuffer(RenderType.entityCutoutNoCull(ResourceLocation.parse(wand.cap(stack).texture())));cap.render(pose,out,light,overlay);if(staff){pose.translate(0,.225,0);pose.scale(1,.66f,1);cap.render(pose,out,light,overlay);}pose.popPose();
        pose.pushPose();pose.translate(0,staff?-.15:-.09,0);pose.scale(staff?.165f:.16f,staff?.1765f:.16f,staff?.165f:.16f);cube(pose,buffers,light,overlay);pose.popPose();
        pose.pushPose();if(staff)pose.translate(0,-.0475,0);pose.scale(staff?.525f:.5f,staff?.5525f:.5f,staff?.525f:.5f);int color=10854849;var now=java.time.LocalDate.now();var texture=ResourceLocation.parse("thaumcraft:textures/models/"+(now.getMonthValue()==10&&now.getDayOfMonth()==31?"spec_h":"wand")+".png");focus.render(pose,buffers.getBuffer(RenderType.entityTranslucent(texture)),(int)(205+Math.sin(ticks/3)*10),overlay,(color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,.6f);pose.popPose();
        if(part.runes())for(int side=0;side<4;side++){pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees(90*(side+1)));for(int i=0;i<4;i++)rune(pose,buffers,.36+i*.14,-.01,-.08,(i+side*3)%16,ticks);pose.popPose();}pose.popPose();
    }
    protected void cube(PoseStack pose,MultiBufferSource buffers,int light,int overlay){var out=buffers.getBuffer(RenderType.entityCutoutNoCull(ResourceLocation.parse("thaumcraft:textures/item/focus_primal_depth.png")));var frame=pose.last();for(var dir:net.minecraft.core.Direction.values()){var n=net.minecraft.world.phys.Vec3.atLowerCornerOf(dir.getNormal());var up=dir.getAxis()==net.minecraft.core.Direction.Axis.Y?new net.minecraft.world.phys.Vec3(0,0,1):new net.minecraft.world.phys.Vec3(0,1,0);var right=up.cross(n);for(int i=0;i<4;i++){int x=i<2?-1:1,y=i==0||i==3?-1:1;var v=n.scale(.5).add(right.scale(x*.5)).add(up.scale(y*.5));out.vertex(frame.pose(),(float)v.x,(float)v.y,(float)v.z).color(1f,1f,1f,1f).uv((x+1)/2f,(1-y)/2f).overlayCoords(overlay).uv2(light).normal(frame.normal(),dir.getStepX(),dir.getStepY(),dir.getStepZ()).endVertex();}}}
    protected void rune(PoseStack pose,MultiBufferSource buffers,double x,double y,double z,int index,float ticks){float r=(float)Math.sin((ticks+index*5)/5)*.1f+.88f,g=(float)Math.sin((ticks+index*5)/7)*.1f+.63f,pulse=(float)Math.sin((ticks+index*5)/10)*.2f,size=.06f+pulse/40;pose.pushPose();pose.mulPose(Axis.ZP.rotationDegrees(90));pose.translate(x,y,z);var matrix=pose.last().pose();var out=buffers.getBuffer(tc4revival.client.TCRenderTypes.runes(ResourceLocation.parse("thaumcraft:textures/misc/script.png")));float low=index/16f,high=low+1/16f;out.vertex(matrix,-size,size,0).color(r,g,.2f,pulse+.6f).uv(high,1).uv2(200).endVertex();out.vertex(matrix,size,size,0).color(r,g,.2f,pulse+.6f).uv(high,0).uv2(200).endVertex();out.vertex(matrix,size,-size,0).color(r,g,.2f,pulse+.6f).uv(low,0).uv2(200).endVertex();out.vertex(matrix,-size,-size,0).color(r,g,.2f,pulse+.6f).uv(low,1).uv2(200).endVertex();pose.popPose();}
}
