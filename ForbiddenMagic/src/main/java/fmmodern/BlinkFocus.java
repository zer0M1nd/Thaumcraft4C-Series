package fmmodern;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
import thaumcraft.api.wands.ItemFocusBasic;
import tc4revival.aspect.*;
import tc4revival.content.*;

public final class BlinkFocus extends ItemFocusBasic {
    private boolean has(ItemStack focus,String key){return getUpgradeLevel(focus,ForbiddenMagic.ID+":"+key)>0;}
    @Override public int color(ItemStack stack){return 0x909090;}
    @Override public String sortingPrefix(){return "BLINK";}
    @Override public String depthTexture(){return "forbidden_magic:item/focus_blink";}
    @Override public String ornamentTexture(){return "forbidden_magic:item/focus_blink_orn";}
    @Override public Map<Aspect,Integer> costs(ItemStack focus){var costs=new LinkedHashMap<Aspect,Integer>();costs.put(TCAspects.catalog().require("perditio"),300);if(has(focus,"hellfire"))costs.put(TCAspects.catalog().require("ignis"),100);else if(has(focus,"pandemonium"))costs.put(TCAspects.catalog().require("ordo"),100);return costs;}
    @Override public List<FocusUpgrade> upgradesForRank(ItemStack focus,int rank){
        if(rank<1||rank>5)return List.of();
        if(rank==2)return List.of(FocusUpgrade.FRUGAL,FocusUpgrade.require("forbidden_magic:hellfire"),FocusUpgrade.require("forbidden_magic:pandemonium"));
        if(rank>=3&&has(focus,"hellfire"))return List.of(FocusUpgrade.POTENCY,FocusUpgrade.FRUGAL,FocusUpgrade.ENLARGE);
        if(rank>=3&&has(focus,"pandemonium"))return List.of(FocusUpgrade.FRUGAL,FocusUpgrade.ENLARGE);
        return List.of(FocusUpgrade.FRUGAL);
    }
    @Override public InteractionResultHolder<ItemStack> cast(ItemStack stack,Level level,Player player,InteractionHand hand,BlockHitResult ignored){
        if(!(player instanceof ServerPlayer server)||!(stack.getItem() instanceof CastingWandItem wand))return InteractionResultHolder.pass(stack);
        var start=player.getEyePosition();var end=start.add(player.getLookAngle().scale(thaumcraft.api.wands.SpellParameters.limited("blink.range",128,1024)));
        var hit=level.clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player));
        if(hit.getType()!=HitResult.Type.BLOCK)return InteractionResultHolder.fail(stack);
        var dest=hit.getLocation();Direction face=hit.getDirection();
        dest=switch(face){case DOWN->dest.add(0,-player.getBbHeight()-.01,0);case UP->dest.add(0,.01,0);default->dest.add(face.getStepX()*(player.getBbWidth()/2+.01),0,face.getStepZ()*(player.getBbWidth()/2+.01));};
        var target=BlockPos.containing(dest);var box=player.getBoundingBox().move(dest.subtract(player.position()));
        if(!level.hasChunkAt(target)||!level.getWorldBorder().isWithinBounds(box)||target.getY()<level.getMinBuildHeight()||dest.y+player.getBbHeight()>=level.getMaxBuildHeight()||!level.noCollision(player,box))return InteractionResultHolder.fail(stack);
        var focus=attached(stack);var cost=costs(focus);
        if(!wand.consumeForPlayer(stack,player,cost,false,false,false))return InteractionResultHolder.fail(stack);
        var teleport=new net.minecraftforge.event.entity.EntityTeleportEvent(player,dest.x,dest.y,dest.z);
        if(net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(teleport))return InteractionResultHolder.fail(stack);
        dest=new Vec3(teleport.getTargetX(),teleport.getTargetY(),teleport.getTargetZ());box=player.getBoundingBox().move(dest.subtract(player.position()));
        if(!level.hasChunkAt(BlockPos.containing(dest))||!level.noCollision(player,box)||!level.getWorldBorder().isWithinBounds(box))return InteractionResultHolder.fail(stack);
        if(!wand.consumeForPlayer(stack,player,cost,false,false,true))return InteractionResultHolder.fail(stack);
        var origin=player.position();double range=3+1.5*upgradeLevel(focus,FocusUpgrade.ENLARGE.id);
        var victims=level.getEntitiesOfClass(LivingEntity.class,new AABB(dest, dest).inflate(range),e->e!=player);
        if(has(focus,"hellfire")){float power=3+3*((tc4revival.content.CastingWandItem)stack.getItem()).focusPotency(stack,player);for(var victim:victims){if(victim instanceof Player other&&!player.canHarmPlayer(other))continue;victim.hurt(player.damageSources().indirectMagic(player,player),power);victim.setSecondsOnFire(tc4revival.content.FocusPotency.discrete(power,player.getRandom()));particles(server,ParticleTypes.FLAME,victim);}}
        else if(has(focus,"pandemonium"))for(var victim:victims)if(victim instanceof Mob&&victim instanceof Enemy){var moved=victim.getBoundingBox().move(origin.subtract(victim.position()));if(level.noCollision(victim,moved)){particles(server,ParticleTypes.SMOKE,victim);victim.teleportTo(origin.x,origin.y,origin.z);}}
        level.playSound(null,BlockPos.containing(origin),net.minecraft.sounds.SoundEvents.GHAST_SHOOT,net.minecraft.sounds.SoundSource.PLAYERS,1,1);
        server.teleportTo(dest.x,dest.y,dest.z);server.fallDistance=0;
        server.serverLevel().sendParticles(ParticleTypes.SMOKE,origin.x,origin.y+.8,origin.z,8,.3,.7,.3,0);server.serverLevel().sendParticles(ParticleTypes.FLAME,dest.x,dest.y+.8,dest.z,8,.3,.7,.3,0);
        level.playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvents.GHAST_SHOOT,net.minecraft.sounds.SoundSource.PLAYERS,1,1);
        return InteractionResultHolder.success(stack);
    }    private static void particles(ServerPlayer player,net.minecraft.core.particles.SimpleParticleType particle,LivingEntity target){player.serverLevel().sendParticles(particle,target.getX(),target.getY()+target.getBbHeight()/2-.25,target.getZ(),8,target.getBbWidth()/2,target.getBbHeight()/2,target.getBbWidth()/2,0);}

}
