package technomancy;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.*;

@GameTestHolder("technomancy") @PrefixGameTestTemplate(false)
public class TechnomancyIntegrationTests {
    private static MachineEntity machine(GameTestHelper h,MachineKind k,int x){var p=new BlockPos(x,3,6);h.setBlock(p,TechRegistry.MACHINES.get(k).get());return (MachineEntity)h.getBlockEntity(p);}
    @GameTest(template="empty") public static void optionalBotaniaInstalledOrAbsent(GameTestHelper h){
        var fab=machine(h,MachineKind.MANA_FABRICATOR,2);fab.energy=1000000;
        if(!MachineKind.MANA_FABRICATOR.enabled()) {fab.tick();h.assertTrue(fab.mana==0&&fab.energy==1000000,"Botania absent: no conversion");h.succeed();return;}
        BotaniaChecks.run(h,fab);
    }
    @GameTest(template="empty") public static void optionalBloodMagicInstalledOrAbsent(GameTestHelper h){
        var fab=machine(h,MachineKind.BLOOD_FABRICATOR,2);fab.energy=1000000;
        if(!MachineKind.BLOOD_FABRICATOR.enabled()){fab.tick();h.assertTrue(fab.fluid.isEmpty()&&fab.energy==1000000,"Blood Magic absent: no conversion");h.succeed();return;}
        BloodChecks.run(h,fab);h.succeed();
    }
    private static class BotaniaChecks {
        static void run(GameTestHelper h,MachineEntity fab){
            fab.tick();h.assertTrue(fab.mana==100&&fab.energy==0&&fab instanceof vazkii.botania.api.mana.ManaPool,"FE fabricator is a real mana pool");
            var dynamo=machine(h,MachineKind.HIPPIE_DYNAMO,4);dynamo.redstone=0;dynamo.tick();h.assertTrue(fab.mana==0&&dynamo.energy==80&&dynamo.mana==80&&!(dynamo instanceof vazkii.botania.api.mana.ManaPool),"Dynamo drains pools, remains a receiver and produces energy");
            var exchanger=machine(h,MachineKind.MANA_EXCHANGER,10);exchanger.redstone=0;exchanger.energy=1000;
            h.setBlock(new BlockPos(10,4,6),net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(new net.minecraft.resources.ResourceLocation("botania","mana_pool")));
            // Real pools initialize capacity on their first server tick. Deposit after that tick.
            h.runAfterDelay(2,()->{
            var pool=(vazkii.botania.api.mana.ManaPool)h.getBlockEntity(new BlockPos(10,4,6));
            h.assertTrue(pool.getMaxMana()>0,"Pool capacity initialized by Botania ticker");pool.receiveMana(1000);exchanger.tick();
            h.assertTrue(pool.getCurrentMana()==0&&exchanger.fluid.getFluidAmount()==1&&exchanger.energy==0,"1000 mana -> 1 mB for 1000 FE: mana="+pool.getCurrentMana()+", fluid="+exchanger.fluid.getFluidAmount()+", FE="+exchanger.energy);
            exchanger.mode=1;exchanger.energy=1000;exchanger.tick();h.assertTrue(pool.getCurrentMana()==1000&&exchanger.fluid.isEmpty()&&exchanger.energy==0,"Reverse conversion has no duplication");
            var fluid=exchanger.getCapability(ForgeCapabilities.FLUID_HANDLER,Direction.NORTH).orElseThrow(IllegalStateException::new);
            h.assertTrue(!exchanger.getCapability(ForgeCapabilities.FLUID_HANDLER,Direction.UP).isPresent()&&fluid.fill(new FluidStack(TechFluids.SOURCE.get(),10),FluidAction.EXECUTE)==10&&fluid.drain(1,FluidAction.EXECUTE).isEmpty(),"Recovery mode receives fluid only, never top");
            h.succeed();});
        }
    }
    private static class BloodChecks {
        static void run(GameTestHelper h,MachineEntity fab){
            fab.tick();h.assertTrue(fab.fluid.getFluidAmount()==200&&fab.energy==0,"Blood fabricator converts one million FE to 200 mB");
            var dyn=machine(h,MachineKind.BLOOD_DYNAMO,5);dyn.redstone=0;var cap=dyn.getCapability(ForgeCapabilities.FLUID_HANDLER,Direction.UP).orElseThrow(IllegalStateException::new);
            h.assertTrue(!dyn.getCapability(ForgeCapabilities.FLUID_HANDLER,Direction.NORTH).isPresent(),"Energy output does not accept fluid");
            var fluid=fab.fluid.drain(100,FluidAction.EXECUTE);cap.fill(fluid,FluidAction.EXECUTE);dyn.tick();h.assertTrue(dyn.energy==80&&dyn.fuel==399&&dyn.fluid.isEmpty(),"100 mB fuels 400 generation ticks");
            var processor=machine(h,MachineKind.SANGUINE_INFUSER,9);processor.owner=java.util.UUID.randomUUID();var network=wayoftime.bloodmagic.util.helper.NetworkHelper.getSoulNetwork(processor.owner);network.setCurrentEssence(60001);processor.inventory.setStackInSlot(0,new ItemStack(Items.RAW_IRON));
            for(int i=0;i<60;i++)OreProcessing.tick(processor);
            h.assertTrue(network.getCurrentEssence()==1&&processor.inventory.getStackInSlot(1).getCount()==1,"First blood pass debits 60000 LP from owner network");
        }
    }
}
