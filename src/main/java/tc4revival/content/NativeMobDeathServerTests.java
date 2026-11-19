package tc4revival.content;

import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.TCAspects;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class NativeMobDeathServerTests {
    @GameTest(template="wand_test_empty")
    public static void wispAndFireBatActualDeathLoot(GameTestHelper test) {
        var level=test.getLevel();var rule=level.getGameRules().getRule(GameRules.RULE_DOMOBLOOT);boolean original=rule.get();
        try {
            rule.set(true,level.getServer());
            for(var aspect:TCAspects.catalog().all()) {
                var wisp=TCEntities.WISP.get().create(level);wisp.setAspectTag(aspect.tag());
                var drops=kill(test,wisp);
                test.assertTrue(drops.size()==1&&drops.get(0).is(TCItems.WISP_ESSENCE.get())&&drops.get(0).getCount()==1,"One essence from actual wisp death: "+aspect.tag());
                var stored=TCItems.WISP_ESSENCE.get().storedAspects(drops.get(0));
                test.assertTrue(stored.size()==1&&stored.amount(aspect)==2,"The dropped essence retains exactly two units of its wisp aspect");
            }
            var unknown=TCEntities.WISP.get().create(level);unknown.setAspectTag("missing_aspect");test.assertTrue(kill(test,unknown).isEmpty(),"Unknown wisp type does not manufacture an essence");
            boolean ordinaryDrop=false, lootingBonus=false;
            for(int looting=0;looting<=3;looting++) for(int seed=0;seed<32;seed++) for(boolean summoned:new boolean[]{false,true}) {
                var bat=TCEntities.FIRE_BAT.get().create(level);var data=new net.minecraft.nbt.CompoundTag();data.putByte("BatFlags",(byte)(summoned?2:0));bat.readAdditionalSaveData(data);bat.setHealth(bat.getMaxHealth());bat.getRandom().setSeed(seed);
                var allDrops=kill(test,bat,looting);
                // Other installed mods may add their own rewards; this contract covers native gunpowder.
                var drops=allDrops.stream().filter(stack->stack.is(Items.GUNPOWDER)).toList();int count=drops.stream().mapToInt(ItemStack::getCount).sum();
                if(allDrops.size()!=drops.size())tc4revival.TC4Revival.LOGGER.info("Fire bat external drop diagnostic: summoned={} drops={}",summoned,allDrops);
                test.assertTrue(drops.stream().allMatch(stack->stack.is(Items.GUNPOWDER)&&!stack.hasTag())&&count>=0&&count<=2+looting,"Fire bat native loot stacks retain the legacy total looting bound; looting="+looting+" seed="+seed+" summoned="+summoned+" drops="+drops.stream().map(s->s.save(new net.minecraft.nbt.CompoundTag()).toString()).toList());
                if(summoned)test.assertTrue(drops.isEmpty(),"Summoned bats cannot yield gunpowder");else { ordinaryDrop|=count>0; lootingBonus|=looting>0&&count>2; }
            }
            test.assertTrue(ordinaryDrop&&lootingBonus,"Actual death pipeline produces both ordinary gunpowder and looting bonuses");
            rule.set(false,level.getServer());
            var disabled=TCEntities.WISP.get().create(level);disabled.setAspectTag("aer");
            test.assertTrue(kill(test,disabled).isEmpty(),"doMobLoot suppresses wisp death drops");
            test.assertTrue(kill(test,TCEntities.FIRE_BAT.get().create(level)).isEmpty(),"doMobLoot suppresses bat death drops");
        } finally {rule.set(original,level.getServer());}
        tc4revival.TC4Revival.LOGGER.info("wispAndFireBatActualDeathLoot PASS: every aspect, invalid aspect, natural/summoned bats, looting 0-3, native stacks and doMobLoot");test.succeed();
    }
    private static List<ItemStack> kill(GameTestHelper test,LivingEntity mob) { return kill(test,mob,0); }
    private static List<ItemStack> kill(GameTestHelper test,LivingEntity mob,int looting) {
        var result=new ArrayList<ItemStack>();
        Consumer<LivingDropsEvent> capture=event->{if(event.getEntity()==mob){for(var item:event.getDrops())result.add(item.getItem().copy());event.setCanceled(true);}};
        Consumer<net.minecraftforge.event.entity.living.LootingLevelEvent> lootLevel=event->{if(event.getEntity()==mob) event.setLootingLevel(looting);};
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST,capture);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST,lootLevel);
        try {
            mob.moveTo(test.absolutePos(new BlockPos(3,3,3)).getCenter());
            test.assertTrue(mob.hurt(test.getLevel().damageSources().genericKill(),10000)&&mob.isDeadOrDying(),"Actual fatal damage reaches the death pipeline");
        } finally {MinecraftForge.EVENT_BUS.unregister(capture);MinecraftForge.EVENT_BUS.unregister(lootLevel);mob.discard();}
        return result;
    }
}
