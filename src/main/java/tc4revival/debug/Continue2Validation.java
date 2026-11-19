package tc4revival.debug;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.content.*;

/** Opt-in integration harness for the packaged mod alongside production-only addon jars. */
@Mod.EventBusSubscriber(modid="thaumcraft")
public class Continue2Validation {
    private static final java.util.Map<String,TestFunction> functions=new java.util.LinkedHashMap<>();
    private static final GameTestTicker ticker=new GameTestTicker();
    @SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.ServerTickEvent event){
        if(Boolean.getBoolean("tc4revival.continue2Validation")&&event.phase==net.minecraftforge.event.TickEvent.Phase.END)ticker.tick();
    }
    @SubscribeEvent public static void commands(RegisterCommandsEvent event){
        if(!Boolean.getBoolean("tc4revival.continue2Validation"))return;
        if(functions.isEmpty()){
            for(var type:java.util.List.of(Continue2ServerTests.class,ScanSessionServerTests.class,
                    InventoryAspectDisplayServerTests.class,InfusionMatchingServerTests.class,InfusionEnchantmentServerTests.class,
                    Report0917ModernAspectsServerTests.class,BlockScanCoverageServerTests.class,Report0917ServerTests.class,Report0916ServerTests.class,GolemSystemServerTests.class,GolemPlacementServerTests.class))
                for(var method:type.getDeclaredMethods())if(method.isAnnotationPresent(GameTest.class)){
                    String name=method.getName().toLowerCase(java.util.Locale.ROOT);
                    // GameTest annotation members are not remapped by the release reobfuscator.
                    // These suites all use the same authored structure; pass it explicitly.
                    functions.put(name,new TestFunction("continue2",name,"thaumcraft:wand_test_empty",2000,0,true,helper->{
                        try{method.invoke(null,helper);}catch(java.lang.reflect.InvocationTargetException failure){throw new RuntimeException(failure.getCause());}
                        catch(ReflectiveOperationException failure){throw new RuntimeException(failure);}
                    }));
                }
        }
        event.getDispatcher().register(net.minecraft.commands.Commands.literal("tccontinue2").requires(s->s.hasPermission(4))
                .then(net.minecraft.commands.Commands.argument("name",com.mojang.brigadier.arguments.StringArgumentType.word()).executes(context->{
                    String name=com.mojang.brigadier.arguments.StringArgumentType.getString(context,"name");
                    try{
                        var function=functions.get(name);if(function==null)throw new IllegalArgumentException("No such test: "+name);
                        var info=new GameTestInfo(function,net.minecraft.world.level.block.Rotation.NONE,context.getSource().getLevel());
                        info.addListener(new GameTestListener(){
                            public void testStructureLoaded(GameTestInfo test){}
                            public void testPassed(GameTestInfo test){tc4revival.TC4Revival.LOGGER.info("CONTINUE2_TEST_PASS {}",test.getTestName());}
                            public void testFailed(GameTestInfo test){tc4revival.TC4Revival.LOGGER.error("CONTINUE2_TEST_FAIL "+test.getTestName(),test.getError());}
                        });
                        GameTestRunner.runTest(info,net.minecraft.core.BlockPos.containing(context.getSource().getPosition()),ticker);
                        context.getSource().sendSuccess(()->net.minecraft.network.chat.Component.literal("Started "+name),false);return 1;
                    }catch(Exception failure){ticker.clear();tc4revival.TC4Revival.LOGGER.error("CONTINUE2_TEST_START_FAIL "+name,failure);context.getSource().sendFailure(net.minecraft.network.chat.Component.literal(failure.toString()));return 0;}
                })));
    }
}
