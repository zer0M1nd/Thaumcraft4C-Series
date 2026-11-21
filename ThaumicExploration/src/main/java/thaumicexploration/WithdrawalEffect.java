package thaumicexploration;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
public class WithdrawalEffect extends MobEffect {
    public WithdrawalEffect(){super(MobEffectCategory.HARMFUL,0x800080);}
    @Override public boolean isDurationEffectTick(int duration,int amplifier){return duration%Math.max(1,20>>amplifier)==0;}
    @Override public void applyEffectTick(LivingEntity entity,int amplifier){
        if(entity.level().isClientSide)return;
        if(tc4revival.content.TCBiomes.isTainted(entity.level(),entity.blockPosition())){entity.removeEffect(this);return;}
        entity.hurt(ExplorationDamage.source(entity.level(),ExplorationDamage.WITHDRAWAL),1);
    }
}
