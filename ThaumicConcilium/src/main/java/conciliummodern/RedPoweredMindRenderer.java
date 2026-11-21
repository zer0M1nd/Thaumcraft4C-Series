package conciliummodern;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;

public final class RedPoweredMindRenderer implements BlockEntityRenderer<RedPoweredMindBlockEntity> {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumicconcilium:textures/models/red_powered_mind.png");
    private final ModelPart model=geometry();
    private static BlockEntityWithoutLevelRenderer item;
    public RedPoweredMindRenderer(BlockEntityRendererProvider.Context context){}
    /** Geometry/UV from the checked-in session Blockbench export, with cord separated for block rendering. */
    private static ModelPart geometry(){
        var mesh=new MeshDefinition();var root=mesh.getRoot();
        root.addOrReplaceChild("brain",CubeListBuilder.create().texOffs(0,0).mirror().addBox(-6,-16,-8,12,10,16),PartPose.offset(0,24,0));
        root.addOrReplaceChild("cerebellum",CubeListBuilder.create().texOffs(64,0).mirror().addBox(-4,-6,0,8,3,7),PartPose.offset(0,24,0));
        var stem=root.addOrReplaceChild("stem",CubeListBuilder.create(),PartPose.offset(0,24,0));
        stem.addOrReplaceChild("stem_r1",CubeListBuilder.create().texOffs(0,32).mirror().addBox(-2,0,0,2,6,2),PartPose.offsetAndRotation(1,-6,-2,.4089647f,0,0));
        root.addOrReplaceChild("cord",CubeListBuilder.create().texOffs(0,32).mirror().addBox(-1,10,-.08f,2,16,2),PartPose.offset(0,24,0));
        return LayerDefinition.create(mesh,128,64).bakeRoot();
    }
    private void draw(PoseStack pose,MultiBufferSource buffers,int light,int overlay,boolean cord){model.getChild("cord").visible=cord;model.render(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)),light,overlay);}
    @Override public void render(RedPoweredMindBlockEntity tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        float ticks=tile.getLevel()==null?0:tile.getLevel().getGameTime()+partial;
        pose.pushPose();pose.translate(.5,.3,.5);pose.mulPose(Axis.XP.rotationDegrees(180));pose.translate(0,-.8+Math.sin(ticks/14)*.03+.03,0);
        pose.mulPose(Axis.YP.rotationDegrees(tile.rotation(partial)-90));pose.scale(.6f,.6f,.6f);draw(pose,buffers,light,overlay,false);pose.popPose();
    }
    public static BlockEntityWithoutLevelRenderer itemRenderer(){if(item==null)item=new BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels()){
        private final RedPoweredMindRenderer renderer=new RedPoweredMindRenderer(null);
        @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
            pose.pushPose();pose.translate(.5,1.1,.5);pose.scale(.6f,-.6f,-.6f);pose.mulPose(Axis.YP.rotationDegrees(context==ItemDisplayContext.GUI?30:0));renderer.draw(pose,buffers,light,overlay,false);pose.popPose();
        }
    };return item;}
    public static final class EntityRenderer extends net.minecraft.client.renderer.entity.EntityRenderer<RedPoweredMind>{
        private final RedPoweredMindRenderer renderer=new RedPoweredMindRenderer(null);
        public EntityRenderer(EntityRendererProvider.Context context){super(context);shadowRadius=.4f;}
        @Override public ResourceLocation getTextureLocation(RedPoweredMind entity){return TEXTURE;}
        @Override public void render(RedPoweredMind entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
            pose.pushPose();pose.translate(0,1.5,0);pose.scale(.6f,-.6f,-.6f);pose.mulPose(Axis.YP.rotationDegrees(-yaw));renderer.draw(pose,buffers,light,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,true);pose.popPose();super.render(entity,yaw,partial,pose,buffers,light);
        }
    }
}
