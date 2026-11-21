package emtmodern.test;
import emtmodern.ElectroMagicTools;
import emtmodern.focus.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.gametest.*;
import tc4revival.content.*;
@GameTestHolder("emt") @PrefixGameTestTemplate(false)
public class ShieldFocusServerTests {
    @GameTest(templateNamespace="emt",template="test_empty")
    public static void shieldVisPurificationCollisionAndRelease(GameTestHelper test){
        var level=test.getLevel();var player=GogglesServerTests.player(test);player.getAbilities().instabuild=false;player.moveTo(test.absolutePos(new net.minecraft.core.BlockPos(3,3,3)).getCenter());
        var wand=TCItems.CASTING_WAND.get();var stack=wand.create(WandParts.Rod.GREATWOOD,WandParts.Cap.GOLD,false,true);var focus=(ShieldFocus)ElectroMagicTools.CONTENT.get("shield_focus").get();WandFocusItem.attach(stack,new ItemStack(focus));player.setItemInHand(InteractionHand.MAIN_HAND,stack);var order=tc4revival.aspect.TCAspects.catalog().require("ordo");int before=wand.vis(stack,order);
        focus.cast(stack,level,player,InteractionHand.MAIN_HAND,null);var shields=level.getEntitiesOfClass(ShieldEntity.class,player.getBoundingBox().inflate(5));test.assertTrue(shields.size()==1&&shields.get(0).owner()==player,"One live shield follows real owner");test.assertTrue(before-wand.vis(stack,order)==5,"Cast consumes original centivis with gold caps");
        player.addEffect(new MobEffectInstance(MobEffects.POISON,300));player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,300));focus.usingTick(stack,player,100);test.assertTrue(player.getActiveEffects().isEmpty(),"Original shield removes beneficial and harmful effects");test.assertTrue(before-wand.vis(stack,order)==10,"Hold pays same cost every tick");var shield=shields.get(0);var mob=EntityType.ZOMBIE.create(level);mob.moveTo(player.getX()+1,player.getY(),player.getZ());shield.push(mob);test.assertTrue(mob.getDeltaMovement().x>.19&&shield.isPickable()&&shield.canBeCollidedWith(),"Physical shield pushes nearby entities and intercepts hits");
        player.stopUsingItem();shield.tick();test.assertTrue(!shield.isAlive(),"Shield disappears after release");test.succeed();
    }
}
