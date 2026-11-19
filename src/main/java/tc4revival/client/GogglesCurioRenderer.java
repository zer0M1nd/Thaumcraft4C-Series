package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/** Reuses the existing goggles armor texture and vanilla head armor model. */
public  class GogglesCurioRenderer implements ICurioRenderer {
    private final HumanoidModel<LivingEntity> model=new HumanoidModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR));
    public <T extends LivingEntity,M extends EntityModel<T>> void render(ItemStack stack,SlotContext context,PoseStack pose,RenderLayerParent<T,M> parent,MultiBufferSource buffers,int light,float limbSwing,float limbAmount,float partial,float age,float yaw,float pitch) {
        if(!tc4revival.config.TCConfig.SERVER.revival.gogglesHeadSlot.get()||!context.identifier().equals("head"))return;
        if(!(parent.getModel() instanceof HumanoidModel<?> wearer))return;
        model.setAllVisible(false);model.head.visible=true;model.head.copyFrom(wearer.head);model.young=wearer.young;model.crouching=wearer.crouching;model.riding=wearer.riding;
        var texture=ResourceLocation.parse("thaumcraft:textures/models/goggles.png");
        var consumer=net.minecraft.client.renderer.entity.ItemRenderer.getArmorFoilBuffer(buffers,RenderType.armorCutoutNoCull(texture),false,stack.hasFoil());
        model.renderToBuffer(pose,consumer,light,OverlayTexture.NO_OVERLAY,1,1,1,1);
    }
}
