package thmodern.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import thmodern.soul.*;

/** Animated contents are separate from the resource-pack-overridable static housings. */
public final class SoulDeviceRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
    private final ModelPart brain,jarBrine,forgeBrine,sieve,sand;
    public SoulDeviceRenderer(BlockEntityRendererProvider.Context context){
        var mesh=new MeshDefinition();var root=mesh.getRoot();
        root.addOrReplaceChild("lobes",CubeListBuilder.create().mirror().texOffs(0,0).addBox(-6,8,-8,12,10,16).texOffs(64,0).addBox(-4,18,0,8,3,7),PartPose.ZERO);
        root.addOrReplaceChild("stem",CubeListBuilder.create().mirror().texOffs(0,32).addBox(0,0,0,2,6,2),PartPose.offsetAndRotation(-1,18,-2,.4089647f,0,0));
        brain=LayerDefinition.create(mesh,128,64).bakeRoot();
        jarBrine=part(CubeListBuilder.create().mirror().addBox(-4,-11,-4,8,10,8),64,32);
        forgeBrine=part(CubeListBuilder.create().mirror().addBox(-7,17,-7,14,6,14),64,32);
        var grid=CubeListBuilder.create().mirror();
        for(int z:new int[]{-7,2})grid.texOffs(64,36).addBox(-1,8,z,2,1,5);
        for(int x:new int[]{-7,2})grid.texOffs(48,18).addBox(x,8,-1,5,1,2);
        for(int x:new int[]{-2,1})grid.texOffs(36,0).addBox(x,8,-1,1,1,2);
        for(int z:new int[]{-2,1})grid.texOffs(32,28).addBox(-2,8,z,4,1,1);
        for(int x:new int[]{-5,1})for(int z:new int[]{-5,4})grid.texOffs(62,24).addBox(x,8,z,4,1,1);
        for(int x:new int[]{-5,4})for(int z:new int[]{-4,1})grid.texOffs(64,32).addBox(x,8,z,1,1,3);
        sieve=part(grid,128,64);sand=part(CubeListBuilder.create().mirror().texOffs(72,16).addBox(-7,10,-7,14,3,14),128,64);
    }
    private static ModelPart part(CubeListBuilder cubes,int width,int height){var mesh=new MeshDefinition();mesh.getRoot().addOrReplaceChild("part",cubes,PartPose.ZERO);return LayerDefinition.create(mesh,width,height).bakeRoot();}
    private static ResourceLocation texture(String name){return ResourceLocation.parse("thaumichorizons:textures/"+name+".png");}
    private static void draw(ModelPart part,String texture,PoseStack pose,MultiBufferSource buffers,int light,int overlay){part.render(pose,buffers.getBuffer(RenderType.entityTranslucent(texture(texture))),light,overlay);}
    @Override public void render(T tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(tile.getLevel()==null)return;float time=tile.getLevel().getGameTime()+partial;
        pose.pushPose();pose.translate(.5,0,.5);pose.mulPose(Axis.XP.rotationDegrees(180));
        if(tile instanceof InspiratronEntity inspiratron){
            pose.translate(0,-.125,0);pose.pushPose();pose.translate(0,-.8+Math.sin(time/14)*.03+.03,0);
            pose.mulPose(Axis.YP.rotationDegrees(inspiratron.brainRotation(partial)-90));pose.scale(.4f,.4f,.4f);draw(brain,"models/brain",pose,buffers,light,overlay);pose.popPose();
            draw(jarBrine,"models/jarbrine",pose,buffers,light,overlay);
        }else if(tile instanceof SoulforgeEntity forge){
            pose.translate(0,-.25,0);
            for(int side:new int[]{-1,1}){pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees(time%360));pose.mulPose(Axis.ZP.rotationDegrees(-90));pose.mulPose(Axis.XP.rotationDegrees(90*side));pose.translate(0,-.55,0);pose.scale(.4f,.4f,.4f);draw(brain,"models/brain",pose,buffers,light,overlay);pose.popPose();}
            pose.translate(0,-1.25,0);draw(forgeBrine,"models/jarbrine2",pose,buffers,light,overlay);
        }else if(tile instanceof SoulExtractorEntity extractor){
            pose.translate(0,-1.5,0);pose.pushPose();pose.translate(0,.15625*(1-Math.cos(Math.toRadians(extractor.sieveRotation(partial)))),0);
            draw(sieve,"models/soulsieve",pose,buffers,light,overlay);pose.popPose();if(extractor.remaining()>0)draw(sand,"models/soulsieve",pose,buffers,light,overlay);
        }
        pose.popPose();
        if(tile instanceof SoulforgeEntity forge){
            double radians=time*Math.PI/180,dist=.1+.1*Math.cos(radians);
            for(int i=0;i<forge.souls();i++){double angle=2*radians+2*Math.PI*i/forge.souls();sprite(pose,buffers,.5+dist*Math.sin(angle),.85,.5+dist*Math.cos(angle),time,.1f);}
        }
    }
    private static void sprite(PoseStack pose,MultiBufferSource buffers,double x,double y,double z,float time,float size){
        pose.pushPose();pose.translate(x,y,z);pose.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());var out=buffers.getBuffer(RenderType.entityTranslucent(texture("misc/soul")));int frame=(int)time%16;
        for(int i=0;i<4;i++)out.vertex(pose.last().pose(),i==0||i==3?-size:size,i<2?-size:size,0).color(255,255,255,230).uv((frame+(i==0||i==3?0:1))/16f,i<2?1:0).overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(pose.last().normal(),0,0,1).endVertex();
        pose.popPose();
    }
}
