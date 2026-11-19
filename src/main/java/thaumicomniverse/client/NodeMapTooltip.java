package thaumicomniverse.client;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** JourneyMap's multiline background scales twice. Use Minecraft's own measured/wrapped tooltip. */
@Mod.EventBusSubscriber(modid="thaumicomniverse", value=Dist.CLIENT)
public final class NodeMapTooltip {
    private static String owner;
    private static String text;
    private static Screen screen;
    public static void hover(String key, String value) {
        owner=key; text=value; screen=Minecraft.getInstance().screen;
    }
    public static void update(String key, String value) { if (Objects.equals(owner,key)) text=value; }
    public static void leave(String key) { if (Objects.equals(owner,key)) clear(); }
    public static void clear() { owner=null; text=null; screen=null; }
    @SubscribeEvent public static void draw(ScreenEvent.Render.Post event) {
        if (text==null || screen==null || event.getScreen()!=screen) return;
        var font=Minecraft.getInstance().font;
        var lines=new ArrayList<net.minecraft.util.FormattedCharSequence>();
        int width=Math.max(20,Math.min(260,screen.width-24));
        for(var line:text.split("\n")) lines.addAll(font.split(Component.literal(line),width));
        event.getGuiGraphics().renderTooltip(font,lines,event.getMouseX(),event.getMouseY());
    }
}
