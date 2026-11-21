package thaumicomniverse.compat.create;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import tc4revival.content.CrucibleBlockEntity;

public final class CreateDeviceRendering {
    private CreateDeviceRendering() {}
    public static void setup(){
        thaumicSetups();
        thaumcraft.api.client.DeviceRendering.registerLayer("thaumicomniverse:crucible_item",0,CrucibleBlockEntity.class,
            entity->thaumicomniverse.RevivalFeatures.Group.CREATE.enabled(),c->render(c.entity(),c.partial(),c.pose(),c.buffers(),c.light(),c.overlay()));
    }
    private static void thaumicSetups(){thaumicomniverse.compat.ponder.PonderRendering.setup();}
    private static void render(CrucibleBlockEntity crucible,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var bridge=thaumcraft.api.devices.DeviceExtensions.component(crucible,CreateDevices.CRUCIBLE).orElse(null);if(bridge==null)return;var stack=bridge.visualStack();if(stack.isEmpty())return;
        Direction direction=bridge.visualDirection();float position=direction==null?0:bridge.visualPosition(partial);
        pose.pushPose();
        pose.translate(.5+(direction==null?0:direction.getStepX()*position*.65),.92+.2*Math.sin(Math.abs(position)*Math.PI),.5+(direction==null?0:direction.getStepZ()*position*.65));
        if(direction!=null)pose.mulPose((direction.getAxis()==Direction.Axis.X?Axis.ZP:Axis.XP).rotationDegrees(position*180*(direction.getAxisDirection()==Direction.AxisDirection.POSITIVE?1:-1)));
        pose.scale(.4f,.4f,.4f);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack,ItemDisplayContext.FIXED,light,overlay,pose,buffers,crucible.getLevel(),0);
        pose.popPose();
    }
}
