package ttmodern;

import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class HazardArmorServerTests {
    @GameTest(template="wand_test_empty") public static void tinkererAwakenedArmorPpeTag(GameTestHelper test){
        var tag=net.minecraft.tags.ItemTags.create(net.minecraft.resources.ResourceLocation.parse("gtceu:ppe_armor"));
        for(var item:java.util.List.of(Tinkerer.AWAKENED_HELM.get(),Tinkerer.AWAKENED_CHEST.get(),Tinkerer.AWAKENED_LEGS.get(),Tinkerer.AWAKENED_BOOTS.get()))test.assertTrue(new ItemStack(item).is(tag),"Every awakened piece participates in modern hazard protection");test.succeed();
    }
    @GameTest(template="wand_test_empty") public static void tinkererOptionalGregTechProtection(GameTestHelper test) throws Exception {
        if(!net.minecraftforge.fml.ModList.get().isLoaded("gtceu")){test.succeed();return;}
        var api=Class.forName("com.gregtechceu.gtceu.api.data.chemical.material.properties.HazardProperty$ProtectionType");var check=api.getMethod("isProtected",LivingEntity.class);
        var player=new net.minecraftforge.common.util.FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"TTHazard"));
        player.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Tinkerer.AWAKENED_HELM.get()));player.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Tinkerer.AWAKENED_CHEST.get()));player.setItemSlot(EquipmentSlot.LEGS,new ItemStack(Tinkerer.AWAKENED_LEGS.get()));player.setItemSlot(EquipmentSlot.FEET,new ItemStack(Tinkerer.AWAKENED_BOOTS.get()));
        for(String type:java.util.List.of("MASK","HANDS","FULL"))test.assertTrue((Boolean)check.invoke(api.getField(type).get(null),player),"GregTech recognizes awakened armor for "+type);
        player.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);test.assertTrue(!(Boolean)check.invoke(api.getField("FULL").get(null),player),"Partial armor does not incorrectly count as a full protective suit");test.succeed();
    }
}
