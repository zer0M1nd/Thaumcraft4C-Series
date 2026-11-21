package wgmodern.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=net.minecraft.client.resources.language.ClientLanguage.class,remap=false)
public class ResearchAliasLanguageMixin {
 @ModifyVariable(method={"getOrDefault","m_118919_"},at=@At("HEAD"),argsOnly=true,ordinal=0)
 private String wg$originalLabel(String key){return wgmodern.ResearchAliases.translation(key);}
}
