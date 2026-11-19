package tc4revival.content;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;

/** Old hand offset and the two successive launch-scatter passes used by focus projectiles. */
 class FocusProjectiles {
    private FocusProjectiles() {}
    /** Constructors retain their legacy right-hand launch; casting supplies the actual hand before spawning. */
    static <T extends Projectile> T fromHand(T projectile, net.minecraft.world.entity.player.Player owner, net.minecraft.world.InteractionHand hand) {
        var arm = hand == net.minecraft.world.InteractionHand.MAIN_HAND ? owner.getMainArm() : owner.getMainArm().getOpposite();
        if (arm == net.minecraft.world.entity.HumanoidArm.LEFT) {
            float yaw = owner.getYRot() * Mth.DEG_TO_RAD;
            projectile.setPos(projectile.position().add(Mth.cos(yaw) * .32, 0, Mth.sin(yaw) * .32));
        }
        return projectile;
    }
    static void launch(Projectile projectile, LivingEntity owner, float speed, float scatter) {
        projectile.setOwner(owner);
        float yaw = owner.getYRot() * Mth.DEG_TO_RAD;
        projectile.setPos(owner.getX() - Mth.cos(yaw) * .16, owner.getEyeY() - .1, owner.getZ() - Mth.sin(yaw) * .16);
        projectile.setYRot(owner.getYRot()); projectile.setXRot(owner.getXRot());
        var look = owner.getLookAngle(); projectile.shoot(look.x, look.y, look.z, speed, 1);
        var first = projectile.getDeltaMovement(); projectile.shoot(first.x, first.y, first.z, speed, scatter);
    }
}
