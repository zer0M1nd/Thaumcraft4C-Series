package gadomancymodern.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import gadomancymodern.automation.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import tc4revival.content.*;

public class BlockProtectorRenderer implements BlockEntityRenderer<BlockProtectorEntity> {
    protected final ModelPart model;
    protected final tc4revival.client.EssentiaJarRenderer liquid=new tc4revival.client.EssentiaJarRenderer(null);
    protected final JarDisplay display=new JarDisplay();
    protected static final ResourceLocation OFF=new ResourceLocation("gadomancy","textures/models/block_protector_off.png"),ON=new ResourceLocation("gadomancy","textures/models/block_protector_on.png");
    public BlockProtectorRenderer(BlockEntityRendererProvider.Context context){
        var mesh=new MeshDefinition();var root=mesh.getRoot();float a=.3490659f;
        // Mirror was set after addBox in the old exporter, so those cubes were not mirrored.
        part(root,"base",0,0,0,0,0,16,3,16,-8,21,-8,0,0);
        part(root,"front",0,20,0,0,0,4,1,1,-2,20,-8,0,0);part(root,"front_slope",11,20,0,0,0,4,1,3,-2,20,-7,-a,0);
        part(root,"back",27,20,0,0,0,4,1,1,-2,20,7,0,0);part(root,"back_slope",38,20,0,0,-3,4,1,3,-2,20,7,a,0);
        part(root,"left",0,25,0,0,0,1,1,4,-8,20,-2,0,0);part(root,"left_slope",11,25,0,0,0,3,1,4,-7,20,-2,0,a);
        part(root,"right_slope",38,25,-3,0,0,3,1,4,7,20,-2,0,-a);part(root,"right",27,25,0,0,0,1,1,4,7,20,-2,0,0);
        part(root,"leg0",0,31,0,-7,0,2,8,2,3,22,3,a,-a);part(root,"leg1",10,31,-2,-7,-2,2,8,2,-3,22,-3,-a,a);
        part(root,"leg2",20,31,0,-7,-2,2,8,2,3,22,-3,-a,-a);part(root,"leg3",30,31,-2,-7,0,2,8,2,-3,22,3,a,a);
        model=LayerDefinition.create(mesh,64,64).bakeRoot();
    }
    protected static void part(PartDefinition root,String name,int u,int v,float bx,float by,float bz,float w,float h,float d,float x,float y,float z,float rx,float rz){root.addOrReplaceChild(name,CubeListBuilder.create().texOffs(u,v).addBox(bx,by,bz,w,h,d),PartPose.offsetAndRotation(x,y,z,rx,0,rz));}
    @Override public void render(BlockProtectorEntity tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){draw(tile,tile.power()>0,tile.getBlockState().getValue(BlockProtector.FACING),partial,pose,buffers,light,overlay);}
    public void draw(BlockProtectorEntity tile,boolean powered,Direction facing,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        pose.pushPose();pose.translate(.5,0,.5);pose.mulPose(Axis.YP.rotationDegrees(switch(facing){case EAST->90;case SOUTH->180;case WEST->270;default->0;}));pose.translate(-.5,0,-.5);
        pose.pushPose();pose.translate(0,-.155,0);pose.scale(.4f,.4f,.4f);pose.translate(.75,.75,.75);
        if(tile!=null){display.source=tile;liquid.render(display,partial,pose,buffers,light,overlay);display.source=null;}
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(TCArcane.ESSENTIA_JAR.get().defaultBlockState(),pose,buffers,light,overlay);pose.popPose();
        pose.translate(.5,1.5,.5);pose.mulPose(Axis.XP.rotationDegrees(180));model.render(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(powered?ON:OFF)),light,overlay);pose.popPose();
    }
    /** Rendering adapter scales the 8-unit reservoir into the core renderer's 64-unit jar geometry. */
    public static class JarDisplay extends EssentiaJarBlockEntity {
        protected BlockProtectorEntity source;
        public JarDisplay(){super(BlockPos.ZERO,TCArcane.ESSENTIA_JAR.get().defaultBlockState());}
        @Override public int amount(){return source==null?0:source.amount()*8;}
        @Override public tc4revival.aspect.Aspect aspect(){return source==null?null:source.aspect();}
        @Override public tc4revival.aspect.Aspect filter(){return source==null?null:source.filter();}
        @Override public Direction facing(){return Direction.NORTH;}
        @Override public BlockPos getBlockPos(){return source==null?BlockPos.ZERO:source.getBlockPos();}
    }
}
