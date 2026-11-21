package gadomancymodern.client;
import gadomancymodern.automation.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.content.*;
@Mod.EventBusSubscriber(modid="gadomancy",value=Dist.CLIENT)
public class DropperOverlay {
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        var mc=Minecraft.getInstance();
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS||mc.player==null||mc.level==null||!(mc.hitResult instanceof BlockHitResult hit)||hit.getType()!=HitResult.Type.BLOCK)return;
        var wand=mc.player.getMainHandItem();if(!(wand.getItem() instanceof CastingWandItem)||WandFocusItem.attached(wand).getItem() instanceof ArchitectFocusItem)return;
        if(!(mc.level.getBlockEntity(hit.getBlockPos()) instanceof ArcaneDropperBlockEntity dropper))return;
        var direction=dropper.getBlockState().getValue(ArcaneDropperBlock.FACING);BlockPos center=hit.getBlockPos().relative(direction,2);
        var positions=new java.util.HashSet<BlockPos>();for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++)positions.add(center.offset(x,y,z));positions.add(hit.getBlockPos().relative(direction));
        var pose=event.getPoseStack();var camera=event.getCamera().getPosition();var buffers=mc.renderBuffers().bufferSource();float tick=mc.player.tickCount;
        for(var pos:positions){var offset=Vec3.atLowerCornerOf(pos).subtract(camera);pose.pushPose();pose.translate(offset.x,offset.y,offset.z);
            tc4revival.client.ConnectedFaces.render(pos,positions::contains,positions::contains,pose,type->buffers.getBuffer(DropperOverlayRender.wrap(type)),(float)Math.sin(tick/2+pos.getX())*.2f+.3f,(float)Math.sin(tick/3+pos.getY())*.2f+.3f,(float)Math.sin(tick/4+pos.getZ())*.2f+.8f,.2f,true);pose.popPose();}
        buffers.endBatch();
    }
}
