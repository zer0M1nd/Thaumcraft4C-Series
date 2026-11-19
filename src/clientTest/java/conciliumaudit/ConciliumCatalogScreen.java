package conciliumaudit;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/** Draws the actual item renderer. Its screenshots certify appearance only. */
public final class ConciliumCatalogScreen extends Screen {
    public static final int PAGE_SIZE=24;
    private final List<ItemStack> stacks;
    private int page;
    public ConciliumCatalogScreen(int page){super(Component.literal("Concilium item rendering audit"));stacks=ConciliumScenes.items().stream().filter(s->ConciliumScenes.unavailable(s).isEmpty()).toList();this.page=Math.max(0,Math.min(page,(stacks.size()-1)/PAGE_SIZE));}
    public int page(){return page;}
    public List<String> visibleIds(){return stacks.subList(page*PAGE_SIZE,Math.min(stacks.size(),(page+1)*PAGE_SIZE)).stream().map(s->ForgeRegistries.ITEMS.getKey(s.getItem()).toString()).toList();}
    @Override public boolean isPauseScreen(){return false;}
    @Override public boolean keyPressed(int key,int scan,int modifiers){if(key==262||key==263){page=Math.max(0,Math.min((stacks.size()-1)/PAGE_SIZE,page+(key==262?1:-1)));return true;}return super.keyPressed(key,scan,modifiers);}
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){
        g.fill(0,0,width,height,0xff20222a);g.drawCenteredString(font,title.getString()+" | "+(page+1)+"/"+Math.max(1,(stacks.size()+23)/24)+" | arrows",width/2,8,0xffffff);
        int cellW=Math.min(100,(width-12)/6),cellH=Math.min(72,(height-36)/4),left=(width-cellW*6)/2;
        ItemStack hover=ItemStack.EMPTY;
        for(int i=0;i<PAGE_SIZE&&page*PAGE_SIZE+i<stacks.size();i++){
            var stack=stacks.get(page*PAGE_SIZE+i);int x=left+i%6*cellW,y=28+i/6*cellH;
            g.fill(x+1,y+1,x+cellW-1,y+cellH-1,0xff343847);
            g.renderItem(stack,x+cellW/2-8,y+6);g.renderItemDecorations(font,stack,x+cellW/2-8,y+6);
            String id=ForgeRegistries.ITEMS.getKey(stack.getItem()).getPath();g.drawCenteredString(font,font.plainSubstrByWidth(stack.getHoverName().getString(),cellW-4),x+cellW/2,y+30,0xffffff);
            g.drawCenteredString(font,font.plainSubstrByWidth(id,cellW-4),x+cellW/2,y+43,0xb8c3d9);
            if(mouseX>=x&&mouseX<x+cellW&&mouseY>=y&&mouseY<y+cellH)hover=stack;
        }
        if(!hover.isEmpty())g.renderTooltip(font,hover,mouseX,mouseY);
    }
}
