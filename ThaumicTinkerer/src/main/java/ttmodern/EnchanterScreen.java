package ttmodern;

import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.registries.ForgeRegistries;

/** Legacy 176x166 layout, external selection rail, and six 48-pixel vis gauges. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid=Tinkerer.ID,bus=net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD,value=net.minecraftforge.api.distmarker.Dist.CLIENT)
public class EnchanterScreen extends AbstractContainerScreen<EnchanterMenu> {
    private static final ResourceLocation GUI=ResourceLocation.parse(Tinkerer.ID+":textures/gui/osmotic_enchanter.png");
    private static final List<String> PRIMALS=List.of("aer","terra","ignis","aqua","ordo","perditio");
    private int page,selectedPage;
    private List<Integer> candidates=List.of(),selected=List.of();
    private final List<Component> tooltip=new ArrayList<>();
    public EnchanterScreen(EnchanterMenu menu,Inventory inventory,Component title){super(menu,inventory,title);}
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event){event.enqueueWork(()->net.minecraft.client.gui.screens.MenuScreens.register(Tinkerer.ENCHANTER_MENU.get(),EnchanterScreen::new));}
    private void send(int id){if(minecraft!=null&&minecraft.gameMode!=null)SoulHearts.enchanterAction(menu.containerId,id);}
    private void refresh(){
        var list=new ArrayList<Integer>();var chosen=new ArrayList<Integer>();var tool=menu.entity.items.getStackInSlot(0);
        for(int i=0;i<menu.options.size();i++){
            var key=menu.options.get(i).enchantment();var enchant=ForgeRegistries.ENCHANTMENTS.getValue(key);
            if(menu.entity.selected().containsKey(key)){chosen.add(i);continue;}
            if(enchant==null||tool.isEmpty()||tool.isEnchanted()||!tool.isEnchantable()||!enchant.canEnchant(tool))continue;
            boolean compatible=true;for(var other:menu.entity.selected().keySet()){
                var otherEnchant=ForgeRegistries.ENCHANTMENTS.getValue(other);
                if(otherEnchant==null||!enchant.isCompatibleWith(otherEnchant)){compatible=false;break;}
            }
            if(compatible)list.add(i);
        }
        candidates=list;selected=chosen;page=Math.min(page,Math.max(0,(list.size()-1)/16));
        selectedPage=Math.min(selectedPage,Math.max(0,chosen.size()-visibleSelected()));
    }
    private int visibleSelected(){return Math.max(1,(height-topPos-12)/26);}
    private int choiceY(int cell){return 54+(Math.min(16,candidates.size()-page*16)>8&&cell<8?-24:0);}
    private boolean at(double x,double y,int px,int py,int w,int h){return x>=leftPos+px&&x<leftPos+px+w&&y>=topPos+py&&y<topPos+py+h;}
    private void blit(GuiGraphics g,int x,int y,int u,int v,int w,int h){tc4revival.client.LegacyGui.blit(g,GUI,leftPos+x,topPos+y,u,v,w,h);}
    private void icon(GuiGraphics g,int index,int x,int y,int mx,int my){
        var entry=menu.options.get(index);tc4revival.client.LegacyGui.blit(g,entry.icon(),leftPos+x,topPos+y,0,0,16,16,16,16);
        if(at(mx,my,x,y,16,16)){
            var enchant=ForgeRegistries.ENCHANTMENTS.getValue(entry.enchantment());
            if(enchant!=null)tooltip.add(enchant.getFullname(Math.max(1,menu.entity.selected().getOrDefault(entry.enchantment(),1))));
        }
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
        refresh();tooltip.clear();g.setColor(1,1,1,1);blit(g,0,0,0,0,imageWidth,imageHeight);
        int count=Math.min(16,candidates.size()-page*16);boolean twoRows=count>8;
        if(!menu.entity.items.getStackInSlot(0).isEmpty()&&!menu.entity.items.getStackInSlot(0).isEnchanted()){
            if(twoRows)blit(g,30,26,0,166,147,24);blit(g,30,50,0,166,147,24);
        }
        for(int i=0;i<count;i++)icon(g,candidates.get(page*16+i),34+i%8*16,choiceY(i),mx,my);
        if(candidates.size()>16){g.drawString(font,"‹",leftPos+4,topPos+60,0xffffffff,true);g.drawString(font,"›",leftPos+18,topPos+60,0xffffffff,true);}
        if(!selected.isEmpty()){
            blit(g,151,33,176,menu.entity.working()?39:24,15,15);
            if(at(mx,my,151,33,15,15)){
                tooltip.add(Component.translatable("ttmisc.startEnchant"));
                String failure=menu.entity.startFailure();
                if(failure!=null)tooltip.add(Component.translatable(failure).withStyle(net.minecraft.ChatFormatting.RED));
                else if(menu.entity.items.getStackInSlot(1).isEmpty())tooltip.add(Component.translatable("ttmodern.enchanter.wand").withStyle(net.minecraft.ChatFormatting.YELLOW));
            }
            for(int i=0;i<PRIMALS.size();i++){
                String name=PRIMALS.get(i);int total=menu.entity.costs().getOrDefault(name,0),paid=menu.entity.paid().getOrDefault(name,0);
                int x=55+i*15,y=twoRows?26:50,size=total==0?11:59;
                if(total==0){blit(g,x,y-size,200,0,10,4);blit(g,x,y-size+4,200,52,10,10);}
                else{
                    int pixels=Math.min(48,(int)(48L*paid/total));int color=thaumcraft.api.aspects.AspectViews.display(tc4revival.aspect.TCAspects.catalog().require(name)).color();
                    g.setColor((color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,1);
                    blit(g,x+1,y-size+4+48-pixels,210,48-pixels,8,pixels);g.setColor(1,1,1,1);blit(g,x,y-size,200,0,10,size);
                }
                if(at(mx,my,x,y-size,10,size)){tooltip.add(Component.literal(thaumcraft.api.aspects.AspectViews.display(tc4revival.aspect.TCAspects.catalog().require(name)).displayName()));tooltip.add(Component.literal(paid+"/"+total));}
            }
        }
        for(int i=0;i<Math.min(visibleSelected(),selected.size()-selectedPage);i++){
            int index=selected.get(selectedPage+i),y=i*26;var entry=menu.options.get(index);int rank=menu.entity.selected().get(entry.enchantment());
            blit(g,176,y-4,176,0,24,24);icon(g,index,180,y,mx,my);
            blit(g,200,y-4,218,0,7,7);blit(g,207,y-4,225,0,7,7);
            g.drawString(font,Component.translatable("enchantment.level."+rank),leftPos+206,topPos+y+8,0xffffffff,true);
        }
    }
    @Override public boolean mouseClicked(double x,double y,int button){
        if(button==0&&!menu.entity.working()){
            refresh();if(at(x,y,151,33,15,15)&&!selected.isEmpty()){send(0);return true;}
            for(int i=0;i<Math.min(16,candidates.size()-page*16);i++)if(at(x,y,34+i%8*16,choiceY(i),16,20)){send(1+candidates.get(page*16+i)*4);return true;}
            for(int i=0;i<Math.min(visibleSelected(),selected.size()-selectedPage);i++){
                int index=selected.get(selectedPage+i),yy=i*26;
                if(at(x,y,180,yy,16,20)){send(2+index*4);return true;}
                if(at(x,y,200,yy-4,7,7)){send(3+index*4);return true;}
                if(at(x,y,207,yy-4,7,7)){send(4+index*4);return true;}
            }
            if(candidates.size()>16&&at(x,y,2,56,28,16)){page=Math.max(0,page+(x<leftPos+16?-1:1));return true;}
        }
        return super.mouseClicked(x,y,button);
    }
    @Override public boolean mouseScrolled(double x,double y,double delta){
        if(x>=leftPos+176&&x<=leftPos+224){selectedPage=Math.max(0,Math.min(Math.max(0,selected.size()-visibleSelected()),selectedPage+(delta<0?1:-1)));return true;}
        return super.mouseScrolled(x,y,delta);
    }
    @Override protected void renderLabels(GuiGraphics g,int x,int y){}
    @Override public void render(GuiGraphics g,int x,int y,float partial){renderBackground(g);super.render(g,x,y,partial);renderTooltip(g,x,y);if(!tooltip.isEmpty())g.renderComponentTooltip(font,tooltip,x,y);}
}
