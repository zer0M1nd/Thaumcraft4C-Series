package emtmodern.test;
import emtmodern.ElectroMagicTools;
import emtmodern.item.ElectricTravellerBoots;
import ic2.api.item.ElectricItem;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.*;
@GameTestHolder("emt") @PrefixGameTestTemplate(false)
public class BootsServerTests {
    public static class TestPlayer extends net.minecraftforge.common.util.FakePlayer {
        public TestPlayer(net.minecraft.server.level.ServerLevel level){super(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"EMTBoots"));}
        public float airSpeed(){return getFlyingSpeed();}
    }
    @GameTest(templateNamespace="emt",template="test_empty")
    public static void movementHookToggleAndEnergyFall(GameTestHelper test){
        var player=new TestPlayer(test.getLevel());player.zza=1;
        for(String kind:java.util.List.of("electric","nano","quantum")){
            var stack=new ItemStack(ElectroMagicTools.CONTENT.get(kind+"_traveller_boots").get());var boots=(ElectricTravellerBoots)stack.getItem();player.setItemSlot(EquipmentSlot.FEET,stack);
            boots.updateToggle(stack,true);boots.updateToggle(stack,true);test.assertTrue(ElectricTravellerBoots.boost(stack),"Held mode chord toggles once");
            test.assertTrue(Math.abs(player.airSpeed()-boots.jump())<.0001,"Actual modern air accessor is hooked for powered boots");
            player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);ElectricTravellerBoots.jump(new net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent(player));test.assertTrue(Math.abs(player.getDeltaMovement().y-boots.jump())<.0001,"Uncharged jump bonus remains original");
            var fall=new net.minecraftforge.event.entity.living.LivingFallEvent(player,30,1);ElectricTravellerBoots.fall(fall);test.assertTrue(!fall.isCanceled(),"Empty boots do not prevent fall damage");
            ElectricItem.manager.charge(stack,200,Integer.MAX_VALUE,true,false);fall=new net.minecraftforge.event.entity.living.LivingFallEvent(player,30,1);ElectricTravellerBoots.fall(fall);
            test.assertTrue(fall.isCanceled()&&ElectricItem.manager.getCharge(stack)==100,"Original shared 100 EU fall protection retained");
            boots.updateToggle(stack,false);boots.updateToggle(stack,true);test.assertTrue(!ElectricTravellerBoots.boost(stack),"Second key edge toggles off");
        }test.succeed();
    }
}
