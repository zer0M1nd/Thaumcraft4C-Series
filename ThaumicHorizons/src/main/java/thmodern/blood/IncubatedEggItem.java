package thmodern.blood;

import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thmodern.Horizons;

@Mod.EventBusSubscriber(modid=Horizons.ID)
public final class IncubatedEggItem extends Item {
    public IncubatedEggItem(){super(new Properties().stacksTo(16));}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(!level.isClientSide){
            var egg=new net.minecraft.world.entity.projectile.ThrownEgg(level,player);egg.setItem(stack.copyWithCount(1));egg.shootFromRotation(player,player.getXRot(),player.getYRot(),0,1.5f,1);
            if(!level.addFreshEntity(egg))return InteractionResultHolder.fail(stack);
            if(!player.getAbilities().instabuild)stack.shrink(1);
            level.playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvents.EGG_THROW,net.minecraft.sounds.SoundSource.PLAYERS,.5f,.4f/(level.random.nextFloat()*.4f+.8f));
        }
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    @SubscribeEvent public static void impact(ProjectileImpactEvent event){
        if(!(event.getProjectile() instanceof net.minecraft.world.entity.projectile.ThrownEgg egg)||!egg.getItem().is(Horizons.INCUBATED_EGG.get()))return;
        event.setCanceled(true);if(egg.level().isClientSide||egg.isRemoved())return;
        if(event.getRayTraceResult() instanceof net.minecraft.world.phys.EntityHitResult hit)hit.getEntity().hurt(egg.damageSources().thrown(egg,egg.getOwner()),0);
        var chicken=net.minecraft.world.entity.EntityType.CHICKEN.create(egg.level());
        if(chicken!=null){chicken.setAge(-24000);chicken.moveTo(egg.getX(),egg.getY(),egg.getZ(),egg.getYRot(),0);egg.level().addFreshEntity(chicken);}
        egg.level().broadcastEntityEvent(egg,(byte)3);egg.discard();
    }
}
