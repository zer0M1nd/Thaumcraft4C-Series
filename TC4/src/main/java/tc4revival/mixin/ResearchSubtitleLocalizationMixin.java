package tc4revival.mixin;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.locale.Language;
import net.minecraft.server.packs.resources.Resource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Group;
import tc4revival.resources.ResearchSubtitleLocalization;

/** Normalize each locale before merging it, preserving English fallback entries. */
@Mixin(value = ClientLanguage.class, remap = false)
public abstract class ResearchSubtitleLocalizationMixin {
    @Group(name = "tc4$researchSubtitles", min = 1, max = 1)
    @Redirect(method = {"appendFrom", "m_235035_"}, require = 0, at = @At(value = "INVOKE", target =
            "Lnet/minecraft/locale/Language;loadFromJson(Ljava/io/InputStream;Ljava/util/function/BiConsumer;)V"))
    private static void tc4$researchSubtitles(InputStream stream, BiConsumer<String, String> sink,
            String language, List<Resource> resources, Map<String, String> translations) {
        Language.loadFromJson(stream, (key, value) ->
                sink.accept(key, ResearchSubtitleLocalization.normalize(language, key, value)));
    }
    @Group(name = "tc4$researchSubtitles", min = 1, max = 1)
    @Redirect(method = {"appendFrom", "m_235035_"}, require = 0, at = @At(value = "INVOKE", target =
            "Lnet/minecraft/locale/Language;m_128108_(Ljava/io/InputStream;Ljava/util/function/BiConsumer;)V"))
    private static void tc4$researchSubtitlesProduction(InputStream stream, BiConsumer<String, String> sink,
            String language, List<Resource> resources, Map<String, String> translations) {
        tc4$researchSubtitles(stream, sink, language, resources, translations);
    }
}
