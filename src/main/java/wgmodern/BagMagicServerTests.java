package wgmodern;

import java.util.UUID;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.*;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.gametest.*;

@GameTestHolder("witchinggadgets") @PrefixGameTestTemplate(false)
public class BagMagicServerTests {
    private static net.minecraftforge.common.util.FakePlayer player(GameTestHelper h, String name) {
        var p = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8)), name));
        p.getInventory().clearContent(); p.getInventory().selected = 0; p.getAbilities().instabuild = false; return p;
    }
    private static ItemStack bag(String id) { return new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation("witchinggadgets", id))); }
    @GameTest(template="empty") public static void hungryPickupKeepsNbtCapacityAndOpenMenuConsistent(GameTestHelper h) {
        var p = player(h,"WGHungryBag"); var bag = bag("hungry_bag"); p.getInventory().setItem(0, bag);
        var items = NonNullList.withSize(18, ItemStack.EMPTY); for(int i=0;i<18;i++)items.set(i,new ItemStack(Items.DIRT,64));
        var named = new ItemStack(Items.DIAMOND,61); named.getOrCreateTag().putString("Test", "retained"); items.set(5,named); BagItem.store(bag,items);
        var menu = new BagMenu(1,p.getInventory(),false); p.containerMenu=menu;
        var stack=named.copyWithCount(7);var entity=new ItemEntity(h.getLevel(),0,0,0,stack);var event=new EntityItemPickupEvent(p,entity);BagEvents.pickup(event);
        h.assertTrue(!event.isCanceled()&&!entity.isRemoved()&&entity.getItem().getCount()==4,"Only available space is collected; remainder stays in world");
        h.assertTrue(menu.contents.getItem(5).getCount()==64&&menu.contents.getItem(5).getTag().getString("Test").equals("retained"),"Open menu reflects pickup without losing NBT");
        menu.removed(p);p.containerMenu=p.inventoryMenu;
        h.assertTrue(BagItem.contents(bag).get(5).getCount()==64,"Closing the GUI cannot overwrite newly collected items");
        pass(h);
    }
    @GameTest(template="empty") public static void hungryBagDoesNotStealLockedOrNestedContainers(GameTestHelper h) {
        var p=player(h,"WGHungryLock");var bag=bag("hungry_bag");bag.getOrCreateTag().putUUID("Owner",UUID.fromString("a0625194-eafb-4f21-83ae-b64cfdb1bc08"));p.getInventory().setItem(0,bag);
        var drop=new ItemEntity(h.getLevel(),0,0,0,new ItemStack(Items.DIAMOND));BagEvents.pickup(new EntityItemPickupEvent(p,drop));
        h.assertTrue(!drop.isRemoved()&&BagItem.contents(bag).stream().allMatch(ItemStack::isEmpty),"Locked other-owner bag cannot absorb drops");
        bag.getOrCreateTag().putBoolean("Unlocked",true);BagEvents.pickup(new EntityItemPickupEvent(p,drop));h.assertTrue(drop.isRemoved()&&BagItem.contents(bag).get(0).is(Items.DIAMOND),"Owner-unlocked bag accepts drops");
        var nested=new ItemEntity(h.getLevel(),0,0,0,bag("bag"));BagEvents.pickup(new EntityItemPickupEvent(p,nested));h.assertTrue(!nested.isRemoved(),"Hungry bag does not create recursively nested inventories");pass(h);
    }
    @GameTest(template="empty") public static void voidBagDeletesMatchingStackAndChargesOneRandomPrimal(GameTestHelper h) {
        var p=player(h,"WGVoidBag");var bag=bag("void_bag");var items=NonNullList.withSize(18,ItemStack.EMPTY);items.set(0,new ItemStack(Items.DIRT));BagItem.store(bag,items);p.getInventory().setItem(1,bag);
        var wand=tc4revival.content.TCItems.CASTING_WAND.get();var stick=wand.create(tc4revival.content.WandParts.Rod.WOOD,tc4revival.content.WandParts.Cap.IRON,false,false);p.getInventory().setItem(0,stick);
        var entity=new ItemEntity(h.getLevel(),0,0,0,new ItemStack(Items.DIRT,64));var event=new EntityItemPickupEvent(p,entity);BagEvents.pickup(event);
        h.assertTrue(entity.isRemoved()&&event.isCanceled(),"Void filter consumes the complete matching dropped stack");
        int total=tc4revival.aspect.TCAspects.catalog().primals().stream().mapToInt(a->wand.vis(stick,a)).sum();
        h.assertTrue(total>0&&total<wand.capacity(stick),"One selected primal is recharged using one item aspect value, not stack size");
        var unknown=new ItemEntity(h.getLevel(),0,0,0,new ItemStack(Items.DIAMOND));BagEvents.pickup(new EntityItemPickupEvent(p,unknown));h.assertTrue(!unknown.isRemoved(),"Unlisted item is untouched");pass(h);
    }
    @GameTest(template="empty") public static void customEffectsUseOriginalDamageAndPassengerRules(GameTestHelper h) {
        var l=h.getLevel();var target=EntityType.COW.create(l);target.setHealth(20);target.addEffect(new MobEffectInstance(WGMagic.CINDER.get(),80,1));
        var event=new net.minecraftforge.event.entity.living.LivingHurtEvent(target,l.damageSources().inFire(),2);WGMagic.hurt(event);h.assertTrue(event.getAmount()==6,"Cinder coat multiplies fire damage by 2 plus amplifier");
        h.assertTrue(WGMagic.DISSOLVE.get().isDurationEffectTick(10,2)&&!WGMagic.DISSOLVE.get().isDurationEffectTick(9,2),"Dissolve interval is 40 shifted by amplifier");
        WGMagic.DISSOLVE.get().applyEffectTick(target,2);h.assertTrue(target.getHealth()<20,"Dissolve applies actual server damage");
        var p=player(h,"WGRideProtect");var helmet=new ItemStack(Items.IRON_HELMET);helmet.enchant(WGMagic.ENCHANTS.get("ride_protect").get(),1);p.setItemSlot(EquipmentSlot.HEAD,helmet);
        var passenger=EntityType.SILVERFISH.create(l);passenger.startRiding(p,true);float health=passenger.getHealth();WGMagic.tick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.START,p));
        h.assertTrue(!passenger.isPassenger()&&passenger.getHealth()<health,"Ride protection ejects and damages the living passenger");pass(h);
    }
    private static void pass(GameTestHelper h) { com.mojang.logging.LogUtils.getLogger().info("WITCHINGGADGETS_TEST_PASS {}",Thread.currentThread().getStackTrace()[2].getMethodName());h.succeed(); }
}
