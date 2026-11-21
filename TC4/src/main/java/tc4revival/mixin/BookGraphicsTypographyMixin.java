package tc4revival.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import tc4revival.client.BookTypography;

@Mixin(value=GuiGraphics.class,remap=false)
public abstract class BookGraphicsTypographyMixin {
    @ModifyVariable(method={"drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)I", "drawCenteredString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V","m_280614_","m_280653_"},at=@At("HEAD"),argsOnly=true,require=2)
    private Component tc4$bookComponent(Component text){return BookTypography.active()?BookTypography.normalize(text):text;}
}
