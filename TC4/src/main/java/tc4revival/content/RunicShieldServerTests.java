package tc4revival.content;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Deterministic timing/absorption cases, authored for the later server checkpoint. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class RunicShieldServerTests {
    private RunicShieldServerTests() {}
    @GameTest(template="wand_test_empty")
    public static void actualHurtCallbackHonorsBypassesOverflowAndEmergency(GameTestHelper test) {
        var player=new net.minecraftforge.common.util.FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"RunicAudit"));
        var state=RunicShieldEvents.state(player);
        state.equipment(new RunicShieldState.Equipment(20,0,0,2,1));
        for (int index=1;index<=3;index++) state.recharge(index,0,()->true);
        var sources=player.damageSources();
        for (var source:java.util.List.of(sources.drown(),sources.wither(),sources.fellOutOfWorld(),sources.starve())) {
            var event=new net.minecraftforge.event.entity.living.LivingHurtEvent(player,source,5);
            RunicShieldEvents.hurt(event);
            test.assertTrue(event.getAmount()==5 && state.charge()==3,"The four old bypass sources must neither absorb nor trigger upgrades");
        }
        var fractional=new net.minecraftforge.event.entity.living.LivingHurtEvent(player,sources.fall(),.25f);
        RunicShieldEvents.hurt(fractional);
        test.assertTrue(fractional.getAmount()==0 && state.exactCharge()==2.75f,"Fractional damage preserves fractional shield points");
        var overflow=new net.minecraftforge.event.entity.living.LivingHurtEvent(player,sources.magic(),5);
        RunicShieldEvents.hurt(overflow);
        var regeneration=player.getEffect(net.minecraft.world.effect.MobEffects.REGENERATION);
        test.assertTrue(overflow.getAmount()==2.25f && state.charge()==8,"Emergency restores eight points without erasing current overflow");
        test.assertTrue(regeneration!=null && regeneration.getDuration()==240 && regeneration.getAmplifier()==2,"Healing upgrade applies the original duration and count-based amplifier");
        var repeated=new net.minecraftforge.event.entity.living.LivingHurtEvent(player,sources.magic(),9);
        RunicShieldEvents.hurt(repeated);
        test.assertTrue(repeated.getAmount()==1 && state.charge()==0,"A second immediate break cannot reuse emergency reserve");
        tc4revival.TC4Revival.LOGGER.info("PASS actualHurtCallbackHonorsBypassesOverflowAndEmergency");
        test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void rechargeRequiresPaymentAndStrictWallClockBoundary(GameTestHelper test) {
        var shield=new RunicShieldState(); shield.equipment(new RunicShieldState.Equipment(3,1,0,0,0));
        test.assertTrue(!shield.recharge(1000,2000,()->false) && shield.charge()==0,"Failed vis payment must not grant charge or advance the timer");
        test.assertTrue(shield.recharge(1000,2000,()->true) && shield.charge()==1,"Successful payment restores exactly one point");
        test.assertTrue(!shield.recharge(2500,2000,()->{ throw new AssertionError("No payment at the exact deadline"); }),"Charged upgrade subtracts 500 ms, and deadline comparison remains strict");
        test.assertTrue(shield.recharge(2501,2000,()->true),"The next millisecond may recharge");
        test.assertTrue(shield.recharge(4002,2000,()->true) && shield.charge()==3,"One cycle never restores more than one point");
        test.assertTrue(!shield.recharge(9999,2000,()->{ throw new AssertionError("Full shield must not consume vis"); }),"Full shields stop paying"); tc4revival.TC4Revival.LOGGER.info("PASS rechargeRequiresPaymentAndStrictWallClockBoundary"); test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void fractionalHitsAndOverflowPreserveExactCharge(GameTestHelper test) {
        var shield=new RunicShieldState(); shield.equipment(new RunicShieldState.Equipment(3,0,0,0,0));
        for (int index=1;index<=3;index++) shield.recharge(index,0,()->true);
        var fractional=shield.hurt(.25f,10);
        test.assertTrue(fractional.shielded() && fractional.remainder()==0 && shield.exactCharge()==2.75f,"Fractional hits consume only the actual damage");
        var overflow=shield.hurt(3.5f,11);
        test.assertTrue(overflow.remainder()==.75f && overflow.broken() && shield.charge()==0,"Damage exceeding remaining charge passes through unchanged");
        test.assertTrue(!shield.hurt(1,12).shielded(),"An empty shield cannot suppress damage"); test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void breakUpgradesHaveIndependentCooldownsAndEmergencyDoesNotEraseOverflow(GameTestHelper test) {
        var shield=new RunicShieldState(); shield.equipment(new RunicShieldState.Equipment(20,0,1,2,1)); shield.recharge(1,0,()->true);
        var first=shield.hurt(5,1000);
        test.assertTrue(first.kinetic()==1 && first.healing()==2 && first.emergency()==1 && shield.charge()==8 && !first.broken(),"All three break effects run before emergency reserve prevents the shared pause");
        test.assertTrue(first.remainder()==4,"Emergency reserve does not retroactively absorb this hit's overflow");
        var second=shield.hurt(9,1001);
        test.assertTrue(second.kinetic()==0 && second.healing()==0 && second.emergency()==0 && second.broken(),"Cooldowns suppress repeated break effects");
        shield.recharge(21001,0,()->true); var third=shield.hurt(2,21001);
        test.assertTrue(third.kinetic()==1 && third.healing()==2 && third.emergency()==0,"Kinetic and healing recover after 20 s, emergency needs 60 s");
        shield.recharge(61001,0,()->true); var fourth=shield.hurt(2,61001);
        test.assertTrue(fourth.emergency()==1 && shield.charge()==8,"Emergency reserve becomes available after its separate cooldown"); tc4revival.TC4Revival.LOGGER.info("PASS breakUpgradesHaveIndependentCooldownsAndEmergencyDoesNotEraseOverflow"); test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void equipmentChangesClampExistingChargeWithoutGrantingFreePoints(GameTestHelper test) {
        var shield=new RunicShieldState(); shield.equipment(new RunicShieldState.Equipment(4,0,0,0,0));
        for (int index=1;index<=4;index++) shield.recharge(index,0,()->true);
        shield.equipment(new RunicShieldState.Equipment(1,0,0,0,0)); test.assertTrue(shield.charge()==1,"Removing equipment clamps charge to the reduced maximum");
        shield.equipment(new RunicShieldState.Equipment(10,0,0,0,0)); test.assertTrue(shield.charge()==1,"Equipping larger capacity does not refill it");
        shield.equipment(new RunicShieldState.Equipment(-1,0,0,0,0)); test.assertTrue(shield.charge()==0 && !shield.recharge(100,0,()->true),"Nonpositive legacy totals disable the shield"); tc4revival.TC4Revival.LOGGER.info("PASS equipmentChangesClampExistingChargeWithoutGrantingFreePoints"); test.succeed();
    }
}
