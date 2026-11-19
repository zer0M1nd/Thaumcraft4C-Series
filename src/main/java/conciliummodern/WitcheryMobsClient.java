package conciliummodern;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid=ThaumicConcilium.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class WitcheryMobsClient {
 @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(RemainingMobs.CHORT.get(),ChortRenderer::new);e.registerEntityRenderer(RemainingMobs.SLOPPY_ALCHEMIST.get(),SloppyRenderer::new);e.registerEntityRenderer(RemainingMobs.BURNED_WITCH.get(),BurnedRenderer::new);e.registerEntityRenderer(RemainingMobs.CHORT_SPIT.get(),ThrownItemRenderer::new);}
 private static ResourceLocation texture(String name){return ResourceLocation.parse("thaumicconcilium:textures/entity/"+name+".png");}
 private static final class ChortModel extends HumanoidModel<Chort>{final ModelPart tail;
  ChortModel(ModelPart root){super(root);tail=root.getChild("tail");}
  @Override public void setupAnim(Chort e,float swing,float amount,float age,float yaw,float pitch){super.setupAnim(e,swing,amount,age,yaw,pitch);tail.xRot=-.7853982f+body.xRot;tail.yRot=body.yRot;if(riding){rightLeg.xRot=leftLeg.xRot=-(float)Math.PI*2/5;rightLeg.yRot=(float)Math.PI/10;leftLeg.yRot=-(float)Math.PI/10;rightLeg.zRot=leftLeg.zRot=0;}}
  @Override public void renderToBuffer(PoseStack pose,VertexConsumer out,int light,int overlay,float r,float g,float b,float a){super.renderToBuffer(pose,out,light,overlay,r,g,b,a);tail.render(pose,out,light,overlay,r,g,b,a);}
 }
 private static final class ChortRenderer extends HumanoidMobRenderer<Chort,ChortModel>{ChortRenderer(EntityRendererProvider.Context c){super(c,new ChortModel(ConciliumChortGeometry.createBodyLayer().bakeRoot()),.5f);}@Override public ResourceLocation getTextureLocation(Chort e){return texture("chort");}}
 private static final class SloppyModel extends EntityModel<SloppyAlchemist>{final ModelPart root=ConciliumSloppyGeometry.createBodyLayer().bakeRoot();final HumanoidModel<SloppyAlchemist> free=new HumanoidModel<>(LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(.5f),0),64,32).bakeRoot());boolean captive;
  @Override public void setupAnim(SloppyAlchemist e,float swing,float amount,float age,float yaw,float pitch){captive=e.captive();if(!captive){free.attackTime=attackTime;free.riding=riding;free.young=young;free.setupAnim(e,swing,amount,age,yaw,pitch);return;}root.getChild("head").xRot=pitch*Mth.DEG_TO_RAD;root.getChild("head").yRot=yaw*Mth.DEG_TO_RAD;root.getChild("headwear").copyFrom(root.getChild("head"));root.getChild("body").xRot=Mth.HALF_PI;root.getChild("rightarm").xRot=root.getChild("leftleg").xRot=Mth.cos(swing*.6662f)*1.4f*amount;root.getChild("leftarm").xRot=root.getChild("rightleg").xRot=Mth.cos(swing*.6662f+Mth.PI)*1.4f*amount;}
  @Override public void renderToBuffer(PoseStack pose,VertexConsumer out,int light,int overlay,float r,float g,float b,float a){if(captive)root.render(pose,out,light,overlay,r,g,b,a);else free.renderToBuffer(pose,out,light,overlay,r,g,b,a);}
 }
 private static final class SloppyRenderer extends MobRenderer<SloppyAlchemist,SloppyModel>{SloppyRenderer(EntityRendererProvider.Context c){super(c,new SloppyModel(),.5f);}@Override public ResourceLocation getTextureLocation(SloppyAlchemist e){return texture("excsi");}}
 private static final class BurnedModel extends HumanoidModel<BurnedWitch>{BurnedModel(){super(LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE,0),64,32).bakeRoot());}@Override public void setupAnim(BurnedWitch e,float swing,float amount,float age,float yaw,float pitch){riding=true;super.setupAnim(e,swing,amount,age,yaw,pitch);rightLeg.xRot=leftLeg.xRot=-Mth.PI*2/5;rightLeg.yRot=Mth.PI/10;leftLeg.yRot=-Mth.PI/10;rightLeg.zRot=leftLeg.zRot=0;}}
 private static final class BurnedRenderer extends HumanoidMobRenderer<BurnedWitch,BurnedModel>{BurnedRenderer(EntityRendererProvider.Context c){super(c,new BurnedModel(),.5f);addLayer(new net.minecraft.client.renderer.entity.layers.RenderLayer<BurnedWitch,BurnedModel>(this){final ModelPart broom=ConciliumBroomGeometry.createBodyLayer().bakeRoot();@Override public void render(PoseStack pose,MultiBufferSource buffers,int light,BurnedWitch e,float swing,float amount,float partial,float age,float yaw,float pitch){if(e.isInvisible())return;var texture=WitcheryBridge.host().map(WitcheryBridge.Host::broomTexture);if(texture.isEmpty())return;pose.pushPose();pose.translate(0,.2,0);broom.render(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(texture.get())),light,LivingEntityRenderer.getOverlayCoords(e,0),.4f,.3f,.2f,1);pose.popPose();}});}@Override public ResourceLocation getTextureLocation(BurnedWitch e){return texture("burned_witch");}}
}
