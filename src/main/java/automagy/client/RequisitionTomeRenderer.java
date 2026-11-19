package automagy.client;

import automagy.content.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;

/** Original ModelNewPedestal geometry and the request book's opening transforms. */
public class RequisitionTomeRenderer implements BlockEntityRenderer<RequisitionTomeBlockEntity> {
    private static final ResourceLocation BOOK=new ResourceLocation("automagy","textures/models/requisition_book.png");
    private static final ResourceLocation STAND=new ResourceLocation("automagy","textures/models/requisition_stand.png");
    private final BookModel book;
    private final ModelPart stand;
    public RequisitionTomeRenderer(BlockEntityRendererProvider.Context context){
        book=new BookModel(context==null?BookModel.createBodyLayer().bakeRoot():context.bakeLayer(ModelLayers.BOOK));
        var mesh=new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("top",CubeListBuilder.create().texOffs(0,0).mirror().addBox(-7,0,-7,14,4,14),PartPose.offset(0,8,0));
        mesh.getRoot().addOrReplaceChild("middle",CubeListBuilder.create().texOffs(0,21).mirror().addBox(-5,0,-5,10,8,10),PartPose.offset(0,12,0));
        mesh.getRoot().addOrReplaceChild("bottom",CubeListBuilder.create().texOffs(0,41).mirror().addBox(-8,0,-8,16,4,16),PartPose.offset(0,20,0));
        stand=LayerDefinition.create(mesh,128,64).bakeRoot();
    }
    @Override public void render(RequisitionTomeBlockEntity tome,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var animation=tome.animation;float open=animation.open;
        pose.pushPose();pose.translate(.5,1.5,.5);pose.mulPose(Axis.ZP.rotationDegrees(180));
        if(!tome.hasLevel()){pose.translate(0,.07,0);pose.scale(.98f,.98f,.98f);}
        else pose.mulPose(Axis.YP.rotationDegrees(180-tome.getBlockState().getValue(RequisitionTomeBlock.FACING).toYRot()));
        stand.render(pose,buffers.getBuffer(RenderType.entityCutout(STAND)),light,overlay);
        float extraY=.05f*Math.min(open*10,1);
        pose.scale(1.1f,1.1f,1.1f);pose.translate(0,.39-animation.bookY-extraY,0);
        pose.mulPose(Axis.YP.rotationDegrees(90));pose.mulPose(Axis.XP.rotationDegrees(90+90*open));pose.mulPose(Axis.ZP.rotationDegrees(90-25*open));
        pose.translate(0,extraY,0);if(open<1)pose.translate(-.195f*(1-open),0,0);
        book.setupAnim(50,0,animation.page,.002f+1.098f*open);
        book.renderToBuffer(pose,buffers.getBuffer(RenderType.entityCutout(BOOK)),light,overlay,1,1,1,1);pose.popPose();
    }
}
