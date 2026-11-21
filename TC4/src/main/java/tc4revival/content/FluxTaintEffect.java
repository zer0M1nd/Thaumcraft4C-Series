package tc4revival.content;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public  class FluxTaintEffect extends MobEffect {
    public static final ResourceKey<DamageType> DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE,new ResourceLocation("thaumcraft","taint"));
    public FluxTaintEffect() { super(MobEffectCategory.HARMFUL,0x663377); }
    @Override public boolean isDurationEffectTick(int duration,int amplifier) { int interval=40>>amplifier; return interval<=0 || duration%interval==0; }
    @Override public void applyEffectTick(LivingEntity target,int amplifier) {
        if (target.level().isClientSide) return;
        if (target instanceof TaintedMob) target.heal(1);
        else if (!target.isInvertedHealAndHarm()) target.hurt(new DamageSource(target.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DAMAGE)),1);
    }
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientMobEffectExtensions> consumer) {
        consumer.accept(new tc4revival.client.LegacyEffectIcon(3,1));
    }
}
