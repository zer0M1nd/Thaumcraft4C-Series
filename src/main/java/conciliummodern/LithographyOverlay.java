package conciliummodern;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=ThaumicConcilium.ID,value=Dist.CLIENT)
public final class LithographyOverlay {
    private static boolean visible;
    private static int[] readings=new int[20];
    private static ShaderInstance shader;
    public static void readings(boolean show,int[] values){visible=show;readings=values.clone();}
    @SubscribeEvent public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event){visible=false;readings=new int[20];}
    public static void register(net.minecraftforge.client.event.RegisterGuiOverlaysEvent event){event.registerAboveAll("lithography",(gui,graphics,partial,width,height)->render(graphics,partial,width,height));}
    public static void shaders(net.minecraftforge.client.event.RegisterShadersEvent event)throws java.io.IOException{event.registerShader(new ShaderInstance(event.getResourceProvider(),ResourceLocation.parse("thaumicconcilium:lithography_blink"),DefaultVertexFormat.POSITION_TEX_COLOR),value->shader=value);}
    private static void render(GuiGraphics graphics,float partial,int width,int height){
        var mc=Minecraft.getInstance();if(!visible||mc.player==null||mc.options.hideGui||mc.player.isSpectator())return;
        float scale=Math.min(1,Math.min(width/320f,height/244f));float blink=60-Math.abs(((mc.player.tickCount+partial)/3)%120-60);
        graphics.pose().pushPose();graphics.pose().translate(0,tc4revival.config.TCConfig.CLIENT.wandDialBottom.get()?height-244*scale:0,0);graphics.pose().scale(scale,scale,1);
        tc4revival.client.LegacyGui.blit(graphics,ResourceLocation.parse("thaumicconcilium:textures/gui/astral_hole.png"),0,0,320,244,0,0,256,256,256,256);graphics.flush();
        graphics.pose().pushPose();graphics.pose().translate(80,80,0);graphics.pose().mulPose(Axis.ZP.rotationDegrees(blink/30));graphics.pose().translate(-80,-80,0);
        quad(graphics,"star",0,0,112,112,0,0,1,1,(60-blink)/110);graphics.pose().popPose();
        int[][] x={{0,20,100,15},{14,37,90,37},{2,53,85,53},{5,73,95,73}};
        int[][] y={{20,0,10,100},{40,17,37,98},{55,12,56,90},{70,2,72,108}};
        for(int metric=0;metric<4;metric++)for(int target=0;target<4;target++){
            int value=readings[target*4+metric];if(value<0)continue;float u=(3-metric)/16f,v=Math.min(10,value)/16f;
            quad(graphics,"constellations",x[metric][target],y[metric][target],16,16,u,v,u+1/16f,v+1/16f,blink/110);
        }
        graphics.pose().pushPose();graphics.pose().mulPose(Axis.ZP.rotationDegrees(blink/30));
        for(int target=0;target<4;target++)if(readings.length>16+target&&readings[16+target]>=0)for(int bit=3;bit>=0;bit--){
            int offset=36-bit*12,power=readings[16+target];
            int px=switch(target){case 0->5;case 1,3->30+offset;default->102-bit*3;};
            int py=switch(target){case 0->25+offset;case 1->3;case 2->20+offset;default->100-bit*3;};
            float v=((power>>bit)&1)/16f;quad(graphics,"constellations",px,py,16,16,.25f,v,.3125f,v+.0625f,blink/110);
        }
        graphics.pose().popPose();
        graphics.flush();graphics.pose().popPose();
    }
    private static void quad(GuiGraphics graphics,String texture,int x,int y,int width,int height,float u,float v,float u2,float v2,float pulse){
        var out=graphics.bufferSource().getBuffer(BlinkType.layer(ResourceLocation.parse("thaumicconcilium:textures/gui/"+texture+".png")));var pose=graphics.pose().last().pose();
        out.vertex(pose,x,y+height,0).uv(u,v2).color(pulse,1,1,1).endVertex();out.vertex(pose,x+width,y+height,0).uv(u2,v2).color(pulse,1,1,1).endVertex();
        out.vertex(pose,x+width,y,0).uv(u2,v).color(pulse,1,1,1).endVertex();out.vertex(pose,x,y,0).uv(u,v).color(pulse,1,1,1).endVertex();
    }
    private static final class BlinkType extends RenderType{
        private BlinkType(String name,VertexFormat format,VertexFormat.Mode mode,int size,boolean crumbling,boolean sort,Runnable setup,Runnable clear){super(name,format,mode,size,crumbling,sort,setup,clear);}
        private static final java.util.Map<ResourceLocation,RenderType> TYPES=new java.util.HashMap<>();
        private static RenderType layer(ResourceLocation texture){return TYPES.computeIfAbsent(texture,key->create("concilium_lithography",DefaultVertexFormat.POSITION_TEX_COLOR,VertexFormat.Mode.QUADS,256,false,false,
            CompositeState.builder().setShaderState(new ShaderStateShard(()->ConciliumShaders.prepare(shader))).setTextureState(new TextureStateShard(key,false,false)).setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setCullState(NO_CULL).setDepthTestState(NO_DEPTH_TEST).setWriteMaskState(COLOR_WRITE).createCompositeState(false)));}
    }
    private LithographyOverlay(){}
}
