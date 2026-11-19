package conciliummodern;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;

/** The original biped mesh and skin, with its local-space liquid vertex deformation. */
public class QuicksilverRenderer extends HumanoidMobRenderer<QuicksilverElemental,HumanoidModel<QuicksilverElemental>> {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumicconcilium:textures/entity/quicksilver_elemental.png");
    public QuicksilverRenderer(EntityRendererProvider.Context context){super(context,new LiquidModel(),.5f);}
    @Override public ResourceLocation getTextureLocation(QuicksilverElemental entity){return TEXTURE;}
    @Override protected void scale(QuicksilverElemental entity,PoseStack pose,float partial){float scale=Math.max(.01f,entity.getHealth()/30f);pose.scale(scale,scale,scale);shadowRadius=scale;}
    @Override public void render(QuicksilverElemental entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
        ((LiquidModel)model).time=ConciliumShaders.time();
        super.render(entity,yaw,partial,pose,buffers,light);
    }
    private static class LiquidModel extends HumanoidModel<QuicksilverElemental>{
        float time;
        LiquidModel(){super(LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE,0),64,32).bakeRoot());}
        @Override public void renderToBuffer(PoseStack pose,VertexConsumer out,int light,int overlay,float r,float g,float b,float alpha){
            super.renderToBuffer(pose,ConciliumShaders.enabled()?new LiquidVertex(out,time,pose.last().pose()):out,light,overlay,r,g,b,alpha);
        }
    }
    private static class LiquidVertex implements VertexConsumer {
        private final VertexConsumer out;private final float time;
        private final org.joml.Matrix4f matrix,inverse;
        LiquidVertex(VertexConsumer out,float time,org.joml.Matrix4f matrix){this.out=out;this.time=time;this.matrix=new org.joml.Matrix4f(matrix);inverse=new org.joml.Matrix4f(matrix).invert();}
        @Override public VertexConsumer vertex(double x,double y,double z){
            // ModelPart submits already transformed vertices. Undo the entity transform so
            // the liquid motion remains attached to its body as the camera/player moves.
            var p=inverse.transformPosition(new org.joml.Vector3f((float)x,(float)y,(float)z));
            p.add((float)Math.cos(time*6+p.x*10)*.02f,(float)Math.cos(time*6+p.y*10)*.02f,(float)Math.cos(time*7+p.z*10)*.02f);
            matrix.transformPosition(p);out.vertex(p.x,p.y,p.z);return this;
        }
        @Override public VertexConsumer color(int r,int g,int b,int a){out.color(r,g,b,a);return this;}
        @Override public VertexConsumer uv(float u,float v){out.uv(u,v);return this;}
        @Override public VertexConsumer overlayCoords(int u,int v){out.overlayCoords(u,v);return this;}
        @Override public VertexConsumer uv2(int u,int v){out.uv2(u,v);return this;}
        @Override public VertexConsumer normal(float x,float y,float z){out.normal(x,y,z);return this;}
        @Override public void endVertex(){out.endVertex();}
        @Override public void defaultColor(int r,int g,int b,int a){out.defaultColor(r,g,b,a);}
        @Override public void unsetDefaultColor(){out.unsetDefaultColor();}
    }
}
