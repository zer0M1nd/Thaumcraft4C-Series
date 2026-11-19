package automagy.client;

import automagy.content.AutoHandMirrorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class AutoHandMirrorScreen extends AbstractContainerScreen<AutoHandMirrorMenu>{
    public AutoHandMirrorScreen(AutoHandMirrorMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=176;imageHeight=166;inventoryLabelY=73;}
    @Override protected void renderBg(GuiGraphics g,float partial,int x,int y){
        tc4revival.client.LegacyGui.blit(g,net.minecraft.resources.ResourceLocation.parse("automagy:textures/gui/auto_hand_mirror.png"),leftPos,topPos,0,0,imageWidth,imageHeight);
        if(menu.heldSlot()>=0&&menu.heldSlot()<9)tc4revival.client.LegacyGui.blit(g,net.minecraft.resources.ResourceLocation.parse("automagy:textures/gui/auto_hand_mirror.png"),leftPos+7+menu.heldSlot()*18,topPos+127,0,imageHeight,menu.heldSlot()==8?18:17,32);
    }
    @Override protected void renderLabels(GuiGraphics g,int x,int y){
        // The original screen has no container titles. Count filters alone add two small labels.
        for(int i=0;i<2;i++){
            var stack=menu.getSlot(i+1).getItem();
            if(!(stack.getItem() instanceof automagy.content.FilterItem filter)
                    ||!automagy.content.ItemFilter.load(stack).useCount)continue;
            String key="Automagy.gui.autoHandMirror."+(filter.blacklist?"blacklist":"whitelist");
            // GUIAutoHandMirror multiplies ModGuiContainer's 0.75 scale by 0.7.
            float scale=.75f*.7f;
            for(int line=0;line<2;line++){
                var text=Component.translatable(key+"."+(line+1));
                LegacyControls.centered(g,text,i==0?29:148,49+line*6,scale,0xFFFFFF,true);
            }
        }
    }
    @Override public void render(GuiGraphics g,int x,int y,float partial){renderBackground(g);super.render(g,x,y,partial);renderTooltip(g,x,y);}
}
