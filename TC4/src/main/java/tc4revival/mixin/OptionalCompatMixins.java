package tc4revival.mixin;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.*;

public final class OptionalCompatMixins implements IMixinConfigPlugin {
    @Override public boolean shouldApplyMixin(String target,String mixin) {
        if (target.startsWith("appeng.") || target.startsWith("mezz.jei.")) {
            String id = target.startsWith("appeng.") ? "ae2" : "jei";
            return net.minecraftforge.fml.loading.FMLLoader.getLoadingModList().getMods().stream()
                    .anyMatch(mod -> mod.getModId().equals(id));
        }
        if(mixin.substring(mixin.lastIndexOf('.')+1).startsWith("EasyVillagers"))
            return net.minecraftforge.fml.loading.FMLLoader.getLoadingModList().getMods().stream().anyMatch(mod->mod.getModId().equals("easy_villagers"));
        if(!mixin.substring(mixin.lastIndexOf('.')+1).startsWith("AutoResearch"))return true;
        return net.minecraftforge.fml.loading.FMLLoader.getLoadingModList().getMods().stream()
                .anyMatch(mod->mod.getModId().equals("tc4auto")&&mod.getVersion().toString().equals("1.0.6"));
    }
    @Override public void onLoad(String pkg) {}
    @Override public String getRefMapperConfig(){return null;}
    @Override public void acceptTargets(Set<String> mine,Set<String> others) {}
    @Override public List<String> getMixins(){return null;}
    @Override public void preApply(String target,ClassNode node,String mixin,IMixinInfo info) {}
    @Override public void postApply(String target,ClassNode node,String mixin,IMixinInfo info) {}
}
