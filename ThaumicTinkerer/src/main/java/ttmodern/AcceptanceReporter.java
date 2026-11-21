package ttmodern;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Explicit opt-in for local acceptance runs; vanilla omits successful tests from its log. */
@Mod.EventBusSubscriber(modid = Tinkerer.ID)
public class AcceptanceReporter {
    /** Forge disables automatic GameTest registration in production, so release-JAR checks opt in. */
    @SubscribeEvent public static void commands(net.minecraftforge.event.RegisterCommandsEvent event) {
        if (!Boolean.getBoolean("ttmodern.gasGameTests") || !net.minecraftforge.fml.loading.FMLLoader.isProduction()) return;
        if (GameTestRegistry.getAllTestFunctions().stream().noneMatch(test -> test.getTestName().equals("personallightneverinterceptsworkstationpicking"))) {
            // Annotation element names are not remapped in release JARs; keep template IDs explicit.
            GameTestRegistry.getAllTestFunctions().add(new TestFunction("tt_gas", "personallightneverinterceptsworkstationpicking",
                "thaumcraft:wand_test_empty", 100, 0, true, GasFunnelServerTests::personalLightNeverInterceptsWorkstationPicking));
            GameTestRegistry.getAllTestFunctions().add(new TestFunction("tt_gas", "tinkerergasspreadlightandcleanup",
                "thaumcraft:wand_test_empty", 100, 0, true, GasFunnelServerTests::tinkererGasSpreadLightAndCleanup));
        }
        // Vanilla /test run requires a player; release regression runs from the server console.
        event.getDispatcher().register(net.minecraft.commands.Commands.literal("ttgascheck")
            .requires(source -> source.hasPermission(4))
            .then(net.minecraft.commands.Commands.argument("name", com.mojang.brigadier.arguments.StringArgumentType.word())
                .executes(context -> {
                    var name = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "name");
                    try {
                        var test = GameTestRegistry.getTestFunction(name);
                        var info = new GameTestInfo(test, test.getRotation(), context.getSource().getLevel());
                        GameTestRunner.runTest(info, net.minecraft.core.BlockPos.containing(context.getSource().getPosition()), GameTestTicker.SINGLETON);
                        return 1;
                    } catch (RuntimeException error) {
                        com.mojang.logging.LogUtils.getLogger().error("TT_ACCEPT FAIL " + name, error);
                        return 0;
                    }
                })));
    }
    @SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (event.phase == net.minecraftforge.event.TickEvent.Phase.END && Boolean.getBoolean("ttmodern.gasGameTests")
            && net.minecraftforge.fml.loading.FMLLoader.isProduction()) GameTestTicker.SINGLETON.tick();
    }
    @SubscribeEvent public static void started(ServerStartedEvent event) {
        if (System.getProperty("ttmodern.workspace") == null && !Boolean.getBoolean("ttmodern.gasGameTests")) return;
        GlobalTestReporter.replaceWith(new TestReporter() {
            private final org.slf4j.Logger log = com.mojang.logging.LogUtils.getLogger();
            public void onTestSuccess(GameTestInfo test) { log.info("TT_ACCEPT PASS {}", test.getTestName()); }
            public void onTestFailed(GameTestInfo test) { log.error("TT_ACCEPT FAIL " + test.getTestName(), test.getError()); }
        });
    }
    private AcceptanceReporter() {}
}
