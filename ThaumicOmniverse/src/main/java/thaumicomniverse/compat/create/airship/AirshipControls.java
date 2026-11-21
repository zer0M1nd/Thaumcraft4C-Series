package thaumicomniverse.compat.create.airship;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.actors.trainControls.*;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import net.createmod.catnip.animation.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;

/** Same lever renderer, easing, axes and equip animation as Create's train controls. */
public final class AirshipControls implements MovementBehaviour {
    private static final class Angles {
        final LerpedFloat steering=LerpedFloat.linear(),speed=LerpedFloat.linear(),equip=LerpedFloat.linear();
    }
    @Override public ItemStack canBeDisabledVia(MovementContext context){return null;}
    @Override public void stopMoving(MovementContext context){context.contraption.entity.stopControlling(context.localPos);}
    @Override public void tick(MovementContext context){
        if(!context.world.isClientSide)return;
        if(!(context.temporaryData instanceof Angles))context.temporaryData=new Angles();
        var a=(Angles)context.temporaryData;
        boolean active=ControlsHandler.getContraption()==context.contraption.entity&&context.localPos.equals(ControlsHandler.getControlsPos());
        var keys=ControlsHandler.currentlyPressed;
        a.equip.chase(active?1:0,.2,LerpedFloat.Chaser.EXP);
        a.steering.chase(active?(keys.contains(3)?1:0)-(keys.contains(2)?1:0):0,.2,LerpedFloat.Chaser.EXP);
        a.speed.chase(active?Math.min(context.motion.length(),.5)*(keys.contains(1)?-1:1):0,.2,LerpedFloat.Chaser.EXP);
        a.equip.tickChaser();a.steering.tickChaser();a.speed.tickChaser();
    }
    @Override public void renderInContraption(MovementContext context,VirtualRenderWorld world,ContraptionMatrices matrices,MultiBufferSource buffers){
        if(!(context.temporaryData instanceof Angles a))return;float pt=AnimationTickHolder.getPartialTicks(context.world);
        ControlsRenderer.render(context,world,matrices,buffers,a.equip.getValue(pt),a.speed.getValue(pt),a.steering.getValue(pt));
    }
}
