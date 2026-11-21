package thmodern.client;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import tc4revival.client.GolemModel;
import tc4revival.content.GolemEntity;

/** Retains native animation and equipment bones, with separate block textures on all six faces. */
public final class BlockGolemModel extends HierarchicalModel<GolemEntity> {
    public final GolemModel animation=new GolemModel();
    private GolemEntity entity;
    @Override public ModelPart root(){return animation.root();}
    @Override public void prepareMobModel(GolemEntity entity,float walk,float amount,float partial){this.entity=entity;animation.prepareMobModel(entity,walk,amount,partial);}
    @Override public void setupAnim(GolemEntity entity,float walk,float amount,float age,float yaw,float pitch){animation.setupAnim(entity,walk,amount,age,yaw,pitch);}
    @Override public void renderToBuffer(PoseStack pose,VertexConsumer out,int light,int overlay,float red,float green,float blue,float alpha){
        var state=entity.configuration().blockAppearance();
        if(state==null){animation.renderToBuffer(pose,out,light,overlay,red,green,blue,alpha);return;}
        pose.pushPose();pose.scale(.4f,.4f,.4f);
        box(pose,out,"head",-4,-11,-5.5f,8,9,8,light,overlay,red,green,blue,alpha);
        box(pose,out,"body",-8,-2,-6,16,12,11,light,overlay,red,green,blue,alpha);
        box(pose,out,"body",-5,9.5f,-3.5f,10,6,7,light,overlay,red,green,blue,alpha);
        box(pose,out,"right_arm",-12,-2.5f,-3,4,25,6,light,overlay,red,green,blue,alpha);
        box(pose,out,"left_arm",8,-2.5f,-3,4,25,6,light,overlay,red,green,blue,alpha);
        box(pose,out,"right_leg",-3.5f,-3,-3,6,16,5,light,overlay,red,green,blue,alpha);
        box(pose,out,"left_leg",-3.5f,-3,-3,6,16,5,light,overlay,red,green,blue,alpha);
        pose.popPose();
    }
    private void box(PoseStack pose,VertexConsumer out,String bone,float x,float y,float z,float w,float h,float d,int light,int overlay,float r,float g,float b,float a){
        pose.pushPose();root().getChild(bone).translateAndRotate(pose);
        float x1=x+w,y1=y+h,z1=z+d;
        face(pose,out,Direction.UP,new float[][]{{x,y,z},{x1,y,z},{x1,y,z1},{x,y,z1}},light,overlay,r,g,b,a);
        face(pose,out,Direction.DOWN,new float[][]{{x,y1,z1},{x1,y1,z1},{x1,y1,z},{x,y1,z}},light,overlay,r,g,b,a);
        face(pose,out,Direction.NORTH,new float[][]{{x1,y,z},{x,y,z},{x,y1,z},{x1,y1,z}},light,overlay,r,g,b,a);
        face(pose,out,Direction.SOUTH,new float[][]{{x,y,z1},{x1,y,z1},{x1,y1,z1},{x,y1,z1}},light,overlay,r,g,b,a);
        face(pose,out,Direction.WEST,new float[][]{{x,y,z},{x,y,z1},{x,y1,z1},{x,y1,z}},light,overlay,r,g,b,a);
        face(pose,out,Direction.EAST,new float[][]{{x1,y,z1},{x1,y,z},{x1,y1,z},{x1,y1,z1}},light,overlay,r,g,b,a);
        pose.popPose();
    }
    private void face(PoseStack pose,VertexConsumer out,Direction side,float[][] points,int light,int overlay,float red,float green,float blue,float alpha){
        var mc=Minecraft.getInstance();var state=entity.configuration().blockAppearance();var model=mc.getBlockRenderer().getBlockModel(state);
        var data=net.minecraftforge.client.model.data.ModelData.EMPTY;
        var quads=model.getQuads(state,side,RandomSource.create(42),data,null);
        if(quads.isEmpty())quads=model.getQuads(state,null,RandomSource.create(42),data,null).stream().filter(q->q.getDirection()==side).toList();
        // Layered grass and resource-pack overlays retain their own tint indices.
        if(quads.isEmpty())draw(pose,out,points,side,model.getParticleIcon(data),light,overlay,red,green,blue,alpha);
        else for(var quad:quads){
            int tint=quad.isTinted()?mc.getBlockColors().getColor(state,entity.level(),entity.blockPosition(),quad.getTintIndex()):-1;
            draw(pose,out,points,side,quad.getSprite(),light,overlay,red*(tint==-1?1:((tint>>16)&255)/255f),green*(tint==-1?1:((tint>>8)&255)/255f),blue*(tint==-1?1:(tint&255)/255f),alpha);
        }
    }
    private void draw(PoseStack pose,VertexConsumer out,float[][] points,Direction side,net.minecraft.client.renderer.texture.TextureAtlasSprite sprite,int light,int overlay,float r,float g,float b,float a){
        float[][] uv={{0,0},{16,0},{16,16},{0,16}};
        for(int i=0;i<4;i++)out.vertex(pose.last().pose(),points[i][0]/16,points[i][1]/16,points[i][2]/16).color(r,g,b,a).uv(sprite.getU(uv[i][0]),sprite.getV(uv[i][1])).overlayCoords(overlay).uv2(light).normal(pose.last().normal(),side.getStepX(),-side.getStepY(),side.getStepZ()).endVertex();
    }
}
