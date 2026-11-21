package emtmodern.test;
import emtmodern.ElectroMagicTools;
import emtmodern.focus.*;
import emtmodern.energy.*;
import ic2.api.item.ElectricItem;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.content.*;
@GameTestHolder("emt") @PrefixGameTestTemplate(false)
public class ChargingServerTests {
    @GameTest(templateNamespace="emt",template="test_empty")
    public static void chargingFocusUsesArmorThenInventoryTransferLimits(GameTestHelper test){
        var player=GogglesServerTests.player(test);player.getAbilities().instabuild=false;var wand=TCItems.CASTING_WAND.get();var stack=wand.create(WandParts.Rod.GREATWOOD,WandParts.Cap.GOLD,false,true);var focus=(ChargeFocus)ElectroMagicTools.CONTENT.get("charge_focus").get();var upgraded=new ItemStack(focus);focus.applyUpgrade(upgraded,FocusUpgrade.POTENCY,1);WandFocusItem.attach(stack,upgraded);player.setItemInHand(InteractionHand.MAIN_HAND,stack);
        var helmet=new ItemStack(ElectroMagicTools.CONTENT.get("electric_goggles").get());player.setItemSlot(EquipmentSlot.HEAD,helmet);var battery=new ItemStack(ForgeRegistries.ITEMS.getValue(ResourceLocation.parse("ic2:energy_crystal")));player.getInventory().setItem(5,battery);
        var primal=tc4revival.aspect.TCAspects.catalog().require("ordo");int before=wand.vis(stack,primal);focus.cast(stack,test.getLevel(),player,InteractionHand.MAIN_HAND,null);
        test.assertTrue(ElectricItem.manager.getCharge(helmet)==10&&ElectricItem.manager.getCharge(battery)==54,"64 EU respects helmet 10-EU limit then charges inventory; original focus potency does not increase pulse");test.assertTrue(before-wand.vis(stack,primal)==10,"Pulse spends original ten centivis of each primal");test.succeed();
    }
    @GameTest(templateNamespace="emt",template="test_empty")
    public static void wandFocusUsesLeggingsFourSequentialPayments(GameTestHelper test){
        var player=GogglesServerTests.player(test);var wand=TCItems.CASTING_WAND.get();var stack=wand.create(WandParts.Rod.GREATWOOD,WandParts.Cap.GOLD,false,false);var focus=(WandChargingFocus)ElectroMagicTools.CONTENT.get("wand_charge_focus").get();WandFocusItem.attach(stack,new ItemStack(focus));player.setItemInHand(InteractionHand.MAIN_HAND,stack);
        var leggings=new ItemStack(ForgeRegistries.ITEMS.getValue(ResourceLocation.parse("ic2:nano_leggings")));player.setItemSlot(EquipmentSlot.LEGS,leggings);ElectricItem.manager.charge(leggings,45000,4,true,false);focus.usingTick(stack,player,100);
        test.assertTrue(ElectricItem.manager.getCharge(leggings)==5000,"Four original quarter payments total 40000 EU");for(var aspect:tc4revival.aspect.TCAspects.catalog().primals())test.assertTrue(wand.vis(stack,aspect)==100,"Cycle adds one whole vis to all six primals");
        ElectricItem.manager.charge(leggings,20000,4,true,false);focus.usingTick(stack,player,100);test.assertTrue(ElectricItem.manager.getCharge(leggings)==5000,"Incomplete cycle retains original two paid quarters");test.assertTrue(wand.vis(stack,tc4revival.aspect.TCAspects.catalog().require("ordo"))==100,"Partial payment produces no vis");test.succeed();
    }
    @GameTest(templateNamespace="emt",template="test_empty")
    public static void industrialWandChargerEuAndInventoryPersistence(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new net.minecraft.core.BlockPos(3,2,3));level.setBlockAndUpdate(pos,ElectroMagicTools.WAND_CHARGER.get().defaultBlockState());var machine=(WandChargerEntity)level.getBlockEntity(pos);var wand=TCItems.CASTING_WAND.get();var stack=wand.create(WandParts.Rod.GREATWOOD,WandParts.Cap.GOLD,false,false);stack.setHoverName(net.minecraft.network.chat.Component.literal("Charging wand"));machine.setItem(0,stack);machine.energy.injectEnergy(net.minecraft.core.Direction.UP,75000,2048);WandChargerEntity.tick(level,pos,machine.getBlockState(),machine);
        test.assertTrue(machine.energy.getEnergyStored()==25000,"Industrial sink consumes author 50000 EU per cycle");for(var aspect:tc4revival.aspect.TCAspects.catalog().primals())test.assertTrue(wand.vis(stack,aspect)==100,"Station charges all six aspects simultaneously");
        var saved=machine.saveWithoutMetadata();var copy=new WandChargerEntity(pos,machine.getBlockState());copy.load(saved);test.assertTrue(copy.energy.getEnergyStored()==25000&&copy.getItem(0).hasCustomHoverName(),"Maintenance fix persists real IC2 sink EU and full wand NBT");
        machine.setItem(0,wand.create(WandParts.Rod.GREATWOOD,WandParts.Cap.GOLD,false,true));machine.energy.setEnergyStored(75000);WandChargerEntity.tick(level,pos,machine.getBlockState(),machine);test.assertTrue(machine.energy.getEnergyStored()==75000,"GTNH maintenance fix prevents charged wand wasting EU");
        var menu=new WandChargerMenu(0,GogglesServerTests.player(test).getInventory(),machine);test.assertTrue(menu.slots.size()==37&&menu.slots.get(36).x==80&&menu.slots.get(36).y==35,"Original 176x165 GUI has wand at 80,35 and all inventory slots");
        var cap=machine.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER,net.minecraft.core.Direction.DOWN).orElseThrow(()->new IllegalStateException("No inventory capability"));test.assertTrue(cap.getSlots()==1,"All-face original inventory is available to automation");test.succeed();
    }
}
