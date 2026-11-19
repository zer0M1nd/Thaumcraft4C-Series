package ttmodern;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid=Tinkerer.ID,value=net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class PlacementMirrorPreview {
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void render(net.minecraftforge.client.event.RenderLevelStageEvent event){
        if(!ClientConfig.MIRROR_PREVIEW.get())return;
        if(event.getStage()!=net.minecraftforge.client.event.RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)return;var client=net.minecraft.client.Minecraft.getInstance();var player=client.player;if(player==null||!(client.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit)||hit.getType()!=net.minecraft.world.phys.HitResult.Type.BLOCK)return;
        var stack=player.getMainHandItem();if(!stack.is(Tinkerer.PLACEMENT_MIRROR.get()))stack=player.getOffhandItem();if(!stack.is(Tinkerer.PLACEMENT_MIRROR.get())||PlacementMirrorItem.template(stack).isAir())return;
        var pose=event.getPoseStack();var camera=event.getCamera().getPosition();pose.pushPose();pose.translate(-camera.x,-camera.y,-camera.z);var buffers=client.renderBuffers().bufferSource();var lines=buffers.getBuffer(net.minecraft.client.renderer.RenderType.lines());var targets=PlacementMirrorItem.targets(stack,player,hit);boolean enough=PlacementMirrorItem.available(player,new net.minecraft.world.item.ItemStack(PlacementMirrorItem.template(stack).getBlock()))>=targets.size();
        for(var pos:targets)net.minecraft.client.renderer.LevelRenderer.renderLineBox(pose,lines,new net.minecraft.world.phys.AABB(pos).inflate(.002),enough?.4f:1f,enough?.9f:.2f,1f,.5f);pose.popPose();buffers.endBatch(net.minecraft.client.renderer.RenderType.lines());
    }
}
