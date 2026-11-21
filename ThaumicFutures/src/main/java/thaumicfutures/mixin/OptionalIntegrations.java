package thaumicfutures.mixin;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import net.minecraftforge.fml.loading.FMLLoader;

/** No optional mod classes are loaded while deciding which mixins to apply. */
public final class OptionalIntegrations implements IMixinConfigPlugin {
    private static final Map<String, String> MODS = Map.of(
        "BotaniaRunicMixin", "botania", "BotaniaArmorSetMixin", "botania", "ApotheosisAllStatsMixin", "apotheosis",
        "CreateBehaviourMixin", "create", "CreateVoidDurabilityMixin", "create");
    @Override public boolean shouldApplyMixin(String target, String mixin) {
        if(mixin.endsWith("ExDeorumJeiCacheMixin")||mixin.endsWith("ExDeorumHeatRecipeAccessor")||mixin.endsWith("ExDeorumHeatDisplayMixin"))return FMLLoader.getLoadingModList().getModFileById("exdeorum")!=null
                && FMLLoader.getLoadingModList().getModFileById("jei")!=null;
        if(mixin.endsWith("ExDeorumHeatMixin"))return FMLLoader.getLoadingModList().getModFileById("exdeorum")!=null;
        if(mixin.endsWith("QuarkAncientForestColorsMixin"))return FMLLoader.getLoadingModList().getModFileById("quark")!=null;
        if(mixin.endsWith("FocusSpeedHintMixin"))return FMLLoader.getLoadingModList().getModFileById("create")!=null;

        if (mixin.endsWith("JeiFiniteFluidMixin")) return FMLLoader.getLoadingModList().getModFileById("jei") != null;
        if (mixin.endsWith("GregTechAspectTooltipMixin")) return FMLLoader.getLoadingModList().getModFileById("gtceu") != null && FMLLoader.getLoadingModList().getModFileById("jei") != null;
        if (mixin.endsWith("CreateIc2RecipesMixin")) return FMLLoader.getLoadingModList().getModFileById("create") != null && FMLLoader.getLoadingModList().getModFileById("ic2") != null;
        if (mixin.endsWith("Ic2FluidContainersMixin")) return FMLLoader.getLoadingModList().getModFileById("ic2") != null;
        if (mixin.substring(mixin.lastIndexOf('.')+1).startsWith("GregTech")) return FMLLoader.getLoadingModList().getModFileById("gtceu") != null;
        if (mixin.endsWith("CeiAuraFillingMixin")||mixin.endsWith("CeiAuraSpoutMixin")) return FMLLoader.getLoadingModList().getModFileById("create") != null;
        if (mixin.substring(mixin.lastIndexOf('.')+1).startsWith("Cei")) return FMLLoader.getLoadingModList().getModFileById("create") != null && FMLLoader.getLoadingModList().getModFileById("create_enchantment_industry") != null;
        if (mixin.endsWith("CreateAssemblyTooltipMixin")) return FMLLoader.getLoadingModList().getModFileById("create") != null && FMLLoader.getLoadingModList().getModFileById("jei") != null;
        if (mixin.substring(mixin.lastIndexOf('.')+1).startsWith("Create")) return FMLLoader.getLoadingModList().getModFileById("create") != null;
        if (mixin.substring(mixin.lastIndexOf('.') + 1).startsWith("Apotheosis")) return FMLLoader.getLoadingModList().getModFileById("apotheosis") != null;
        if (mixin.endsWith("AtmRecipeReloadMixin")) return true;
        if (mixin.endsWith("TinkersFeatureSlotsMixin")) return FMLLoader.getLoadingModList().getModFileById("tconstruct") != null;
        if (mixin.substring(mixin.lastIndexOf('.') + 1).startsWith("Productive")) return FMLLoader.getLoadingModList().getModFileById("productivebees") != null;
        if (mixin.substring(mixin.lastIndexOf('.') + 1).startsWith("Mystical")) return FMLLoader.getLoadingModList().getModFileById("mysticalagriculture") != null;
        if (mixin.endsWith("AttributeDisplayMixin")) return FMLLoader.getLoadingModList().getModFileById("attributeslib") != null;
        if (mixin.substring(mixin.lastIndexOf('.') + 1).startsWith("Forestry"))
            return FMLLoader.getLoadingModList().getModFileById("forestry") != null
                    && FMLLoader.getLoadingModList().getModFileById("mobees") != null;
        if (mixin.endsWith("MachineryJeiTextMixin"))
            return FMLLoader.getLoadingModList().getModFileById("modular_machinery_reborn") != null
                    && FMLLoader.getLoadingModList().getModFileById("jei") != null;
        if (mixin.substring(mixin.lastIndexOf('.') + 1).startsWith("Machinery"))
            return FMLLoader.getLoadingModList().getModFileById("modular_machinery_reborn") != null;
        if (mixin.substring(mixin.lastIndexOf('.') + 1).startsWith("Backpack"))
            return FMLLoader.getLoadingModList().getModFileById("sophisticatedbackpacks") != null;
        if (mixin.substring(mixin.lastIndexOf('.') + 1).startsWith("Atm"))
            return FMLLoader.getLoadingModList().getModFileById("allthemodium") != null;
        if (mixin.substring(mixin.lastIndexOf('.') + 1).startsWith("Industrial"))
            return FMLLoader.getLoadingModList().getModFileById("industrialforegoing") != null;
        if (mixin.substring(mixin.lastIndexOf('.') + 1).startsWith("Avaritia"))
            return FMLLoader.getLoadingModList().getModFileById("avaritia") != null;
        String mod = MODS.get(mixin.substring(mixin.lastIndexOf('.') + 1));
        return mod == null || FMLLoader.getLoadingModList().getModFileById(mod) != null;
    }
    @Override public void onLoad(String pkg) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> mine, Set<String> others) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
