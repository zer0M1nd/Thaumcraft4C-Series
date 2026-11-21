package tc4revival.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Rebuild cached chunk meshes when the local glow setting changes. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public  class TaintVisualPreference {
    private static Boolean previous;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;
        var mc=net.minecraft.client.Minecraft.getInstance();
        if(mc.level==null){previous=null;return;}
        boolean value=tc4revival.config.TCConfig.CLIENT.glowingTaint.get();
        if(previous!=null&&previous!=value)mc.levelRenderer.allChanged();
        previous=value;
    }
}
