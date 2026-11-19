package conciliummodern;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import tc4revival.client.GroupedObjMesh;

/** Original external meshes with independent reloadable caches. */
public final class CrimsonObjRenderer extends BlockEntityWithoutLevelRenderer {
    private static CrimsonObjRenderer instance;
    private static final java.util.Map<String,GroupedObjMesh> MESHES=new java.util.HashMap<>();
    private static ShaderInstance etherealShader;
    public static void shaders(net.minecraftforge.client.event.RegisterShadersEvent event)throws java.io.IOException{event.registerShader(new ShaderInstance(event.getResourceProvider(),ResourceLocation.parse("thaumicconcilium:ethereal"),com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX_COLOR),value->etherealShader=value);}
    public CrimsonObjRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    public static CrimsonObjRenderer instance(){if(instance==null)instance=new CrimsonObjRenderer();return instance;}
    public static void reload(){MESHES.clear();}
    private static GroupedObjMesh mesh(String name){
        return MESHES.computeIfAbsent(name,key->{try(var stream=Minecraft.getInstance().getResourceManager().getResourceOrThrow(ResourceLocation.parse("thaumicconcilium:models/"+key+".obj")).open()){
            return GroupedObjMesh.read(new java.io.InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8));
        }catch(java.io.IOException error){throw new IllegalStateException("Missing Concilium mesh "+key,error);}});
    }
    public static void draw(String name,PoseStack pose,MultiBufferSource buffers,int light,int overlay,float alpha){
        var mesh=mesh(name);
        var out=buffers.getBuffer(RenderType.entityTranslucent(ResourceLocation.parse("thaumicconcilium:textures/models/"+name+".png")));var frame=pose.last();
        for(var vertices:mesh.groups().values())for(var v:vertices)out.vertex(frame.pose(),v.x(),v.y(),v.z()).color(1f,1f,1f,alpha).uv(v.u(),v.v()).overlayCoords(overlay).uv2(light).normal(frame.normal(),v.nx(),v.ny(),v.nz()).endVertex();
    }
    public static void ethereal(String name,PoseStack pose,MultiBufferSource buffers,float progress){
        var out=buffers.getBuffer(EtherealLayer.TYPES.computeIfAbsent(name,EtherealLayer::layer));
        for(var vertices:mesh(name).groups().values())for(var v:vertices)out.vertex(pose.last().pose(),v.x(),v.y(),v.z()).uv(v.u(),v.v()).color(1f,1f,1f,progress).endVertex();
    }
    private static final class EtherealLayer extends RenderType {
        private static final java.util.Map<String,RenderType> TYPES=new java.util.HashMap<>();
        private EtherealLayer(String n,com.mojang.blaze3d.vertex.VertexFormat f,com.mojang.blaze3d.vertex.VertexFormat.Mode m,int s,boolean c,boolean o,Runnable a,Runnable b){super(n,f,m,s,c,o,a,b);}
        static RenderType layer(String name){return create("concilium_ethereal_"+name,com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX_COLOR,com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,256,false,true,
            CompositeState.builder().setShaderState(new ShaderStateShard(()->ConciliumShaders.prepare(etherealShader))).setTextureState(new TextureStateShard(ResourceLocation.parse("thaumicconcilium:textures/models/"+name+".png"),false,false)).setCullState(NO_CULL)
            .setTransparencyState(new TransparencyStateShard("ethereal",()->{com.mojang.blaze3d.systems.RenderSystem.enableBlend();com.mojang.blaze3d.systems.RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_COLOR,com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);},()->{com.mojang.blaze3d.systems.RenderSystem.disableBlend();com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();})).createCompositeState(false));}
    }
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        pose.pushPose();pose.translate(.5,.5,.5);
        switch(context){
            case GROUND->pose.translate(0,1.5,0);
            case GUI,FIXED->{pose.scale(.5f,.5f,.5f);pose.mulPose(Axis.XP.rotationDegrees(-65));}
            case FIRST_PERSON_LEFT_HAND,FIRST_PERSON_RIGHT_HAND->{pose.translate(.5,1,.3);pose.mulPose(Axis.YP.rotationDegrees(50));}
            default->{pose.mulPose(Axis.YP.rotationDegrees(45));pose.translate(0,1.3,.7);}
        }
        draw("molot",pose,buffers,light,overlay,1);pose.popPose();
    }
    public static final class Shackles extends EntityRenderer<EtherealShackles>{
        public Shackles(EntityRendererProvider.Context context){super(context);}
        @Override public ResourceLocation getTextureLocation(EtherealShackles entity){return ResourceLocation.parse("thaumicconcilium:textures/models/shackles.png");}
        @Override public void render(EtherealShackles entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
            pose.pushPose();pose.translate(0,.5,0);pose.mulPose(Axis.YP.rotationDegrees((entity.tickCount+partial)*10));
            draw("shackles",pose,buffers,light,OverlayTexture.NO_OVERLAY,1);draw("chain",pose,buffers,light,OverlayTexture.NO_OVERLAY,1);pose.popPose();
        }
    }
}
