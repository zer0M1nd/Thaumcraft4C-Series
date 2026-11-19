package thaumicomniverse.mixin;

import dev.shadowsoffire.attributeslib.api.IFormattableAttribute;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.Mixin;

/** Use the same formatter for gems, equipment modifiers and the attributes screen. */
@Mixin(Attribute.class)
public abstract class AttributeDisplayMixin implements IFormattableAttribute {
    private boolean omniverse$visCost() {
        String id=ths().getDescriptionId();
        return id.startsWith("attribute.name.thaumcraft.") && id.endsWith("vis_cost");
    }
    @Override public MutableComponent toValueComponent(AttributeModifier.Operation op,double value,TooltipFlag flag) {
        if(omniverse$visCost())return Component.translatable("attributeslib.value.percent",ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(value*100));
        return IFormattableAttribute.super.toValueComponent(op,value,flag);
    }
    @Override public MutableComponent toComponent(AttributeModifier modifier,TooltipFlag flag) {
        var result=IFormattableAttribute.super.toComponent(modifier,flag);
        if(ths().getDescriptionId().equals("attribute.name.thaumcraft.warp"))return result.withStyle(ChatFormatting.DARK_PURPLE);
        if(omniverse$visCost())return result.withStyle(modifier.getAmount()<=0?ChatFormatting.BLUE:ChatFormatting.RED);
        return result;
    }
}
