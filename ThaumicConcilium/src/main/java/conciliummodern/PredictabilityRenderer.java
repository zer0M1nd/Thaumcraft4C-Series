package conciliummodern;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import tc4revival.client.GroupedObjMesh;

public final class PredictabilityRenderer implements BlockEntityRenderer<PredictabilityBlockEntity> {
    private static final java.util.Map<String,GroupedObjMesh> MODELS=new java.util.HashMap<>();
    private static ShaderInstance shader;
    public PredictabilityRenderer(BlockEntityRendererProvider.Context context){}
    public static void reload(){MODELS.clear();}
    public static void shaders(net.minecraftforge.client.event.RegisterShadersEvent event)throws java.io.IOException{
        event.registerShader(new ShaderInstance(event.getResourceProvider(),ResourceLocation.parse("thaumicconcilium:hex_runes"),DefaultVertexFormat.POSITION_TEX_COLOR),value->shader=value);
    }
    private static GroupedObjMesh mesh(String name){return MODELS.computeIfAbsent(name,key->{
        try(var stream=Minecraft.getInstance().getResourceManager().getResourceOrThrow(ResourceLocation.parse("thaumicconcilium:models/"+key+".obj")).open()){
            return GroupedObjMesh.read(new java.io.InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8));
        }catch(java.io.IOException exception){throw new IllegalStateException("Cannot load predictability model "+key,exception);}
    });}
    @Override public void render(PredictabilityBlockEntity tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(!tile.isMaster())return;
        float ticks=tile.getLevel()==null?0:(tile.getLevel().getGameTime()%7200)+partial;
        // Per-instance time replaces the old renderer-global counter that ran once per rendered device.
        float progress=tile.hasRift()?(60-Math.abs(ticks%120-60))/60:0;
        pose.pushPose();pose.translate(.5,0,.5);
        draw("hex_caps","hex_caps",progress,pose,buffers);
        for(int ring=1;ring<=3;ring++){
            pose.pushPose();if(tile.hasRift())pose.mulPose(Axis.YP.rotationDegrees((ring==2?-1:1)*ticks));
            draw("hex"+ring,"hex_runes",progress,pose,buffers);pose.popPose();
        }
        pose.popPose();
    }
    private static void draw(String name,String texture,float progress,PoseStack pose,MultiBufferSource buffers){
        var out=buffers.getBuffer(HexRenderType.layer(ResourceLocation.parse("thaumicconcilium:textures/models/"+texture+".png")));
        for(var vertices:mesh(name).groups().values())for(var vertex:vertices)
            out.vertex(pose.last().pose(),vertex.x(),vertex.y(),vertex.z()).uv(vertex.u(),vertex.v()).color(progress,1,1,1).endVertex();
    }
    private static final class HexRenderType extends RenderType{
        private HexRenderType(String name,VertexFormat format,VertexFormat.Mode mode,int size,boolean crumbling,boolean sorting,Runnable setup,Runnable clear){super(name,format,mode,size,crumbling,sorting,setup,clear);}
        private static final java.util.Map<ResourceLocation,RenderType> LAYERS=new java.util.HashMap<>();
        private static RenderType layer(ResourceLocation texture){return LAYERS.computeIfAbsent(texture,key->create("concilium_hex",DefaultVertexFormat.POSITION_TEX_COLOR,VertexFormat.Mode.QUADS,256,false,true,
                CompositeState.builder().setShaderState(new ShaderStateShard(()->ConciliumShaders.prepare(shader))).setTextureState(new TextureStateShard(key,false,false))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL).createCompositeState(false)));}
    }
}
