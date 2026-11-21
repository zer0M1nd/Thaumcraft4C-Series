package conciliummodern.mixin;

import conciliummodern.EtherealState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Player.tick resets noPhysics; restore it at the actual movement boundary on both sides. */
@Mixin(value=Entity.class,remap=false)
public abstract class EtherealMoveMixin {
    @ModifyVariable(method={"move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V","m_6478_(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"},at=@At("HEAD"),argsOnly=true,ordinal=0,require=1)
    private Vec3 concilium$phase(Vec3 movement){
        if((Object)this instanceof Player player && EtherealState.active(player)){player.noPhysics=true;return player.isShiftKeyDown()?movement:movement.multiply(1,0,1);}
        return movement;
    }
}
