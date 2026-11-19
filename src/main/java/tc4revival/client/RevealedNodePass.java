package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import tc4revival.content.AuraNodeBlockEntity;

/** Draw revealed nodes after world geometry; disabling depth alone cannot prevent later overdraw. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public  class RevealedNodePass {
    private record Entry(AuraNodeBlockEntity node,Matrix4f pose,double distance,boolean revealed) {}
    private static final ArrayList<Entry> PENDING=new ArrayList<>();
    private RevealedNodePass() {}
    static void enqueue(AuraNodeBlockEntity node,PoseStack pose,double distance,boolean revealed) {
        PENDING.add(new Entry(node,new Matrix4f(pose.last().pose()),distance,revealed));
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void render(RenderLevelStageEvent event) {
        if(event.getStage()==RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS) { PENDING.clear();return; }
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        var mc=Minecraft.getInstance();var buffers=mc.renderBuffers().bufferSource();
        try {
            if(mc.level==null) return;
            buffers.endBatch();
            for(var entry:PENDING) {
                var node=entry.node();
                if(node.isRemoved() || node.getLevel()!=mc.level) continue;
                var pose=new PoseStack();pose.last().pose().set(entry.pose());
                EnergizedNodeRenderer.renderNode(node.getBlockPos(),node.aspects(),node.nodeType(),node.nodeModifier(),entry.revealed(),entry.revealed(),entry.distance(),pose,buffers);
            }
            buffers.endBatch();
        } finally { PENDING.clear(); }
    }
}
