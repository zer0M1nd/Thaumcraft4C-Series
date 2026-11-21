package thmodern.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.world.item.*;
import thmodern.blood.InjectorItem;

public final class InjectorRenderer extends BlockEntityWithoutLevelRenderer {
    private final ModelPart model=create().bakeRoot();
    public InjectorRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    private static void part(PartDefinition root,String name,int u,int v,float x,float y,float z,float w,float h,float d,float px,float py,float pz){root.addOrReplaceChild(name,CubeListBuilder.create().texOffs(u,v).mirror().addBox(x,y,z,w,h,d),PartPose.offset(px,py,pz));}
    private static LayerDefinition create(){
        var mesh=new MeshDefinition();var root=mesh.getRoot();
        part(root,"drum",0,0,-2.5f,-2.5f,0,5,5,1,2.5f,2.25f,0);part(root,"front",12,0,0,0,0,3,1,8,1,1,-8);
        part(root,"left",0,6,0,-1,0,4,1,1,2.5f,1,-8);part(root,"right",0,6,0,-1,-1,4,1,1,2.5f,1,-8);
        part(root,"left_tip",0,8,0,-1,0,3,1,1,6,1,-7.5f);part(root,"right_tip",0,8,0,-1,-1,3,1,1,-1,1,-7.5f);
        part(root,"slider",13,0,-.5f,0,.5f,1,1,1,2.5f,0,-6.5f);part(root,"stock",0,10,0,0,0,3,1,3,1,1,1);part(root,"grip",0,14,0,0,0,3,2,1,1,2,3);
        return LayerDefinition.create(mesh,64,32);
    }
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var player=Minecraft.getInstance().player;
        float power=player!=null&&player.isUsingItem()&&player.getUseItem()==stack?InjectorItem.power(player.getTicksUsingItem()):0;
        model.getChild("left").yRot=-power*(float)Math.PI/16;model.getChild("right").yRot=(float)Math.PI+power*(float)Math.PI/16;
        model.getChild("left_tip").yRot=-.3490659f-power*(float)Math.PI/4;model.getChild("right_tip").yRot=3.490659f+power*(float)Math.PI/4;
        model.getChild("drum").zRot=stack.hasTag()?stack.getTag().getInt("Rotation")*(float)Math.PI/90:0;
        model.getChild("slider").z=-6.5f+5*power;
        pose.pushPose();pose.translate(.5,.5,.5);
        if(context==ItemDisplayContext.GUI){pose.mulPose(Axis.XP.rotationDegrees(25));pose.mulPose(Axis.YP.rotationDegrees(45));}
        pose.scale(1.5f,-1.5f,-1.5f);pose.translate(-2.5/16,-2.0/16,2.0/16);
        model.render(pose,buffers.getBuffer(RenderType.entityCutout(net.minecraft.resources.ResourceLocation.parse("thaumichorizons:textures/models/injector.png"))),light,overlay);
        var left=model.getChild("left_tip");var right=model.getChild("right_tip");
        var a=new org.joml.Vector3f((right.x+.6f+power*.25f)*.5f,(right.y-.9375f)*.5f,(right.z+6.8125f+power*.125f)*.5f);
        var b=new org.joml.Vector3f(((right.x+left.x)/2-2.175f)*.5f,a.y,(right.z+6.8125f+power*.65f)*.5f);
        var c=new org.joml.Vector3f((left.x-4.95f-power*.25f)*.5f,a.y,a.z);
        line(pose,buffers,a,b);line(pose,buffers,b,c);pose.popPose();
    }
    private static void line(PoseStack pose,MultiBufferSource buffers,org.joml.Vector3f a,org.joml.Vector3f b){
        var normal=new org.joml.Vector3f(b).sub(a).normalize();var out=buffers.getBuffer(RenderType.lines());
        for(var point:new org.joml.Vector3f[]{a,b})out.vertex(pose.last().pose(),point.x,point.y,point.z).color(0,0,0,255).normal(pose.last().normal(),normal.x,normal.y,normal.z).endVertex();
    }
}
