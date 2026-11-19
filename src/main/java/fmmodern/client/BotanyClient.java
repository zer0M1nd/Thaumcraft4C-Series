package fmmodern.client;

import com.mojang.blaze3d.vertex.PoseStack;
import fmmodern.Content;
import net.minecraft.client.model.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.*;
import vazkii.botania.api.item.CosmeticAttachable;
import vazkii.botania.client.render.AccessoryRenderRegistry;

/** Optional renderer bridge; never loaded on dedicated servers or without Botania. */
public final class BotanyClient implements ICurioRenderer {
    public static void setup(){
        CuriosRendererRegistry.register(Content.RING.get(),BotanyClient::new);
        CuriosRendererRegistry.register(Content.COLLAR.get(),BotanyClient::new);
    }
    @Override public <T extends LivingEntity,M extends EntityModel<T>> void render(ItemStack stack,SlotContext context,PoseStack pose,RenderLayerParent<T,M> parent,MultiBufferSource buffers,int light,float swing,float swingAmount,float partialTicks,float age,float headYaw,float headPitch){
        if(!(parent.getModel() instanceof HumanoidModel<?> model)||!(stack.getItem() instanceof CosmeticAttachable attachable))return;
        var cosmetic=attachable.getCosmeticItem(stack);if(cosmetic.isEmpty())return;
        var renderer=AccessoryRenderRegistry.get(cosmetic);if(renderer==null)return;
        pose.pushPose();
        try{renderer.doRender(model,cosmetic,context.entity(),pose,buffers,light,swing,swingAmount,partialTicks,age,headYaw,headPitch);}finally{pose.popPose();}
    }
    public static void flowers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event){
        for(String name:new String[]{"euclidaisy","whisperweed","tainthistle","bloodthorn"})event.registerBlockEntityRenderer(fmmodern.compat.Botany.type(name),vazkii.botania.client.render.block_entity.SpecialFlowerBlockEntityRenderer::new);
    }
}
