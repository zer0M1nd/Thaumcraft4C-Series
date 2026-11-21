package tc4revival.client;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import tc4revival.content.TaintHiveEntity;

public  class TaintHiveRenderer extends MobRenderer<TaintHiveEntity,TaintHiveRenderer.Model> {
    public TaintHiveRenderer(EntityRendererProvider.Context context) { super(context,new Model(),.25f); }
    @Override public ResourceLocation getTextureLocation(TaintHiveEntity entity) { return ResourceLocation.parse("thaumcraft:textures/models/taint_spore.png"); }
    @Override protected float getFlipDegrees(TaintHiveEntity entity) { return 0; }
    static  class Model extends EntityModel<TaintHiveEntity> {
        private final ModelPart core,base;
        private float size,pulse;
        Model() {
            super(RenderType::entityTranslucent);var mesh=new MeshDefinition();
            mesh.getRoot().addOrReplaceChild("core",CubeListBuilder.create().texOffs(0,0).addBox(-8,0,-8,16,16,16),PartPose.ZERO);
            mesh.getRoot().addOrReplaceChild("base",CubeListBuilder.create().texOffs(0,32).addBox(-8,-8,-8,16,16,16),PartPose.offset(0,16,0));
            var root=LayerDefinition.create(mesh,64,64).bakeRoot();core=root.getChild("core");base=root.getChild("base");
        }
        @Override public void setupAnim(TaintHiveEntity hive,float swing,float amount,float age,float yaw,float pitch) {
            float intensity=hive.hurtTime>0?.04f:.02f;core.xRot=intensity*Mth.sin(age*.05f);core.zRot=intensity*Mth.sin(age*.1f);
            size=-.07f*hive.displaySize;pulse=.025f*Mth.sin(hive.tickCount*.075f);
        }
        @Override public void renderToBuffer(PoseStack pose,VertexConsumer buffer,int light,int overlay,float red,float green,float blue,float alpha) {
            pose.pushPose();pose.translate(0,1.6f,0);pose.scale(size-pulse,size+pulse,size-pulse);pose.translate(0,-(size+pulse)/2,0);
            core.render(pose,buffer,LightTexture.FULL_BRIGHT,overlay,red,green,blue,alpha);pose.popPose();
            base.render(pose,buffer,light,overlay,red,green,blue,alpha);
        }
    }
}
