package thaumicomniverse.mixin;

import com.simibubi.create.content.kinetics.crusher.CrushingWheelControllerBlockEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Group;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.entity.EntityAspects;
import thaumicomniverse.compat.create.ThaumicCrushingWheel;
import thaumicomniverse.compat.create.AuraDrops;

/** Attribute crystals to a successful lethal crusher hit, including the first processing tick. */
@Mixin(value=CrushingWheelControllerBlockEntity.class,remap=false)
public abstract class CreateCrushingEssentiaMixin {
    @Group(name="thaumicomniverse$crushingDamage",min=1,max=1)
    @Redirect(method="tick",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",remap=false),require=0,remap=false)
    private boolean thaumicomniverse$crushDev(Entity entity,DamageSource source,float amount){return thaumicomniverse$crush(entity,source,amount);}
    @Group(name="thaumicomniverse$crushingDamage",min=1,max=1)
    @Redirect(method="tick",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;m_6469_(Lnet/minecraft/world/damagesource/DamageSource;F)Z",remap=false),require=0,remap=false)
    private boolean thaumicomniverse$crushProduction(Entity entity,DamageSource source,float amount){return thaumicomniverse$crush(entity,source,amount);}
    @org.spongepowered.asm.mixin.Unique
    private boolean thaumicomniverse$crush(Entity entity,DamageSource source,float amount){
        var controller=(Object)this instanceof ThaumicCrushingWheel.ControllerTile tile?tile:null;
        boolean alive=controller!=null&&!entity.level().isClientSide&&entity instanceof LivingEntity&&entity.isAlive();
        var aspects=alive?EntityAspects.resolve(entity).map(result->result.aspects()).orElseGet(AspectAmounts::new):null;
        // One redirect owns the hit. CEI's aura conversion must wrap that same hit,
        // not compete with the essentia hook for the original hurt invocation.
        boolean hurt=ModList.get().isLoaded("create_enchantment_industry")
                ?AuraDrops.crush(((BlockEntity)(Object)this).getBlockPos(),entity,source,amount)
                :entity.hurt(source,amount);
        // Totems, canceled damage/death and ordinary Create wheels must not yield essentia.
        if(alive&&hurt&&!entity.isAlive())controller.releaseEssentia(aspects);
        return hurt;
    }
}
