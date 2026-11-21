package thaumicomniverse.mixin;

import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.kinetics.deployer.DeployerFakePlayer;
import com.simibubi.create.content.kinetics.deployer.DeployerMovementBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value=DeployerMovementBehaviour.class,remap=false)
public interface CreateDeployerAccess {
    @Invoker("getPlayer") DeployerFakePlayer omniverse$player(MovementContext context);
}
