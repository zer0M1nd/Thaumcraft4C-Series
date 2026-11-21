package conciliummodern;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;

public class VoidBodyRenderer {
    public static final ResourceLocation BODY=ResourceLocation.parse("thaumicconcilium:effect/protobody"),CUBE=ResourceLocation.parse("thaumicconcilium:effect/distortion_field");
    public static final ResourceLocation[] LIMBS=java.util.stream.IntStream.range(0,6).mapToObj(i->ResourceLocation.parse("thaumicconcilium:effect/protolimb_"+i)).toArray(ResourceLocation[]::new);
    public static final BlockEntityWithoutLevelRenderer ITEM=new BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels()){
        @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
            pose.pushPose();
            if(stack.is(ConciliumItems.PROTOBODY.get())){pose.translate(.5,0,.5);pose.scale(.5f,.5f,.5f);draw(BODY,pose,buffers,ConciliumShaders.end());}
            else draw(CUBE,pose,buffers,ConciliumShaders.end());
            pose.popPose();
        }
    };
    static void draw(ResourceLocation modelId,PoseStack pose,MultiBufferSource buffers,RenderType layer){
        if(modelId.equals(CUBE)){ConciliumShaders.cube(pose,buffers.getBuffer(layer));return;}
        var model=Minecraft.getInstance().getModelManager().getModel(modelId);var out=buffers.getBuffer(layer);
        for(var quad:model.getQuads(null,null,net.minecraft.util.RandomSource.create(0))){var data=quad.getVertices();int stride=data.length/4;
            for(int i=0;i<4;i++){int j=i*stride;var sprite=quad.getSprite();float u=(Float.intBitsToFloat(data[j+4])-sprite.getU0())/(sprite.getU1()-sprite.getU0()),v=(Float.intBitsToFloat(data[j+5])-sprite.getV0())/(sprite.getV1()-sprite.getV0());out.vertex(pose.last().pose(),Float.intBitsToFloat(data[j]),Float.intBitsToFloat(data[j+1]),Float.intBitsToFloat(data[j+2])).uv(u,v).color(1f,1f,1f,1f).endVertex();}
        }
    }
    public static class Block implements BlockEntityRenderer<SolidVoidBlock.Tile>{
        public Block(BlockEntityRendererProvider.Context context){}
        @Override public void render(SolidVoidBlock.Tile tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){draw(CUBE,pose,buffers,ConciliumShaders.end());}
    }
    public static class Limb extends EntityRenderer<Protolimb>{
        public Limb(EntityRendererProvider.Context context){super(context);}
        @Override public ResourceLocation getTextureLocation(Protolimb entity){return TheEndPortalRenderer.END_PORTAL_LOCATION;}
        @Override public void render(Protolimb entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
            pose.pushPose();pose.mulPose(new org.joml.Quaternionf().rotationAxis((entity.tickCount+partial)*net.minecraft.util.Mth.DEG_TO_RAD,.70710677f,0,.70710677f));
            for(int pass=0;pass<4;pass++){
                pose.pushPose();float scale=pass<3?(float)Math.pow(1.1,pass):1;pose.scale(scale,scale,scale);
                draw(LIMBS[entity.limbType()],pose,buffers,pass<3?ConciliumShaders.endGlow(false):ConciliumShaders.end());pose.popPose();
            }
            pose.popPose();super.render(entity,yaw,partial,pose,buffers,light);
        }
    }
}
