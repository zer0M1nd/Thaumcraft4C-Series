package tc4revival.content;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import tc4revival.TC4Revival;
import tc4revival.aspect.TCAspects;
import tc4revival.config.TCConfig;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class RunicIntegrationServerTests {
    private static FakePlayer player(GameTestHelper test,String name) {
        var player=new FakePlayer(test.getLevel(),new GameProfile(UUID.randomUUID(),name));
        player.tickCount=1;
        return player;
    }
    private static void tick(FakePlayer player) { RunicShieldEvents.tick(new LivingEvent.LivingTickEvent(player)); }
    private static ItemStack equip(FakePlayer player) {
        var goggles=new ItemStack(TCItems.GOGGLES.get());
        goggles.getOrCreateTag().putByte("RS.HARDEN",(byte)3);
        player.setItemSlot(EquipmentSlot.HEAD,goggles);
        player.doTick(); // Install attributes while no donor wand can recharge the shield.
        player.tickCount=40; // Keep the following direct callbacks at an equipment-refresh checkpoint.
        var wand=TCItems.CASTING_WAND.get().create(WandParts.Rod.WOOD,WandParts.Cap.GOLD,false,true);
        player.getInventory().setItem(35,wand);
        RunicShieldEvents.equipmentChanged(player);
        return wand;
    }
    @GameTest(template="wand_test_empty")
    public static void realVisPaymentAndSharedPlayerDelayFollowLegacyTicks(GameTestHelper test) {
        test.assertTrue(TCConfig.SERVER.shieldWaitTicks()==80 && TCConfig.SERVER.shieldCostCentiVis()==50,"This checkpoint uses the legacy default server configuration");
        var neutral=player(test,"RunicNeutral");
        var owner=player(test,"RunicOwner");var wand=equip(owner);
        var waiting=player(test,"RunicWaiting");var waitingWand=equip(waiting);
        var shortfall=player(test,"RunicShortfall");var missing=equip(shortfall);missing.getOrCreateTag().putInt("terra",0);
        // Complete real equipment ticks before measuring exactly eighty shield callbacks.
        for(int i=0;i<80;i++) tick(neutral);
        var air=TCAspects.catalog().require("aer");var earth=TCAspects.catalog().require("terra");
        int before=TCItems.CASTING_WAND.get().vis(wand,air);
        tick(owner);
        test.assertTrue(RunicShieldEvents.state(owner).charge()==1,"Actual inventory payment charges one point");
        test.assertTrue(TCItems.CASTING_WAND.get().vis(wand,air)==before-47 && TCItems.CASTING_WAND.get().vis(wand,earth)==before-47,"Both primals pay 50 centi-vis with goggles five percent discount and integer truncation");
        RunicShieldEvents.hurt(new LivingHurtEvent(owner,owner.damageSources().magic(),1));
        tick(waiting);
        for(int i=1;i<80;i++) tick(neutral);
        test.assertTrue(RunicShieldEvents.state(waiting).charge()==0 && TCItems.CASTING_WAND.get().vis(waitingWand,air)==before,"Eighty player callbacks decrement the shared wait without paying or charging");
        tick(waiting);
        test.assertTrue(RunicShieldEvents.state(waiting).charge()==1 && TCItems.CASTING_WAND.get().vis(waitingWand,air)==before-47,"A different player can exhaust the shared pause; the next callback recharges");
        tick(shortfall);
        test.assertTrue(RunicShieldEvents.state(shortfall).charge()==0 && TCItems.CASTING_WAND.get().vis(missing,air)==before,"Missing earth must not debit air or grant a shield point");
        missing.getOrCreateTag().putInt("terra",47);tick(shortfall);
        test.assertTrue(RunicShieldEvents.state(shortfall).charge()==1 && TCItems.CASTING_WAND.get().vis(missing,earth)==0,"Failed payment did not advance the wall-clock deadline");
        TC4Revival.LOGGER.info("PASS realVisPaymentAndSharedPlayerDelayFollowLegacyTicks");test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void kineticBreakCreatesOneNonTerrainExplosionAndHonorsCooldown(GameTestHelper test) {
        var player=player(test,"RunicKinetic");var origin=test.absolutePos(new net.minecraft.core.BlockPos(3,5,3));
        player.moveTo(origin.getCenter());var state=RunicShieldEvents.state(player);
        state.equipment(new RunicShieldState.Equipment(10,0,1,0,0));state.recharge(1,0,()->true);
        var count=new int[1];
        Consumer<ExplosionEvent.Start> listener=event->{
            if(event.getLevel()!=test.getLevel() || event.getExplosion().getDirectSourceEntity()!=player) return;
            count[0]++;
            test.assertTrue(!event.getExplosion().interactsWithBlocks(),"Kinetic shield does not destroy terrain");
            test.assertTrue(event.getExplosion().getPosition().distanceToSqr(player.position().add(0,player.getBbHeight()/2,0))<1e-8,"Explosion is centered halfway up the wearer");
        };
        MinecraftForge.EVENT_BUS.addListener(listener);
        try {
            RunicShieldEvents.hurt(new LivingHurtEvent(player,player.damageSources().magic(),2));
            test.assertTrue(count[0]==1 && state.charge()==0,"The first break triggers one actual explosion");
            state.recharge(2,0,()->true);
            RunicShieldEvents.hurt(new LivingHurtEvent(player,player.damageSources().magic(),2));
            test.assertTrue(count[0]==1,"The next break is suppressed by kinetic cooldown");
        } finally { MinecraftForge.EVENT_BUS.unregister(listener); }
        TC4Revival.LOGGER.info("PASS kineticBreakCreatesOneNonTerrainExplosionAndHonorsCooldown");test.succeed();
    }
}
