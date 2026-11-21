package conciliummodern;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import tc4revival.aspect.AspectAmounts;
import tc4revival.content.*;

/** Original held-focus target protection and direct magical-kill essence rewards. */
@Mod.EventBusSubscriber(modid=ThaumicConcilium.ID)
public final class ConciliumCombatEvents {
    @SubscribeEvent public static void target(LivingChangeTargetEvent event){
        var mob=event.getEntity();
        if(!(mob instanceof TaintedMob)||mob instanceof GiantTaintacleEntity||mob instanceof ThaumcraftBossEntity
                ||mob.getType().is(TagKey.create(Registries.ENTITY_TYPE,ResourceLocation.parse("forge:bosses")))
                ||!(event.getNewTarget() instanceof Player player))return;
        var held=player.getMainHandItem();
        if(held.getItem() instanceof CastingWandItem&&WandFocusItem.attached(held).getItem() instanceof TaintAnimationFocus)event.setNewTarget(null);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void death(LivingDeathEvent event){
        var victim=event.getEntity();var damage=event.getSource();
        if(victim.level().isClientSide||!(damage.getDirectEntity() instanceof ServerPlayer player)
                ||!(damage.is(DamageTypes.MAGIC)||damage.is(DamageTypes.INDIRECT_MAGIC)
                ||damage.is(TagKey.create(Registries.DAMAGE_TYPE,ResourceLocation.parse("forge:is_magic")))))return;
        var held=player.getMainHandItem();if(!(held.getItem() instanceof CastingWandItem))return;
        var focus=WandFocusItem.attached(held);if(!(focus.getItem() instanceof VisConductorFocus conductor)
                ||conductor.upgradeLevel(focus,FocusUpgrade.require("thaumicconcilium:wisp_launcher").id)>0)return;
        tc4revival.aspect.entity.EntityAspects.resolve(victim).ifPresent(result->{
            var aspects=result.aspects().aspects();if(aspects.isEmpty())return;
            var essence=new ItemStack(TCItems.WISP_ESSENCE.get());var contents=new AspectAmounts().add(aspects.get(victim.getRandom().nextInt(aspects.size())),2);
            ((StoredAspectItem)essence.getItem()).setStoredAspects(essence,contents);victim.spawnAtLocation(essence,.2f);
        });
    }
    private ConciliumCombatEvents(){}
}
