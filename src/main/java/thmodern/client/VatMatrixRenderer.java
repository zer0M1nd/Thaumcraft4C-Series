package thmodern.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import thmodern.vat.VatMatrixEntity;

/** Vat matrix rotates about Y only; ordinary infusion matrices also tilt on X/Z. */
public final class VatMatrixRenderer implements BlockEntityRenderer<VatMatrixEntity> {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumcraft:textures/models/infuser.png");
    private final ModelPart stone=cube(0),runes=cube(32);
    public VatMatrixRenderer(BlockEntityRendererProvider.Context context){}
    private static ModelPart cube(int row){
        var mesh=new MeshDefinition();mesh.getRoot().addOrReplaceChild("cube",CubeListBuilder.create().texOffs(0,row).addBox(-8,-8,-8,16,16,16),PartPose.ZERO);
        return LayerDefinition.create(mesh,64,64).bakeRoot();
    }
    @Override public void render(VatMatrixEntity tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        float time=tile.getLevel()==null?0:tile.getLevel().getGameTime()+partial;
        var vat=tile.vat();float start=tile.visualStartup;
        float shake=Math.min(6,1+(vat==null?0:Math.max(0,vat.instability()))*.66f*Math.min(tile.visualCraftTicks,50)/50f);
        draw(time,start,shake,pose,buffers,light,overlay);
        if(vat!=null&&vat.mode()==thmodern.vat.VatEntity.INFUSING)halo(tile.visualCraftTicks,pose,buffers);
    }
    public void draw(float time,float start,float shake,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        pose.pushPose();pose.translate(.5,.5,.5);pose.mulPose(Axis.YP.rotationDegrees(time%360*start));
        for(int layer=0;layer<(start>0?2:1);layer++){
            var out=buffers.getBuffer(layer==0?RenderType.entitySolid(TEXTURE):tc4revival.client.TCRenderTypes.infusionRunes(TEXTURE));
            for(int x=0;x<2;x++)for(int y=0;y<2;y++)for(int z=0;z<2;z++){
                pose.pushPose();pose.translate(offset(x,15,time,start,shake),offset(y,14,time,start,shake),offset(z,13,time,start,shake));
                if(x==1)pose.mulPose(Axis.XP.rotationDegrees(90));if(y==1)pose.mulPose(Axis.YP.rotationDegrees(90));if(z==1)pose.mulPose(Axis.ZP.rotationDegrees(90));
                pose.scale(.45f,.45f,.45f);
                if(layer==0)stone.render(pose,out,light,overlay);
                else runes.render(pose,out,0xf000f0,overlay,.8f,.1f,1,((float)Math.sin((time+x*2+y*3+z*4)/4)*.1f+.2f)*start);
                pose.popPose();
            }
        }
        pose.popPose();
    }
    private static double offset(int side,int period,float time,float start,float shake){return (side==0?-.25:.25)+Math.sin((time+side*10)/(period-shake/2))*.01*start*shake;}
    private static void halo(int ticks,PoseStack pose,MultiBufferSource buffers){
        var random=new java.util.Random(245);var out=buffers.getBuffer(tc4revival.client.TCRenderTypes.PRIMAL_RAYS);
        pose.pushPose();pose.translate(.5,.5,.5);
        for(int ray=0;ray<20;ray++){
            for(int i=0;i<6;i++){var axis=i%3==0?Axis.XP:i%3==1?Axis.YP:Axis.ZP;pose.mulPose(axis.rotationDegrees(random.nextFloat()*360+(i==5?ticks*.72f:0)));}
            float length=(5+random.nextFloat()*20)*Math.min(ticks,50)/1000f,width=(1+random.nextFloat()*2)*Math.min(ticks,50)/1000f;
            float[][] tips={{-.866f*width,length,-.5f*width},{.866f*width,length,-.5f*width},{0,length,width}};
            for(int side=0;side<3;side++){
                out.vertex(pose.last().pose(),0,0,0).color(255,255,255,255).endVertex();
                for(int i:new int[]{side,(side+1)%3})out.vertex(pose.last().pose(),tips[i][0],tips[i][1],tips[i][2]).color(204,0,255,0).endVertex();
            }
        }
        pose.popPose();
    }
}
