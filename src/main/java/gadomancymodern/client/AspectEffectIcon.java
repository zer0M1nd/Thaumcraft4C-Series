package gadomancymodern.client;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.client.extensions.common.IClientMobEffectExtensions;
public class AspectEffectIcon implements IClientMobEffectExtensions {
    protected final java.util.function.Supplier<String> aspect;
    public AspectEffectIcon(String aspect){this(()->aspect);}
    public AspectEffectIcon(java.util.function.Supplier<String> aspect){this.aspect=aspect;}
    protected void draw(GuiGraphics graphics,int x,int y,float alpha){var definition=tc4revival.aspect.TCAspects.catalog().require(aspect.get());int color=definition.color();graphics.setColor((color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,alpha);tc4revival.client.LegacyGui.blit(graphics,definition.texture(),x,y,18,18,0,0,16,16,16,16);graphics.setColor(1,1,1,1);}
    @Override public boolean renderInventoryIcon(MobEffectInstance effect,net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen<?> screen,GuiGraphics graphics,int x,int y,int z){draw(graphics,x,y+7,1);return true;}
    @Override public boolean renderGuiIcon(MobEffectInstance effect,net.minecraft.client.gui.Gui gui,GuiGraphics graphics,int x,int y,float z,float alpha){draw(graphics,x+3,y+3,alpha);return true;}
}
