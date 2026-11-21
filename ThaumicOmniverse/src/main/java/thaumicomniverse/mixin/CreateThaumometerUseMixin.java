package thaumicomniverse.mixin;

import com.simibubi.create.content.contraptions.ContraptionHandlerClient;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.InputEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tc4revival.content.ThaumometerItem;
import tc4revival.content.CastingWandItem;
import tc4revival.content.WandFocusItem;
import thaumicomniverse.compat.create.ArcaneContraptions;
import thaumicomniverse.compat.create.KineticFocusItem;
import thaumicomniverse.compat.create.RelocationFocusItem;

@Mixin(value=ContraptionHandlerClient.class,remap=false)
public abstract class CreateThaumometerUseMixin {
    @Inject(method="rightClickingOnContraptionsGetsHandledLocally",at=@At("HEAD"),cancellable=true)
    private static void omniverse$scanInsteadOfContainer(InputEvent.InteractionKeyMappingTriggered event,CallbackInfo ci){
        var player=Minecraft.getInstance().player;
        if(!ArcaneContraptions.enabled()||!event.isUseItem()||player==null)return;
        // Create consumes the key even when the moving block has no interaction.
        // Scanning and wand control select their targets on the server instead.
        var held=player.getItemInHand(event.getHand());
        if(held.getItem() instanceof ThaumometerItem){ci.cancel();return;}
        if(held.getItem() instanceof CastingWandItem){
            var focus=WandFocusItem.attached(held);
            if(focus.getItem() instanceof KineticFocusItem||focus.getItem() instanceof RelocationFocusItem)
                ci.cancel();
        }
    }
}
