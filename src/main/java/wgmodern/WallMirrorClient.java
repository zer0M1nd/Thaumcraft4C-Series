package wgmodern;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Original colored pixel geometry, the glass strip, and real player reflections. */
@Mod.EventBusSubscriber(modid="witchinggadgets",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public class WallMirrorClient implements BlockEntityRenderer<WallMirror.Tile> {
    static final ResourceLocation GLASS=new ResourceLocation("witchinggadgets","textures/models/glass.png"),WHITE=new ResourceLocation("witchinggadgets","textures/blocks/white.png");
    static final int[] COLORS={0,0xf3b838,0x634506,0xdc9b0e,0x9c6d09,0x845c08,0xf1aa0e,0xb37e0b,0xf1b024,0x875e07,0xcb8f0d,0xbe860c,0xf2b530,0xd0e0f8};
    static final int[][] SHAPE={
        {0,0,0,0,0,11,11,10},{0,4,9,0,7,1,10,0},{10,13,13,5,7,0,0,0},{6,13,1,3,1,0,0,0},
        {0,4,4,11,0,0,0,0},{0,0,5,11,0,0,0,0},{0,0,11,4,0,0,0,0},{0,0,4,11,0,0,0,0},
        {0,0,3,4,0,0,0,0},{0,0,3,1,0,0,0,0},{0,0,4,3,0,0,0,0},{0,3,4,0,0,0,0,0},
        {0,3,10,0,0,0,0,0},{0,1,10,0,0,0,0,0},{0,8,12,0,0,0,0,0},{8,8,12,0,0,0,0,0}};
    public record FrameVertex(float x,float y,float z,float u,float v,int nx,int ny,int nz,int rgb){}
    static final java.util.List<FrameVertex> FRAME=buildFrame();
    public WallMirrorClient(BlockEntityRendererProvider.Context c){}
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(WallMirror.TILE.get(),WallMirrorClient::new);}
    @Override public void render(WallMirror.Tile tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(tile.dummy())return;
        pose.pushPose();orient(pose,tile.facing());
        if(tile.activating||tile.deactivating||!tile.active)glass(pose,buffers,tile.activating||tile.deactivating?tile.activationAnimation:0,.0005f,false,light,overlay);
        frame(pose,buffers,light,overlay);
        for(var player:tile.reflectedPlayers())reflection(tile,player,pose,buffers,light);
        pose.popPose();
    }
    static void orient(PoseStack p,Direction facing){switch(facing){case NORTH->{p.mulPose(Axis.YP.rotationDegrees(90));p.translate(-1,0,0);}case SOUTH->{p.mulPose(Axis.YP.rotationDegrees(270));p.translate(0,0,-1);}case WEST->{p.mulPose(Axis.YP.rotationDegrees(180));p.translate(-1,0,-1);}default->{}}}
    static void glass(PoseStack p,MultiBufferSource b,int frame,float depth,boolean both,int light,int overlay){
        var out=b.getBuffer(RenderType.entityTranslucent(GLASS));float u=frame/32f,U=(frame+1)/32f;
        face(p,out,new float[][]{{depth,0,0,U,.5f},{depth,2,0,U,0},{depth,2,1,u,0},{depth,0,1,u,.5f}},-1,0,0,0xffffff,1,light,overlay);
        if(both)face(p,out,new float[][]{{depth,0,0,u,.5f},{depth,0,1,U,.5f},{depth,2,1,U,0},{depth,2,0,u,0}},1,0,0,0xffffff,1,light,overlay);
    }
    static java.util.List<FrameVertex> buildFrame(){var vertices=new java.util.ArrayList<FrameVertex>();for(int y=0;y<16;y++)for(int z=0;z<8;z++)if(SHAPE[y][z]!=0){int rgb=COLORS[SHAPE[y][z]];pixel(vertices,y,z,rgb);pixel(vertices,y,15-z,rgb);pixel(vertices,31-y,z,rgb);pixel(vertices,31-y,15-z,rgb);}return java.util.List.copyOf(vertices);}
    static void pixel(java.util.List<FrameVertex> vertices,int row,int column,int rgb){int[][] faces={{0,4,5,1},{2,3,7,6},{0,2,6,4},{1,5,7,3},{0,1,3,2},{4,6,7,5}},normals={{0,-1,0},{0,1,0},{0,0,-1},{0,0,1},{-1,0,0},{1,0,0}};for(int f=0;f<6;f++)for(int i=0;i<4;i++){int corner=faces[f][i];vertices.add(new FrameVertex((corner>>2&1)/16f,(row+(corner>>1&1))/16f,(column+(corner&1))/16f,i==1||i==2?1:0,i>=2?1:0,normals[f][0],normals[f][1],normals[f][2],rgb));}}
    static void frame(PoseStack p,MultiBufferSource b,int light,int overlay){var out=b.getBuffer(RenderType.entityCutoutNoCull(WHITE));for(var v:FRAME)out.vertex(p.last().pose(),v.x,v.y,v.z).color((v.rgb>>16&255)/256f,(v.rgb>>8&255)/256f,(v.rgb&255)/256f,1).uv(v.u,v.v).overlayCoords(overlay).uv2(light).normal(p.last().normal(),v.nx,v.ny,v.nz).endVertex();}
    static void face(PoseStack p,VertexConsumer out,float[][] vertices,int nx,int ny,int nz,int rgb,float tint,int light,int overlay){for(var v:vertices)out.vertex(p.last().pose(),v[0],v[1],v[2]).color((rgb>>16&255)/255f*tint,(rgb>>8&255)/255f*tint,(rgb&255)/255f*tint,1).uv(v[3],v[4]).overlayCoords(overlay).uv2(light).normal(p.last().normal(),nx,ny,nz).endVertex();}
    static void reflection(WallMirror.Tile tile,Player player,PoseStack p,MultiBufferSource b,int light){
        var mc=Minecraft.getInstance();var pos=tile.getBlockPos();var facing=tile.facing();double dx=pos.getX()-player.getX(),dz=pos.getZ()-player.getZ();float scale=WallMirrorRules.reflectionScale(Math.sqrt(dx*dx+dz*dz)),h=(float)(facing.getAxis()==Direction.Axis.Z?player.getX()-pos.getX()-.5:player.getZ()-pos.getZ()-.5)/4;
        // Old RenderPlayer subtracted yOffset after the mirror added yOffset/2.
        float v=WallMirrorRules.reflectionOriginY(player.getY(),pos.getY(),player==mc.player,player.isCrouching());
        p.pushPose();p.translate(.01875,.875,.5);int sign=facing==Direction.SOUTH||facing==Direction.WEST?(h>0?1:-1):(h>0?-1:1);p.mulPose(Axis.YP.rotationDegrees(sign*2.875f));p.scale(-.0048f,1,1);p.scale(1,scale,scale);p.translate(.5,0,0);
        switch(facing){case NORTH->p.mulPose(Axis.YP.rotationDegrees(-90));case SOUTH->p.mulPose(Axis.YP.rotationDegrees(-270));case WEST->p.mulPose(Axis.YP.rotationDegrees(-180));default->{}}
        p.translate(facing.getAxis()==Direction.Axis.Z?h:0,v,facing.getAxis()==Direction.Axis.Z?0:h);
        var main=player.getMainHandItem();var off=player.getOffhandItem();
        try{player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);player.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);mc.getEntityRenderDispatcher().getRenderer(player).render(player,0,1,p,b,light);}
        finally{player.setItemInHand(InteractionHand.MAIN_HAND,main);player.setItemInHand(InteractionHand.OFF_HAND,off);p.popPose();}
    }
    public static class ItemRenderer extends BlockEntityWithoutLevelRenderer {
        public ItemRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
        @Override public void renderByItem(ItemStack item,ItemDisplayContext context,PoseStack p,MultiBufferSource b,int light,int overlay){
            p.pushPose();p.translate(.5,.5,.5);
            if(context==ItemDisplayContext.GROUND){
                // Old Forge's BLOCK_3D helper scaled this render-type -1 block to one quarter.
                // Modern ItemEntityRenderer also adds a model-center lift absent from that helper.
                p.translate(0,-.25,0);p.scale(.25f,.25f,.25f);
            }else if(context.firstPerson()){
                // 1.7 ItemRenderer applied these before Forge's EQUIPPED_BLOCK helper.
                // Modern ItemInHandRenderer has the same hand translation but omits this model basis.
                if(context==ItemDisplayContext.FIRST_PERSON_LEFT_HAND)p.scale(-1,1,1);
                p.mulPose(Axis.YP.rotationDegrees(45));p.scale(.4f,.4f,.4f);p.translate(-.5,-.5,-.5);
            }else if(context==ItemDisplayContext.THIRD_PERSON_LEFT_HAND||context==ItemDisplayContext.THIRD_PERSON_RIGHT_HAND){
                boolean left=context==ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
                // Undo modern ItemInHandLayer's item basis, then reproduce the old BLOCK_3D helper.
                p.translate(left?1/16f:-1/16f,-.125,.625);
                p.mulPose(Axis.YP.rotationDegrees(-180));p.mulPose(Axis.XP.rotationDegrees(90));
                if(left)p.scale(-1,1,1);
                p.translate(-.0625,.625,-.25);
                p.mulPose(Axis.XP.rotationDegrees(20));p.mulPose(Axis.YP.rotationDegrees(45));
                p.scale(-.375f,-.375f,.375f);p.translate(-.5,-.5,-.5);
            }
            p.mulPose(Axis.YP.rotationDegrees(90));
            switch(context){case GROUND->p.translate(0,0,-.5);case THIRD_PERSON_LEFT_HAND,THIRD_PERSON_RIGHT_HAND->{p.mulPose(Axis.YP.rotationDegrees(-5));p.scale(.8f,.8f,.8f);p.translate(-.8,-.25,.2);}case GUI->{p.mulPose(Axis.YP.rotationDegrees(45));p.mulPose(Axis.YP.rotationDegrees(180));p.scale(.9f,.9f,.9f);p.translate(0,-1,-.5);}case FIRST_PERSON_LEFT_HAND,FIRST_PERSON_RIGHT_HAND->{p.scale(2,2,2);p.mulPose(Axis.YP.rotationDegrees(60));p.mulPose(Axis.XP.rotationDegrees(30));p.translate(-2,-1,.5);}default->{p.scale(.5f,.5f,.5f);p.translate(0,-1,-.5);}}
            glass(p,b,0,.03125f,true,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);frame(p,b,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);p.popPose();
        }
    }
}
