package thmodern.vat;

import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.gametest.*;
import thmodern.Horizons;

@GameTestHolder(Horizons.ID) @PrefixGameTestTemplate(false)
public final class CreatureAugmentationServerTests {
    private static VatInfusionRecipe recipe(String key){return VatInfusionRecipes.all().stream().filter(recipe->recipe.id.getPath().equals(key)).findFirst().orElseThrow();}
    @GameTest(template="empty")
    public static void runicShieldPaysFractionalDamageAndDoesNotBlockDrowning(GameTestHelper test){
        var mob=EntityType.COW.create(test.getLevel());mob.moveTo(test.absolutePos(new BlockPos(3,3,3)).getCenter());CreatureInfusions.apply(mob,recipe("runic_hide"),UUID.randomUUID());
        for(int i=1;i<=8;i++){mob.tickCount=i*100;CreatureInfusionEvents.tick(new LivingEvent.LivingTickEvent(mob));}
        test.assertTrue(CreatureInfusionEvents.charge(mob)==6,"Charge caps at six after one point per hundred ticks");
        var drowning=new LivingHurtEvent(mob,mob.damageSources().drown(),1);CreatureInfusionEvents.shield(drowning);test.assertTrue(drowning.getAmount()==1&&CreatureInfusionEvents.charge(mob)==6,"Drowning bypasses charge");
        for(int i=0;i<24;i++){var hit=new LivingHurtEvent(mob,mob.damageSources().generic(),.25f);CreatureInfusionEvents.shield(hit);test.assertTrue(hit.getAmount()==0,"Available quarter-point hit is fully shielded");}
        var empty=new LivingHurtEvent(mob,mob.damageSources().generic(),.25f);CreatureInfusionEvents.shield(empty);test.assertTrue(empty.getAmount()==.25f&&CreatureInfusionEvents.charge(mob)==0,"Small hits consume their actual fraction instead of shielding forever");test.succeed();
    }
    @GameTest(template="empty")
    public static void eldritchFangsPoisonVictimNotTheInfusedAttacker(GameTestHelper test){
        var attacker=EntityType.WOLF.create(test.getLevel());var victim=EntityType.COW.create(test.getLevel());CreatureInfusions.apply(attacker,recipe("eldritch_fangs"),UUID.randomUUID());
        CreatureInfusionEvents.poison(new LivingDamageEvent(victim,victim.damageSources().mobAttack(attacker),2));
        test.assertTrue(victim.hasEffect(MobEffects.POISON)&&victim.getEffect(MobEffects.POISON).getDuration()==40&&!attacker.hasEffect(MobEffects.POISON),"Successful melee applies two seconds of poison to the damaged creature");test.succeed();
    }
    @GameTest(template="empty")
    public static void loyaltyOwnerAndWaitCommandSurviveEntitySave(GameTestHelper test){
        var player=new FakePlayer(test.getLevel(),new GameProfile(UUID.randomUUID(),"LoyalOwner"));var mob=EntityType.COW.create(test.getLevel());
        test.assertTrue(CreatureInfusions.apply(mob,recipe("instilled_loyalty"),player.getUUID()),"Cow accepts loyalty and replaces its ordinary goal set");int goals=mob.goalSelector.getAvailableGoals().size();CreatureInfusions.refresh(mob);test.assertTrue(mob.goalSelector.getAvailableGoals().size()==goals,"Refreshing does not append duplicate goals");
        var interaction=new PlayerInteractEvent.EntityInteract(player,net.minecraft.world.InteractionHand.MAIN_HAND,mob);CreatureInfusionEvents.interact(interaction);
        test.assertTrue(interaction.isCanceled()&&LoyalCreatureGoals.sitting(mob),"Only owner interaction switches waiting state");
        var tag=new CompoundTag();mob.save(tag);var restored=(Mob)EntityType.create(tag,test.getLevel()).orElseThrow();CreatureInfusions.refresh(restored);
        test.assertTrue(LoyalCreatureGoals.sitting(restored)&&restored.getPersistentData().getCompound(CreatureInfusions.DATA).getUUID("Owner").equals(player.getUUID()),"Ownership and waiting persist through capture save and reload");test.succeed();
    }
    @GameTest(template="empty")
    public static void enderHeartCancelsProjectileDamage(GameTestHelper test){
        var mob=EntityType.COW.create(test.getLevel());mob.moveTo(test.absolutePos(new BlockPos(3,3,3)).getCenter());CreatureInfusions.apply(mob,recipe("ender_heart"),UUID.randomUUID());
        var arrow=new net.minecraft.world.entity.projectile.Arrow(EntityType.ARROW,test.getLevel());var event=new LivingAttackEvent(mob,mob.damageSources().arrow(arrow,null),4);CreatureInfusionEvents.attack(event);
        test.assertTrue(event.isCanceled(),"Legacy projectile immunity remains even when no safe teleport destination was found");test.succeed();
    }
}
