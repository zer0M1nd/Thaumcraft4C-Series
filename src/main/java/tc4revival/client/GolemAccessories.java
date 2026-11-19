package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import tc4revival.content.GolemEntity;

/** Independent accessory geometry and animation periods from ModelGolemAccessories. */
public  class GolemAccessories {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumcraft:textures/models/golem_decoration.png");
    private final ModelPart root=create().bakeRoot();
    private static void box(PartDefinition root,String name,int u,int v,boolean mirror,float x,float y,float z,int w,int h,int d,boolean head) {
        root.addOrReplaceChild(name,CubeListBuilder.create().texOffs(u,v).mirror(mirror).addBox(x,y,z,w,h,d),PartPose.offset(0,30,head?-2:0));
    }
    private static LayerDefinition create() {
        var mesh=new MeshDefinition(); var root=mesh.getRoot();
        box(root,"fez",0,94,false,-4.5f,-15,-6,9,7,9,true);
        box(root,"plate",32,40,false,-6.5f,-1,-7,13,12,13,false);
        box(root,"plate_left",0,44,false,-8.5f,-4,-6.5f,3,6,12,false);
        box(root,"plate_right",0,44,true,5.5f,-4,-6.5f,3,6,12,false);
        box(root,"hat",0,110,false,-4.5f,-17,-6,9,9,9,true);
        box(root,"glasses",0,80,false,-4.5f,-8,-6,9,4,9,true);
        box(root,"visor",0,70,false,-5,-8,-6,10,5,5,true);
        box(root,"rim",36,114,false,-6.5f,-9,-8,13,1,13,true);
        box(root,"dart",80,80,false,7.9f,7.5f,-3.5f,6,16,7,false);
        box(root,"mace",80,26,false,-13,15,-5,6,8,10,false);
        box(root,"bowtie",0,0,false,-8.5f,-2,-6.5f,17,4,12,false);
        box(root,"jar",96,56,false,-4,-15,-5.5f,8,4,8,true);
        box(root,"brain",96,70,false,-3.5f,-14,-5,7,3,7,true);
        box(root,"evil",64,65,false,-4,-9,-5.5f,8,7,8,true);
        return LayerDefinition.create(mesh,128,128);
    }
    public void render(GolemEntity entity,GolemModel model,PoseStack pose,MultiBufferSource buffers,int light,float walk,float amount,float partial) {
        var config=entity.configuration(); var head=model.root().getChild("head");
        for (String name:new String[]{"fez","hat","glasses","visor","rim","jar","brain","evil"}) { var part=root.getChild(name); part.xRot=head.xRot; part.yRot=head.yRot; }
        var dart=root.getChild("dart"); var mace=root.getChild("mace"); int action=3-Math.abs(entity.actionTicks()-3);
        if (action>0) dart.xRot=mace.xRot=-2+1.5f*triangle(action-partial,10);
        else if (entity.leftArmTicks()>0 || entity.rightArmTicks()>0) {
            if (entity.leftArmTicks()>0) dart.xRot=-2+1.5f*triangle(entity.leftArmTicks()-partial,10);
            if (entity.rightArmTicks()>0) mace.xRot=-2+1.5f*triangle(entity.rightArmTicks()-partial,10);
        } else if (!entity.carried().isEmpty() || !entity.fluid().isEmpty() || entity.essentia()!=null) dart.xRot=mace.xRot=-1;
        else { dart.xRot=(-.2f-1.5f*triangle(walk,13))*amount; mace.xRot=(-.2f+1.5f*triangle(walk,13))*amount; }
        pose.pushPose(); pose.scale(.4f,.4f,.4f);
        if (config.decorated('R')) part("dart",pose,buffers,light,false);
        pose.pushPose();
        if (config.decorated('F')) { if (config.advanced()) pose.translate(0,-.01,0); part("fez",pose,buffers,light,false); }
        if (config.decorated('H')) { if (config.advanced()) pose.translate(0,-.01,0); part("hat",pose,buffers,light,false); part("rim",pose,buffers,light,false); }
        pose.popPose();
        if (config.decorated('B')) part("bowtie",pose,buffers,light,true);
        if (config.decorated('P')) for (String name:new String[]{"plate","plate_left","plate_right"}) part(name,pose,buffers,light,false);
        if (config.decorated('G')) part("glasses",pose,buffers,light,false);
        if (config.decorated('V')) part("visor",pose,buffers,light,false);
        if (config.decorated('M')) part("mace",pose,buffers,light,false);
        if (config.advanced()) {
            part("brain",pose,buffers,light,false); part("jar",pose,buffers,light,true);
            if (config.core()!=null) { pose.pushPose(); pose.scale(1.01f,1,1.01f); part("evil",pose,buffers,light,false); pose.popPose(); }
        }
        pose.popPose();
    }
    private void part(String name,PoseStack pose,MultiBufferSource buffers,int light,boolean translucent) {
        root.getChild(name).render(pose,buffers.getBuffer(translucent?RenderType.entityTranslucent(TEXTURE):RenderType.entityCutout(TEXTURE)),light,OverlayTexture.NO_OVERLAY);
    }
    private static float triangle(float value,float period) { return (Math.abs(value%period-period*.5f)-period*.25f)/(period*.25f); }
}
