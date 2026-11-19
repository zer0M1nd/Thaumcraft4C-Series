package conciliummodern;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

/** The original noise-warp shader over the Blockbench-authored unit cube. */
public final class DistortionRenderer extends EntityRenderer<DistortionField> {
    public static final ResourceLocation MODEL=ResourceLocation.parse("thaumicconcilium:effect/distortion_field");
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumicconcilium:textures/misc/noise.png");
    private static ShaderInstance shader;
    public DistortionRenderer(EntityRendererProvider.Context context){super(context);}
    public static void shaders(net.minecraftforge.client.event.RegisterShadersEvent event)throws java.io.IOException{
        event.registerShader(new ShaderInstance(event.getResourceProvider(),ResourceLocation.parse("thaumicconcilium:distortion"),DefaultVertexFormat.POSITION_TEX_COLOR),value->shader=value);
    }
    @Override public ResourceLocation getTextureLocation(DistortionField entity){return TEXTURE;}
    @Override public void render(DistortionField entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
        var out=buffers.getBuffer(Layer.TYPE);
        pose.pushPose();pose.translate(-1.5,-.5,-1.5);pose.scale(3,3,3);
        ConciliumShaders.cube(pose,out);
        pose.popPose();super.render(entity,yaw,partial,pose,buffers,light);
    }
    private static final class Layer extends RenderType {
        private Layer(String name,VertexFormat format,VertexFormat.Mode mode,int size,boolean crumbling,boolean sorting,Runnable setup,Runnable clear){super(name,format,mode,size,crumbling,sorting,setup,clear);}
        static final RenderType TYPE=create("concilium_distortion",DefaultVertexFormat.POSITION_TEX_COLOR,VertexFormat.Mode.QUADS,256,false,true,
                CompositeState.builder().setShaderState(new ShaderStateShard(()->ConciliumShaders.prepare(shader))).setTextureState(new TextureStateShard(TEXTURE,false,false))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL).createCompositeState(false));
    }
}
