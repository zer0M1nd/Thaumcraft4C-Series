package thmodern.client;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import thmodern.Horizons;
import thmodern.alchemy.AlchemiteEntity;
public final class AlchemiteRenderer extends EntityRenderer<AlchemiteEntity> {
 private final net.minecraft.client.renderer.block.BlockRenderDispatcher blocks;
 public AlchemiteRenderer(EntityRendererProvider.Context context){super(context);blocks=context.getBlockRenderDispatcher();shadowRadius=.5f;}
 @Override public void render(AlchemiteEntity entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
  pose.pushPose();pose.translate(0,.5,0);float swelling=net.minecraft.util.Mth.clamp(1-(entity.getFuse()-partial+1)/10f,0,1);swelling*=swelling;swelling*=swelling;float scale=1+swelling*.3f;pose.scale(scale,scale,scale);pose.translate(-.5,-.5,-.5);TntMinecartRenderer.renderWhiteSolidBlock(blocks,Horizons.ALCHEMITE.get().defaultBlockState(),pose,buffers,light,entity.getFuse()/5%2==0);pose.popPose();super.render(entity,yaw,partial,pose,buffers,light);
 }
 @Override public ResourceLocation getTextureLocation(AlchemiteEntity entity){return net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS;}
}
