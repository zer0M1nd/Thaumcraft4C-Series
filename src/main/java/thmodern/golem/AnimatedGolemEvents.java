package thmodern.golem;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import tc4revival.content.*;
import thmodern.Horizons;

@Mod.EventBusSubscriber(modid=Horizons.ID)
public final class AnimatedGolemEvents {
    @SubscribeEvent public static void lightning(net.minecraftforge.event.entity.EntityStruckByLightningEvent event){
        if(event.getLightning() instanceof thmodern.plane.FiniteLightning&&event.getEntity() instanceof GolemEntity golem&&AnimatedGolems.isAnimated(golem))event.setCanceled(true);
    }
    @SubscribeEvent public static void tick(LivingEvent.LivingTickEvent event){if(event.getEntity() instanceof GolemEntity golem)AnimatedGolems.tick(golem);}
    @SubscribeEvent public static void interact(PlayerInteractEvent.EntityInteract event){
        if(!(event.getTarget() instanceof GolemEntity golem))return;
        var held=event.getItemStack();
        if(!AnimatedGolems.isAnimated(golem)||golem.configuration().core()!=null)return;
        if(held.getItem() instanceof GolemBellItem)return;
        if(held.getItem() instanceof GolemComponentItem component&&component.kind()==GolemComponentItem.Kind.CORE)return;
        // Coreless guardians have no core menu and cannot receive work upgrades yet.
        event.setCanceled(true);event.setCancellationResult(InteractionResult.PASS);
    }
    @SubscribeEvent public static void bell(AttackEntityEvent event){
        if(!(event.getTarget() instanceof GolemEntity golem)||!AnimatedGolems.isAnimated(golem)
                ||!(event.getEntity().getMainHandItem().getItem() instanceof GolemBellItem))return;
        event.setCanceled(true);AnimatedGolemRecovery.recover(event.getEntity(),golem);
    }
    @SubscribeEvent public static void hurt(LivingHurtEvent event){
        if(event.getEntity().level().isClientSide)return;
        if(event.getEntity() instanceof GolemEntity golem&&AnimatedGolems.isAnimated(golem)){
            var state=golem.configuration().blockAppearance();if(state==null)return;
            if(state.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,net.minecraft.resources.ResourceLocation.parse("forge:storage_blocks/thaumium")))&&event.getSource().is(DamageTypes.MAGIC))event.setAmount(event.getAmount()*.5f);
            var attacker=event.getSource().getDirectEntity();
            if(state.is(Blocks.CACTUS)&&attacker!=null&&attacker!=golem&&!event.getSource().is(DamageTypes.THORNS)&&golem.configuration().upgrades(5)==0)
                attacker.hurt(golem.damageSources().thorns(golem),golem.getRandom().nextInt(2));
        }
        if(event.getSource().getDirectEntity() instanceof GolemEntity golem&&AnimatedGolems.isAnimated(golem)
                &&golem.configuration().blockAppearance()!=null&&golem.configuration().blockAppearance().is(Blocks.COBWEB))
            event.getEntity().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,20,0));
    }
}
