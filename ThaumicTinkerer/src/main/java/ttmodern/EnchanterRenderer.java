package ttmodern;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid=Tinkerer.ID,bus=net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD,value=net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class EnchanterRenderer implements net.minecraft.client.renderer.blockentity.BlockEntityRenderer<EnchanterEntity> {
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void register(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event){event.registerBlockEntityRenderer(Tinkerer.ENCHANTER_ENTITY.get(),context->new EnchanterRenderer());}
    @Override public void render(EnchanterEntity entity,float partial,com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,int light,int overlay){
        for(int slot=0;slot<2;slot++){var item=entity.items.getStackInSlot(slot);if(item.isEmpty())continue;pose.pushPose();pose.translate(slot==0?.6:.3,.79+(slot==1?.03*Math.cos((entity.getLevel().getGameTime()+partial)/20):0),slot==0?.45:.7);pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90));pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(slot==0?30:-70));float scale=slot==0?.7f:.5f;pose.scale(scale,scale,scale);net.minecraft.client.Minecraft.getInstance().getItemRenderer().renderStatic(item,net.minecraft.world.item.ItemDisplayContext.FIXED,light,overlay,pose,buffers,entity.getLevel(),0);pose.popPose();}
    }
}
