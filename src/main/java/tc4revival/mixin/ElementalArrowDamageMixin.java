package tc4revival.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import tc4revival.content.PrimalArrowEntity;

/** Preserve elemental damage tags while delegating the complete impact lifecycle to vanilla. */
@Mixin(value=AbstractArrow.class,remap=false)
public abstract class ElementalArrowDamageMixin {
    @ModifyVariable(method={"onHitEntity(Lnet/minecraft/world/phys/EntityHitResult;)V","m_5790_(Lnet/minecraft/world/phys/EntityHitResult;)V"},
            at=@At("STORE"),ordinal=0,remap=false)
    private DamageSource thaumcraft$elementalDamage(DamageSource source) {
        return (Object)this instanceof PrimalArrowEntity arrow?arrow.elementalDamageSource(source):source;
    }
}
