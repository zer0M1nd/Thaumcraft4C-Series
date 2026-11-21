package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import tc4revival.content.*;

/** Original bucket OBJ and liquid plane, driven by explicit synchronized FluidStack data. */
public  class GolemBucketRenderer {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumcraft:textures/models/bucket.png");
    public static void render(GolemEntity golem,PoseStack pose,MultiBufferSource buffers,int light) {
        if (golem.configuration().core()!=GolemCoreType.LIQUID) return;
        pose.pushPose(); pose.scale(.4f,.4f,.4f); pose.translate(0,3,-1.1); pose.mulPose(Axis.XP.rotationDegrees(90));
        EldritchMeshes.draw("golem_bucket","Bucket",pose,buffers,RenderType.entityCutoutNoCull(TEXTURE),light,OverlayTexture.NO_OVERLAY,1);
        var fluid=golem.fluid();
        if (!fluid.isEmpty()) {
            float fraction=fluid.getAmount()/(float)Math.max(fluid.getAmount(),golem.configuration().fluidCarryLimit());
            pose.translate(0,0,.2+.8*fraction); pose.scale(.8f,.8f,.8f);
            var extension=IClientFluidTypeExtensions.of(fluid.getFluid());
            var sprite=Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(extension.getStillTexture(fluid));
            int brightness=Math.max(light,0xF00000 | fluid.getFluid().getFluidType().getLightLevel(fluid)<<4);
            // Modern water sprites are grayscale and require the fluid tint.
            int tint=extension.getTintColor(fluid);
            ClientFocusEffects.quad(TCRenderTypes.buffer(buffers,TCRenderTypes.sharpTranslucent(TextureAtlas.LOCATION_BLOCKS)),pose.last().pose(),.5f,
                    sprite.getU0(),sprite.getV0(),sprite.getU1(),sprite.getV1(),(tint>>16&255)/255f,(tint>>8&255)/255f,(tint&255)/255f,(tint>>>24)/255f,brightness);
        }
        pose.popPose();
    }
    private GolemBucketRenderer() {}
}
