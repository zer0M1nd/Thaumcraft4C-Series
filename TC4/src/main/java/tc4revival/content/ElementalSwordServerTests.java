package tc4revival.content;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class ElementalSwordServerTests {
    private static FakePlayer player(GameTestHelper test,BlockPos pos,String name) {
        var player=new FakePlayer(test.getLevel(),new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8)),name)){
            @Override public float getAttackStrengthScale(float partialTick){return 1;}
        };
        player.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(TCItems.ELEMENTAL_SWORD.get()));
        return player;
    }
    @GameTest(template="wand_test_empty")
    public static void elementalSwordLiftRepulsionAndWear(GameTestHelper test) {
        var level=test.getLevel(); var pos=test.absolutePos(new BlockPos(3,4,3)); var player=player(test,pos,"SwordLift");
        var sword=player.getMainHandItem(); var item=(ElementalSwordItem)sword.getItem();
        var cow=EntityType.COW.create(level); cow.setPos(player.getX()+1,player.getY(),player.getZ()); cow.setNoAi(true); level.addFreshEntity(cow);
        player.startUsingItem(InteractionHand.MAIN_HAND); player.setDeltaMovement(.1,-.6,.2); player.fallDistance=6;
        item.onUseTick(level,player,sword,72000);
        test.assertTrue(Math.abs(player.getDeltaMovement().y-(-.6/(double)1.2f+(double).08f))<1e-6 && Math.abs(player.fallDistance-5)<1e-5,"Downward motion and accumulated fall distance are divided by 1.2");
        test.assertTrue(cow.getDeltaMovement().x>0 && sword.getDamageValue()==1,"Repulsion points outward and first tick consumes durability");
        player.setDeltaMovement(0,.49,0); item.onUseTick(level,player,sword,71999);
        test.assertTrue(Math.abs(player.getDeltaMovement().y-(double).2f)<1e-7 && sword.getDamageValue()==1,"Crossing the vertical cap resets to 0.2, no intermediate wear");
        item.onUseTick(level,player,sword,71980);
        test.assertTrue(sword.getDamageValue()==2 && !player.getAbilities().mayfly,"Twenty-tick wear never grants creative flight");
        test.assertTrue(LegacySwordRules.blockedDamage(7,false)==4,"Inherited old sword reduction remains available");
        pass(test,"lift-repulsion-wear");
    }
    @GameTest(template="wand_test_empty")
    public static void elementalSwordSecondaryCombatAndRejectedDamage(GameTestHelper test) {
        var level=test.getLevel(); var pos=test.absolutePos(new BlockPos(3,4,3)); var player=player(test,pos.west(2),"SwordCombat");
        // FakePlayer does not tick equipment attributes; supply the actual 1 + 7 sword attack value.
        player.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(8);
        var sword=player.getMainHandItem(); var item=(ElementalSwordItem)sword.getItem();
        var primary=EntityType.COW.create(level); primary.setPos(pos.getX(),pos.getY(),pos.getZ()); primary.setNoAi(true); level.addFreshEntity(primary);
        var secondary=EntityType.COW.create(level); secondary.setPos(pos.getX()+.6,pos.getY(),pos.getZ()); secondary.setNoAi(true); level.addFreshEntity(secondary);
        item.onLeftClickEntity(sword,player,primary);
        test.assertTrue(primary.getHealth()==primary.getMaxHealth() && secondary.getHealth()==2,"Callback attacks neighbors only, leaving primary attack to vanilla");
        test.assertTrue(sword.getDamageValue()==1,"One independent side attack wears sword once");
        secondary.setInvulnerable(true); int wear=sword.getDamageValue(); sword.enchant(Enchantments.FIRE_ASPECT,1);
        ElementalSwordCombat.attack(player,secondary);
        test.assertTrue(sword.getDamageValue()==wear && !secondary.isOnFire(),"Rejected damage preserves durability and clears speculative fire");
        var wolf=EntityType.WOLF.create(level); wolf.setPos(pos.getX()+.4,pos.getY(),pos.getZ()+.4); wolf.setNoAi(true); level.addFreshEntity(wolf);
        item.onLeftClickEntity(sword,player,primary);
        test.assertTrue(wolf.getHealth()<wolf.getMaxHealth(),"Untamed wolf with no owner does not crash side attacks");
        pass(test,"secondary-combat-rejected-damage");
    }
    private static void pass(GameTestHelper test,String name) { com.mojang.logging.LogUtils.getLogger().info("Elemental sword test passed: {}",name); test.succeed(); }
}
