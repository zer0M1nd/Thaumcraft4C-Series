package tc4revival.client.search;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import tc4revival.TC4Revival;

/** Each character keeps all readings supplied by JECharacters, without expanding their Cartesian product. */
record JecPronunciation(List<List<String>> syllables) {
    static final class Dictionary {
        private final Object context;
        private final Method getChar;
        private final Method readings;
        private final Map<Character, List<String>> cache = new HashMap<>();

        private Dictionary() throws ReflectiveOperationException {
            context = Class.forName("me.towdium.jecharacters.utils.Match").getField("context").get(null);
            getChar = context.getClass().getMethod("getChar", char.class);
            readings = getChar.getReturnType().getMethod("pinyins");
        }

        static Dictionary open() {
            try {
                return new Dictionary();
            } catch (ClassNotFoundException absent) {
                return null;
            } catch (ReflectiveOperationException | LinkageError error) {
                TC4Revival.LOGGER.warn("Could not load JECharacters aspect name dictionary", error);
                return null;
            }
        }

        JecPronunciation spell(String text) {
            if (text.codePoints().noneMatch(c -> Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN))
                return null;
            var result = new ArrayList<List<String>>();
            for (char ch : text.toCharArray()) result.add(cache.computeIfAbsent(ch, this::read));
            return new JecPronunciation(result);
        }

        private List<String> read(char ch) {
            var result = new LinkedHashSet<String>();
            try {
                for (Object reading : (Object[]) readings.invoke(getChar.invoke(context, ch))) {
                    String raw = reading.toString().replace('ü', 'v');
                    if (!raw.isEmpty() && Character.isDigit(raw.charAt(raw.length() - 1)))
                        raw = raw.substring(0, raw.length() - 1);
                    if (!raw.isEmpty()) result.add(raw);
                }
            } catch (ReflectiveOperationException failure) {
                TC4Revival.LOGGER.debug("No JECharacters reading for {}", ch, failure);
            }
            return result.isEmpty() ? List.of(String.valueOf(ch)) : List.copyOf(result);
        }
    }

    boolean matches(String term, boolean exact) {
        if (term.isEmpty()) return false;
        term = term.replace('ü', 'v');
        var positions = new BitSet();
        positions.set(0);
        for (int i = 0; i < syllables.size(); i++) {
            var next = new BitSet();
            for (String word : syllables.get(i)) {
                for (int pos = positions.nextSetBit(0); pos >= 0; pos = positions.nextSetBit(pos + 1)) {
                    int length = Math.min(word.length(), term.length() - pos);
                    if (!term.regionMatches(pos, word, 0, length)) continue;
                    if (pos + length < term.length()) next.set(pos + length);
                    else if (!exact || i + 1 == syllables.size() && length == word.length()) return true;
                }
                if (!exact) {
                    for (int start = 0; start < word.length(); start++) {
                        int length = Math.min(term.length(), word.length() - start);
                        if (!term.regionMatches(0, word, start, length)) continue;
                        if (length == term.length()) return true;
                        next.set(length);
                    }
                }
            }
            positions = next;
            if (exact && positions.isEmpty()) return false;
        }
        return false;
    }
}
