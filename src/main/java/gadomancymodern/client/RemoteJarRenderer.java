package gadomancymodern.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import gadomancymodern.Gadomancy;
import gadomancymodern.automation.RemoteJarEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.*;
import tc4revival.content.*;

/** Five original pot parts, projected mirror bottom when empty, jar liquid/label and rotated sticky support. */
public class RemoteJarRenderer implements BlockEntityRenderer<RemoteJarEntity> {
    protected final ModelPart pot;
    protected final tc4revival.client.EssentiaJarRenderer contents=new tc4revival.client.EssentiaJarRenderer(null);
    protected final tc4revival.client.EssentiaMirrorRenderer portal=new tc4revival.client.EssentiaMirrorRenderer(null);
    protected final MirrorDisplay mirror=new MirrorDisplay();
    public RemoteJarRenderer(BlockEntityRendererProvider.Context context){
        var mesh=new MeshDefinition();var root=mesh.getRoot();
        part(root,"bottom",0,0,-5,24,-5,10,0,10);part(root,"west",0,28,-6,18,-6,1,6,12);part(root,"east",0,10,5,18,-6,1,6,12);part(root,"south",0,53,-5,18,5,10,6,1);part(root,"north",0,46,-5,18,-6,10,6,1);
        pot=LayerDefinition.create(mesh,32,64).bakeRoot();
    }
    protected static void part(PartDefinition root,String name,int u,int v,float x,float y,float z,float w,float h,float d){root.addOrReplaceChild(name,CubeListBuilder.create().texOffs(u,v).addBox(0,0,0,w,h,d),PartPose.offset(x,y,z));}
    @Override public void render(RemoteJarEntity jar,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        pose.pushPose();StickyJarRenderer.rotate(pose,jar.support(),jar.facing());
        if(jar.amount()==0){pose.pushPose();pose.translate(.225,.003,.225);pose.scale(.55f,1,.55f);mirror.source=jar;portal.render(mirror,partial,pose,buffers,light,overlay);mirror.source=null;pose.popPose();}
        pose.pushPose();pose.translate(.5,.002,.5);pose.mulPose(Axis.ZP.rotationDegrees(180));pose.scale(1.002f,1,1.002f);pose.translate(0,-1.5,0);pot.render(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(net.minecraft.resources.ResourceLocation.parse("gadomancy:textures/models/jar_pot.png"))),light,overlay);pose.popPose();
        contents.render(jar,partial,pose,buffers,light,overlay);
        // Keep glass in the deferred entity translucent batch; the chunk layer can
        // flush before later block entities and incorrectly hide them through the jar.
        var blocks=Minecraft.getInstance().getBlockRenderer();var state=Gadomancy.REMOTE_JAR.get().defaultBlockState();blocks.getModelRenderer().renderModel(pose.last(),buffers.getBuffer(Sheets.translucentCullBlockSheet()),state,blocks.getBlockModel(state),1,1,1,light,overlay,net.minecraftforge.client.model.data.ModelData.EMPTY,RenderType.translucent());pose.popPose();
    }
    public static class MirrorDisplay extends EssentiaMirrorBlockEntity {
        protected RemoteJarEntity source;
        public MirrorDisplay(){super(BlockPos.ZERO,TCArcane.ESSENTIA_MIRROR.get().defaultBlockState());}
        @Override public boolean linked(){return source!=null&&source.network()!=null;}
        @Override public Direction facing(){return Direction.UP;}
        @Override public BlockPos getBlockPos(){return source==null?BlockPos.ZERO:source.hasLevel()?source.getBlockPos():Minecraft.getInstance().player==null?BlockPos.ZERO:Minecraft.getInstance().player.blockPosition();}
    }
}
