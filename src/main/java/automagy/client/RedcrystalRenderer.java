package automagy.client;

import automagy.content.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

public final class RedcrystalRenderer implements BlockEntityRenderer<RedcrystalBlockEntity> {
    public RedcrystalRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(RedcrystalBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(.5,.5,.5); pose.mulPose(tile.face().getRotation()); pose.translate(0,-.5,0);
        if(tile.kind().equals("merc"))drawMerc(pose,buffers,light,overlay,tile.power>0,tile.connections,
            1+net.minecraft.util.Mth.lerp(partial,tile.previousRotation,tile.rotation)/3240f*360,
            net.minecraft.util.Mth.lerp(partial,tile.previousFloating,tile.floating));
        else draw(pose,buffers,light,overlay,tile.kind(),tile.power>0,tile.connections,tile.face());
        pose.popPose();
    }
    public static void draw(PoseStack pose, MultiBufferSource buffers, int light, int overlay, String kind, boolean powered, int mask, Direction face) {
        if(kind.equals("merc")){drawMerc(pose,buffers,light,overlay,powered,mask,0,0);return;}
        float scale=kind.equals("standard")?.25f:.4f; pose.scale(scale,scale,scale);
        String variant = switch(kind) { case "amp" -> "Amp"; case "dim" -> "Dim"; case "dense" -> "Dense"; case "res" -> "Res"; default -> "Standard"; };
        String texture=kind.equals("standard")&&powered ? "redcrystalActive" : "redcrystal"+variant+(powered?"active":"");
        var out=buffers.getBuffer(RenderType.entityCutoutNoCull(new ResourceLocation("automagy","textures/models/"+texture.toLowerCase(java.util.Locale.ROOT)+".png")));
        ExternalObj.REDCRYSTAL.draw(pose,out,light,overlay,1,1,"primary_Plane001");
        Direction[] local={Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST};
        String[] parts={"adjunctA_PlaneA","adjunctB_PlaneB","adjunctC_PlaneC","adjunctD_PlaneD"};
        for (int i=0;i<4;i++) {
            var transformed=new org.joml.Vector3f(local[i].getStepX(),0,local[i].getStepZ()).rotate(face.getRotation());
            Direction world=Direction.getNearest(transformed.x,transformed.y,transformed.z);
            if ((mask & 1<<world.ordinal())!=0) ExternalObj.REDCRYSTAL.draw(pose,out,light,overlay,1,1,parts[i]);
        }
    }
    private static void drawMerc(PoseStack pose,MultiBufferSource buffers,int light,int overlay,boolean powered,int mask,float rotation,float floating){
        pose.pushPose();pose.scale(.5f,.5f,.5f);
        var out=buffers.getBuffer(RenderType.entityCutoutNoCull(ResourceLocation.parse("automagy:textures/models/merccrystal"+(powered?"active":"")+".png")));
        String[] parts={"adjunctA_PlaneA","adjunctB_PlaneB","adjunctC_PlaneC","adjunctD_PlaneD"};
        Direction[] directions={Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST};
        for(int i=0;i<4;i++)if((mask&1<<directions[i].ordinal())!=0)ExternalObj.MERCCRYSTAL.draw(pose,out,light,overlay,1,1,parts[i]);
        ExternalObj.MERCCRYSTAL.draw(pose,out,light,overlay,1,1,"Slab");pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rotation));pose.translate(0,floating,0);
        ExternalObj.MERCCRYSTAL.draw(pose,out,light,overlay,1,1,"CrystalsMain");pose.popPose();
    }
}
