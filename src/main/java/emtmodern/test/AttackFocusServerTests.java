package emtmodern.test;
import emtmodern.ElectroMagicTools;
import emtmodern.focus.*;
import ic2.api.item.ElectricItem;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;
import tc4revival.content.*;
@GameTestHolder("emt") @PrefixGameTestTemplate(false)
public class AttackFocusServerTests {
    @GameTest(templateNamespace="emt",template="test_empty")
    public static void christmasFocusRequiresBlockAndSpendsFiveVis(GameTestHelper test){var level=test.getLevel();var player=GogglesServerTests.player(test);var wand=TCItems.CASTING_WAND.get();var stack=wand.create(WandParts.Rod.GREATWOOD,WandParts.Cap.GOLD,false,true);var focus=(ChristmasFocus)ElectroMagicTools.CONTENT.get("christmas_focus").get();WandFocusItem.attach(stack,new ItemStack(focus));var air=tc4revival.aspect.TCAspects.catalog().require("aer");int before=wand.vis(stack,air);focus.cast(stack,level,player,InteractionHand.MAIN_HAND,null);test.assertTrue(wand.vis(stack,air)==before,"A missed ray spends no vis");var pos=test.absolutePos(new net.minecraft.core.BlockPos(3,3,3));focus.cast(stack,level,player,InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(pos),net.minecraft.core.Direction.WEST,pos,false));var snow=level.getEntitiesOfClass(net.minecraft.world.entity.animal.SnowGolem.class,new net.minecraft.world.phys.AABB(pos).inflate(3));test.assertTrue(snow.size()==1&&snow.get(0).getX()==pos.getX()&&snow.get(0).getY()==pos.getY()+1,"Summons ordinary snowman above target block at original integer coordinates regardless of clicked face");test.assertTrue(before-wand.vis(stack,air)==500,"Original five whole vis price retained");snow.get(0).discard();test.succeed();}
    @GameTest(templateNamespace="emt",template="test_empty")
    public static void laserUsesOriginalVisAndDetonatesAfterBlockContact(GameTestHelper test){
        var level=test.getLevel();var player=GogglesServerTests.player(test);var origin=Vec3.atCenterOf(test.absolutePos(new net.minecraft.core.BlockPos(3,3,3)));player.setPos(origin);player.setYRot(0);player.setXRot(0);var wand=TCItems.CASTING_WAND.get();var stack=wand.create(WandParts.Rod.GREATWOOD,WandParts.Cap.GOLD,false,true);var focus=(ExplosionFocus)ElectroMagicTools.CONTENT.get("explosion_focus").get();var attached=new ItemStack(focus);focus.applyUpgrade(attached,FocusUpgrade.POTENCY,1);WandFocusItem.attach(stack,attached);player.setItemInHand(InteractionHand.MAIN_HAND,stack);var fire=tc4revival.aspect.TCAspects.catalog().require("ignis");int before=wand.vis(stack,fire);focus.cast(stack,level,player,InteractionHand.MAIN_HAND,null);
        var projectiles=level.getEntitiesOfClass(LaserEntity.class,player.getBoundingBox().inflate(3));test.assertTrue(projectiles.size()==1,"Focus emits one physical laser");var laser=projectiles.get(0);test.assertTrue(before-wand.vis(stack,fire)==200&&laser.strength==1&&laser.getDeltaMovement().length()==1,"Original 2 vis per primal cost, wand-stack potency quirk and speed retained");
        laser.setPos(origin);laser.setDeltaMovement(0,0,1);var block=net.minecraft.core.BlockPos.containing(origin.add(0,0,1));level.setBlockAndUpdate(block,Blocks.GLASS.defaultBlockState());laser.tick();test.assertTrue(laser.isAlive(),"Block hit waits original one tick before exploding");laser.tick();test.assertTrue(!laser.isAlive(),"Laser detonates and removes itself on next tick");
        var blast=Vec3.atLowerCornerOf(test.absolutePos(new net.minecraft.core.BlockPos(5,5,5)));
        var victim=EntityType.COW.create(level);victim.setPos(blast.add(1,0,0));level.addFreshEntity(victim);
        var attributedLaser=new LaserEntity(ElectroMagicTools.LASER_ENTITY.get(),level);attributedLaser.setOwner(player);attributedLaser.setPos(blast);attributedLaser.strength=1;level.addFreshEntity(attributedLaser);attributedLaser.explode();
        var damage=victim.getLastDamageSource();
        test.assertTrue(damage!=null&&damage.is(net.minecraft.world.damagesource.DamageTypes.EXPLOSION)&&damage.getEntity()==null&&damage.getDirectEntity()==null,"Owned laser still deals original unattributed explosion damage, not player explosion damage");
        victim.discard();test.succeed();
    }
    @GameTest(templateNamespace="emt",template="test_empty")
    public static void energyBallArmorDrainMagicRadiusAndLifetime(GameTestHelper test){
        var level=test.getLevel();var player=GogglesServerTests.player(test);var center=Vec3.atCenterOf(test.absolutePos(new net.minecraft.core.BlockPos(3,5,3)));player.setPos(center);var stack=TCItems.CASTING_WAND.get().create(WandParts.Rod.GREATWOOD,WandParts.Cap.GOLD,false,false);var focus=(EnergyBallFocus)ElectroMagicTools.CONTENT.get("energy_ball_focus").get();var chest=new ItemStack(ElectroMagicTools.CONTENT.get("nano_wings").get());player.setItemSlot(EquipmentSlot.CHEST,chest);ElectricItem.manager.charge(chest,1000,4,true,false);focus.cast(stack,level,player,InteractionHand.MAIN_HAND,null);
        test.assertTrue(ElectricItem.manager.getCharge(chest)==0&&level.getEntitiesOfClass(EnergyBallEntity.class,player.getBoundingBox().inflate(4)).isEmpty(),"Undercharged shot preserves author's partial EU loss and emits nothing");ElectricItem.manager.charge(chest,6000,4,true,false);focus.cast(stack,level,player,InteractionHand.MAIN_HAND,null);var balls=level.getEntitiesOfClass(EnergyBallEntity.class,player.getBoundingBox().inflate(4));test.assertTrue(balls.size()==1&&ElectricItem.manager.getCharge(chest)==880,"A shot costs exactly 5120 EU from chest armor");var ball=balls.get(0);test.assertTrue(Math.abs(ball.acceleration.length()-.1)<.00001&&ball.getDeltaMovement().equals(Vec3.ZERO),"Original initial acceleration and stationary first tick retained");ball.setPos(center);ball.ground();var near=EntityType.COW.create(level);near.setPos(center.add(2,0,0));level.addFreshEntity(near);var far=EntityType.COW.create(level);far.setPos(center.add(9,0,0));level.addFreshEntity(far);float health=near.getHealth();ball.tick();test.assertTrue(Math.abs(health-near.getHealth()-6)<.0001&&far.getHealth()==far.getMaxHealth(),"Stationary ball deals magic 8-distance damage only within eight blocks");
        ball.groundedTicks=30;var saved=new net.minecraft.nbt.CompoundTag();ball.saveWithoutId(saved);var copy=new EnergyBallEntity(ElectroMagicTools.ENERGY_BALL_ENTITY.get(),level);copy.load(saved);test.assertTrue(copy.grounded()&&copy.groundedTicks==30&&copy.acceleration.equals(ball.acceleration),"Save/reload retains active lifetime and acceleration");ball.groundedTicks=59;ball.tick();test.assertTrue(!ball.isAlive(),"Grounded projectile expires at sixty ticks");near.discard();far.discard();test.succeed();
    }
}
