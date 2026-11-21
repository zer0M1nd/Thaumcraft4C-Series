package tc4revival.mixin;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.*;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tc4revival.client.BookTypography;

/** Includes third-party CUSTOM pages, but never processes already wrapped visual-order lines. */
@Mixin(value=Font.class,remap=false)
public abstract class BookFontTypographyMixin {
    @ModifyVariable(method={"width(Lnet/minecraft/network/chat/FormattedText;)I", "split(Lnet/minecraft/network/chat/FormattedText;I)Ljava/util/List;","m_92852_","m_92923_"},at=@At("HEAD"),argsOnly=true,require=2)
    private FormattedText tc4$bookText(FormattedText text){return BookTypography.active()?BookTypography.normalize(text):text;}
    @Inject(method={"width(Ljava/lang/String;)I","m_92895_"},at=@At("HEAD"),cancellable=true)
    private void tc4$bookWidth(String text,CallbackInfoReturnable<Integer> result) {
        if(text!=null&&BookTypography.active())result.setReturnValue(((Font)(Object)this).width(BookTypography.text(text)));
    }
    @ModifyVariable(method={"drawInBatch(Lnet/minecraft/network/chat/Component;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)I","m_272077_"},at=@At("HEAD"),argsOnly=true)
    private Component tc4$bookComponent(Component text){return BookTypography.active()?BookTypography.normalize(text):text;}
    @Inject(method={"drawInBatch(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;IIZ)I","m_272078_"},at=@At("HEAD"),cancellable=true)
    private void tc4$bookString(String text,float x,float y,int color,boolean shadow,Matrix4f pose,MultiBufferSource buffers,Font.DisplayMode mode,int background,int light,boolean bidi,CallbackInfoReturnable<Integer> result) {
        if(text!=null&&BookTypography.active())result.setReturnValue(((Font)(Object)this).drawInBatch(BookTypography.text(text).getVisualOrderText(),x,y,color,shadow,pose,buffers,mode,background,light));
    }
}
