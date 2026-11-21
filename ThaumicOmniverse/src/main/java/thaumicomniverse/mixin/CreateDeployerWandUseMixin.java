package thaumicomniverse.mixin;

import com.simibubi.create.content.kinetics.deployer.DeployerFakePlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Group;
import tc4revival.content.CastingWandItem;

/** Create completes held-item use immediately; continuous foci need their first use tick. */
@Mixin(targets="com.simibubi.create.content.kinetics.deployer.DeployerHandler",remap=false)
public class CreateDeployerWandUseMixin {
    @Group(name="omniverse$deployerWandUse",min=1)
    @Redirect(method="activateInner",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/ItemStack;finishUsingItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;",remap=false),require=0,remap=false)
    private static ItemStack omniverse$tickWandDev(ItemStack stack,Level level,LivingEntity actor){
        return omniverse$tickWandBeforeFinishing(stack,level,actor);
    }
    @Group(name="omniverse$deployerWandUse",min=1)
    @Redirect(method="activateInner",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/ItemStack;m_41671_(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;",remap=false),require=0,remap=false)
    private static ItemStack omniverse$tickWandProduction(ItemStack stack,Level level,LivingEntity actor){
        return omniverse$tickWandBeforeFinishing(stack,level,actor);
    }
    @org.spongepowered.asm.mixin.Unique
    private static ItemStack omniverse$tickWandBeforeFinishing(ItemStack stack,Level level,LivingEntity actor){
        if(thaumicomniverse.RevivalFeatures.Group.CREATE.enabled()&&actor instanceof DeployerFakePlayer
                &&stack.getItem() instanceof CastingWandItem&&actor.isUsingItem()&&actor.getUseItem()==stack)
            stack.getItem().onUseTick(level,actor,stack,stack.getUseDuration()-1);
        return stack.finishUsingItem(level,actor);
    }
}
