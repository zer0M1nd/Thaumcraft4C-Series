package thaumicomniverse.mixin;

import com.simibubi.create.foundation.gui.AllGuiTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import thaumicomniverse.compat.create.ThaumicCreateScreen;

@Mixin(value=AllGuiTextures.class,remap=false)
public abstract class CreateGuiPaletteMixin {
    @Inject(method="bind",at=@At("HEAD"),cancellable=true)
    private void omniverse$bind(CallbackInfo ci){
        var mapped=palette(((AllGuiTextures)(Object)this).location);
        if(mapped!=null){com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0,mapped);ci.cancel();}
    }
    private static ResourceLocation palette(ResourceLocation location){
        var mc=Minecraft.getInstance();
        boolean airshipSchedule=mc.screen instanceof com.simibubi.create.content.trains.schedule.ScheduleScreen&&mc.player!=null
            &&mc.player.getMainHandItem().getItem() instanceof thaumicomniverse.compat.create.airship.AirshipScheduleItem;
        if((airshipSchedule||mc.screen instanceof thaumicomniverse.compat.create.airship.AirshipStationScreen)&&location.getNamespace().equals("create")&&java.util.Set.of("textures/gui/schedule.png","textures/gui/schedule_widgets.png","textures/gui/schedule_2.png","textures/gui/assemble.png").contains(location.getPath()))
            return ResourceLocation.fromNamespaceAndPath("thaumicomniverse",location.getPath().replace("textures/","textures/airship/"));
        if(!(mc.screen instanceof ThaumicCreateScreen)&&!airshipSchedule
            ||!location.getNamespace().equals("create")||!java.util.Set.of(
                "textures/gui/sequencer.png","textures/gui/value_settings.png","textures/gui/widgets.png").contains(location.getPath()))return null;
        return ResourceLocation.fromNamespaceAndPath("thaumicomniverse",location.getPath().replace("gui/","gui/create/"));
    }
    @Inject(method="render(Lnet/minecraft/client/gui/GuiGraphics;II)V",at=@At("HEAD"),cancellable=true)
    private void omniverse$palette(GuiGraphics graphics,int x,int y,CallbackInfo ci){
        var region=(AllGuiTextures)(Object)this;var location=palette(region.location);
        if(location==null)return;
        graphics.blit(location,
                x,y,region.startX,region.startY,region.width,region.height);
        ci.cancel();
    }
}
