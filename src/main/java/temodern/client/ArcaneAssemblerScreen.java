package temodern.client;

import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.StyleManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import temodern.crafting.ArcaneAssemblerMenu;

public class ArcaneAssemblerScreen extends UpgradeableScreen<ArcaneAssemblerMenu> {
    public ArcaneAssemblerScreen(ArcaneAssemblerMenu menu,Inventory inventory,Component title){
        super(menu,inventory,title,StyleManager.loadStyleDoc("/screens/thaumicenergistics_assembler.json"));
    }
    @Override public void drawBG(GuiGraphics g,int x,int y,int mx,int my,float partial){
        super.drawBG(g,x,y,mx,my,partial);
        // AE2 paints these for IOptionalSlot; plain AppEngSlot needs explicit slot backgrounds.
        for(var slot:menu.getSlots(appeng.menu.SlotSemantics.MACHINE_CRAFTING_GRID))
            tc4revival.client.LegacyGui.blit(g,net.minecraft.resources.ResourceLocation.parse("ae2:textures/guis/molecular_assembler.png"),x+slot.x-1,y+slot.y-1,7,114,18,18,256,256);
        // Native background with one additional knowledge-core slot at the requested position.
        tc4revival.client.LegacyGui.blit(g,net.minecraft.resources.ResourceLocation.parse("ae2:textures/guis/molecular_assembler.png"),x+91,y+15,125,15,18,18,256,256);
        tc4revival.client.LegacyGui.blit(g,net.minecraft.resources.ResourceLocation.parse("ae2:textures/guis/molecular_assembler.png"),x+73,y+15,125,15,18,18,256,256);
        int index=0;
        for(String entry:menu.vis.split(";")){
            var parts=entry.split(":");if(parts.length!=2)continue;
            var key=temodern.essentia.EssentiaKey.parse(parts[0]);if(key==null)continue;
            int px=x+94+index%3*23,py=y+76+index/3*18;index++;
            new EssentiaRendering().drawInGui(minecraft,g,px,py,key);
            String number=String.format(java.util.Locale.ROOT,"%.1f",Integer.parseInt(parts[1])/100f);
            if(number.endsWith(".0"))number=number.substring(0,number.length()-2);
            g.pose().pushPose();g.pose().translate(px+16-font.width(number)*.5f,py+12,1);g.pose().scale(.5f,.5f,1);
            g.drawString(font,number,0,0,0xffffff,true);g.pose().popPose();
        }
    }
}
