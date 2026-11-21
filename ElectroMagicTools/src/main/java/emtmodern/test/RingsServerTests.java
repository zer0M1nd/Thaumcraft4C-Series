package emtmodern.test;

import emtmodern.ElectroMagicTools;
import emtmodern.item.*;
import ic2.api.item.ElectricItem;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraftforge.gametest.*;
import top.theillusivec4.curios.api.SlotContext;

@GameTestHolder("emt") @PrefixGameTestTemplate(false)
public class RingsServerTests {
    public static ItemStack stack(String id){return new ItemStack(ElectroMagicTools.CONTENT.get(id).get());}
    @GameTest(templateNamespace="emt",template="test_empty")
    public static void chargingRingsUseRealEuAndIndependentOrderedBudgets(GameTestHelper test){
        var player=GogglesServerTests.player(test);var slot=new SlotContext("ring",player,0,false,true);
        var first=stack("industrial_electric_hoe");var second=stack("thaumium_drill");
        ElectricItem.manager.charge(first,9990,Integer.MAX_VALUE,true,false);player.getInventory().items.set(0,first);player.getInventory().items.set(1,second);
        var ring=stack("inventory_charging_ring");((EnergyChargingRing)ring.getItem()).curioTick(slot,ring);
        test.assertTrue(ElectricItem.manager.getCharge(first)==10000&&ElectricItem.manager.getCharge(second)==22,"One 32 EU budget finishes first item and charges second by 22 EU");
        var boots=stack("electric_traveller_boots");player.getInventory().armor.set(0,boots);var armor=stack("armor_charging_ring");((EnergyChargingRing)armor.getItem()).curioTick(slot,armor);
        test.assertTrue(ElectricItem.manager.getCharge(boots)==32&&ElectricItem.manager.getCharge(second)==22,"Armor ring charges armor only, independently");
        var limit=stack("industrial_electric_hoe");test.assertTrue(EnergyChargingRing.chargeSlots(java.util.List.of(limit),1000)==900&&ElectricItem.manager.getCharge(limit)==100,"Ring respects the 100 EU per-item transfer limit");
        for(String id:java.util.List.of("inventory_charging_ring","armor_charging_ring"))test.assertTrue(test.getLevel().getRecipeManager().byKey(new ResourceLocation("emt",id)).isPresent(),"Original ring infusion recipe loads: "+id);
        test.succeed();
    }
    @GameTest(templateNamespace="emt",template="test_empty")
    public static void oneRingLockEffectsAndOtherModDataSurviveRemoval(GameTestHelper test){
        var player=GogglesServerTests.player(test);player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);var context=new SlotContext("ring",player,0,false,true);var stack=stack("one_ring");var ring=(OneRing)stack.getItem();
        player.getPersistentData().putString("another_mod_progress","preserve");ring.onEquip(context,ItemStack.EMPTY,stack);ring.curioTick(context,stack);
        test.assertTrue(player.isInvisible()&&player.getAbilities().invulnerable&&OneRing.corruption(player)==1&&!ring.canUnequip(context,stack),"Equipping starts invisible/invulnerable with removal locked");
        player.getPersistentData().putInt(OneRing.CORRUPTION,600);test.assertTrue(!ring.canUnequip(context,stack),"Exactly 600 ticks is still locked");player.getPersistentData().putInt(OneRing.CORRUPTION,601);test.assertTrue(ring.canUnequip(context,stack),"Removal becomes available after 600 ticks");
        OneRing.applyEffect(player,"launch");test.assertTrue(player.getDeltaMovement().y==2&&!player.getAbilities().invulnerable,"Original launch removes invulnerability and adds two vertical velocity");
        OneRing.applyEffect(player,"wither");var effect=player.getEffect(net.minecraft.world.effect.MobEffects.WITHER);test.assertTrue(effect!=null&&effect.getDuration()==5000&&effect.getAmplifier()==4,"Original corruption wither duration and potency");
        ring.onUnequip(context,ItemStack.EMPTY,stack);test.assertTrue(!player.isInvisible()&&!player.getAbilities().invulnerable&&!player.getPersistentData().contains(OneRing.CORRUPTION)&&player.getPersistentData().getString("another_mod_progress").equals("preserve"),"Unequip removes ring state without deleting other mod progress");
        test.assertTrue(player.getEffect(net.minecraft.world.effect.MobEffects.CONFUSION).getDuration()==500,"Unequip retains original nausea penalty");test.succeed();
    }
    @GameTest(templateNamespace="emt",template="test_empty")
    public static void equippedOneRingRejectsActualMobTargetSelection(GameTestHelper test){
        var player=GogglesServerTests.player(test);var inventory=top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).resolve().orElseThrow();inventory.reset();var slots=inventory.getStacksHandler("ring").orElseThrow().getStacks();
        var zombie=net.minecraft.world.entity.EntityType.ZOMBIE.create(test.getLevel());zombie.setTarget(player);test.assertTrue(zombie.getTarget()==player,"Mob can normally choose player");
        slots.setStackInSlot(0,stack("one_ring"));zombie.setTarget(player);test.assertTrue(zombie.getTarget()==null,"Actual Curios ring cancels new target through Forge event");
        slots.setStackInSlot(0,ItemStack.EMPTY);zombie.setTarget(player);test.assertTrue(zombie.getTarget()==player,"Removing ring permits targeting again");test.succeed();
    }
    @GameTest(templateNamespace="emt",template="test_empty")
    public static void dungeonRingUsesLiveWeightedLootAndDisableSwitch(GameTestHelper test){
        var table=test.getLevel().getServer().getLootData().getLootTable(new ResourceLocation("minecraft","chests/simple_dungeon"));
        String data=Deserializers.createLootTableSerializer().create().toJson(table);test.assertTrue(data.contains("emt:one_ring"),"Ring entry is injected into actual dungeon table");
        var params=new LootParams.Builder(test.getLevel()).withParameter(LootContextParams.ORIGIN,net.minecraft.world.phys.Vec3.ZERO).create(LootContextParamSets.CHEST);
        boolean disabled=RingConfig.DISABLE_ONE_RING.get();int weight=RingConfig.ONE_RING_WEIGHT.get();
        try{RingConfig.DISABLE_ONE_RING.set(false);RingConfig.ONE_RING_WEIGHT.set(15);int rings=0;for(int seed=1;seed<=128;seed++)for(var drop:table.getRandomItems(params,seed))if(drop.is(stack("one_ring").getItem()))rings+=drop.getCount();test.assertTrue(rings>0,"Seeded real dungeon loot generates original dungeon-only reward");
            RingConfig.DISABLE_ONE_RING.set(true);for(int seed=1;seed<=32;seed++)test.assertTrue(table.getRandomItems(params,seed).stream().noneMatch(drop->drop.is(stack("one_ring").getItem())),"Disable applies without replacing/reloading table");
        }finally{RingConfig.DISABLE_ONE_RING.set(disabled);RingConfig.ONE_RING_WEIGHT.set(weight);}
        test.succeed();
    }
}
