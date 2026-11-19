package thaumicfutures.autocaster.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import tc4revival.content.CastingWandItem;
import thaumicfutures.autocaster.*;

/** Original focus socket/heart/inventory art, with wand vis and optional EU supply controls. */
public class AutocasterScreen extends AbstractContainerScreen<AutocasterMenu> {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumicfutures:textures/gui/autocaster.png");
    public AutocasterScreen(AutocasterMenu menu,Inventory inventory,Component title) { super(menu,inventory,title);imageWidth=175;imageHeight=232; }
    @Override protected void init() {
        super.init();
        for(int bit=0;bit<4;bit++) {
            final int key=bit;
            addRenderableWidget(new AbstractButton(leftPos+90,topPos+13+bit*14,83,12,Component.translatable("thaumicfutures.autocaster.target."+bit)) {
                @Override public void onPress() { if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,key); }
                @Override protected void renderWidget(GuiGraphics graphics,int x,int y,float partial) {
                    graphics.blit(TEXTURE,getX(),getY()+1,192,menu.turret.targets(key)?24:16,8,8);
                    small(graphics,getMessage(),getX()+12,getY()+1,isHoveredOrFocused()?0xffffbb:0xffffff,.7f);
                }
                @Override protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) { defaultButtonNarrationText(output); }
            });
        }
        addRenderableWidget(Button.builder(Component.translatable("thaumicfutures.autocaster.toggle"),button->{
            if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,4);
        }).bounds(leftPos+8,topPos+214,159,18).build());
    }
    private void small(GuiGraphics graphics,Component text,int x,int y,int color,float scale) {
        graphics.pose().pushPose();graphics.pose().translate(x,y,0);graphics.pose().scale(scale,scale,1);graphics.drawString(font,text,0,0,color,false);graphics.pose().popPose();
    }
    @Override protected void renderLabels(GuiGraphics graphics,int x,int y) { graphics.drawString(font,title,8,-12,0xffffff,false); }
    @Override protected void renderBg(GuiGraphics graphics,float partial,int mouseX,int mouseY) {
        graphics.blit(TEXTURE,leftPos,topPos,0,0,imageWidth,imageHeight);
        int health=Math.round(39*menu.turret.getHealth()/menu.turret.getMaxHealth());graphics.blit(TEXTURE,leftPos+24,topPos+59,192,48,health,6);
        graphics.blit(TEXTURE,leftPos+64,topPos+28,7,83,18,18);
        small(graphics,Component.translatable("thaumicfutures.autocaster.eu"),leftPos+64,topPos+48,0xcccccc,.65f);
        small(graphics,Component.translatable("thaumicfutures.autocaster.range",(int)menu.turret.attackRange()),leftPos+24,topPos+9,0xffffff,.6f);
        var stack=menu.getSlot(0).getItem();
        if(stack.getItem() instanceof CastingWandItem wand) {
            var aspects=tc4revival.aspect.TCAspects.catalog().primals().stream().sorted(java.util.Comparator.comparing(tc4revival.aspect.Aspect::tag)).toList();
            for(int index=0;index<aspects.size();index++) {
                var aspect=aspects.get(index);int x=leftPos+8+27*index;int stored=wand.vis(stack,aspect);int capacity=wand.capacity(stack);
                graphics.fill(x,topPos+173,x+23,topPos+193,0xff252329);
                int height=capacity<=0?0:(int)(18L*stored/capacity);graphics.fill(x+1,topPos+192-height,x+22,topPos+192,0xff000000|aspect.color());
                small(graphics,Component.literal(String.format(java.util.Locale.ROOT,"%.1f",stored/100f)),x,topPos+197,0xffffff,.65f);
            }
        }
        small(graphics,Component.translatable(menu.turret.enabled()?"thaumicfutures.autocaster.active":"thaumicfutures.autocaster.paused"),leftPos+8,topPos+68,menu.turret.enabled()?0x99ff99:0xffcc77,.75f);
    }
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partial) {
        renderBackground(graphics);super.render(graphics,mouseX,mouseY,partial);renderTooltip(graphics,mouseX,mouseY);
        if(isHovering(36,29,16,16,mouseX,mouseY) && menu.getSlot(0).getItem().isEmpty())
            graphics.renderTooltip(font,Component.translatable("thaumicfutures.autocaster.wand_slot"),mouseX,mouseY);
        if(isHovering(65,29,16,16,mouseX,mouseY) && menu.getSlot(1).getItem().isEmpty())
            graphics.renderTooltip(font,Component.translatable("thaumicfutures.autocaster.battery_slot"),mouseX,mouseY);
        var stack=menu.getSlot(0).getItem();
        if(stack.getItem() instanceof CastingWandItem wand) {
            var aspects=tc4revival.aspect.TCAspects.catalog().primals().stream().sorted(java.util.Comparator.comparing(tc4revival.aspect.Aspect::tag)).toList();
            for(int i=0;i<aspects.size();i++)if(isHovering(8+i*27,173,23,35,mouseX,mouseY)) {
                var aspect=aspects.get(i);graphics.renderTooltip(font,Component.translatable("tc.aspect."+aspect.tag()).append(String.format(java.util.Locale.ROOT," %.2f / %.2f",wand.vis(stack,aspect)/100f,wand.capacity(stack)/100f)),mouseX,mouseY);
            }
        }
    }
}
