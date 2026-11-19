package tc4revival.client;

import java.util.*;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import tc4revival.research.ResearchPage;

/** One native-font flow, split into physical sheets only after all visible additions have been composed. */
public final class BookFlow {
    public static final String ANCHOR="thaumcraft:book_anchor/";
    public record Element(int y,int height,FormattedCharSequence line,ResearchPage widget,Set<String> anchors) {}
    public record Sheet(ResearchPage source,List<Element> elements) {}
    public record Result(List<ResearchPage> pages,Map<Integer,Sheet> sheets,Map<String,Integer> anchors) {}
    private final Font font;
    private final List<ResearchPage> pages=new ArrayList<>();
    private final Map<Integer,Sheet> sheets=new HashMap<>();
    private final Map<String,Integer> anchors=new HashMap<>();
    private final List<Element> elements=new ArrayList<>();
    private MutableComponent pending=Component.empty();
    private final Set<String> waitingAnchors=new LinkedHashSet<>();
    private ResearchPage owner;
    private int y;
    private BookFlow(Font font){this.font=font;}
    public static Component marker(String id){return Component.literal("\u200b").withStyle(Style.EMPTY
            .withFont(ResourceLocation.parse("thaumcraft:book_anchor")).withInsertion(ANCHOR+id));}
    public static Result layout(Font font,List<ResearchPage> source) {
        return layout(font,source,"");
    }
    public static Result layout(Font font,List<ResearchPage> source,String research) {
        var flow=new BookFlow(font);
        for(var page:source) {
            if(page.pageBreak())flow.finish();
            if(!page.flow()) {flow.finish();flow.anchors.put(page.id(),flow.pages.size());flow.pages.add(page);continue;}
            if(flow.owner==null)flow.owner=page;
            flow.pending.append(marker(page.id()));
            for(var part:BookAmendments.expand(page,research)) {
                if(part.type()==ResearchPage.Type.TEXT||part.type()==ResearchPage.Type.TEXT_CONCEALED) {
                    flow.pending.append(marker(part.id())).append(
                            BookComponents.decode(part.component(),part.id()+"/text",AspectTextColorMode.DARK));
                }else {
                    flow.flush();
                    int height=height(part,font.lineHeight);
                    flow.add(new Element(0,height,null,part,Set.of(part.id())));
                }
            }
        }
        flow.finish();return new Result(List.copyOf(flow.pages),Map.copyOf(flow.sheets),Map.copyOf(flow.anchors));
    }
    public static int number(ResearchPage page,String field,int fallback){var p=page.properties();return p.has(field)?p.get(field).getAsInt():fallback;}
    public static float scale(ResearchPage page){var p=page.properties();return p.has("scale")?p.get("scale").getAsFloat():1;}
    private static int height(ResearchPage page,int lineHeight) {
        if(page.type()==ResearchPage.Type.DIVIDER)return lineHeight;
        int count=page.properties().has("research_links")?page.properties().getAsJsonArray("research_links").size():
                page.properties().has("items")?page.properties().getAsJsonArray("items").size():1;
        int value=page.type()==ResearchPage.Type.ITEM?Math.min(139/Math.max(1,count),number(page,"size",16)):
                Math.round(number(page,"height",64)*Math.min(scale(page),139f/number(page,"width",64)));
        return Math.max(1,value)+(gif(page)?12:0);
    }
    public static boolean gif(ResearchPage page){return page.image().map(s->s.endsWith(".gif")).orElse(false);}
    private void flush() {
        if(pending.getSiblings().isEmpty())return;
        var lines=net.minecraft.locale.Language.getInstance().getVisualOrder(
                BookLineWrapping.split(font.getSplitter(),BookTypography.normalize(pending),139));
        boolean trailingBreak=pending.getString().replace("\u200b","").endsWith("\n");
        for(int i=0;i<lines.size();i++) {
            var line=lines.get(i);var ids=new LinkedHashSet<String>();
            boolean[] visible={false};
            line.accept((offset,style,code)->{var tag=style.getInsertion();if(tag!=null&&tag.startsWith(ANCHOR))ids.add(tag.substring(ANCHOR.length()));
                else visible[0]=true;return true;});
            if(!visible[0]&&!ids.isEmpty()&&i==lines.size()-1&&!trailingBreak) {waitingAnchors.addAll(ids);continue;}
            add(new Element(0,font.lineHeight,line,null,ids));
        }
        pending=Component.empty();
    }
    private void add(Element element) {
        int limit=pages.isEmpty()?166:191;
        if(y+element.height()>limit&&!elements.isEmpty()){sheet();limit=191;}
        int height=Math.min(element.height(),limit);
        for(String id:waitingAnchors)anchors.putIfAbsent(id,pages.size());waitingAnchors.clear();
        for(String id:element.anchors())anchors.putIfAbsent(id,pages.size());
        elements.add(new Element(y,height,element.line(),element.widget(),element.anchors()));y+=height;
    }
    private void sheet(){if(owner==null)return;int index=pages.size();pages.add(owner);sheets.put(index,new Sheet(owner,List.copyOf(elements)));elements.clear();y=0;}
    private void finish(){flush();if(owner!=null){for(String id:waitingAnchors)anchors.putIfAbsent(id,pages.size());waitingAnchors.clear();
        if(!elements.isEmpty()){sheet();}else if(!pages.contains(owner)){sheet();}owner=null;}y=0;}
}
