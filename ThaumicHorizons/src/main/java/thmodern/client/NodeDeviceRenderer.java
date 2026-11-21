package thmodern.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import thmodern.*;
import thmodern.node.*;

public final class NodeDeviceRenderer<T extends BlockEntity> implements BlockEntityRenderer<T>{
    private static final ResourceLocation SCANNER=ResourceLocation.parse("thaumcraft:models/scanner.obj");
    private final net.minecraft.client.model.geom.ModelPart crystal;
    public NodeDeviceRenderer(BlockEntityRendererProvider.Context context){
        var mesh=new net.minecraft.client.model.geom.builders.MeshDefinition();
        mesh.getRoot().addOrReplaceChild("crystal",net.minecraft.client.model.geom.builders.CubeListBuilder.create().texOffs(0,0).mirror().addBox(-16,-16,0,16,16,16),net.minecraft.client.model.geom.PartPose.offsetAndRotation(0,32,0,.7071f,0,.7071f));
        crystal=net.minecraft.client.model.geom.builders.LayerDefinition.create(mesh,64,32).bakeRoot();
    }
    public static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath(Horizons.ID,path);}
    @Override public void render(T entity,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        float time=entity.hasLevel()?entity.getLevel().getGameTime()+partial:0;
        if(entity instanceof NodeMonitorEntity monitor){
            pose.pushPose();pose.translate(.5,.5,.5);
            var facing=entity.getBlockState().getValue(NodeMonitorBlock.FACING);
            pose.mulPose(facing.getRotation());pose.scale(.4f,.4f,.4f);pose.translate(0,-.8,0);
            boolean powered=entity.getBlockState().getValue(NodeMonitorBlock.ACTIVE);
            pose.mulPose(Axis.YP.rotationDegrees(net.minecraft.util.Mth.lerp(partial,monitor.previousRotation,monitor.rotation)%360));
            ExternalMesh.render(SCANNER,id("textures/models/nodemon.png"),pose,buffers,light,overlay);
            ExternalMesh.render(id("models/hexagon.obj"),id("textures/models/"+(powered&&(int)time%30<15?"nodemonscreenactive":"nodemonscreen")+".png"),pose,buffers,LightTexture.FULL_BRIGHT,overlay);
            pose.popPose();
        }else if(entity instanceof DynamoEntity dynamo){
            boolean essentia=entity instanceof EssentiaDynamoEntity;
            if(dynamo.active()&&dynamo.rise>=.3f){
                pose.pushPose();pose.translate(.5,.5,.5);pose.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
                int rgb=dynamo.visualColor(),frame=(int)time%32;
                var glow=buffers.getBuffer(RenderType.entityTranslucent(ResourceLocation.parse("thaumcraft:textures/misc/nodes.png")));
                for(int i=0;i<4;i++){
                    boolean left=i==0||i==3,bottom=i<2;
                    glow.vertex(pose.last().pose(),left?-.2f:.2f,bottom?-.2f:.2f,0).color(rgb>>16&255,rgb>>8&255,rgb&255,230)
                        .uv((frame+(left?0:1))/32f,(bottom?2:1)/32f).overlayCoords(overlay).uv2(LightTexture.FULL_BRIGHT).normal(pose.last().normal(),0,0,1).endVertex();
                }
                pose.popPose();
            }
            // The pedestal is the baked JSON block model. The two rings rotate independently.
            pose.pushPose();pose.translate(.5,.2+net.minecraft.util.Mth.lerp(partial,dynamo.previousRise,dynamo.rise),.5);
            float rotation=net.minecraft.util.Mth.lerp(partial,dynamo.previousRotation,dynamo.rotation),tilt=net.minecraft.util.Mth.lerp(partial,dynamo.previousTilt,dynamo.tilt);
            pose.mulPose(Axis.YP.rotationDegrees(rotation%360));pose.mulPose(Axis.XP.rotationDegrees(tilt%360));pose.scale(.36f,.36f,.36f);
            var texture=id("textures/models/"+(essentia?"thaumiumring":"goldring")+".png");
            ExternalMesh.render(SCANNER,texture,pose,buffers,light,overlay);
            pose.mulPose(Axis.XP.rotationDegrees(-2*tilt%360));pose.scale(.5f,1,.5f);
            ExternalMesh.render(SCANNER,texture,pose,buffers,light,overlay);pose.popPose();
        }else if(entity instanceof SyntheticNodeEntity node){
            long r=255,g=255,b=255,total=1;var capacity=node.capacity();var charge=node.aspects();
            for(var aspect:capacity.aspects()){long n=capacity.amount(aspect);r+=(aspect.color()>>16&255)*n;g+=(aspect.color()>>8&255)*n;b+=(aspect.color()&255)*n;total+=n;}
            var pos=entity.getBlockPos();var random=new java.util.Random(pos.getX()+(long)pos.getY()*pos.getZ());
            random.nextInt(10);random.nextFloat();
            double radian=Math.toRadians(time%360);
            pose.pushPose();pose.translate(.5,-.15+Math.sin(radian)*.1,.5);pose.mulPose(Axis.YP.rotationDegrees(time%360));
            pose.scale((.15f+random.nextFloat()*.075f)*1.1f,(.5f+random.nextFloat()*.1f)*1.1f,(.15f+random.nextFloat()*.05f)*1.1f);
            crystal.render(pose,buffers.getBuffer(RenderType.entityTranslucent(ResourceLocation.parse("thaumcraft:textures/models/crystal.png"))),LightTexture.FULL_BRIGHT,overlay,Math.min(1,r/(total*220f)),Math.min(1,g/(total*220f)),Math.min(1,b/(total*220f)),1);
            pose.popPose();
            int index=0;
            for(var aspect:capacity.aspects()){
                double offset=Math.PI*2*index++/capacity.size(),distance=.4+.1*Math.cos(radian);
                pose.pushPose();pose.translate(.5+distance*Math.sin(2*radian+offset),.64+.1*Math.sin(radian),.5+distance*Math.cos(2*radian+offset));
                pose.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
                float size=.1f+.005f*charge.amount(aspect);int frame=(int)time%32;
                var out=buffers.getBuffer(RenderType.entityTranslucent(ResourceLocation.parse("thaumcraft:textures/misc/nodes.png")));
                for(int i=0;i<4;i++){
                    float x=(i==0||i==3)?-size:size,y=i<2?-size:size;
                    out.vertex(pose.last().pose(),x,y,0).color(aspect.color()>>16&255,aspect.color()>>8&255,aspect.color()&255,230)
                            .uv((frame+(i==0||i==3?0:1))/32f,(i<2?2:1)/32f).overlayCoords(overlay).uv2(LightTexture.FULL_BRIGHT).normal(pose.last().normal(),0,0,1).endVertex();
                }
                pose.popPose();
            }
        }
    }
}
