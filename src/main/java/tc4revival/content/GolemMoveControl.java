package tc4revival.content;

import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.util.Mth;

/** One-block stepping is handled by collision movement; never request a hop for a step. */
public class GolemMoveControl extends MoveControl {
    private final GolemEntity golem;
    public GolemMoveControl(GolemEntity golem) { super(golem); this.golem=golem; }
    @Override public void tick() {
        if (operation!=Operation.MOVE_TO) { super.tick(); return; }
        operation=Operation.WAIT;
        double dx=wantedX-golem.getX(), dz=wantedZ-golem.getZ();
        if (dx*dx+dz*dz<0.0004) { golem.setZza(0); return; }
        if (golem.configuration().axisAligned()) {
            // Navigation supplies centered nodes. Finish one horizontal component before the next.
            if (Math.abs(dx)>.02) dz=0; else dx=0;
            var velocity=golem.getDeltaMovement();
            golem.setDeltaMovement(dx==0?0:velocity.x,velocity.y,dz==0?0:velocity.z);
        }
        float yaw=(float)(Mth.atan2(dz,dx)*180/Math.PI)-90;
        golem.setYRot(golem.configuration().axisAligned()?yaw:rotlerp(golem.getYRot(),yaw,90));
        golem.setSpeed((float)(speedModifier*golem.getAttributeValue(Attributes.MOVEMENT_SPEED)));
        // Swimming still uses the vanilla navigation's float goal; grounded steps use maxUpStep.
    }
}
