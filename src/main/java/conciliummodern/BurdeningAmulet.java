package conciliummodern;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ClipContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import tc4revival.content.TCEffects;
import tc4revival.research.KnowledgeSavedData;
import tc4revival.research.PlayerKnowledge.WarpKind;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/** The original gaze command and temporary-warp/death-gaze tradeoff. */
public final class BurdeningAmulet extends Item implements ICurioItem,thaumcraft.api.IWarpingGear {
    public BurdeningAmulet(){super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON));}
    @Override public int getWarp(ItemStack stack,net.minecraft.world.entity.player.Player player){return 3;}
    @Override public void curioTick(SlotContext context,ItemStack stack){
        if(!(context.entity() instanceof ServerPlayer player))return;
        if(player.tickCount%20==0)command(player);
        if(player.hasEffect(TCEffects.WARP_WARD.get()))return;
        var data=KnowledgeSavedData.get(player.server);
        var knowledge=data.snapshot(player.getUUID()).orElse(null);
        int warp=knowledge==null?0:knowledge.warp(WarpKind.TEMPORARY);
        if(warp<=0)return;
        player.addEffect(new MobEffectInstance(TCEffects.DEATH_GAZE.get(),60,Math.min(3,warp/15),true,true));
        if(player.getRandom().nextInt(10)==0)data.update(player.getUUID(),player.getRandom()::nextInt,next->{next.changeWarp(WarpKind.TEMPORARY,-1);return null;});
    }
    private static void command(ServerPlayer player){
        var start=player.getEyePosition();var end=start.add(player.getLookAngle().scale(32));
        var wall=player.level().clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player));
        var hit=ProjectileUtil.getEntityHitResult(player,start,wall.getLocation(),player.getBoundingBox().expandTowards(end.subtract(start)).inflate(1),
                entity->entity instanceof LivingEntity && entity.isAlive() && !entity.isSpectator() && entity.isPickable(),start.distanceToSqr(wall.getLocation()));
        if(hit==null || !(hit.getEntity() instanceof LivingEntity target) || player.isAlliedTo(target)
                || target instanceof OwnableEntity owned && player.getUUID().equals(owned.getOwnerUUID())
                || target instanceof net.minecraft.world.entity.player.Player other && !player.canHarmPlayer(other))return;
        if(MinecraftForge.EVENT_BUS.post(new AttackEntityEvent(player,target)))return;
        // No direct attack is performed: mods can handle the attack event, owned mobs receive the target.
        for(var pet:player.level().getEntitiesOfClass(Mob.class,player.getBoundingBox().inflate(32)))
            if(pet!=target && pet instanceof OwnableEntity owned && player.getUUID().equals(owned.getOwnerUUID())
                    && !pet.isAlliedTo(target) && pet.canAttack(target))pet.setTarget(target);
    }
}
