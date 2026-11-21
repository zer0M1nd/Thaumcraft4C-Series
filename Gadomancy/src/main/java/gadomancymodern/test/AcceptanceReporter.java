package gadomancymodern.test;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid="gadomancy")
public class AcceptanceReporter {
    @SubscribeEvent public static void started(ServerStartedEvent event){
        if(!Boolean.getBoolean("gadomancy.acceptance"))return;
        GlobalTestReporter.replaceWith(new TestReporter(){
            public void onTestSuccess(GameTestInfo test){com.mojang.logging.LogUtils.getLogger().info("GAD_ACCEPT PASS {}",test.getTestName());}
            public void onTestFailed(GameTestInfo test){com.mojang.logging.LogUtils.getLogger().error("GAD_ACCEPT FAIL "+test.getTestName(),test.getError());}
        });
    }
}
