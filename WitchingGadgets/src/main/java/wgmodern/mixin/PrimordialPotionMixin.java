package wgmodern.mixin;
import net.minecraft.world.entity.*;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import wgmodern.*;
import tc4revival.content.TCEffects;
@Mixin(value=LivingEntity.class,remap=false)
public class PrimordialPotionMixin {
 @ModifyVariable(method={"addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z","m_147207_(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z"},at=@At("HEAD"),argsOnly=true)
 private MobEffectInstance weaken(MobEffectInstance effect){var e=(LivingEntity)(Object)this;if(e.level().isClientSide||e.hasEffect(effect.getEffect())||!WGConfig.PRIMORDIAL_POTIONS.get())return effect;var type=effect.getEffect();if(!java.util.List.of(TCEffects.VIS_EXHAUST.get(),TCEffects.THAUMARHIA.get(),TCEffects.UNNATURAL_HUNGER.get(),TCEffects.BLURRED_VISION.get(),TCEffects.SUN_SCORNED.get(),TCEffects.INFECTIOUS_VIS_EXHAUST.get(),TCEffects.DEATH_GAZE.get()).contains(type))return effect;int n=0;for(var s:e.getArmorSlots())if(s.getItem() instanceof PrimordialGear.Armor&&PrimordialGear.mode(s)==4)n++;if(n==0)return effect;return new MobEffectInstance(type,PrimordialRules.reducedDuration(effect.getDuration(),n),PrimordialRules.reducedAmplifier(effect.getAmplifier(),n),effect.isAmbient(),effect.isVisible(),effect.showIcon());}
}
