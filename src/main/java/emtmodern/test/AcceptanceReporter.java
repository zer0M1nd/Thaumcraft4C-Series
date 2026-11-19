package emtmodern.test;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid="emt")
public class AcceptanceReporter {
    @SubscribeEvent public static void started(ServerStartedEvent event){
        if(!Boolean.getBoolean("emt.acceptance"))return;
        GlobalTestReporter.replaceWith(new TestReporter(){
            public void onTestSuccess(GameTestInfo test){com.mojang.logging.LogUtils.getLogger().info("EMT_ACCEPT PASS {}",test.getTestName());}
            public void onTestFailed(GameTestInfo test){com.mojang.logging.LogUtils.getLogger().error("EMT_ACCEPT FAIL "+test.getTestName(),test.getError());}
        });
    }
}
