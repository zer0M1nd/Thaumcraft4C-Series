package tc4revival.client;

import com.google.gson.*;
import java.util.*;
import net.minecraft.network.chat.*;
import net.minecraft.locale.Language;
import net.minecraft.util.StringDecomposer;
import tc4revival.research.*;

/** Import boundary for external original translations. Output uses the same typed flow as new data packs. */
public final class BookLegacyImport {
    private record Word(String text,JsonObject component,boolean latin) {}
    private static Language language;
    private static ResearchDefinitions.Snapshot definitions;
    private static List<Word> words=List.of();
    private BookLegacyImport() {}
    private static List<Word> glossary() {
        if(language==Language.getInstance()&&definitions==ResearchDefinitions.display())return words;
        language=Language.getInstance();definitions=ResearchDefinitions.display();var result=new ArrayList<Word>();
        for(var aspect:tc4revival.aspect.TCAspects.catalog().all()) {
            var json=new JsonObject();json.addProperty("aspect",aspect.tag());
            result.add(new Word(aspect.displayName(),json,true));
            String description=language.getOrDefault(aspect.descriptionKey());
            if(!description.equals(aspect.descriptionKey())) {
                for(String alias:List.of(description+"要素",description+"之要素",description+"系要素")) {
                    result.add(new Word(alias,json,false));
                    // A bilingual mention is one reference. Keep its original explanatory suffix.
                    for(var brackets:List.of(new String[]{"(",")"},new String[]{"（","）"})) {
                        String suffix=brackets[0]+alias+brackets[1];var combined=json.deepCopy();
                        var explanation=new JsonObject();explanation.addProperty("text",suffix);
                        explanation.addProperty("underlined",false);explanation.addProperty("color","black");
                        var extra=new JsonArray();extra.add(explanation);combined.add("extra",extra);
                        result.add(new Word(aspect.displayName()+suffix,combined,true));
                    }
                }
            }
        }
        for(var research:ResearchCatalogs.bundled().all()) {
            // Navigation mirrors and tutorials may share a title with gameplay research.
            // Only the original research owns completion and a prose link target.
            if(research.mirror() || research.tutorial() || !ResearchCatalogs.enabled(research))continue;
            String key="tc.research_name."+research.key(),name=language.getOrDefault(key);
            if(name.equals(key)||name.length()<2||Set.of("研究","要素","魔法","基础").contains(name))continue;
            var json=new JsonObject();json.addProperty("research",research.key());
            result.add(new Word(name,json,name.codePoints().allMatch(c->c<128)));
        }
        result.sort(Comparator.<Word>comparingInt(w->w.text().length()).reversed());words=List.copyOf(result);return words;
    }
    public static Component prose(String text,Style initial) {
        return prose(text,initial,"");
    }
    public static Component prose(String text,Style initial,String research) {
        return prose(text,initial,research,AspectTextColorMode.DEFAULT);
    }
    public static Component prose(String text,Style initial,String research,AspectTextColorMode defaultColorMode) {
        text=withoutAdjacentResearchFormatting(text,initial,research);
        var plain=new StringBuilder();var styles=new ArrayList<Style>();
        StringDecomposer.iterateFormatted(text,initial,(index,style,code)->{
            plain.appendCodePoint(code);for(int i=0;i<Character.charCount(code);i++)styles.add(style);return true;
        });
        return link(plain.toString(),styles,research,defaultColorMode);
    }
    public static Component prose(Component source, String research) {
        return prose(source,research,AspectTextColorMode.DEFAULT);
    }
    public static Component prose(Component source,String research,AspectTextColorMode defaultColorMode) {
        var text = new StringBuilder(); var styles = new ArrayList<Style>();
        source.visit((style, value) -> {
            StringDecomposer.iterateFormatted(value, style, (index, s, code) -> {
                text.appendCodePoint(code); for (int i = 0; i < Character.charCount(code); i++) styles.add(s); return true;
            });
            return Optional.empty();
        }, Style.EMPTY);
        return link(text.toString(), styles, research,defaultColorMode);
    }
    private static Component plain(String text, Style style) {
        var out = Component.empty(); var run = new StringBuilder(); Style[] current = {style};
        StringDecomposer.iterateFormatted(text, style, (index, s, code) -> {
            if (!s.equals(current[0])) {
                if (!run.isEmpty()) { out.append(Component.literal(run.toString()).withStyle(current[0])); run.setLength(0); }
                current[0] = s;
            }
            run.appendCodePoint(code); return true;
        });
        if (!run.isEmpty()) out.append(Component.literal(run.toString()).withStyle(current[0]));
        return out;
    }
    private static boolean latinLetter(char c){return Character.isLetter(c)&&Character.UnicodeScript.of(c)==Character.UnicodeScript.LATIN;}
    private static Word match(String text,int offset,List<Word> dictionary) {
        for(var word:dictionary)if(text.regionMatches(word.latin(),offset,word.text(),0,word.text().length())
                &&(!word.latin()||((offset==0||!latinLetter(text.charAt(offset-1)))
                &&(offset+word.text().length()==text.length()||!latinLetter(text.charAt(offset+word.text().length()))))))return word;
        return null;
    }
    private static boolean self(Word word,String research) {
        return word.component().has("research")&&word.component().get("research").getAsString().equals(research);
    }
    /** Remove only format codes immediately preceding an actual external research mention. */
    private static String withoutAdjacentResearchFormatting(String source,Style initial,String research) {
        if(source.indexOf('\u00a7')<0)return source;
        var plain=new StringBuilder();var offsets=new ArrayList<Integer>();
        StringDecomposer.iterateFormatted(source,initial,(index,style,code)->{
            plain.appendCodePoint(code);for(int j=0;j<Character.charCount(code);j++)offsets.add(index+j);return true;
        });
        String text=plain.toString();var dictionary=glossary();var removed=new BitSet(source.length());
        for(int i=0;i<text.length();) {
            var word=match(text,i,dictionary);
            if(word==null){i++;continue;}
            if(word.component().has("research")&&!self(word,research)) {
                int end=offsets.get(i),start=end;
                while(start>=2&&source.charAt(start-2)=='\u00a7'
                        &&net.minecraft.ChatFormatting.getByCode(source.charAt(start-1))!=null)start-=2;
                removed.set(start,end);
            }
            i+=word.text().length();
        }
        if(removed.isEmpty())return source;
        var result=new StringBuilder();for(int i=0;i<source.length();i++)if(!removed.get(i))result.append(source.charAt(i));
        return result.toString();
    }
    private static MutableComponent link(String text,List<Style> styles,String research,AspectTextColorMode defaultColorMode) {
        var result=Component.empty();var buffer=new StringBuilder();var dictionary=glossary();Style current=Style.EMPTY;int literalUntil=0;
        for(int i=0;i<text.length();) {
            if(!styles.get(i).equals(current)) {
                if(!buffer.isEmpty()){result.append(Component.literal(buffer.toString()).withStyle(current));buffer.setLength(0);}
                current=styles.get(i);
            }
            Word match=i<literalUntil||BookTextAmendments.CORRECTION.equals(current.getInsertion())?null:match(text,i,dictionary);
            if(match!=null&&self(match,research)){literalUntil=i+match.text().length();match=null;}
            if(match==null){buffer.append(text.charAt(i++));continue;}
            if(!buffer.isEmpty()){result.append(Component.literal(buffer.toString()).withStyle(current));buffer.setLength(0);}
            var component=BookComponents.decode(match.component(),null,defaultColorMode).copy();
            component.setStyle(component.getStyle().applyTo(current));
            result.append(component);i+=match.text().length();
        }
        if(!buffer.isEmpty())result.append(Component.literal(buffer.toString()).withStyle(current));return result;
    }
    public static List<ResearchPage> expand(ResearchPage page) {
        return expand(page,"");
    }
    public static List<ResearchPage> expand(ResearchPage page,String research) {
        if(!page.properties().has("import_legacy")||!page.properties().get("import_legacy").getAsBoolean())return List.of(page);
        String key=page.text().orElse("");
        String source=withoutAdjacentResearchFormatting(BookTypography.patch(key,Language.getInstance().getOrDefault(key)),Style.EMPTY,research);
        var result=new ArrayList<ResearchPage>();var text=new StringBuilder();Style style=Style.EMPTY;boolean after=false;int index=0;
        for(var part:ResearchText.parseForDisplay(source)) {
            if(part instanceof ResearchText.Text prose){text.append(prose.value());after=false;}
            else if(part instanceof ResearchText.Break){if(after)after=false;else text.append('\n');}
            else {
                if(!text.isEmpty()&&text.charAt(text.length()-1)=='\n')text.setLength(text.length()-1);
                if(!text.isEmpty()){result.add(text(page,index++,text.toString(),style,research));Style[] end={style};
                    StringDecomposer.iterateFormatted(text+"\0",style,(i,s,c)->{end[0]=s;return true;});style=end[0];text.setLength(0);}
                var json=new JsonObject();json.addProperty("id",page.id()+"/part/"+index++);
                if(part instanceof ResearchText.Picture picture) {
                    json.addProperty("type","ILLUSTRATION");json.addProperty("image",picture.texture());
                    json.addProperty("u",picture.u());json.addProperty("v",picture.v());
                    json.addProperty("width",picture.width());json.addProperty("height",picture.height());json.addProperty("scale",picture.scale());
                    BookItemImages.find(picture).ifPresent(items->{
                        json.addProperty("type","ITEM");json.add("items",items);
                        json.addProperty("size",Math.max(1,Math.round(picture.height()*picture.scale())));
                        json.remove("image");
                    });
                }else json.addProperty("type","DIVIDER");
                result.add(ResearchPage.decode(json));after=true;
            }
        }
        if(!text.isEmpty()||result.isEmpty())result.add(text(page,index,text.toString(),style,research));
        return BookAmendments.legacy(page,result,research);
    }
    private static ResearchPage text(ResearchPage page,int index,String text,Style style,String research) {
        var json=new JsonObject();json.addProperty("id",page.id()+"/part/"+index);json.addProperty("type","TEXT");
        json.add("text",Component.Serializer.toJsonTree(plain(text,style)));return ResearchPage.decode(json);
    }
}
