package ttmodern;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
import top.theillusivec4.curios.api.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class TalismanServerTests {
    private static net.minecraftforge.common.util.FakePlayer player(GameTestHelper test){var player=new net.minecraftforge.common.util.FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"TTTalisman"));player.moveTo(test.absolutePos(new BlockPos(2,2,2)).getCenter());return player;}
    private static SlotContext equip(net.minecraft.world.entity.player.Player player,ItemStack stack){var inventory=CuriosApi.getCuriosInventory(player).resolve().orElseThrow();inventory.reset();var slots=inventory.getStacksHandler("necklace").orElseThrow().getStacks();slots.setStackInSlot(0,stack);return new SlotContext("necklace",player,0,false,true);}
    private static void tick(SlotContext context,ItemStack stack){CuriosApi.getCurio(stack).resolve().orElseThrow().curioTick(context);}
    private static ExperienceOrb orb(GameTestHelper test,net.minecraft.world.entity.player.Player player,int value,int count,double offset){var orb=new ExperienceOrb(test.getLevel(),player.getX()+offset,player.getY(),player.getZ(),value);var tag=new CompoundTag();orb.addAdditionalSaveData(tag);tag.putInt("Count",count);orb.readAdditionalSaveData(tag);test.getLevel().addFreshEntity(orb);return orb;}
    @GameTest(template="wand_test_empty")
    public static void tinkererXpTalismanMergedOrbsCapacityAndPersistence(GameTestHelper test){
        var player=player(test);var stack=new ItemStack(Tinkerer.XP_TALISMAN.get());var context=equip(player,stack);stack.getOrCreateTag().putBoolean("enabled",true);
        var merged=orb(test,player,7,5,1);var outside=orb(test,player,3,1,4);var small=orb(test,player,2,1,-1);
        try{
            tick(context,stack);test.assertTrue(XpTalismanItem.xp(stack)==37&&!merged.isAlive()&&!small.isAlive()&&outside.isAlive(),"Every absorbed orb counted once, merged denominations preserved, range three");
            XpTalismanItem.setXp(stack,1486);var overflow=orb(test,player,7,5,1);tick(context,stack);var remaining=new CompoundTag();overflow.addAdditionalSaveData(remaining);
            test.assertTrue(XpTalismanItem.xp(stack)==1500&&!XpTalismanItem.active(stack)&&remaining.getInt("Count")==3&&overflow.isAlive(),"Capacity consumes two denominations and keeps three in the world");
            var saved=ItemStack.of(stack.save(new CompoundTag()));test.assertTrue(XpTalismanItem.xp(saved)==1500&&!XpTalismanItem.active(saved),"XP and toggle survive stack persistence");
            tick(context,stack);test.assertTrue(overflow.isAlive(),"Disabled full talisman does not consume remaining XP");overflow.discard();test.succeed();
        }finally{merged.discard();outside.discard();small.discard();}
    }
    @GameTest(template="wand_test_empty")
    public static void tinkererXpTalismanBottlesAndToggle(GameTestHelper test){
        var player=player(test);var stack=new ItemStack(Tinkerer.XP_TALISMAN.get());player.setItemInHand(InteractionHand.MAIN_HAND,stack);XpTalismanItem.setXp(stack,20);
        Tinkerer.XP_TALISMAN.get().use(test.getLevel(),player,InteractionHand.MAIN_HAND);test.assertTrue(XpTalismanItem.xp(stack)==20,"No bottle means no XP charge");
        player.getInventory().setItem(1,new ItemStack(Items.GLASS_BOTTLE,2));Tinkerer.XP_TALISMAN.get().use(test.getLevel(),player,InteractionHand.MAIN_HAND);
        test.assertTrue(XpTalismanItem.xp(stack)==10&&player.getInventory().countItem(Items.GLASS_BOTTLE)==1&&player.getInventory().countItem(Items.EXPERIENCE_BOTTLE)==1,"One bottle plus ten XP becomes exactly one experience bottle");
        player.setShiftKeyDown(true);Tinkerer.XP_TALISMAN.get().use(test.getLevel(),player,InteractionHand.MAIN_HAND);test.assertTrue(XpTalismanItem.active(stack)&&XpTalismanItem.xp(stack)==10,"Sneaking toggles rather than crafting");
        XpTalismanItem.setXp(stack,1500);stack.getOrCreateTag().putBoolean("enabled",false);Tinkerer.XP_TALISMAN.get().use(test.getLevel(),player,InteractionHand.MAIN_HAND);test.assertTrue(!XpTalismanItem.active(stack),"Full storage cannot be enabled");
        player.setShiftKeyDown(false);for(int i=1;i<36;i++)player.getInventory().setItem(i,new ItemStack(Items.STONE,64));player.getInventory().setItem(1,new ItemStack(Items.GLASS_BOTTLE,2));var before=test.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,player.getBoundingBox().inflate(3)).size();
        Tinkerer.XP_TALISMAN.get().use(test.getLevel(),player,InteractionHand.MAIN_HAND);var drops=test.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,player.getBoundingBox().inflate(3),e->e.getItem().is(Items.EXPERIENCE_BOTTLE));
        test.assertTrue(XpTalismanItem.xp(stack)==1490&&player.getInventory().getItem(1).getCount()==1&&drops.size()==1,"Full inventory drops crafted bottle without duplication");drops.forEach(net.minecraft.world.entity.Entity::discard);test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void tinkererCleansingTalismanRepeatResetAndRepair(GameTestHelper test){
        var player=player(test);var stack=new ItemStack(Tinkerer.CLEANSING_TALISMAN.get());stack.getOrCreateTag().putBoolean("enabled",true);var context=equip(player,stack);
        player.tickCount=20;player.addEffect(new MobEffectInstance(MobEffects.POISON,200));tick(context,stack);test.assertTrue(!player.hasEffect(MobEffects.POISON)&&stack.getDamageValue()==1,"First harmful effect costs one use");
        player.tickCount=40;player.addEffect(new MobEffectInstance(MobEffects.POISON,200));tick(context,stack);test.assertTrue(!player.hasEffect(MobEffects.POISON)&&stack.getDamageValue()==1,"Immediately recurring effect becomes free");
        player.tickCount=60;player.addEffect(new MobEffectInstance(MobEffects.POISON,200));tick(context,stack);test.assertTrue(stack.getDamageValue()==1,"Continuous recurrence remains free");
        player.tickCount=80;tick(context,stack);player.tickCount=100;player.addEffect(new MobEffectInstance(MobEffects.POISON,200));tick(context,stack);test.assertTrue(stack.getDamageValue()==2,"A break in recurrence restores charge");
        Tinkerer.CLEANSING_TALISMAN.get().onUnequip(context,ItemStack.EMPTY,stack);player.tickCount=120;player.addEffect(new MobEffectInstance(MobEffects.POISON,200));tick(context,stack);test.assertTrue(stack.getDamageValue()==3,"Unequip clears cached immunity");
        test.assertTrue(Tinkerer.CLEANSING_TALISMAN.get().isValidRepairItem(stack,new ItemStack(Tinkerer.DARK_QUARTZ.get()))&&!Tinkerer.CLEANSING_TALISMAN.get().isValidRepairItem(stack,new ItemStack(Items.QUARTZ)),"Only dark quartz repairs talisman");test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void tinkererCleansingTalismanWarpFireAndExhaustion(GameTestHelper test){
        var player=player(test);var stack=new ItemStack(Tinkerer.CLEANSING_TALISMAN.get());stack.getOrCreateTag().putBoolean("enabled",true);var context=equip(player,stack);
        player.addEffect(new MobEffectInstance(tc4revival.content.TCEffects.WARP_WARD.get(),200));player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,19));player.addEffect(new MobEffectInstance(tc4revival.content.TCEffects.BLURRED_VISION.get(),200));
        player.tickCount=20;tick(context,stack);test.assertTrue(stack.getDamageValue()==10&&!player.hasEffect(tc4revival.content.TCEffects.BLURRED_VISION.get())&&player.hasEffect(tc4revival.content.TCEffects.WARP_WARD.get())&&player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),"Warp penalty costs ten, ward and short effects preserved");
        player.setSecondsOnFire(5);player.addEffect(new MobEffectInstance(MobEffects.POISON,-1));player.tickCount=40;tick(context,stack);test.assertTrue(!player.isOnFire()&&player.hasEffect(MobEffects.POISON)&&stack.getDamageValue()==11,"Burning has priority over one new harmful effect");
        player.tickCount=60;tick(context,stack);test.assertTrue(!player.hasEffect(MobEffects.POISON)&&stack.getDamageValue()==12,"Modern infinite effect can be cleansed");
        stack.setDamageValue(99);player.tickCount=80;player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,200));tick(context,stack);test.assertTrue(stack.isEmpty()&&CuriosApi.getCuriosInventory(player).resolve().orElseThrow().getStacksHandler("necklace").orElseThrow().getStacks().getStackInSlot(0).isEmpty(),"Final use consumes actual equipped stack");test.succeed();
    }
}
