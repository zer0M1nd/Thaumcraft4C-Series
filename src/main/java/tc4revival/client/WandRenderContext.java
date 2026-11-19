package tc4revival.client;

import java.util.ArrayDeque;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Modern item renderers omit the holder argument that TC4's equipped renderer received. */
@Mod.EventBusSubscriber(modid = "thaumcraft", value = Dist.CLIENT)
public  class WandRenderContext {
    private static final ArrayDeque<LivingEntity> HOLDERS = new ArrayDeque<>();
    private WandRenderContext() {}
    public static LivingEntity holder() { return HOLDERS.peek(); }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void before(RenderLivingEvent.Pre<?, ?> event) { HOLDERS.push(event.getEntity()); }
    @SubscribeEvent public static void after(RenderLivingEvent.Post<?, ?> event) { HOLDERS.removeFirstOccurrence(event.getEntity()); }
    @SubscribeEvent public static void endEntities(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) HOLDERS.clear();
    }
}
