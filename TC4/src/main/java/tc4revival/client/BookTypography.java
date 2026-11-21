package tc4revival.client;

import java.util.*;
import java.util.regex.Pattern;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.StringDecomposer;
import tc4revival.research.ChineseBookTypography;

/** One book-only display boundary for native components, imported prose and addon renderers. */
public final class BookTypography {
    private record Glyph(char value,Style style) {}
    private record Marker(int offset,Component component) {}
    private static final ThreadLocal<Integer> scope=ThreadLocal.withInitial(()->0);
    private static Language language;
    private static String selected="";
    private static long generation=-1;
    private static final Map<List<Glyph>,Component> cache=new LinkedHashMap<>(128,.75f,true) {
        @Override protected boolean removeEldestEntry(Map.Entry<List<Glyph>,Component> entry){return size()>256;}
    };
    private static final Pattern ARGUMENT=Pattern.compile("%(?:(\\d+)\\$)?([A-Za-z%]|$)");
    private BookTypography() {}
    public static String locale() {var mc=Minecraft.getInstance();return mc==null||mc.getLanguageManager()==null?"":mc.getLanguageManager().getSelected();}
    public static boolean active(){return scope.get()>0&&ChineseBookTypography.chinese(locale());}
    public static <T> T inBook(Supplier<T> action) {scope.set(scope.get()+1);try{return action.get();}finally{scope.set(scope.get()-1);}}
    public static void inBook(Runnable action){inBook(()->{action.run();return null;});}
    public static String patch(String key,String value){return thaumcraft.api.client.ResearchPresentationClient.text(locale(),key,BookTextResources.patch(locale(),key,value));}
    public static Component text(String source){return normalize(Component.literal(source));}
    public static Component normalize(FormattedText source) {return normalize(source,locale());}
    public static Component normalize(FormattedText source,String locale) {
        if(!ChineseBookTypography.chinese(locale))return source instanceof Component c?c:literal(source);
        if(language!=Language.getInstance()||!selected.equals(locale)||generation!=BookTextResources.generation()) {
            cache.clear();language=Language.getInstance();selected=locale;generation=BookTextResources.generation();
        }
        var glyphs=new ArrayList<Glyph>();
        FormattedText patched=source instanceof Component c?patchComponents(c,locale):source;
        patched.visit((style,value)->{
            String text=BookTextResources.patchResolved(locale,value);
            StringDecomposer.iterateFormatted(text,style,(offset,s,code)->{for(char c:Character.toChars(code))glyphs.add(new Glyph(c,s));return true;});
            return Optional.empty();
        },Style.EMPTY);
        var key=List.copyOf(glyphs);var cached=cache.get(key);if(cached!=null)return cached;
        var plain=new StringBuilder();var styles=new ArrayList<Style>();var markers=new ArrayList<Marker>();
        for(var glyph:glyphs) {
            String insertion=glyph.style().getInsertion();
            if(insertion!=null&&insertion.startsWith(BookFlow.ANCHOR))markers.add(new Marker(plain.length(),Component.literal(String.valueOf(glyph.value())).withStyle(glyph.style())));
            else {plain.append(glyph.value());styles.add(glyph.style());}
        }
        var ranges=new ArrayList<ChineseBookTypography.Range>();
        for(int i=0;i<plain.length();i++) {
            var style=styles.get(i);String insertion=style.getInsertion();
            if(insertion!=null&&insertion.startsWith(BookComponents.ICON)) {
                int end=i+1;var click=style.getClickEvent();
                while(end<plain.length()&&Character.isLetter(plain.charAt(end))&&Objects.equals(click,styles.get(end).getClickEvent()))end++;
                ranges.add(new ChineseBookTypography.Range(i,end,'A','A'));i=end-1;
            } else if(plain.charAt(i)=='?'&&i+2<plain.length()&&plain.charAt(i+1)=='?'&&plain.charAt(i+2)=='?'&&style.getClickEvent()!=null
                    &&style.getClickEvent().getValue().startsWith(BookComponents.LINK+"research/")
                    &&i+2<styles.size()&&Objects.equals(style.getClickEvent(),styles.get(i+1).getClickEvent())
                    &&Objects.equals(style.getClickEvent(),styles.get(i+2).getClickEvent())) {
                // A hidden research name is a semantic placeholder, not prose question marks.
                ranges.add(new ChineseBookTypography.Range(i,i+3,(char)0,(char)0));i+=2;
            } else if((plain.charAt(i)==' '||plain.charAt(i)=='\t')&&(style.isUnderlined()
                    ||i>0&&",.!?;:，。！？；：".indexOf(plain.charAt(i-1))<0&&(styles.get(i-1).isUnderlined()
                    ||styles.get(i-1).getColor()!=null&&styles.get(i-1).getColor().getValue()==0xaa00aa))) {
                int end=i+1;while(end<plain.length()&&(plain.charAt(end)==' '||plain.charAt(end)=='\t'))end++;
                ranges.add(new ChineseBookTypography.Range(i,end,(char)0,(char)0));i=end-1;
            }
        }
        var result=ChineseBookTypography.normalize(plain.toString(),locale,BookTextResources.protect(locale),ranges);
        var output=Component.empty();var buffer=new StringBuilder();Style current=Style.EMPTY;int marker=0;
        for(int i=0;i<result.text().length();i++) {
            int origin=result.origins()[i];Style style=origin<0?Style.EMPTY:styles.get(origin);
            if(!current.equals(style)||origin>=0&&marker<markers.size()&&markers.get(marker).offset()<=origin) {
                if(!buffer.isEmpty()){output.append(Component.literal(buffer.toString()).withStyle(current));buffer.setLength(0);}
                while(origin>=0&&marker<markers.size()&&markers.get(marker).offset()<=origin)output.append(markers.get(marker++).component());
                current=style;
            }
            buffer.append(result.text().charAt(i));
        }
        if(!buffer.isEmpty())output.append(Component.literal(buffer.toString()).withStyle(current));
        while(marker<markers.size())output.append(markers.get(marker++).component());
        cache.put(key,output);return output;
    }
    private static Component literal(FormattedText source) {
        var out=Component.empty();source.visit((style,text)->{out.append(Component.literal(text).withStyle(style));return Optional.empty();},Style.EMPTY);return out;
    }
    private static Component patchComponents(Component source,String locale) {
        MutableComponent out;
        if(source.getContents() instanceof TranslatableContents tr) {
            Object[] args=tr.getArgs().clone();for(int i=0;i<args.length;i++)if(args[i] instanceof Component c)args[i]=patchComponents(c,locale);
            String before=tr.getFallback()==null?Language.getInstance().getOrDefault(tr.getKey()):Language.getInstance().getOrDefault(tr.getKey(),tr.getFallback());
            String after=BookTextResources.patch(locale,tr.getKey(),before);
            out=before.equals(after)?MutableComponent.create(new TranslatableContents(tr.getKey(),tr.getFallback(),args)):format(tr,after,args);
        } else out=MutableComponent.create(source.getContents());
        out.setStyle(source.getStyle());for(var child:source.getSiblings())out.append(patchComponents(child,locale));return out;
    }
    private static MutableComponent format(TranslatableContents tr,String template,Object[] args) {
        var out=Component.empty();var matcher=ARGUMENT.matcher(template);int next=0,cursor=0;
        try {
            while(matcher.find()) {
                String literal=template.substring(cursor,matcher.start());if(literal.indexOf('%')>=0)throw new IllegalArgumentException();out.append(literal);
                if(matcher.group().equals("%%"))out.append("%");
                else {if(!matcher.group(2).equals("s"))throw new IllegalArgumentException();int index=matcher.group(1)==null?next++:Integer.parseInt(matcher.group(1))-1;
                    Object arg=args[index];out.append(arg instanceof Component c?c:Component.literal(String.valueOf(arg)));}
                cursor=matcher.end();
            }
            if(cursor==0)cursor=net.minecraftforge.internal.TextComponentMessageFormatHandler.handle(tr,p->out.append(literal(p)),args,template);
            String tail=template.substring(cursor);if(tail.indexOf('%')>=0)throw new IllegalArgumentException();out.append(tail);return out;
        }catch(IllegalArgumentException|IndexOutOfBoundsException failure){return Component.literal(template);}
    }
}
