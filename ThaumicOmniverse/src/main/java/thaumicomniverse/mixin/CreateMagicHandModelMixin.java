package thaumicomniverse.mixin;
import com.simibubi.create.content.equipment.extendoGrip.ExtendoGripItemRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumicomniverse.compat.create.*;

@Mixin(value=ExtendoGripItemRenderer.class,remap=false)
public abstract class CreateMagicHandModelMixin {
    @Shadow @Final protected static PartialModel COG,THIN_SHORT,WIDE_SHORT,THIN_LONG,WIDE_LONG;
    @Unique private boolean omniverse$magic;
    @Inject(method="render",at=@At("HEAD"))
    private void omniverse$select(net.minecraft.world.item.ItemStack stack,com.simibubi.create.foundation.item.render.CustomRenderedItemModel model,
            com.simibubi.create.foundation.item.render.PartialItemModelRenderer renderer,net.minecraft.world.item.ItemDisplayContext context,
            com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffer,int light,int overlay,CallbackInfo ci){omniverse$magic=stack.getItem() instanceof MagicHand;}
    @Redirect(method="render",at=@At(value="FIELD",target="Lcom/simibubi/create/content/equipment/extendoGrip/ExtendoGripItemRenderer;COG:Ldev/engine_room/flywheel/lib/model/baked/PartialModel;"))
    private PartialModel omniverse$cog(){return omniverse$magic?ArcaneMachineRenderers.HAND.get("cog"):COG;}
    @Redirect(method="render",at=@At(value="FIELD",target="Lcom/simibubi/create/content/equipment/extendoGrip/ExtendoGripItemRenderer;THIN_SHORT:Ldev/engine_room/flywheel/lib/model/baked/PartialModel;"))
    private PartialModel omniverse$thinShort(){return omniverse$magic?ArcaneMachineRenderers.HAND.get("thin_short"):THIN_SHORT;}
    @Redirect(method="render",at=@At(value="FIELD",target="Lcom/simibubi/create/content/equipment/extendoGrip/ExtendoGripItemRenderer;WIDE_SHORT:Ldev/engine_room/flywheel/lib/model/baked/PartialModel;"))
    private PartialModel omniverse$wideShort(){return omniverse$magic?ArcaneMachineRenderers.HAND.get("wide_short"):WIDE_SHORT;}
    @Redirect(method="render",at=@At(value="FIELD",target="Lcom/simibubi/create/content/equipment/extendoGrip/ExtendoGripItemRenderer;THIN_LONG:Ldev/engine_room/flywheel/lib/model/baked/PartialModel;"))
    private PartialModel omniverse$thinLong(){return omniverse$magic?ArcaneMachineRenderers.HAND.get("thin_long"):THIN_LONG;}
    @Redirect(method="render",at=@At(value="FIELD",target="Lcom/simibubi/create/content/equipment/extendoGrip/ExtendoGripItemRenderer;WIDE_LONG:Ldev/engine_room/flywheel/lib/model/baked/PartialModel;"))
    private PartialModel omniverse$wideLong(){return omniverse$magic?ArcaneMachineRenderers.HAND.get("wide_long"):WIDE_LONG;}
    @Redirect(method="render",at=@At(value="FIELD",target="Lcom/simibubi/create/content/equipment/extendoGrip/ExtendoGripRenderHandler;pose:Ldev/engine_room/flywheel/lib/model/baked/PartialModel;"))
    private PartialModel omniverse$pose(){var pose=com.simibubi.create.content.equipment.extendoGrip.ExtendoGripRenderHandler.pose;return omniverse$magic?ArcaneMachineRenderers.HAND.get(pose==com.simibubi.create.AllPartialModels.DEPLOYER_HAND_HOLDING?"holding":"punching"):pose;}
    @Redirect(method="render",at=@At(value="FIELD",target="Lcom/simibubi/create/AllPartialModels;DEPLOYER_HAND_POINTING:Ldev/engine_room/flywheel/lib/model/baked/PartialModel;"))
    private PartialModel omniverse$pointing(){return omniverse$magic?ArcaneMachineRenderers.HAND.get("pointing"):com.simibubi.create.AllPartialModels.DEPLOYER_HAND_POINTING;}
}
