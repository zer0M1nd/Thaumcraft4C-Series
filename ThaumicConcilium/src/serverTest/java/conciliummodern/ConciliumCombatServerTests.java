package conciliummodern;

import com.mojang.authlib.GameProfile;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import tc4revival.content.*;

@GameTestHolder("thaumicconcilium") @PrefixGameTestTemplate(false)
public final class ConciliumCombatServerTests {
    private static FakePlayer player(GameTestHelper h,String name){return new FakePlayer(h.getLevel(),new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8)),name));}
    private static ItemStack wand(ItemStack focus){var wand=new ItemStack(TCItems.CASTING_WAND.get());WandFocusItem.attach(wand,focus);return wand;}
    @GameTest(template="empty") public static void taintAnimationProtectsOnlyHeldWandAndNeverBosses(GameTestHelper h){
        var player=player(h,"ConciliumTaintGuard");var focus=new ItemStack(ConciliumItems.TAINT_ANIMATION.get());player.setItemInHand(InteractionHand.MAIN_HAND,wand(focus));
        var taint=TCEntities.TAINTACLE.get().create(h.getLevel());taint.setTarget(player);h.assertTrue(taint.getTarget()==null,"Tainted mob refuses holder of taint animation wand");
        player.setItemInHand(InteractionHand.MAIN_HAND,focus);taint.setTarget(player);h.assertTrue(taint.getTarget()==player,"Bare focus is not a protective wand");
        player.setItemInHand(InteractionHand.OFF_HAND,wand(focus));player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);taint.setTarget(player);h.assertTrue(taint.getTarget()==player,"Only original held/main hand grants protection");
        player.setItemInHand(InteractionHand.MAIN_HAND,wand(focus));var boss=TCEntities.TAINTACLE_GIANT.get().create(h.getLevel());boss.setTarget(player);h.assertTrue(boss.getTarget()==player,"Giant taintacle boss ignores protection");
        var zombie=EntityType.ZOMBIE.create(h.getLevel());zombie.setTarget(player);h.assertTrue(zombie.getTarget()==player,"Untainted mobs are unaffected");h.succeed();
    }
    private static List<ItemEntity> essence(GameTestHelper h,BlockPos pos){return h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2),entity->entity.getItem().is(TCItems.WISP_ESSENCE.get()));}
    private static LivingEntity kill(GameTestHelper h,BlockPos pos,DamageSource source){var zombie=EntityType.ZOMBIE.create(h.getLevel());zombie.setNoAi(true);zombie.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);h.getLevel().addFreshEntity(zombie);h.assertTrue(zombie.hurt(source,1000)&&zombie.isDeadOrDying(),"Test victim died through the real damage/event path");return zombie;}
    @GameTest(template="empty") public static void visConductorEssenceDropRequiresDirectMagicAndNoLauncher(GameTestHelper h){
        var player=player(h,"ConciliumEssence");var focus=new ItemStack(ConciliumItems.VIS_CONDUCTOR.get());player.setItemInHand(InteractionHand.MAIN_HAND,wand(focus));var pos=h.absolutePos(new BlockPos(3,3,3));
        var victim=kill(h,pos,h.getLevel().damageSources().indirectMagic(player,player));var drops=essence(h,pos);h.assertTrue(drops.size()==1&&drops.get(0).getItem().getCount()==1,"Valid magical kill produces exactly one wispy essence");
        var stored=((StoredAspectItem)drops.get(0).getItem().getItem()).storedAspects(drops.get(0).getItem());var possible=tc4revival.aspect.entity.EntityAspects.resolve(victim).orElseThrow().aspects();
        h.assertTrue(stored.aspects().size()==1&&stored.amount(stored.aspects().get(0))==2&&possible.amount(stored.aspects().get(0))>0,"Drop contains exactly two of a real victim aspect");drops.forEach(Entity::discard);
        pos=h.absolutePos(new BlockPos(8,3,3));kill(h,pos,h.getLevel().damageSources().playerAttack(player));h.assertTrue(essence(h,pos).isEmpty(),"Ordinary melee does not produce essence");
        var launcher=FocusUpgrade.require("thaumicconcilium:wisp_launcher");ConciliumItems.VIS_CONDUCTOR.get().applyUpgrade(focus,launcher,1);player.setItemInHand(InteractionHand.MAIN_HAND,wand(focus));pos=h.absolutePos(new BlockPos(13,3,3));kill(h,pos,h.getLevel().damageSources().indirectMagic(player,player));h.assertTrue(essence(h,pos).isEmpty(),"Wisp launcher disables the kill reward");
        player.setItemInHand(InteractionHand.MAIN_HAND,wand(new ItemStack(ConciliumItems.VIS_CONDUCTOR.get())));pos=h.absolutePos(new BlockPos(18,3,3));kill(h,pos,h.getLevel().damageSources().indirectMagic(new Arrow(h.getLevel(),player),player));h.assertTrue(essence(h,pos).isEmpty(),"Player-owned projectile is not original direct-player magic");
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK));pos=h.absolutePos(new BlockPos(23,3,3));kill(h,pos,h.getLevel().damageSources().indirectMagic(player,player));h.assertTrue(essence(h,pos).isEmpty(),"Magic without the held conductor wand gives no reward");h.succeed();
    }
}
