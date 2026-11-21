package automagy.client;

import automagy.content.*;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;

public class UnseenScribeRenderer implements BlockEntityRenderer<UnseenScribeBlockEntity> {
    private static final ResourceLocation BOOK=new ResourceLocation("minecraft","textures/entity/enchanting_table_book.png");
    private static final ResourceLocation QUILL=new ResourceLocation("thaumcraft","textures/blocks/tablequill.png");
    private static final ResourceLocation TABLE=new ResourceLocation("automagy","textures/models/scribe_table.png");
    private final BookModel book;
    private final net.minecraft.client.model.geom.ModelPart inkwell;
    public UnseenScribeRenderer(BlockEntityRendererProvider.Context context){
        book=new BookModel(context==null?net.minecraft.client.Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.BOOK):context.bakeLayer(ModelLayers.BOOK));
        var mesh=new net.minecraft.client.model.geom.builders.MeshDefinition();
        mesh.getRoot().addOrReplaceChild("inkwell",net.minecraft.client.model.geom.builders.CubeListBuilder.create()
                .texOffs(52,27).mirror().addBox(-6,6,3,3,2,3),net.minecraft.client.model.geom.PartPose.ZERO);
        inkwell=net.minecraft.client.model.geom.builders.LayerDefinition.create(mesh,64,32).bakeRoot();
    }
    @Override public void render(UnseenScribeBlockEntity tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var a=tile.animation;
        pose.pushPose();pose.translate(.5,1.5,.5);pose.mulPose(Axis.ZP.rotationDegrees(180));
        pose.mulPose(Axis.YP.rotationDegrees(180-tile.getBlockState().getValue(UnseenScribeBlock.FACING).toYRot()));
        // Legacy ModelScribeTable renders the glass inkwell with SRC_ALPHA blending.
        inkwell.render(pose,buffers.getBuffer(RenderType.entityTranslucent(TABLE)),light,overlay);
        pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees(-90));pose.mulPose(Axis.XP.rotationDegrees(180));
        pose.mulPose(Axis.XP.rotationDegrees((float)a.quillR));pose.mulPose(Axis.ZP.rotationDegrees((float)a.quillR));pose.mulPose(Axis.YP.rotationDegrees((float)a.quillR));
        pose.translate(-.17+a.quillHoriz,-.4+a.quillY,-.15);pose.mulPose(Axis.YP.rotationDegrees(15));pose.scale(.5f,.5f,.5f);
        quill(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(QUILL)),light,overlay);pose.popPose();
        pose.translate(0,.4+a.bookY,-.15);pose.scale(.8f,.8f,.8f);pose.mulPose(Axis.YP.rotationDegrees(90));pose.mulPose(Axis.XP.rotationDegrees(180));pose.mulPose(Axis.ZP.rotationDegrees(80));
        book.setupAnim(50,1,a.page,1.1f);book.renderToBuffer(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(BOOK)),light,overlay,1,1,1,1);pose.popPose();
    }
    private static void vertex(PoseStack p,VertexConsumer out,int light,int overlay,float x,float y,float z,float u,float v,float nx,float ny,float nz){
        out.vertex(p.last().pose(),x,y,z).color(255,255,255,255).uv(u,v).overlayCoords(overlay).uv2(light).normal(p.last().normal(),nx,ny,nz).endVertex();
    }
    private static void quill(PoseStack p,VertexConsumer out,int light,int overlay){
        for(float z:new float[]{0,-.025f})for(int corner=0;corner<4;corner++){
            float x=corner==1||corner==2?1:0,y=corner<2?1:0;vertex(p,out,light,overlay,x,y,z,1-x,1-y,0,0,-1);
        }
        for(int axis=0;axis<2;axis++)for(int pixel=0;pixel<16;pixel++)for(int side=0;side<2;side++){
            float edge=(pixel+side)/16f,sample=1-(pixel+.5f)/16f;
            for(int corner=0;corner<4;corner++){
                float across=corner==1||corner==2?1:0,z=corner>=2?-.025f:0;
                vertex(p,out,light,overlay,axis==0?edge:across,axis==0?across:edge,z,axis==0?sample:1-across,axis==0?1-across:sample,axis==0?side*2-1:0,axis==1?side*2-1:0,0);
            }
        }
    }
}
