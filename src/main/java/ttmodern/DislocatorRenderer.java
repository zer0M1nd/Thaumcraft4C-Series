package ttmodern;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.vertex.PoseStack;

@Mod.EventBusSubscriber(modid=Tinkerer.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class DislocatorRenderer implements BlockEntityRenderer<DislocatorEntity> {
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event){event.registerBlockEntityRenderer(Tinkerer.DISLOCATOR_ENTITY.get(),context->new DislocatorRenderer());}
    @Override public void render(DislocatorEntity entity,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var cover=entity.camouflage();if(cover==null || cover.isAir() || cover.getBlock() instanceof InterfaceBlock || cover.getBlock() instanceof DislocatorBlock)return;
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(cover,pose,buffers,light,overlay);
    }
}
