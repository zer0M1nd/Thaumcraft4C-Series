package thaumicomniverse.mixin;

import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumicomniverse.research.UniversalEnchantments;

/** Dynamic research keeps the installed enchantment's own localized name, including resource packs. */
@Mixin(value=ClientLanguage.class,remap=false)
public abstract class DynamicResearchLanguageMixin {
    @Unique private static net.minecraft.world.item.enchantment.Enchantment enchantment(String key) {
        String suffix;
        if(key.startsWith("tc.research_name."+UniversalEnchantments.PREFIX))suffix=key.substring(("tc.research_name."+UniversalEnchantments.PREFIX).length());
        else if(key.startsWith("tc.research_text."+UniversalEnchantments.PREFIX))suffix=key.substring(("tc.research_text."+UniversalEnchantments.PREFIX).length());
        else return null;
        int slash=suffix.indexOf('/');if(slash<1)return null;
        var id=ResourceLocation.tryParse(suffix.substring(0,slash)+":"+suffix.substring(slash+1));
        return id==null?null:ForgeRegistries.ENCHANTMENTS.getValue(id);
    }
    @Inject(method={"getOrDefault","m_118919_"},at=@At("RETURN"),cancellable=true,remap=false)
    private void omniverse$name(String key,String fallback,CallbackInfoReturnable<String> cir) {
        var language=(ClientLanguage)(Object)this;if(language.getLanguageData().containsKey(key))return;
        var enchantment=enchantment(key);if(enchantment==null)return;
        cir.setReturnValue(key.startsWith("tc.research_name.")?language.getOrDefault(enchantment.getDescriptionId()):language.getOrDefault("omniverse.research.universal_enchantment"));
    }
    @Inject(method={"has","m_6722_"},at=@At("RETURN"),cancellable=true,remap=false)
    private void omniverse$has(String key,CallbackInfoReturnable<Boolean> cir) { if(!cir.getReturnValue()&&enchantment(key)!=null)cir.setReturnValue(true); }
}
