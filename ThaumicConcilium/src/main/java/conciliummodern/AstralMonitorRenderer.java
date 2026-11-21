package conciliummodern;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import tc4revival.client.GroupedObjMesh;

public final class AstralMonitorRenderer extends BlockEntityWithoutLevelRenderer {
    private static AstralMonitorRenderer instance;
    private static ShaderInstance frameShader;
    private static long colorEpoch=Long.MIN_VALUE;
    private static final float[] previousColors=new float[6],nextColors=new float[6];
    private static final java.util.Random COLORS=new java.util.Random();
    private GroupedObjMesh frame,glass;
    private AstralMonitorRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    public static AstralMonitorRenderer instance(){if(instance==null)instance=new AstralMonitorRenderer();return instance;}
    public static void reload(){colorEpoch=Long.MIN_VALUE;if(instance!=null){instance.frame=null;instance.glass=null;}}
    public static void shaders(net.minecraftforge.client.event.RegisterShadersEvent event)throws java.io.IOException{
        event.registerShader(new ShaderInstance(event.getResourceProvider(),ResourceLocation.parse("thaumicconcilium:monitor_frame"),DefaultVertexFormat.POSITION_TEX_COLOR),value->frameShader=value);
    }
    private static GroupedObjMesh load(String path){
        try(var stream=Minecraft.getInstance().getResourceManager().getResourceOrThrow(ResourceLocation.parse(path)).open()){
            return GroupedObjMesh.read(new java.io.InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8));
        }catch(java.io.IOException exception){throw new IllegalStateException("Cannot load astral monitor mesh",exception);}
    }
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        pose.pushPose();pose.translate(.5,.5,.5);
        if(context==ItemDisplayContext.GUI){pose.scale(.3125f,-.3125f,-.3125f);pose.mulPose(Axis.XP.rotationDegrees(210));pose.mulPose(Axis.YP.rotationDegrees(-45));pose.mulPose(Axis.XP.rotationDegrees(60));pose.mulPose(Axis.ZP.rotationDegrees(-30));pose.mulPose(Axis.YP.rotationDegrees(-248));}
        else if(context==ItemDisplayContext.FIRST_PERSON_LEFT_HAND || context==ItemDisplayContext.FIRST_PERSON_RIGHT_HAND){pose.scale(.3f,.3f,.3f);pose.mulPose(Axis.XP.rotationDegrees(65));}
        else if(context==ItemDisplayContext.GROUND){pose.translate(0,-.25,0);pose.scale(.25f,.25f,.25f);}
        else{pose.scale(.5f,.5f,.5f);pose.mulPose(Axis.XP.rotationDegrees(60));}
        renderModel(stack,context==ItemDisplayContext.FIRST_PERSON_LEFT_HAND || context==ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,pose,buffers);
        pose.popPose();
    }
    public void renderModel(ItemStack stack,boolean stars,PoseStack pose,MultiBufferSource buffers){
        if(frame==null)frame=load("thaumcraft:models/scanner.obj");if(glass==null)glass=load("thaumicconcilium:models/screen.obj");
        var mc=Minecraft.getInstance();double ticks=mc.level==null?0:mc.level.getGameTime()+mc.getFrameTime();long epoch=(long)(ticks/60);
        if(epoch!=colorEpoch){
            for(int i=0;i<6;i++){
                previousColors[i]=colorEpoch==Long.MIN_VALUE?COLORS.nextInt(6):nextColors[i];
                int next;do{next=COLORS.nextInt(6);}while(next==(int)previousColors[i]);nextColors[i]=next;
            }
            colorEpoch=epoch;
        }
        if(frameShader!=null){frameShader.safeGetUniform("PreviousColors").set(previousColors);frameShader.safeGetUniform("NextColors").set(nextColors);frameShader.safeGetUniform("Progress").set((float)(ticks%60/60));}
        var out=buffers.getBuffer(FrameType.TYPE);
        for(var vertices:frame.groups().values())for(var vertex:vertices)out.vertex(pose.last().pose(),vertex.x(),vertex.y(),vertex.z()).uv(vertex.u(),vertex.v()).color(1f,1f,1f,1f).endVertex();
        var screen=buffers.getBuffer(ConciliumShaders.end());
        for(var vertices:glass.groups().values())for(var vertex:vertices)screen.vertex(pose.last().pose(),vertex.x(),vertex.y(),vertex.z()).uv(vertex.u(),vertex.v()).color(1f,1f,1f,1f).endVertex();
        if(stars){
            int[] values=stack.hasTag()?stack.getTag().getIntArray("vals"):new int[0];
            if(values.length==4){
                pose.pushPose();pose.translate(0,.12,0);pose.mulPose(Axis.XP.rotationDegrees(90));pose.mulPose(Axis.ZP.rotationDegrees(90));pose.scale(.02f,.02f,.02f);
                star(pose,buffers,3,values[0],-32,-16);star(pose,buffers,2,values[1],-10,5);star(pose,buffers,1,values[2],0,-24);star(pose,buffers,0,values[3],16,0);pose.popPose();
            }
            if(stack.hasTag()&&stack.getTag().contains("mindPower")&&stack.getTag().getInt("mindPower")>=0){
                int power=stack.getTag().getInt("mindPower");pose.pushPose();pose.translate(0,.12,0);pose.mulPose(Axis.XP.rotationDegrees(90));pose.mulPose(Axis.ZP.rotationDegrees(90));pose.scale(.02f,.02f,.02f);
                for(int bit=3;bit>=0;bit--)star(pose,buffers,4,(power>>bit)&1,-32+(3-bit)*16,-8);pose.popPose();
            }
        }
    }
    private static void star(PoseStack pose,MultiBufferSource buffers,int column,int value,int x,int y){
        if(value<0)return;value=Math.min(10,value);
        var out=buffers.getBuffer(RenderType.entityTranslucent(ResourceLocation.parse("thaumicconcilium:textures/gui/constellations.png")));
        float u=column/16f,v=value/16f;
        float[][] points={{x,y+16,u,v+1/16f},{x+16,y+16,u+1/16f,v+1/16f},{x+16,y,u+1/16f,v},{x,y,u,v}};
        for(var point:points)out.vertex(pose.last().pose(),point[0],point[1],-.1f).color(1f,1f,1f,1f).uv(point[2],point[3]).overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(pose.last().normal(),0,0,-1).endVertex();
    }
    private static final class FrameType extends RenderType{
        private FrameType(String name,VertexFormat format,VertexFormat.Mode mode,int size,boolean crumbling,boolean sorting,Runnable setup,Runnable clear){super(name,format,mode,size,crumbling,sorting,setup,clear);}
        private static final RenderType TYPE=create("concilium_monitor_frame",DefaultVertexFormat.POSITION_TEX_COLOR,VertexFormat.Mode.QUADS,256,false,true,CompositeState.builder()
                .setShaderState(new ShaderStateShard(()->ConciliumShaders.prepare(frameShader))).setTextureState(new TextureStateShard(ResourceLocation.parse("thaumicconcilium:textures/models/atsral_frame.png"),false,false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL).createCompositeState(false));
    }
}
