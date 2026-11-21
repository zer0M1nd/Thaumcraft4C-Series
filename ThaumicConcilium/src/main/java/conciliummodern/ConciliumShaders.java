package conciliummodern;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.TheEndPortalRenderer;
import net.minecraft.resources.ResourceLocation;

/** Original ShaderHelper time/config contract, applied when buffered draws actually flush. */
public final class ConciliumShaders {
    private static ShaderInstance end;
    private static final net.minecraft.client.model.geom.ModelPart CUBE=ConciliumCubeGeometry.createBodyLayer().bakeRoot();
    public static void cube(PoseStack pose,VertexConsumer out){CUBE.render(pose,model(out),0,0,1,1,1,1);}
    public static boolean enabled(){return tc4revival.config.TCConfig.CLIENT.shaders.get();}
    public static float time(){var viewer=Minecraft.getInstance().getCameraEntity();return viewer==null?0:viewer.tickCount;}
    public static ShaderInstance prepare(ShaderInstance shader){
        if(shader!=null){shader.safeGetUniform("Enabled").set(enabled()?1f:0f);shader.safeGetUniform("Time").set(time());
            var player=Minecraft.getInstance().player;
            shader.safeGetUniform("Yaw").set(player==null?0:player.getYRot()*(float)Math.PI/180);
            shader.safeGetUniform("Pitch").set(player==null?0:-player.getXRot()*(float)Math.PI/180);
        }return shader;
    }
    public static void register(net.minecraftforge.client.event.RegisterShadersEvent event)throws java.io.IOException{
        event.registerShader(new ShaderInstance(event.getResourceProvider(),ResourceLocation.parse("thaumicconcilium:ender"),DefaultVertexFormat.POSITION_TEX_COLOR),value->end=value);
    }
    public static RenderType end(){return Layers.INNER;}
    public static RenderType endTranslucent(){return Layers.TRANSLUCENT;}
    public static RenderType endGlow(boolean revealed){return revealed?Layers.REVEALED:Layers.GLOW;}
    /** ModelPart emits the larger NEW_ENTITY format; retain the original UV and color. */
    public static VertexConsumer model(VertexConsumer out){return new VertexConsumer(){
        double x,y,z;float u,v;int r=255,g=255,b=255,a=255;
        @Override public VertexConsumer vertex(double x,double y,double z){this.x=x;this.y=y;this.z=z;return this;}
        @Override public VertexConsumer color(int r,int g,int b,int a){this.r=r;this.g=g;this.b=b;this.a=a;return this;}
        @Override public VertexConsumer uv(float u,float v){this.u=u;this.v=v;return this;}
        @Override public VertexConsumer overlayCoords(int u,int v){return this;}
        @Override public VertexConsumer uv2(int u,int v){return this;}
        @Override public VertexConsumer normal(float x,float y,float z){return this;}
        @Override public void endVertex(){out.vertex(x,y,z).uv(u,v).color(r,g,b,a).endVertex();}
        @Override public void defaultColor(int r,int g,int b,int a){color(r,g,b,a);}
        @Override public void unsetDefaultColor(){r=g=b=a=255;}
    };}
    private static final class Layers extends RenderType {
        private Layers(String n,VertexFormat f,VertexFormat.Mode m,int s,boolean c,boolean o,Runnable a,Runnable b){super(n,f,m,s,c,o,a,b);}
        static final RenderType INNER=layer("concilium_end",false,false,false),TRANSLUCENT=layer("concilium_end_ghost",false,false,true),GLOW=layer("concilium_end_glow",true,false,true),REVEALED=layer("concilium_end_revealed",true,true,true);
        private static RenderType layer(String name,boolean additive,boolean revealed,boolean noWrite){return create(name,DefaultVertexFormat.POSITION_TEX_COLOR,VertexFormat.Mode.QUADS,256,false,true,
            CompositeState.builder().setShaderState(new ShaderStateShard(()->prepare(end)))
                .setTextureState(new TextureStateShard(TheEndPortalRenderer.END_PORTAL_LOCATION,false,false))
                .setTransparencyState(additive?ADDITIVE_TRANSPARENCY:TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL)
                .setDepthTestState(revealed?NO_DEPTH_TEST:LEQUAL_DEPTH_TEST).setWriteMaskState(noWrite?COLOR_WRITE:COLOR_DEPTH_WRITE).createCompositeState(false));}
    }
}
