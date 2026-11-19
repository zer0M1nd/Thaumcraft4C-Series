package tc4revival.client;

import java.lang.reflect.Method;
import java.util.Locale;
import org.slf4j.LoggerFactory;

/** Optional JEC bridge: resolves once and uses JEC's configured PinIn context. */
public class ClientTextSearch {
    private static boolean resolved;
    private static Method contains;

    public static boolean contains(String text, String query) {
        text = text.toLowerCase(Locale.ROOT);
        query = query.toLowerCase(Locale.ROOT);
        if (text.contains(query)) return true;
        if (!resolved) {
            resolved = true;
            try {
                contains = Class.forName("me.towdium.jecharacters.utils.Match")
                        .getMethod("contains", String.class, CharSequence.class);
            } catch (ClassNotFoundException ignored) {
                // The addon is optional.
            } catch (ReflectiveOperationException | LinkageError error) {
                LoggerFactory.getLogger(ClientTextSearch.class).warn("Cannot initialize optional JEC search", error);
            }
        }
        if (contains != null) try {
            return (boolean) contains.invoke(null, text, query);
        } catch (ReflectiveOperationException | LinkageError error) {
            contains = null;
            LoggerFactory.getLogger(ClientTextSearch.class).warn("JEC search failed; using literal search", error);
        }
        return false;
    }
}
