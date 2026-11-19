package fmmodern.client;

import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.client.extensions.common.IClientMobEffectExtensions;

/** Legacy potion atlas loaded from the player's external Forbidden Magic jar. */
public final class BloodSealIcon implements IClientMobEffectExtensions {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("forbidden_magic:textures/misc/potions.png");
    @Override public boolean renderInventoryIcon(MobEffectInstance effect,EffectRenderingInventoryScreen<?> screen,GuiGraphics graphics,int x,int y,int z){
        tc4revival.client.LegacyGui.blit(graphics,TEXTURE,x+6,y+7,72,198,18,18);return true;
    }
    @Override public boolean renderGuiIcon(MobEffectInstance effect,Gui gui,GuiGraphics graphics,int x,int y,float z,float alpha){
        graphics.setColor(1,1,1,alpha);
        try{tc4revival.client.LegacyGui.blit(graphics,TEXTURE,x+3,y+3,72,198,18,18);}finally{graphics.setColor(1,1,1,1);}
        return true;
    }
}
