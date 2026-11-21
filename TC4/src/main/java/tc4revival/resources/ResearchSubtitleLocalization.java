package tc4revival.resources;

import java.util.Set;
import java.util.Optional;
import java.util.regex.Pattern;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/** Remove the legacy addon badges only from Chinese research subtitle translations. */
public final class ResearchSubtitleLocalization {
    private static final Set<String> CHINESE = Set.of("zh_cn", "zh_tw", "zh_hk");
    private static final Pattern PREFIX = Pattern.compile(
            "^((?:§[0-9a-fk-or])*)[ \\t]*(?:\\[[a-z][a-z0-9_+&.: -]{0,31}\\][ \\t]*)+",
            Pattern.CASE_INSENSITIVE);

    private ResearchSubtitleLocalization() {}

    public static String normalize(String language, String key, String value) {
        if(key.startsWith("tc.research_name.") && key.substring("tc.research_name.".length()).matches("(?:[a-z0-9_]+:)?TUT_[A-Z0-9_]+")) {
            String prefix=CHINESE.contains(language) || value.codePoints().anyMatch(c->Character.UnicodeScript.of(c)==Character.UnicodeScript.HAN)
                    ? "教程：" : "Tutorial: ";
            return value.startsWith(prefix)?value:prefix+value;
        }
        if (!CHINESE.contains(language) || !key.startsWith("tc.research_text.")) return value;
        return PREFIX.matcher(value).replaceFirst("$1");
    }

    /** Addons can also prepend styled components after the translation has been loaded. */
    public static MutableComponent normalize(String language, Component source) {
        if (!CHINESE.contains(language)) return source.copy();
        var prefix = PREFIX.matcher(source.getString());
        if (!prefix.find()) return source.copy();
        int removeStart = prefix.end(1), removeEnd = prefix.end();
        var result = Component.empty();
        int[] position = {0};
        source.visit((style, part) -> {
            int start = position[0];
            position[0] += part.length();
            int before = Math.max(0, Math.min(part.length(), removeStart - start));
            int after = Math.max(0, Math.min(part.length(), removeEnd - start));
            String remaining = part.substring(0, before) + part.substring(after);
            if (!remaining.isEmpty()) result.append(Component.literal(remaining).setStyle(style));
            return Optional.empty();
        }, Style.EMPTY);
        return result;
    }
}
