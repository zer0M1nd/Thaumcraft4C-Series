package temodern.client;

import appeng.client.gui.me.items.CraftingTermScreen;
import appeng.client.gui.style.StyleManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import temodern.crafting.ArcaneTerminalMenu;

public class ArcaneTerminalScreen extends CraftingTermScreen<ArcaneTerminalMenu> {
    public ArcaneTerminalScreen(ArcaneTerminalMenu menu,Inventory player,Component title){
        super(menu,player,title,StyleManager.loadStyleDoc("/screens/terminals/thaumicenergistics_arcane.json"));
        var swap=new Button(0,0,8,8,Component.translatable("gui.thaumicenergistics.swap_armor"),b->menu.swapArmor(),message->message.get()){
            @Override public void renderWidget(GuiGraphics g,int mx,int my,float partial){
                g.pose().pushPose();g.pose().translate(getX(),getY(),0);g.pose().scale(.5f,.5f,1);
                com.mojang.blaze3d.systems.RenderSystem.enableBlend();
                com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
                appeng.client.gui.Icon.TOOLBAR_BUTTON_BACKGROUND.getBlitter().dest(0,0).blit(g);
                tc4revival.client.LegacyGui.blit(g,ResourceLocation.parse("thaumicenergistics:textures/gui/states.png"),0,0,0,0,16,16,256,256);
                g.pose().popPose();
            }
        };
        if(!menu.wireless()){swap.setTooltip(Tooltip.create(swap.getMessage()));widgets.add("swapArmor",swap);}
        if(menu.wireless()){
            var wand=((tc4revival.content.CastingWandItem)tc4revival.content.TCItems.CASTING_WAND.get()).create(
                    tc4revival.content.WandParts.Rod.SILVERWOOD,tc4revival.content.WandParts.Cap.THAUMIUM,false,false);
            var toggle=new appeng.client.gui.widgets.TabButton(wand,Component.translatable("gui.thaumicenergistics.inventory_wands"),b->menu.inventoryWands(!menu.useInventoryWands)){
                @Override public void renderWidget(GuiGraphics g,int mx,int my,float partial){
                    super.renderWidget(g,mx,my,partial);
                    if(!menu.useInventoryWands)g.renderItem(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BARRIER),getX()+3,getY()+3);
                }
            };
            toggle.setStyle(appeng.client.gui.widgets.TabButton.Style.BOX);addToLeftToolbar(toggle);
        }
    }
    @Override public void drawBG(GuiGraphics g,int x,int y,int mx,int my,float partial){
        super.drawBG(g,x,y,mx,my,partial);
        if(menu.wireless()){
            g.fill(x+7,y+imageHeight-163,x+43,y+imageHeight-89,0xffc6c6c6);
            for(var semantic:java.util.List.of(ArcaneTerminalMenu.SINGULARITY))
                for(var slot:menu.getSlots(semantic)){
                    int sx=x+slot.x-1,sy=y+slot.y-1;
                    g.fill(sx,sy,sx+18,sy+18,0xff373737);
                    g.fill(sx+1,sy+1,sx+18,sy+18,0xffffffff);
                    g.fill(sx+1,sy+1,sx+17,sy+17,0xff8b8b8b);
                }
        }
        if(!menu.wand().isEmpty())g.fill(x+116,y+imageHeight-153,x+132,y+imageHeight-137,0xff8b8b8b);
        if(menu.visCosts.isEmpty())return;
        g.flush();com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        int index=0;
        for(String entry:menu.visCosts.split(";")){
            var values=entry.split(":");if(values.length!=2)continue;
            var aspect=tc4revival.aspect.TCAspects.catalog().find(values[0]).orElse(null);
            if(aspect==null)continue;
            int amount=Integer.parseInt(values[1]);
            int px=x+136+(index%2)*18,py=y+imageHeight-153+(index/2)*18;index++;
            float alpha=menu.canPayVis||tc4revival.content.WandVis.stored(menu.wand().getTag(),aspect)>=amount?1f:
                    .5f+.25f*(float)Math.sin(net.minecraft.Util.getMillis()*Math.PI/1000);
            g.setColor(((aspect.color()>>16)&255)/255f,((aspect.color()>>8)&255)/255f,(aspect.color()&255)/255f,alpha);
            tc4revival.client.LegacyGui.blit(g,aspect.texture(),px,py,0,0,16,16,16,16);g.setColor(1,1,1,1);
            String number=amount%100==0?Integer.toString(amount/100):String.format(java.util.Locale.ROOT,"%.1f",amount/100f);
            g.pose().pushPose();g.pose().translate(px+16-font.width(number)*.5f,py+12,1);g.pose().scale(.5f,.5f,1);
            g.drawString(font,number,0,0,0xffffff,true);g.pose().popPose();
        }
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }
}
