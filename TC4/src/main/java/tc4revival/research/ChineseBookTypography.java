package tc4revival.research;

import java.util.*;
import java.util.regex.*;

/** Display-only typography. Offsets refer to UTF-16 input, so callers can retain every glyph's style. */
public final class ChineseBookTypography {
    private ChineseBookTypography() {}
    public record Range(int start, int end, char left, char right) {}
    public record Result(String text, int[] origins) {}
    private record Token(String text, int[] origins, char left, char right) {}
    private static final String HAN = "\\p{IsHan}";
    private static final Pattern ENGLISH = Pattern.compile("(?<![A-Za-z0-9_])[A-Za-z][A-Za-z0-9_]*(?:[-'][A-Za-z0-9_]+)*");
    private static final Pattern NUMBER = Pattern.compile("(?<![0-9.])[+-]?(?:\\d{1,3}(?:,\\d{3})+|\\d+)(?:\\.\\d+)*(?:[–~～-]\\d+(?:\\.\\d+)*)?");
    private static final List<Pattern> TECHNICAL = List.of(
            Pattern.compile("(?m)^(?=[ .RLN]*[RLN])[ .RLN]+$"),
            Pattern.compile("(?<![A-Za-z0-9_])(?:https?://|www\\.)[A-Za-z0-9_~:/?#\\[\\]@!$&'()*+,;=%.-]*[A-Za-z0-9_~/#=%-]|(?<![A-Za-z0-9_])[A-Za-z][A-Za-z0-9.-]*\\.(?:us|com|org|net)(?:/[A-Za-z0-9_./-]*[A-Za-z0-9_/-])?"),
            Pattern.compile("(?<![A-Za-z0-9_])[a-z_][a-z0-9_]*:[a-z_][a-z0-9_/-]*(?:\\.[a-z0-9_/-]+)*"),
            Pattern.compile("(?<![A-Za-z0-9_])(?:[A-Z]\\.)+[A-Za-z]+"),
            Pattern.compile("%(?:\\d+\\$)?[-#+ 0,(]*\\d*(?:\\.\\d+)?[A-Za-z%]"),
            Pattern.compile("\\[[A-Za-z][A-Za-z0-9 _-]*\\]"),
            Pattern.compile("(?<=[\\]】])-(?=[\\[【])"),
            Pattern.compile("(?<![A-Za-z0-9_])\\d+(?:[xX×]\\d+)+"));
    public static boolean chinese(String locale) { return "zh_cn".equals(locale) || "zh_tw".equals(locale); }
    public static boolean han(int code) { return Character.UnicodeScript.of(code) == Character.UnicodeScript.HAN; }
    private static char edge(int c) { return han(c)?'H':c<128&&Character.isLetter(c)?'E':Character.isDigit(c)?'N':0; }
    public static String normalize(String text, String locale) { return normalize(text,locale,List.of(),List.of()).text(); }
    /** Protected phrases are supplied by resource packs, never a hard-coded translation dictionary. */
    public static Result normalize(String text, String locale, List<String> phrases, List<Range> ranges) {
        var state = new State(text);
        boolean hasHan = text.codePoints().anyMatch(ChineseBookTypography::han);
        if (!chinese(locale) || !hasHan && ranges.isEmpty()) return state.result();
        // Descending offsets keep the caller's ranges stable while they become opaque tokens.
        ranges.stream().sorted(Comparator.comparingInt(Range::start).reversed()).forEach(r -> state.keep(r.start(),r.end(),r.left(),r.right()));
        // Semantic icons can need external spacing even in an untranslated English fallback.
        // They do not make its punctuation Chinese; structural ranges must not do so either.
        if (!hasHan) { state.spaces(); return state.result(); }
        for (String phrase : phrases) if (!phrase.isEmpty()) state.keep(Pattern.compile(Pattern.quote(phrase)),
                edge(phrase.codePointAt(0)),edge(phrase.codePointBefore(phrase.length())));
        for (int i=0;i<TECHNICAL.size();i++) state.keep(TECHNICAL.get(i),i>=1&&i<=3?'E':0,i>=1&&i<=3?'E':0);
        // Also allow already tokenized ASCII menu labels on either side.
        state.replace(Pattern.compile("-"), m -> {
            int i=m.start(); return i>0&&i+1<state.text.length()&&state.tokenEnds(i-1,"]")&&state.tokenStarts(i+1,"[") ? state.protectMatch(m,(char)0,(char)0) : "-";
        });
        state.keep(Pattern.compile("(?<=[\\p{IsHan}A-Za-z])\\.\\d+"), (char)0, 'N'); // semantic decimal/sentence ambiguity
        state.keep(ENGLISH,'E','E');
        state.keep(Pattern.compile("(?<![\\p{IsHan}A-Za-z0-9_.\\p{Co}])[+-]?\\.\\d+"),'N','N');
        state.keep(NUMBER,'N','N');
        state.replace(Pattern.compile("\\.{2,}|…+"), m -> "……");
        state.replace(Pattern.compile("[ \\t]*[-—]+[ \\t]*"), m -> "——");
        String from=",.!?;:()[]", to="，。！？；：（）【】";
        state.replace(Pattern.compile("[,\\.!?;:()\\[\\]]"), m -> String.valueOf(to.charAt(from.indexOf(m.group()))));
        state.quotes();
        state.replace(Pattern.compile("(?<="+HAN+")[ \\t]+(?="+HAN+")"), m -> "");
        state.replace(Pattern.compile("(?<=\\S)[ \\t]+(?=[，。！？；：、）】”’])"), m -> "");
        state.replace(Pattern.compile("(?<=[，。！？；：、（【“‘])[ \\t]+"), m -> "");
        // Vanilla prefers the last ordinary space when wrapping Chinese prose.
        // Keep a list marker attached to its first word instead of leaving it
        // alone on a line when the rest of the item exceeds the sheet width.
        state.replace(Pattern.compile("(?m)(?<=[·•])[ \\t]+(?=\\S)"), m -> "\u00a0");
        state.spaces();
        return state.result();
    }
    private static final class State {
        String text;
        int[] origins;
        final Map<Character,Token> tokens = new HashMap<>();
        int next = 0xf000;
        State(String text) { this.text=text; origins=new int[text.length()]; for(int i=0;i<origins.length;i++)origins[i]=i; }
        String token(String value,int[] source,char left,char right) {
            while(next<=0xf8ff&&(text.indexOf((char)next)>=0||tokens.containsKey((char)next)))next++;
            if(next>0xf8ff)throw new IllegalArgumentException("Too many protected typography tokens");
            char id=(char)next++;tokens.put(id,new Token(value,source,left,right));return String.valueOf(id);
        }
        String protectMatch(Matcher m,char left,char right) { return token(m.group(),Arrays.copyOfRange(origins,m.start(),m.end()),left,right); }
        void keep(Pattern pattern,char left,char right) { replace(pattern,m -> protectMatch(m,left,right)); }
        void keep(int start,int end,char left,char right) {
            if(start<0||end>text.length()||end<=start)return;
            String value=token(text.substring(start,end),Arrays.copyOfRange(origins,start,end),left,right);
            String before=text;int[] old=origins;
            text=before.substring(0,start)+value+before.substring(end);origins=new int[text.length()];
            System.arraycopy(old,0,origins,0,start);origins[start]=old[start];System.arraycopy(old,end,origins,start+1,old.length-end);
        }
        void replace(Pattern pattern,java.util.function.Function<Matcher,String> fn) {
            var matcher=pattern.matcher(text);var out=new StringBuilder();var offsets=new ArrayList<Integer>();int cursor=0;
            while(matcher.find()) {
                append(out,offsets,text.substring(cursor,matcher.start()),origins,cursor);
                String value=fn.apply(matcher);
                if(value.equals(matcher.group()))append(out,offsets,value,origins,matcher.start());
                else {out.append(value);for(int i=0;i<value.length();i++)offsets.add(origins[matcher.start()]);}
                cursor=matcher.end();
            }
            if(cursor==0)return;
            append(out,offsets,text.substring(cursor),origins,cursor);text=out.toString();origins=offsets.stream().mapToInt(Integer::intValue).toArray();
        }
        static void append(StringBuilder out,List<Integer> offsets,String value,int[] source,int start) {
            out.append(value);for(int i=0;i<value.length();i++)offsets.add(source[start+i]);
        }
        boolean tokenEnds(int i,String end) { var t=tokens.get(text.charAt(i));return t!=null&&t.text().endsWith(end); }
        boolean tokenStarts(int i,String start) { var t=tokens.get(text.charAt(i));return t!=null&&t.text().startsWith(start); }
        void quotes() {
            record Open(char kind,int offset,int depth) {}
            var stack=new ArrayDeque<Open>();char[] value=text.toCharArray();
            for(int i=0;i<value.length;i++) {
                char c=value[i];
                if(c=='“'||c=='‘')stack.push(new Open(c,i,stack.size()));
                else if(c=='”'||c=='’') {if(!stack.isEmpty()&&stack.peek().kind()==(c=='”'?'“':'‘'))stack.pop();}
                else if(c=='"'||c=='\'') {
                    if(!stack.isEmpty()&&stack.peek().kind()==c) {var open=stack.pop();boolean outer=open.depth()%2==0;value[open.offset()]=outer?'“':'‘';value[i]=outer?'”':'’';}
                    else stack.push(new Open(c,i,stack.size()));
                }
            }
            text=new String(value); // unpaired quotes deliberately remain literal
        }
        char boundary(int i,boolean left) {
            var t=tokens.get(text.charAt(i));return t==null?edge(left?text.codePointAt(i):text.codePointBefore(i+1)):left?t.left():t.right();
        }
        void spaces() {
            var out=new StringBuilder();var offsets=new ArrayList<Integer>();
            for(int i=0;i<text.length();i++) {
                if(i>0) {char a=boundary(i-1,false),b=boundary(i,true);
                    if(a=='H'&&b=='E'||a=='E'&&b=='H'||a=='A'&&"HENA".indexOf(b)>=0&&b!=0||b=='A'&&"HENA".indexOf(a)>=0&&a!=0) {
                        out.append(' ');offsets.add(-1);
                    }
                }
                out.append(text.charAt(i));offsets.add(origins[i]);
            }
            text=out.toString();origins=offsets.stream().mapToInt(Integer::intValue).toArray();
        }
        void expand(String value,int[] source,StringBuilder out,List<Integer> offsets) {
            for(int i=0;i<value.length();i++) {var t=tokens.get(value.charAt(i));
                if(t!=null)expand(t.text(),t.origins(),out,offsets);else {out.append(value.charAt(i));offsets.add(source[i]);}}
        }
        Result result() {var out=new StringBuilder();var offsets=new ArrayList<Integer>();expand(text,origins,out,offsets);return new Result(out.toString(),offsets.stream().mapToInt(Integer::intValue).toArray());}
    }
}
