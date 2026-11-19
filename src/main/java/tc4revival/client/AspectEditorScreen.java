package tc4revival.client;

import com.google.gson.JsonObject;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.network.chat.Component;
import tc4revival.aspect.*;
import tc4revival.aspect.editor.*;
import tc4revival.network.*;

/** Admin-only client draft. Authoritative changes are validated and saved on the server. */
public class AspectEditorScreen extends Screen {
    private static final int W=800,H=600;
    private static final String[] STATES={"已定义","由标签定义","由配方定义","未定义","由高级要素推导定义"};
    private static final int[] COLORS={0x99407045,0x993d638e,0x998c793c,0x997a4148,0x99d17a24};
    private static final String[] MODES={"原版顺序","原版倒序","按阶数升序","按阶数降序","按阶数分类","按阶数分类·倒序"};
    private final UUID token;
    private final EditorViewModel model=new EditorViewModel();
    private final List<Click> clicks=new ArrayList<>();
    private final List<Cell> cells=new ArrayList<>();
    private List<EditorViewModel.Target> filtered=List.of();
    private EditorViewModel.Target selected,hovered;
    private EditBox search,name;
    private String defaultName,tag,message="",holdAspect,holdKey;
    private Map<String,Integer> holdBase;
    private int filter=-1,sort,aspectPage,valuePage,row,tagRow,dataVersion=-1,ticks,queryDelay;
    private int holdValue;private boolean typed,icons,tagOpen,saving,help;
    private double scale=1,ox,oy,mx,my;
    private boolean draggingBrowser;
    private double browserDragOffset;
    private record Click(int x,int y,int w,int h,Runnable action){boolean hit(double px,double py){return px>=x&&px<x+w&&py>=y&&py<y+h;}}
    private record Cell(Aspect aspect,int x,int y){boolean hit(double px,double py){return px>=x&&px<x+44&&py>=y&&py<y+23;}}
    public AspectEditorScreen(AspectEditorOpen packet){super(Component.literal("要素编辑器"));token=packet.token();defaultName=packet.defaultName();}
    public static void open(AspectEditorOpen packet){Minecraft.getInstance().setScreen(new AspectEditorScreen(packet));}
    public static void result(AspectEditorResult packet){
        var mc=Minecraft.getInstance();
        if(mc.screen instanceof AspectEditorScreen screen&&screen.token.equals(packet.token())){
            screen.saving=false;screen.message=packet.message();
            if(packet.success()){screen.model.drafts.clear();screen.model.invalidate();screen.defaultName=packet.nextName();screen.name.setValue("");screen.name.setHint(Component.literal(screen.defaultName));screen.refresh();}
        }else if(mc.player!=null)mc.player.sendSystemMessage(Component.literal(packet.message()));
    }
    @Override protected void init(){
        String query=search==null?"":search.getValue(),pack=name==null?"":name.getValue();
        scale=Math.min((width-8d)/W,(height-8d)/H);ox=(width-W*scale)/2;oy=(height-H*scale)/2;
        search=new EditBox(font,18,49,270,18,Component.literal("搜索物品、方块、实体"));search.setMaxLength(160);search.setBordered(false);search.setValue(query);
        search.setHint(Component.literal("搜索物品 / 方块 / 实体"));search.setResponder(s->{queryDelay=4;row=0;draggingBrowser=false;});addRenderableWidget(search);
        name=new EditBox(font,110,576,426,18,Component.literal("数据包名称"));name.setMaxLength(64);name.setBordered(false);name.setValue(pack);name.setHint(Component.literal(defaultName));addRenderableWidget(name);
        refresh();
    }
    private boolean textFocus(){return search.isFocused()||name.isFocused();}
    private Map<String,Integer> values(){return selected==null?Map.of():model.values(selected,tag);}
    private String key(){return selected==null?null:selected.kind()+(tag==null?":"+selected.id():"_tag:"+tag);}
    private void select(EditorViewModel.Target target){commit();selected=target;tag=null;tagOpen=false;tagRow=0;valuePage=0;}
    private void refresh(){
        if(search==null)return;
        String query=search.getValue().strip().toLowerCase(Locale.ROOT);Set<net.minecraft.world.item.Item> jei=null;
        if(!query.isEmpty()&&EditorSearch.search!=null)try{jei=EditorSearch.search.apply(query);}catch(RuntimeException error){message="JEI 搜索异常，暂按 ID 搜索";}
        var found=new ArrayList<EditorViewModel.Target>();
        for(var target:model.targets){
            boolean match=query.isEmpty()||(target.kind().equals("item")?(jei!=null?jei.contains(target.stack().getItem()):target.id().contains(query)):
                    EditorNavigation.matchesNonItem(query,target.key(),target.name().getString()));
            if(match&&(filter<0||(ClientItemAspects.ready()&&model.view(target).status()==filter)))found.add(target);
        }
        filtered=found;row=Math.max(0,Math.min(row,maxRow()));
    }
    private int columns(){return icons?8:1;}
    private int visibleRows(){return icons?13:19;}
    private int maxRow(){return Math.max(0,(filtered.size()+columns()-1)/columns()-visibleRows());}
    private int browserThumbHeight(){return EditorNavigation.thumbHeight(visibleRows(),maxRow(),434);}
    private int browserThumbY(){return 105+(maxRow()==0?0:(434-browserThumbHeight())*row/maxRow());}
    private void dragBrowser(double y){row=EditorNavigation.dragRow(y,browserDragOffset,105,434,browserThumbHeight(),maxRow());}
    @Override public void tick(){
        super.tick();search.tick();name.tick();ticks++;
        if(queryDelay>0&&--queryDelay==0)refresh();
        if(ClientItemAspects.ready()&&dataVersion!=ClientItemAspects.version()){
            dataVersion=ClientItemAspects.version();model.invalidate();refresh();
        }
    }
    private void button(GuiGraphics g,int x,int y,int w,int h,String text,Runnable action){button(g,x,y,w,h,text,action,true);}
    private void button(GuiGraphics g,int x,int y,int w,int h,String text,Runnable action,boolean enabled){
        var hit=new Click(x,y,w,h,action);boolean over=hit.hit(mx,my);
        g.fill(x,y,x+w,y+h,enabled?(over?0xff536279:0xff333f50):0xff272c35);
        g.drawCenteredString(font,Component.literal(text),x+w/2,y+(h-8)/2,enabled?0xffe4e9ee:0xff6e7680);
        if(enabled)clicks.add(hit);
    }
    private void text(GuiGraphics g,String text,int x,int y,int max,int color){g.drawString(font,font.plainSubstrByWidth(text,max),x,y,color,false);}
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float delta){
        mx=(mouseX-ox)/scale;my=(mouseY-oy)/scale;clicks.clear();cells.clear();hovered=null;help=false;
        g.fill(0,0,width,height,0x88000000);g.pose().pushPose();g.pose().translate(ox,oy,0);g.pose().scale((float)scale,(float)scale,1);
        g.fill(0,0,W,H,0xf0191d25);g.fill(10,40,296,562,0xff222833);g.fill(304,40,790,353,0xff222833);g.fill(304,359,790,562,0xff222833);
        text(g,"要素编辑器",14,15,220,0xffedf0f5);
        text(g,saving?"正在保存并重载数据包…":!ClientItemAspects.ready()?"正在等待服务器要素数据…":message,310,17,470,0xffadbed4);
        g.fill(14,45,292,71,0xff111720);
        button(g,14,77,160,21,filter<0?"筛选：全部":"筛选："+STATES[filter],()->{filter=filter==STATES.length-1?-1:filter+1;row=0;refresh();});
        button(g,180,77,112,21,icons?"浏览：图标":"浏览：列表",()->{icons=!icons;row=0;});
        int step=icons?32:23;
        for(int i=row*columns();i<Math.min(filtered.size(),(row+visibleRows())*columns());i++){
            var target=filtered.get(i);int n=i-row*columns(),x=14+(n%columns())*34,y=105+(n/columns())*step,w=icons?30:272;
            var hit=new Click(x,y,w,step-2,()->select(target));
            int color=ClientItemAspects.ready()?COLORS[model.view(target).status()]:0xff303744;
            g.fill(x,y,x+w,y+step-2,color);
            if(target==selected){g.fill(x,y,x+2,y+step-2,0xffedf2fa);g.fill(x+w-2,y,x+w,y+step-2,0xffedf2fa);}
            EditorIcons.draw(g,target,x+(icons?7:3),y+(icons?6:2));
            if(!icons)text(g,(target.kind().equals("item")?"":"["+target.typeLabel()+"] ")+target.name().getString(),x+24,y+7,244,0xffebedf3);
            clicks.add(hit);if(hit.hit(mx,my))hovered=target;
        }
        text(g,filtered.size()+" 项 · 滚轮 / 拖动滚动条",15,547,274,0xffa7b1c0);
        g.fill(288,105,295,539,0xff121820);
        g.fill(288,browserThumbY(),295,browserThumbY()+browserThumbHeight(),draggingBrowser?0xffa8bdd8:0xff7889a0);
        button(g,312,47,228,22,"排序："+MODES[sort],()->{sort=(sort+1)%6;aspectPage=0;});
        var rows=EditorAspectLayout.rows(TCAspects.catalog(),sort,10);int pages=Math.max(1,(rows.size()+9)/10);aspectPage=Math.min(aspectPage,pages-1);
        button(g,622,47,30,22,"<",()->aspectPage=Math.max(0,aspectPage-1),aspectPage>0);
        text(g,(aspectPage+1)+" / "+pages,665,54,70,0xffb9c5d6);
        button(g,748,47,30,22,">",()->aspectPage=Math.min(pages-1,aspectPage+1),aspectPage+1<pages);
        var current=values();
        for(int r=aspectPage*10;r<Math.min(rows.size(),aspectPage*10+10);r++)for(int c=0;c<rows.get(r).size();c++)drawAspect(g,rows.get(r).get(c),312+c*46,80+(r%10)*24,current);
        button(g,679,326,99,21,"⊘ 无要素",()->clear(),editable());
        AspectTooltips.draw(g,font,new AspectTooltips.Cell("_empty",""),657,329);
        if(selected!=null){
            EditorIcons.draw(g,selected,313,368);text(g,selected.name().getString(),338,367,237,0xffedf0f6);text(g,selected.id(),338,382,440,0xff9cabbe);
            button(g,580,364,198,22,selected.kind().equals("entity")?"实体类型（无标签）":tag==null?"标签：未选择":"#"+font.plainSubstrByWidth(tag,174),()->tagOpen=!tagOpen,!saving&&!selected.kind().equals("entity"));
            button(g,312,399,150,20,"清空（X）",this::clear,editable());
            button(g,470,399,308,20,"将当前计算结果固化为直接定义",()->{model.put(selected.key(),model.view(selected).values());tag=null;refresh();},editable()&&tag==null&&EditorViewModel.canSolidify(model.view(selected).status()));
            var owned=TCAspects.catalog().all().stream().filter(a->current.containsKey(a.tag())).toList();int ownedPages=Math.max(1,(owned.size()+49)/50);valuePage=Math.min(valuePage,ownedPages-1);
            for(int i=valuePage*50;i<Math.min(owned.size(),valuePage*50+50);i++)drawAspect(g,owned.get(i),312+(i%10)*46,425+((i/10)%5)*24,current);
            if(current.isEmpty()){AspectTooltips.draw(g,font,new AspectTooltips.Cell("_empty",""),318,432);text(g,"无要素",342,438,220,0xffa4b0bf);}
            text(g,(tag==null?STATES[model.view(selected).status()]:"编辑标签 #"+tag)+(model.drafts.containsKey(key())?" · 未保存":""),312,550,338,0xffb3c0d1);
            if(ownedPages>1){button(g,700,545,28,15,"<",()->valuePage=Math.max(0,valuePage-1));button(g,749,545,28,15,">",()->valuePage=Math.min(ownedPages-1,valuePage+1));}
        }else text(g,"从左侧选择物品、技术性方块或实体",322,378,440,0xffb6c2d1);
        g.fill(10,568,790,596,0xff252c38);text(g,"数据包名称",16,580,90,0xffcad3df);g.fill(105,573,541,593,0xff111720);
        button(g,551,573,96,21,"保存（"+model.drafts.size()+"）",this::save,!saving&&ClientItemAspects.ready()&&!model.drafts.isEmpty());
        button(g,654,573,96,21,"退出",this::onClose,!saving);
        button(g,758,573,27,21,"?",()->{});help=mx>=758&&mx<785&&my>=573&&my<594;
        super.render(g,(int)mx,(int)my,delta);
        if(tagOpen&&selected!=null)renderTags(g);
        if(holdAspect!=null&&typed)text(g,"输入："+holdValue+" · 松开左键生效",312,340,330,0xffffd987);
        if(help)tooltip(g,List.of("管理员要素编辑器（物品 / 方块 / 实体 / 标签，无 NBT 条件）","左键选中目标；标签按钮选择编辑目标。","悬浮要素：滚轮 ±1，右键删除；直接按数字设置一位数。","按住左键并输入数字：多位数，松开左键生效（0–10000）。","Q W E A S D Z 对应 4 5 6 7 8 9 0；输入框有焦点时不触发。","X：明确清空；⊘：明确无要素，仍算已定义。","Tab：下一项；Shift+Tab：下一未定义项；","Ctrl+Tab：下一未定义或推导项（当前搜索结果内循环）。","配方/高级推导均可固化；橙色表示高级要素推导。","物品直接定义优先于标签；标签修改会影响所有匹配成员。","方块和实体可按中文名称或 ID 搜索；拖动左侧滚动条翻阅。","实体用屏障图标显示；编辑值统一覆盖该类型的各个变体。","修改只进入草稿，保存才写入存档数据包并重载生效。","未命名使用玩家名+编号；后保存的同一目标覆盖先前保存。","保存时请等待服务端反馈；退出时可取消丢弃未保存草稿。"));
        else if(!tagOpen&&hovered!=null)targetTooltip(g,hovered);
        else if(!tagOpen){var cell=hoverCell();if(cell!=null)tooltip(g,List.of(thaumcraft.api.aspects.AspectViews.display(cell.aspect()).displayName(),Component.translatable(thaumcraft.api.aspects.AspectViews.display(cell.aspect()).descriptionKey()).getString(),"阶数："+EditorAspectLayout.tier(cell.aspect(),TCAspects.catalog()),"数量："+current.getOrDefault(cell.aspect().tag(),0)));}
        g.pose().popPose();
    }
    private void renderTags(GuiGraphics g){
        int count=selected.tags().size()+1;tagRow=Math.max(0,Math.min(tagRow,Math.max(0,count-7)));
        g.pose().pushPose();g.pose().translate(0,0,450);g.fill(572,389,789,552,0xff111720);
        for(int i=tagRow;i<Math.min(count,tagRow+7);i++){
            String value=i==0?null:selected.tags().get(i-1);int y=392+(i-tagRow)*22;
            button(g,577,y,206,20,value==null?"未选择（编辑物品/方块）":"#"+font.plainSubstrByWidth(value,188),()->{commit();tag=value;tagOpen=false;valuePage=0;});
        }
        g.pose().popPose();
    }
    private void drawAspect(GuiGraphics g,Aspect a,int x,int y,Map<String,Integer> values){
        var cell=new Cell(a,x,y);cells.add(cell);g.fill(x,y,x+44,y+23,cell.hit(mx,my)?0xff49556a:0xff2b3443);
        AspectTooltips.draw(g,font,new AspectTooltips.Cell(a.tag(),values.containsKey(a.tag())?Integer.toString(values.get(a.tag())):""),x+12,y+3);
    }
    private void tooltip(GuiGraphics g,List<String> lines){tooltipBox(g,lines.stream().<Component>map(Component::literal).toList(),Map.of());}
    private void tooltipBox(GuiGraphics g,List<Component> text,Map<String,Integer> aspects){
        var lines=new ArrayList<net.minecraft.util.FormattedCharSequence>();
        text.forEach(c->lines.addAll(font.split(c,help?470:280)));
        int w=lines.stream().mapToInt(font::width).max().orElse(100);
        int shown=Math.min(100,aspects.size()),columns=Math.max(1,Math.min(10,shown));
        w=Math.max(w,columns*27);int h=lines.size()*11+((shown+columns-1)/columns)*21+(aspects.size()>100?12:0);
        int x=Math.max(5,Math.min((int)mx+12,W-w-13)),y=Math.max(5,Math.min((int)my+12,H-h-12));
        g.pose().pushPose();g.pose().translate(0,0,600);g.fill(x-5,y-5,x+w+5,y+h+5,0xf410141c);
        int at=y;for(var line:lines){g.drawString(font,line,x,at,0xffe5eaf3,false);at+=11;}
        int i=0;for(var entry:aspects.entrySet()){
            if(i>=100)break;AspectTooltips.draw(g,font,new AspectTooltips.Cell(entry.getKey(),entry.getValue()<0?"":Integer.toString(entry.getValue())),x+(i%columns)*27,at+(i/columns)*21);i++;
        }
        if(aspects.size()>100)g.drawString(font,"另有 "+(aspects.size()-100)+" 种要素，可在编辑区翻页",x,at+((shown+columns-1)/columns)*21,0xffa7b5c9,false);
        g.pose().popPose();
    }
    private void targetTooltip(GuiGraphics g,EditorViewModel.Target target){
        var v=model.view(target);var lines=new ArrayList<Component>(List.of(target.name(),Component.literal(target.id()),Component.literal(target.typeLabel()+" · "+STATES[v.status()])));
        if(target.kind().equals("entity"))lines.add(Component.literal("按实体类型统一编辑；原始值预览默认状态，特殊变体可能不同。"));
        tooltipBox(g,lines,v.values().isEmpty()?Map.of("_empty",-1):v.values());
    }
    private Cell hoverCell(){for(var c:cells)if(c.hit(mx,my))return c;return null;}
    private boolean editable(){return selected!=null&&!saving&&ClientItemAspects.ready();}
    private void set(String aspect,int amount){if(!editable())return;var next=new LinkedHashMap<>(values());if(amount<=0)next.remove(aspect);else next.put(aspect,Math.min(EditorDefinitions.MAX_AMOUNT,amount));model.put(key(),next);refresh();}
    private void clear(){if(editable()){commit();model.put(key(),Map.of());refresh();}}
    private void commit(){
        if(holdAspect!=null&&typed&&!saving){var next=new LinkedHashMap<>(holdBase);if(holdValue==0)next.remove(holdAspect);else next.put(holdAspect,holdValue);model.put(holdKey,next);refresh();}
        holdAspect=null;holdKey=null;holdBase=null;typed=false;holdValue=0;
    }
    @Override public boolean mouseClicked(double x,double y,int button){
        mx=(x-ox)/scale;my=(y-oy)/scale;
        if(search.isMouseOver(mx,my)||name.isMouseOver(mx,my)){
            commit();var box=search.isMouseOver(mx,my)?search:name;search.setFocused(box==search);name.setFocused(box==name);setFocused(box);return box.mouseClicked(mx,my,button);
        }
        search.setFocused(false);name.setFocused(false);setFocused(null);
        if(tagOpen){for(int i=clicks.size()-1;i>=0;i--)if(clicks.get(i).hit(mx,my)&&clicks.get(i).y()>=392&&clicks.get(i).x()==577){clicks.get(i).action().run();return true;}tagOpen=false;return true;}
        if(button==0&&mx>=286&&mx<296&&my>=105&&my<539&&maxRow()>0){
            commit();draggingBrowser=true;
            browserDragOffset=my>=browserThumbY()&&my<browserThumbY()+browserThumbHeight()?my-browserThumbY():browserThumbHeight()/2d;
            dragBrowser(my);return true;
        }
        var cell=hoverCell();if(cell!=null&&editable()){
            if(button==1)set(cell.aspect().tag(),0);
            else if(button==0){commit();holdAspect=cell.aspect().tag();holdKey=key();holdBase=new LinkedHashMap<>(values());holdValue=0;typed=false;}
            return true;
        }
        if(button==0)for(int i=clicks.size()-1;i>=0;i--)if(clicks.get(i).hit(mx,my)){commit();clicks.get(i).action().run();return true;}
        return false;
    }
    @Override public boolean mouseReleased(double x,double y,int button){if(button==0&&draggingBrowser){draggingBrowser=false;return true;}if(button==0&&holdAspect!=null){commit();return true;}return super.mouseReleased((x-ox)/scale,(y-oy)/scale,button);}
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){if(button==0&&draggingBrowser){dragBrowser((y-oy)/scale);return true;}return super.mouseDragged((x-ox)/scale,(y-oy)/scale,button,dx/scale,dy/scale);}
    @Override public boolean mouseScrolled(double x,double y,double delta){
        mx=(x-ox)/scale;my=(y-oy)/scale;
        if(tagOpen){tagRow=Math.max(0,Math.min(selected.tags().size()-6,tagRow-(int)Math.signum(delta)));return true;}
        var cell=hoverCell();if(cell!=null&&editable()){commit();set(cell.aspect().tag(),values().getOrDefault(cell.aspect().tag(),0)+(int)Math.signum(delta));return true;}
        if(mx>=10&&mx<=296&&my>=100&&my<562){row=Math.max(0,Math.min(maxRow(),row-(int)Math.signum(delta)*3));return true;}return false;
    }
    @Override public boolean keyPressed(int key,int scan,int mods){
        if(key==256){onClose();return true;}
        if(key==258&&!saving){
            commit();if(queryDelay>0){queryDelay=0;refresh();}if(filtered.isEmpty())return true;int current=filtered.indexOf(selected);
            for(int i=1;i<=filtered.size();i++){var next=filtered.get(Math.floorMod(current+i,filtered.size()));int status=model.view(next).status();
                if((mods&2)!=0?(status==EditorViewModel.UNDEFINED||EditorViewModel.canSolidify(status)):(mods&1)!=0?status==EditorViewModel.UNDEFINED:true){select(next);row=Math.min(maxRow(),filtered.indexOf(next)/columns());break;}}
            return true;
        }
        if(!textFocus()&&editable()){
            if(key==88){clear();return true;}
            int digit=EditorAspectLayout.digit(key);
            if(digit>=0){
                if(holdAspect!=null){holdValue=EditorAspectLayout.append(holdValue,digit);typed=true;return true;}
                var cell=hoverCell();if(cell!=null){set(cell.aspect().tag(),digit);return true;}
            }
        }
        return super.keyPressed(key,scan,mods);
    }
    private void save(){
        commit();if(model.drafts.isEmpty()||saving)return;
        try{
            var json=new JsonObject();json.addProperty("name",name.getValue());json.add("data",new EditorDefinitions(model.drafts,0).encode());
            var frames=EditorUpload.frames(json.toString());saving=true;message="";
            for(int i=0;i<frames.size();i++)TCNetwork.CHANNEL.sendToServer(new AspectEditorSave(token,i,frames.size(),frames.get(i)));
        }catch(RuntimeException error){saving=false;message=error.getMessage();}
    }
    @Override public void onClose(){
        if(saving)return;commit();
        if(model.drafts.isEmpty()){super.onClose();return;}
        minecraft.setScreen(new ConfirmScreen(discard->{if(discard)minecraft.setScreen(null);else minecraft.setScreen(this);},Component.literal("退出要素编辑器？"),Component.literal("未保存的 "+model.drafts.size()+" 项修改将被丢弃。")));
    }
    @Override public boolean isPauseScreen(){return false;}
}
