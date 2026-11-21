package conciliummodern;

import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder("thaumicconcilium") @PrefixGameTestTemplate(false)
public final class ConciliumDyesServerTests {
    @GameTest(template="empty") public static void optionalEquipmentBridgeAndActualMobLoadouts(GameTestHelper test){
        if(!ModList.get().isLoaded("thaumicdyes")){
            test.assertTrue(!ConciliumDyesIntegration.available(),"Absent Dyes remains disabled");
            for(String role:java.util.List.of("paladin","archer","ranger"))
                for(var slot:EquipmentSlot.values())test.assertTrue(ConciliumDyesIntegration.equipment(role,slot).isEmpty(),"Absent host returns no invented equipment");
            System.out.println("CONCILIUM_BRANCH_UNAVAILABLE dyes: present-host equipment and mobs not run");test.succeed();return;
        }
        test.assertTrue(ConciliumDyesIntegration.available(),"Real Dyes API attached during common setup");
        for(String role:java.util.List.of("paladin","archer","ranger")){
            for(var slot:EquipmentSlot.values()){
                ItemStack stack=ConciliumDyesIntegration.equipment(role,slot);
                boolean supported=slot==EquipmentSlot.HEAD||(!role.equals("paladin")&&(slot==EquipmentSlot.CHEST||slot==EquipmentSlot.LEGS));
                test.assertTrue(stack.isEmpty()!=supported,"Original supported slots: "+role+"/"+slot);
                if(!supported)continue;
                test.assertTrue(ForgeRegistries.ITEMS.getKey(stack.getItem()).getNamespace().equals("thaumicdyes"),"Host supplies real Dyes registry item");
                test.assertTrue(stack.getItem() instanceof net.minecraft.world.item.ArmorItem armor&&armor.getEquipmentSlot()==slot,"Host equipment matches body slot");
                stack.getOrCreateTag().putBoolean("ConciliumCopyProbe",true);
                var next=ConciliumDyesIntegration.equipment(role,slot);
                test.assertTrue(!next.hasTag()||!next.getTag().contains("ConciliumCopyProbe"),"Each request owns an independent stack");
            }
        }
        for(var type:java.util.List.of(CrimsonContent.ARCHER.get(),CrimsonContent.RANGER.get())){
            var mob=type.create(test.getLevel());String role=mob.archer()?"archer":"ranger";
            mob.populateDefaultEquipmentSlots(test.getLevel().random,test.getLevel().getCurrentDifficultyAt(test.absolutePos(new net.minecraft.core.BlockPos(1,1,1))));
            for(var slot:java.util.List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS))
                test.assertTrue(ItemStack.isSameItemSameTags(mob.getItemBySlot(slot),ConciliumDyesIntegration.equipment(role,slot)),"Real brigade loadout uses Dyes "+role+"/"+slot);
        }
        test.assertTrue(ConciliumDyesIntegration.equipment("unknown",EquipmentSlot.HEAD).isEmpty(),"Unknown role has no equipment");test.succeed();
    }
}
