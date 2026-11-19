package tc4revival.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import tc4revival.content.*;

public  class ArcaneBoreScreen extends AbstractContainerScreen<ArcaneBoreMenu> {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumcraft:textures/gui/gui_arcanebore.png");
    public ArcaneBoreScreen(ArcaneBoreMenu menu,Inventory inventory,Component title) { super(menu,inventory,title); imageWidth=176; imageHeight=141; }
    @Override protected void renderLabels(GuiGraphics graphics,int x,int y) {}
    @Override protected void renderBg(GuiGraphics graphics,float partial,int x,int y) {
        LegacyGui.blit(graphics,TEXTURE,leftPos,topPos,0,0,imageWidth,imageHeight);
        var bore=menu.bore(); var pick=bore.pick(); var equipment=ArcaneBoreEquipment.inspect(bore.focus(),pick);
        if (!pick.isEmpty() && pick.getDamageValue()+1>=pick.getMaxDamage()) LegacyGui.blit(graphics,TEXTURE,leftPos+74,topPos+18,184,0,16,16);
        var pose=graphics.pose(); pose.pushPose(); pose.translate(leftPos+112,topPos+8,505); pose.scale(.5f,.5f,1);
        graphics.drawString(font,Component.translatable("thaumcraft.bore.width",equipment.width()),0,0,0xffffff,true);
        graphics.drawString(font,Component.translatable("thaumcraft.bore.speed",equipment.speed()),0,10,0xffffff,true);
        graphics.drawString(font,Component.translatable("thaumcraft.bore.properties"),0,24,0xffffff,true);
        int line=34;
        if (equipment.dowsing()) { graphics.drawString(font,Component.translatable("thaumcraft.bore.clusters"),4,line,0xc0c0c0,true); line+=9; }
        if (equipment.fortune()>0) { graphics.drawString(font,Component.translatable("enchantment.minecraft.fortune").append(" "+equipment.fortune()),4,line,15648330,true); line+=9; }
        if (equipment.silk()) graphics.drawString(font,Component.translatable("enchantment.minecraft.silk_touch"),4,line,0x8080ff,true);
        pose.popPose();
    }
    @Override public void render(GuiGraphics graphics,int x,int y,float partial) { renderBackground(graphics); super.render(graphics,x,y,partial); renderTooltip(graphics,x,y); }
}
