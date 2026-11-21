package ttmodern;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class ComputerServerTests {
    @GameTest(template="wand_test_empty",timeoutTicks=240) public static void tinkererOptionalComputerRunsLuaPeripheral(GameTestHelper test) throws Exception {
        if(!net.minecraftforge.fml.ModList.get().isLoaded("computercraft")){test.succeed();return;}ComputerRuntimeChecks.run(test,false);
    }
    @GameTest(template="wand_test_empty",timeoutTicks=240) public static void tinkererOptionalTurtleEquipsAndClearsGas(GameTestHelper test) throws Exception {
        if(!net.minecraftforge.fml.ModList.get().isLoaded("computercraft")){test.succeed();return;}ComputerRuntimeChecks.run(test,true);
    }
    @GameTest(template="wand_test_empty") public static void tinkererOptionalTransvectorPeripherals(GameTestHelper test) throws Exception {
        if(!net.minecraftforge.fml.ModList.get().isLoaded("computercraft")){test.succeed();return;}
        MachinePeripheralChecks.transvector(test);
    }
    @GameTest(template="wand_test_empty") public static void tinkererOptionalMachinePeripherals(GameTestHelper test) throws Exception {
        if(!net.minecraftforge.fml.ModList.get().isLoaded("computercraft")){test.succeed();return;}
        MachinePeripheralChecks.run(test);
    }
}
