package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import tc4revival.content.TaintSporeEntity;

public  class TaintSporeRenderer extends MobRenderer<TaintSporeEntity,TaintSporeRenderer.SporeModel> {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumcraft:textures/models/taint_spore.png");
    public TaintSporeRenderer(EntityRendererProvider.Context context) { super(context,new SporeModel(),.25f); }
    @Override public ResourceLocation getTextureLocation(TaintSporeEntity entity) { return TEXTURE; }
    @Override protected int getBlockLightLevel(TaintSporeEntity entity,BlockPos pos) { return 15; }
    @Override protected int getSkyLightLevel(TaintSporeEntity entity,BlockPos pos) { return 15; }
    @Override protected float getFlipDegrees(TaintSporeEntity entity) { return 0; }
    @Override protected void scale(TaintSporeEntity entity,PoseStack pose,float partial) {
        float size=entity.displaySize+(entity.displaySize<entity.sporeSize() ? .02f*partial : 0);
        float pulse=.025f*Mth.sin(entity.tickCount*.075f);
        pose.scale(-.12f*size-pulse,-.12f*size+pulse,-.12f*size-pulse);
    }
    static  class SporeModel extends EntityModel<TaintSporeEntity> {
        private final ModelPart cube;
        SporeModel() {
            super(RenderType::entityTranslucent);
            var mesh=new MeshDefinition();
            mesh.getRoot().addOrReplaceChild("cube",CubeListBuilder.create().texOffs(0,0)
                    .addBox(-6,2,-6,12,12,12).addBox(-8,0,-8,16,16,16),PartPose.offset(0,24,0));
            cube=LayerDefinition.create(mesh,64,64).bakeRoot().getChild("cube");
        }
        @Override public void setupAnim(TaintSporeEntity entity,float limb,float amount,float age,float yaw,float pitch) {
            float intensity=entity.hurtTime>0 ? .04f : .02f;
            cube.xRot=intensity*Mth.sin(age*.05f); cube.zRot=intensity*Mth.sin(age*.1f);
        }
        @Override public void renderToBuffer(PoseStack pose,VertexConsumer buffer,int light,int overlay,float red,float green,float blue,float alpha) {
            cube.render(pose,buffer,light,overlay,red,green,blue,alpha);
        }
    }
}
