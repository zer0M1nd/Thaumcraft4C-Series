package thmodern.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.*;
import thmodern.vat.VatEntity;

public final class VatRenderer implements BlockEntityRenderer<VatEntity> {
    private final net.minecraft.client.renderer.entity.EntityRenderDispatcher entities;
    private final net.minecraft.client.model.geom.ModelPart effigy=CorpseEffigyModel.layer().bakeRoot();
    public VatRenderer(BlockEntityRendererProvider.Context context){entities=context.getEntityRenderer();}
    @Override public boolean shouldRenderOffScreen(VatEntity vat){return true;}
    @Override public void render(VatEntity vat,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(vat.hasEffigy()||vat.mode()==VatEntity.GROWING_EFFIGY){
            pose.pushPose();double time=vat.getLevel()==null?0:vat.getLevel().getGameTime()+partial;
            pose.translate(.5,-.6+.05*Math.cos(time*Math.PI/180),.5);pose.scale(.8f,-.8f,-.8f);
            var texture=net.minecraft.resources.ResourceLocation.parse("thaumichorizons:textures/models/"+(vat.hasEffigy()?"corpseeffigyrevived":"corpseeffigy")+".png");
            effigy.render(pose,buffers.getBuffer(net.minecraft.client.renderer.RenderType.entityCutoutNoCull(texture)),light,overlay);
            pose.popPose();return;
        }
        var subject=vat.renderSubject();if(subject==null||subject instanceof net.minecraft.world.entity.player.Player)return;
        pose.pushPose();pose.translate(.5,-1.75+.1*Math.cos((vat.getLevel().getGameTime()+partial)*Math.PI/180),.5);
        float scale=Math.min(1,Math.min(1.6f/Math.max(.1f,subject.getBbHeight()),.9f/Math.max(.1f,subject.getBbWidth())));
        if(vat.mode()==VatEntity.CLONING)scale*=Math.max(.05f,1-vat.progress()/800f);
        pose.scale(scale,scale,scale);entities.render(subject,0,0,0,subject.getYRot(),partial,pose,buffers,light);pose.popPose();
    }
}
