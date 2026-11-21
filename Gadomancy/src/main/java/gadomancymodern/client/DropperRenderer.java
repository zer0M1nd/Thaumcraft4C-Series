package gadomancymodern.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import gadomancymodern.automation.*;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import tc4revival.content.*;

public class DropperRenderer implements BlockEntityRenderer<ArcaneDropperBlockEntity> {
    private final ModelPart body;
    private final tc4revival.client.BellowsRenderer bellows=new tc4revival.client.BellowsRenderer(null);
    private BellowsBlockEntity referenceBellows;
    private static final ResourceLocation TEXTURE=new ResourceLocation("gadomancy","textures/models/arcane_dropper.png");
    public DropperRenderer(BlockEntityRendererProvider.Context context){
        MeshDefinition mesh=new MeshDefinition();var root=mesh.getRoot();
        int[][] parts={{44,0,-1,10,-4,2,14,8},{0,0,-8,8,4,16,16,4},{0,20,-8,8,-8,16,16,4},{34,22,1,10,-4,7,2,8},{34,32,1,20,-4,7,4,8},{34,52,-8,20,-4,7,4,8},{0,40,-8,10,-4,7,2,8},{21,50,3,8,-4,5,2,8},{0,50,-8,8,-4,5,2,8},{0,61,-3,8,-4,6,2,1},{14,61,-3,8,3,6,2,1}};
        for(int i=0;i<parts.length;i++){var p=parts[i];root.addOrReplaceChild("part"+i,CubeListBuilder.create().texOffs(p[0],p[1]).addBox(0,0,0,p[5],p[6],p[7]),PartPose.offset(p[2],p[3],p[4]));}
        body=LayerDefinition.create(mesh,64,64).bakeRoot();
    }
    @Override public void render(ArcaneDropperBlockEntity tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){draw(tile.getBlockState().getValue(ArcaneDropperBlock.FACING),tile.getBlockState().getValue(ArcaneDropperBlock.FLIPPED),pose,buffers,light,overlay);}
    public void draw(Direction side,boolean flipped,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        pose.pushPose();pose.translate(.5,-.5,.5);
        if(side==Direction.UP){pose.mulPose(Axis.XP.rotationDegrees(180));pose.translate(0,-2,0);}
        else if(side!=Direction.DOWN){pose.mulPose(new org.joml.Quaternionf().rotationAxis((float)-Math.PI/2,side.getStepZ(),0,-side.getStepX()));pose.translate(side.getStepX(),-1,side.getStepZ());}
        if(side.getAxis()==Direction.Axis.X)flipped=!flipped;
        if(flipped)pose.mulPose(Axis.YP.rotationDegrees(90));
        body.render(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)),light,overlay);
        renderBellows(pose,buffers,light,overlay);pose.mulPose(Axis.YP.rotationDegrees(180));renderBellows(pose,buffers,light,overlay);pose.popPose();
    }
    private void renderBellows(PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(referenceBellows==null)referenceBellows=new BellowsBlockEntity(BlockPos.ZERO,TCArcane.BELLOWS.get().defaultBlockState().setValue(BellowsBlock.FACING,Direction.SOUTH));
        pose.pushPose();pose.translate(0,.75,.25);pose.mulPose(Axis.YP.rotationDegrees(90));pose.translate(-.05,.03,0);pose.scale(.6f,.44f,.55f);bellows.render(referenceBellows,0,pose,buffers,light,overlay);pose.popPose();
    }
}
