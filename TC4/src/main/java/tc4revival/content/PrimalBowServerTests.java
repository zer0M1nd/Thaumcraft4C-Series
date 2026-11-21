package tc4revival.content;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class PrimalBowServerTests {
    private static FakePlayer player(GameTestHelper test) {
        var player=new FakePlayer(test.getLevel(),new GameProfile(UUID.randomUUID(),"PrimalBowTest"));
        var pos=test.absolutePos(new BlockPos(3,8,3)); player.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5); return player;
    }
    @GameTest(template="wand_test_empty")
    public static void primalBowAmmoPriorityAndBoneLaunch(GameTestHelper test) {
        var level=test.getLevel(); var player=player(test); var bow=new ItemStack(Items.BOW); bow.enchant(Enchantments.INFINITY_ARROWS,1);
        player.setItemInHand(InteractionHand.MAIN_HAND,bow);
        player.getInventory().setItem(1,new ItemStack(Items.ARROW,8)); player.getInventory().setItem(2,new ItemStack(TCItems.PRIMAL_ARROWS.get(2).get(),2));
        player.getInventory().setItem(3,new ItemStack(TCItems.PRIMAL_ARROWS.get(1).get(),3));
        Items.BOW.releaseUsing(bow,level,player,bow.getUseDuration()-20);
        var vanilla=level.getEntitiesOfClass(Arrow.class,player.getBoundingBox().inflate(5));
        test.assertTrue(vanilla.size()==1 && player.getInventory().getItem(1).getCount()==8,"Native inventory priority and Infinity preserve ordinary ammunition");
        vanilla.forEach(net.minecraft.world.entity.Entity::discard);
        player.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(TCItems.PRIMAL_ARROWS.get(2).get(),2));
        Items.BOW.releaseUsing(bow,level,player,bow.getUseDuration()-20);
        var arrows=level.getEntitiesOfClass(PrimalArrowEntity.class,player.getBoundingBox().inflate(5));
        test.assertTrue(arrows.size()==1 && arrows.get(0).element()==2 && arrows.get(0).isCritArrow(),"Native offhand priority launches a critical elemental arrow");
        test.assertTrue(player.getOffhandItem().getCount()==2 && bow.getDamageValue()==2,"Infinity preserves elemental ammunition and wears the bow");
        arrows.forEach(net.minecraft.world.entity.Entity::discard);
        player.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
        player.getInventory().setItem(2,ItemStack.EMPTY); player.getInventory().setItem(3,ItemStack.EMPTY);
        var bone=new ItemStack(TCItems.BONE_BOW.get()); player.setItemInHand(InteractionHand.MAIN_HAND,bone);
        TCItems.BONE_BOW.get().releaseUsing(bone,level,player,bone.getUseDuration()-10);
        var ordinary=level.getEntitiesOfClass(Arrow.class,player.getBoundingBox().inflate(5));
        test.assertTrue(ordinary.size()==1 && !ordinary.get(0).isCritArrow() && ordinary.get(0).getBaseDamage()==2.5,"Bone bow reaches full draw at ten ticks, adds damage and never enables critical arrows");
        test.assertTrue(ordinary.get(0).getDeltaMovement().length()>3.5 && player.getInventory().getItem(1).getCount()==7 && bone.getDamageValue()==1,"Bone bow preserves faster launch and one-arrow cost");
        ordinary.forEach(net.minecraft.world.entity.Entity::discard); pass(test,"ammo-priority-bone-launch");
    }
    @GameTest(template="wand_test_empty")
    public static void primalArrowElementsAndRejectedHits(GameTestHelper test) {
        var level=test.getLevel(); var player=player(test);
        for (int element=0;element<6;element++) {
            var target=EntityType.COW.create(level); target.setPos(player.position());
            var arrow=PrimalArrowEntity.launch(player,2,element); arrow.setDeltaMovement(1,0,0); arrow.setBaseDamage(5);
            float health=target.getHealth(); boolean hit=arrow.inflictElementDamage(target);
            test.assertTrue(hit && Math.abs(target.getHealth()-(health-5))<.001,"Element damage respects the arrow base damage: "+element);
            if (element==1) test.assertTrue(target.isOnFire(),"Fire arrow ignites target");
            if (element==2) test.assertTrue(target.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier()==4,"Water slows at level five");
            if (element==4) test.assertTrue(target.getEffect(MobEffects.WEAKNESS).getDuration()==200,"Order weakness lasts 200 ticks");
            if (element==5) test.assertTrue(target.getEffect(MobEffects.WITHER).getDuration()==100,"Entropy wither lasts 100 ticks");
            var saved=new net.minecraft.nbt.CompoundTag(); arrow.saveWithoutId(saved); var restored=TCEntities.PRIMAL_ARROW.get().create(level); restored.load(saved);
            test.assertTrue(restored.element()==element,"Element survives projectile NBT");
        }
        var immune=EntityType.COW.create(level); immune.setInvulnerable(true); var order=PrimalArrowEntity.launch(player,2,4); order.setDeltaMovement(1,0,0);
        test.assertTrue(!order.inflictElementDamage(immune) && !immune.hasEffect(MobEffects.WEAKNESS),"Rejected damage does not apply an elemental effect");
        pass(test,"elements-rejected-hits-persistence");
    }
    @GameTest(template="wand_test_empty")
    public static void primalArrowUsesNativeMovement(GameTestHelper test) {
        var player=player(test); var arrow=PrimalArrowEntity.launch(player,2,0); arrow.setPos(player.position().add(0,10,0)); arrow.setDeltaMovement(.5,0,0);
        var start=arrow.position(); arrow.tick();
        test.assertTrue(Math.abs(arrow.getX()-start.x-.5)<.0001 && Math.abs(arrow.getY()-start.y)<.0001,"Movement executes once per tick");
        test.assertTrue(Math.abs(arrow.getDeltaMovement().x-.495)<.0001 && Math.abs(arrow.getDeltaMovement().y+.05)<.0001,"Native drag and gravity execute once");
        pass(test,"native-movement");
    }
    private static void pass(GameTestHelper test,String name) { com.mojang.logging.LogUtils.getLogger().info("Primal bow test passed: {}",name); test.succeed(); }
}
