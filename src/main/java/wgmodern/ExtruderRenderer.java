package wgmodern;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Direction;

/** Original extruder geometry, pipe orientation and two fluid columns, using the modern animated atlas. */
public class ExtruderRenderer implements BlockEntityRenderer<StoneDevices.ExtruderTile> {
    public ExtruderRenderer(BlockEntityRendererProvider.Context context){}
    @Override public void render(StoneDevices.ExtruderTile tile,float pt,PoseStack pose,MultiBufferSource buffers,int light,int overlay){draw(tile.getBlockState().getValue(StoneDevices.Extruder.FACING),tile.progress,!tile.getLevel().hasNeighborSignal(tile.getBlockPos()),pose,buffers,light,overlay);}
    public static void draw(Direction direction,int tick,boolean working,PoseStack pose,MultiBufferSource buffers,int light,int overlay){pose.pushPose();pose.translate(.5,0,.5);pose.mulPose(Axis.YP.rotationDegrees(switch(direction){case SOUTH->180;case EAST->270;case WEST->90;default->0;}));pose.translate(-.5,0,-.5);
        var wood=texture("thaumcraft:textures/blocks/woodplain.png");var stone=texture("thaumcraft:textures/block/arcane_stone.png");var pillar=texture("thaumcraft:textures/block/pedestal_top.png");var side=texture("thaumcraft:textures/block/lifterside.png");var bore=texture("thaumcraft:textures/models/bore.png");
        box(pose,buffers,wood,0,0,0,1,.1875,1,0,0,1,.1875,light,overlay);box(pose,buffers,wood,.0625,.1875,.0625,.9375,.3125,.9375,0,0,1,.125,light,overlay);box(pose,buffers,wood,0,.8125,0,1,.9375,1,0,0,1,.1875,light,overlay);
        box(pose,buffers,stone,0,1,0,1,1,1,0,0,1,1,light,overlay);box(pose,buffers,stone,0,.9375,0,1,.999,1,0,0,.125,.125,light,overlay);box(pose,buffers,stone,.0625,.3125,.0625,.9375,.3125,.9375,.0625,.0625,.9375,.9375,light,overlay);box(pose,buffers,stone,0,.8125,0,1,.8125,1,0,0,1,1,light,overlay);
        for(double x:new double[]{0,.875})for(double z:new double[]{0,.875})box(pose,buffers,pillar,x,.1875,z,x+.125,.8125,z+.125,0,0,.125,1,light,overlay);
        box(pose,buffers,direction.getAxis()==Direction.Axis.Y?side:texture("thaumcraft:textures/block/liftertop.png"),.125,.125,.0624,.875,.875,.0625,.125,.125,.875,.875,light,overlay);box(pose,buffers,side,.125,.125,.9375,.875,.875,.9376,.125,.125,.875,.875,light,overlay);box(pose,buffers,side,.9375,.125,.125,.9376,.625,.875,.125,.125,.875,.625,light,overlay);box(pose,buffers,side,.0624,.125,.125,.0625,.625,.875,.125,.125,.875,.625,light,overlay);
        if(direction.getAxis()==Direction.Axis.Y){double y=direction==Direction.UP?1:-.125;box(pose,buffers,bore,.375,y,.375,.625,y+.125,.625,.859375,.65625,.8984375,.71875,light,overlay);double end=direction==Direction.UP?1.125:-.125;box(pose,buffers,bore,.375,end,.375,.625,end,.625,.828125,.875,.8671875,.953125,light,overlay);}else{box(pose,buffers,bore,.4075,.4075,0,.5925,.5925,.0624,.859375,.65625,.8984375,.71875,light,overlay);box(pose,buffers,bore,.375,.375,-.125,.625,.625,0,.859375,.65625,.8984375,.71875,light,overlay);box(pose,buffers,bore,.375,.375,-.125,.625,.625,-.125,.828125,.875,.8671875,.953125,light,overlay);}
        if(working){atlasBox(pose,buffers,"minecraft:block/lava_flow",.1875,.3125,.375,.3125,.8125,.625,.375,0,.625,.25,LightTexture.FULL_BRIGHT,overlay);atlasBox(pose,buffers,"minecraft:block/water_flow",.6875,.3125,.375,.8125,.8125,.625,.375,0,.625,.25,light,overlay);if(tick>32)atlasBox(pose,buffers,"minecraft:block/cobblestone",.3125,.3125,.3125,.6875,.6875,.6875,0,0,1,1,light,overlay);}
        pose.popPose();
    }
    private static ResourceLocation texture(String id){return new ResourceLocation(id);}
    private static void atlasBox(PoseStack p,MultiBufferSource b,String id,double x,double y,double z,double X,double Y,double Z,double u,double v,double U,double V,int light,int overlay){var sprite=Minecraft.getInstance().getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).apply(texture(id));box(p,b,net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS,x,y,z,X,Y,Z,sprite.getU((float)(u*16)),sprite.getV((float)(v*16)),sprite.getU((float)(U*16)),sprite.getV((float)(V*16)),light,overlay);}
    private static void box(PoseStack pose,MultiBufferSource buffers,ResourceLocation texture,double x,double y,double z,double X,double Y,double Z,double u,double v,double U,double V,int light,int overlay){var out=buffers.getBuffer(RenderType.entityTranslucent(texture));var m=pose.last();quad(out,m,new double[][]{{x,y,z},{X,y,z},{X,y,Z},{x,y,Z}},u,v,U,V,0,-1,0,light,overlay);quad(out,m,new double[][]{{x,Y,z},{x,Y,Z},{X,Y,Z},{X,Y,z}},u,v,U,V,0,1,0,light,overlay);quad(out,m,new double[][]{{x,y,z},{x,Y,z},{X,Y,z},{X,y,z}},u,v,U,V,0,0,-1,light,overlay);quad(out,m,new double[][]{{x,y,Z},{X,y,Z},{X,Y,Z},{x,Y,Z}},u,v,U,V,0,0,1,light,overlay);quad(out,m,new double[][]{{x,y,z},{x,y,Z},{x,Y,Z},{x,Y,z}},u,v,U,V,-1,0,0,light,overlay);quad(out,m,new double[][]{{X,y,z},{X,Y,z},{X,Y,Z},{X,y,Z}},u,v,U,V,1,0,0,light,overlay);}
    private static void quad(VertexConsumer out,PoseStack.Pose matrix,double[][] vertices,double u,double v,double U,double V,int nx,int ny,int nz,int light,int overlay){double[][] uv={{u,v},{U,v},{U,V},{u,V}};for(int i=0;i<4;i++)out.vertex(matrix.pose(),(float)vertices[i][0],(float)vertices[i][1],(float)vertices[i][2]).color(255,255,255,255).uv((float)uv[i][0],(float)uv[i][1]).overlayCoords(overlay).uv2(light).normal(matrix.normal(),nx,ny,nz).endVertex();}
}
