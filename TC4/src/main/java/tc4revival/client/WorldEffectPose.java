package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;

/** Stage callbacks emit camera-relative vertices, but still need the stage's view rotation. */
public class WorldEffectPose {
    public static PoseStack copy(PoseStack view) {
        var result=new PoseStack();result.last().pose().set(view.last().pose());
        result.last().normal().set(view.last().normal());return result;
    }
    private WorldEffectPose() {}
}
