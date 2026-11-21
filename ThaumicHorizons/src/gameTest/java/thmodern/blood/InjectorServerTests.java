package thmodern.blood;

import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.effect.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import thmodern.Horizons;

@GameTestHolder(Horizons.ID) @PrefixGameTestTemplate(false)
public final class InjectorServerTests {
    private static FakePlayer player(GameTestHelper test){var player=new FakePlayer(test.getLevel(),new GameProfile(UUID.randomUUID(),"InjectorTest"));player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);player.setPos(test.absolutePos(new BlockPos(3,3,3)).getCenter());return player;}
    private static ItemStack injection(){var stack=new ItemStack(Horizons.INJECTION.get());PotionUtils.setCustomEffects(stack,List.of(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,600,1)));stack.getOrCreateTag().putInt("CustomPotionColor",0x123456);return stack;}
    @GameTest(template="empty")
    public static void magazineUpdatesPreserveOtherTagsAndAdvanceOnlyOnce(GameTestHelper test){
        var injector=new ItemStack(Horizons.INJECTOR.get());injector.setDamageValue(37);injector.enchant(Enchantments.POWER_ARROWS,3);injector.setHoverName(net.minecraft.network.chat.Component.literal("Seven doses"));
        InjectorItem.saveAmmo(injector,List.of(injection(),new ItemStack(Horizons.INJECTION_PHIAL.get()),injection()));InjectorItem.advance(injector);
        test.assertTrue(InjectorItem.ammo(injector,0).is(Horizons.INJECTION_PHIAL.get())&&InjectorItem.ammo(injector,1).is(Horizons.INJECTION.get())&&InjectorItem.ammo(injector,6).isEmpty(),"Advances FIFO queue and empties last chamber");
        test.assertTrue(injector.getDamageValue()==37&&injector.isEnchanted()&&injector.getHoverName().getString().equals("Seven doses"),"Ammo write preserves durability enchantments and name");
        var copy=ItemStack.of(injector.save(new CompoundTag()));test.assertTrue(PotionUtils.getMobEffects(InjectorItem.ammo(copy,1)).get(0).getAmplifier()==1,"Magazine payload survives stack save");test.succeed();
    }
    @GameTest(template="empty")
    public static void shortChargeDoesNotSpendAmmoAndFullChargeSpawnsOnePayload(GameTestHelper test){
        var player=player(test);var stack=new ItemStack(Horizons.INJECTOR.get());player.setItemInHand(InteractionHand.MAIN_HAND,stack);InjectorItem.saveAmmo(stack,List.of(injection(),injection()));
        var item=(InjectorItem)stack.getItem();item.releaseUsing(stack,test.getLevel(),player,71999);
        test.assertTrue(stack.getDamageValue()==0&&!InjectorItem.ammo(stack,1).isEmpty(),"Insufficient charge consumes nothing");
        item.releaseUsing(stack,test.getLevel(),player,71970);
        var projectiles=test.getLevel().getEntitiesOfClass(SyringeProjectile.class,player.getBoundingBox().inflate(3));
        test.assertTrue(projectiles.size()==1&&projectiles.get(0).isCritArrow()&&stack.getDamageValue()==1&&InjectorItem.ammo(stack,1).isEmpty(),"One full-charge projectile consumes one dose and durability");
        test.assertTrue(Math.abs(projectiles.get(0).getDeltaMovement().length()-3)<.1,"Legacy maximum projectile speed");
        test.assertTrue(PotionUtils.getMobEffects(projectiles.get(0).getItem()).get(0).getDuration()==600,"Full-duration injection payload");test.succeed();
    }
    @GameTest(template="empty")
    public static void projectilePreservesPayloadAcrossSaveAndAppliesFullEffects(GameTestHelper test){
        var player=player(test);var arrow=new SyringeProjectile(test.getLevel(),player,injection());var saved=new CompoundTag();arrow.addAdditionalSaveData(saved);
        var restored=new SyringeProjectile(Horizons.SYRINGE_PROJECTILE.get(),test.getLevel());restored.readAdditionalSaveData(saved);
        test.assertTrue(restored.getItem().getTag().getInt("CustomPotionColor")==0x123456,"Syringe color persists");
        var target=net.minecraft.world.entity.EntityType.PIG.create(test.getLevel());restored.doPostHurtEffects(target);
        test.assertTrue(target.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)&&target.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getDuration()==600,"No tipped-arrow duration division");
        test.assertTrue(ItemStack.isSameItemSameTags(restored.getPickupItem(),injection()),"Missed needle pickup returns complete dose");test.succeed();
    }
    @GameTest(template="empty")
    public static void menuLocksInjectorAndWritesBothAmmunitionTypes(GameTestHelper test){
        var player=player(test);var gun=new ItemStack(Horizons.INJECTOR.get());player.setItemInHand(InteractionHand.MAIN_HAND,gun);gun.enchant(Enchantments.PUNCH_ARROWS,1);
        player.getInventory().setItem(9,injection());var menu=new InjectorMenu(1,player.getInventory(),InteractionHand.MAIN_HAND);
        menu.quickMoveStack(player,7);test.assertTrue(InjectorItem.ammo(gun,0).is(Horizons.INJECTION.get())&&player.getInventory().getItem(9).isEmpty(),"Shift transfer persists to held gun");
        menu.clicked(34,0,net.minecraft.world.inventory.ClickType.PICKUP,player);test.assertTrue(player.getMainHandItem()==gun&&menu.getCarried().isEmpty(),"Held gun cannot be picked up");
        menu.clicked(0,0,net.minecraft.world.inventory.ClickType.SWAP,player);test.assertTrue(player.getMainHandItem()==gun&&InjectorItem.ammo(gun,0).is(Horizons.INJECTION.get()),"Number-key swap cannot move held gun");
        menu.slots.get(1).set(new ItemStack(Horizons.INJECTION_PHIAL.get()));test.assertTrue(InjectorItem.ammo(gun,1).is(Horizons.INJECTION_PHIAL.get())&&gun.isEnchanted(),"Phial accepted without erasing enchantment");
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Horizons.INJECTOR.get()));test.assertTrue(!menu.stillValid(player),"Different identical-type gun cannot hijack menu");test.succeed();
    }
}
