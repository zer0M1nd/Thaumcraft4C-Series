package tc4revival.client;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import tc4revival.content.EtherealBloomBlockEntity;
public  class EtherealBloomRenderer implements BlockEntityRenderer<EtherealBloomBlockEntity>{
    private static final ResourceLocation LEAVES=ResourceLocation.parse("thaumcraft:textures/block/purifier_leaves.png"),STALK=ResourceLocation.parse("thaumcraft:textures/block/purifier_stalk.png"),CRYSTAL=ResourceLocation.parse("thaumcraft:textures/models/crystalcapacitor.png"),NODES=ResourceLocation.parse("thaumcraft:textures/misc/nodes.png");
    private final ModelPart cube;
    public EtherealBloomRenderer(BlockEntityRendererProvider.Context context){var mesh=new MeshDefinition();mesh.getRoot().addOrReplaceChild("cube",CubeListBuilder.create().texOffs(0,0).mirror().addBox(-8,-8,-8,16,16,16),PartPose.offset(8,8,8));cube=LayerDefinition.create(mesh,64,32).bakeRoot();}
    @Override public void render(EtherealBloomBlockEntity bloom,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        float age=bloom.growthCounter+partial,stem=Math.min(age,100)/100f,width=Math.min(age,50)/60f+.1666666f,lower=net.minecraft.util.Mth.clamp(age-33,0,33)/33,upper=net.minecraft.util.Mth.clamp(age-66,0,33)/33*.7f;
        pose.pushPose();pose.translate(.5,stem,.5);pose.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());int frame=Math.floorMod(bloom.counter,32);
        ClientFocusEffects.quad(TCRenderTypes.buffer(buffers,TCRenderTypes.softParticles(NODES)),pose.last().pose(),stem,frame/32f,6/32f,(frame+1)/32f,7/32f,170/255f,221/255f,1,1,220);pose.popPose();
        if(upper>0){pose.pushPose();pose.translate(.5-upper/8,stem-upper/6,.5-upper/8);pose.scale(upper/4,upper/3,upper/4);cube.render(pose,TCRenderTypes.buffer(buffers,RenderType.eyes(CRYSTAL)),LightTexture.FULL_BRIGHT,overlay);pose.popPose();}
        whorl(pose,buffers,LEAVES,.25f,0,lower,stem,0);whorl(pose,buffers,LEAVES,.6f,45,upper,stem*.7f,0);whorl(pose,buffers,STALK,.5f,0,width,stem,(1-stem)/2);
    }
    private static void whorl(PoseStack pose,MultiBufferSource buffers,ResourceLocation texture,float y,float angle,float width,float height,float offset){
        pose.pushPose();pose.translate(.5,y,.5);pose.mulPose(Axis.YP.rotationDegrees(angle));pose.mulPose(Axis.XP.rotationDegrees(180));
        for(int leaf=0;leaf<4;leaf++){
            pose.pushPose();pose.translate(0,offset,0);pose.scale(width,height,width);pose.mulPose(Axis.YP.rotationDegrees(leaf*90));var matrix=pose.last().pose();var out=TCRenderTypes.buffer(buffers,TCRenderTypes.bloomPlant(texture));
            out.vertex(matrix,-.5f,.5f,0).color(1f,1f,1f,1f).uv(1,1).uv2(200).endVertex();out.vertex(matrix,.5f,.5f,0).color(1f,1f,1f,1f).uv(0,1).uv2(200).endVertex();
            out.vertex(matrix,.5f,-.5f,0).color(1f,1f,1f,1f).uv(0,0).uv2(200).endVertex();out.vertex(matrix,-.5f,-.5f,0).color(1f,1f,1f,1f).uv(1,0).uv2(200).endVertex();pose.popPose();
        }pose.popPose();
    }
}
