package tc4revival.client;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import tc4revival.aspect.*;
import tc4revival.content.JarLabelItem;
import tc4revival.network.*;

/** The research desk's aspect browser, without inventory slots or a research sheet. */
public class JarLabelScreen extends Screen {
    private static final ResourceLocation GUI=ResourceLocation.parse("thaumcraft:textures/gui/guiresearchtable2.png");
    private final InteractionHand hand;
    private final int slot;
    private int page,left,top;
    private List<Aspect> aspects=List.of();
    public JarLabelScreen(InteractionHand hand) { super(Component.translatable("thaumcraft.label.choose"));this.hand=hand;slot=Minecraft.getInstance().player.getInventory().selected; }
    public static void open(InteractionHand hand) { Minecraft.getInstance().setScreen(new JarLabelScreen(hand)); }
    @Override protected void init() { left=(width-96)/2;top=(height-108)/2; }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void tick() {
        if(minecraft.player==null||!minecraft.player.isAlive()||!(minecraft.player.getItemInHand(hand).getItem() instanceof JarLabelItem)
                ||hand==InteractionHand.MAIN_HAND&&minecraft.player.getInventory().selected!=slot)onClose();
    }
    private int lastPage() { return Math.max(0,(aspects.size()-21)/5); }
    private Aspect hovered(double x,double y) {
        int col=(int)(x-left-10)/16,row=(int)(y-top-10)/16,index=page*5+col*5+row;
        return x>=left+10&&x<left+90&&y>=top+10&&y<top+90&&col<5&&row<5&&index<aspects.size()?aspects.get(index):null;
    }
    @Override public void render(GuiGraphics g,int x,int y,float partial) {
        var knowledge=ClientKnowledge.displaySnapshot();
        aspects=TCAspects.catalog().all().stream().filter(a->minecraft.player!=null&&(minecraft.player.isCreative()||knowledge.map(k->k.knows(a)).orElse(false)))
                .sorted(java.util.Comparator.comparing(Aspect::tag)).toList();
        page=Math.min(page,lastPage());renderBackground(g);g.setColor(1,1,1,1);
        // Crop the browser itself; no ink, combination slots, notes or player inventory.
        LegacyGui.blit(g,GUI,left,top,0,30,96,108);
        for(int i=0;i<25&&page*5+i<aspects.size();i++) {
            var a=aspects.get(page*5+i);int c=thaumcraft.api.client.ClientAspectViews.view(a).color();g.setColor((c>>16&255)/255f,(c>>8&255)/255f,(c&255)/255f,1);
            g.blit(thaumcraft.api.client.ClientAspectViews.view(a).texture(),left+10+i/5*16,top+10+i%5*16,0,0,16,16,16,16);
        }
        g.setColor(1,1,1,1);
        if(page>0)LegacyGui.blit(g,GUI,left+27,top+91,184,208,24,8);
        if(page<lastPage())LegacyGui.blit(g,GUI,left+51,top+91,208,208,24,8);
        var a=hovered(x,y);
        if(a!=null)g.renderComponentTooltip(font,List.of(Component.literal(thaumcraft.api.client.ClientAspectViews.view(a).displayName()).withStyle(net.minecraft.ChatFormatting.AQUA),Component.translatable(thaumcraft.api.client.ClientAspectViews.view(a).descriptionKey()).withStyle(net.minecraft.ChatFormatting.GRAY),
                Component.translatable("thaumcraft.label.one"),Component.translatable("thaumcraft.label.all")),x,y);
        super.render(g,x,y,partial);
    }
    @Override public boolean mouseClicked(double x,double y,int button) {
        if(button!=0)return super.mouseClicked(x,y,button);
        var a=hovered(x,y);
        if(a!=null) { TCNetwork.CHANNEL.sendToServer(new JarLabelSelectPacket(hand,slot,a.tag(),hasShiftDown()));return true; }
        if(y>=top+91&&y<top+101) {
            if(x>=left+27&&x<left+51){page=Math.max(0,page-1);return true;}
            if(x>=left+51&&x<left+75){page=Math.min(lastPage(),page+1);return true;}
        }
        return super.mouseClicked(x,y,button);
    }
    @Override public boolean mouseScrolled(double x,double y,double delta) { page=Math.max(0,Math.min(lastPage(),page+(delta<0?1:-1)));return true; }
}
