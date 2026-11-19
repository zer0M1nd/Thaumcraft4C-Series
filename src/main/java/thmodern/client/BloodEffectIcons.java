package thmodern.client;

import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.client.extensions.common.IClientMobEffectExtensions;

public final class BloodEffectIcons implements IClientMobEffectExtensions {
    private final java.util.function.Supplier<String> kind;
    public BloodEffectIcons(java.util.function.Supplier<String> kind){this.kind=kind;}
    private void draw(GuiGraphics graphics,int x,int y,float alpha){
        int index=switch(kind.get()){case "synthesis"->1;case "vacuum"->2;case "vis_regeneration"->3;default->0;};
        graphics.setColor(1,1,1,alpha);tc4revival.client.LegacyGui.blit(graphics,net.minecraft.resources.ResourceLocation.parse("thaumichorizons:textures/misc/potions.png"),x,y,index*18,198,18,18);graphics.setColor(1,1,1,1);
    }
    @Override public boolean renderInventoryIcon(MobEffectInstance effect,EffectRenderingInventoryScreen<?> screen,GuiGraphics graphics,int x,int y,int z){draw(graphics,x+6,y+7,1);return true;}
    @Override public boolean renderGuiIcon(MobEffectInstance effect,Gui gui,GuiGraphics graphics,int x,int y,float z,float alpha){draw(graphics,x+3,y+3,alpha);return true;}
}
