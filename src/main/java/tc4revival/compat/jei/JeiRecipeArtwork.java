package tc4revival.compat.jei;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import tc4revival.client.LegacyGui;
import tc4revival.content.TCItems;

/** Uses the player's original-resource pack, including resource-pack overrides. */
public class JeiRecipeArtwork {
    private static final ResourceLocation OVERLAY=ResourceLocation.parse("thaumcraft:textures/gui/gui_researchbook_overlay.png");
    private static void region(GuiGraphics g,int x,int y,int u,int v,int w,int h) {
        g.pose().pushPose();
        g.pose().translate(x,y,0);
        g.pose().scale(2,2,1);
        LegacyGui.blit(g,OVERLAY,0,0,u,v,w,h);
        g.pose().popPose();
    }
    public static void draw(GuiGraphics g,String kind) {
        switch(kind) {
            case "arcane" -> {
                region(g,4,4,112,15,52,52);
                region(g,136,32,20,3,16,16);
            }
            case "crucible" -> {
                region(g,136,4,20,3,16,16);
                region(g,0,32,0,20,56,48);
                region(g,42,16,100,84,11,13);
            }
            case "infusion", "infusion_enchantment" -> {
                region(g,136,32,20,3,16,16);
                region(g,0,8,200,77,60,44);
            }
            case "aspect_combine" -> {
                g.drawString(Minecraft.getInstance().font,"→",110,28,0x555555,false);
            }
            case "item_aspects" -> {
                // The regular scanner item renderer honors the original model and pack replacements.
                // GUI item rendering adds 150 Z, including the scanner's three-dimensional lens.
                // Ordering alone cannot put JEI's later item above that depth-writing geometry.
                // Keep the scanner above the page but below the normal item layer.
                g.flush();
                g.pose().pushPose();
                g.pose().translate(0,12,-100);
                g.pose().scale(2.5f,2.5f,1);
                g.renderItem(new ItemStack(TCItems.THAUMOMETER.get()),0,0);
                g.flush();
                g.pose().popPose();
            }
        }
    }
}
