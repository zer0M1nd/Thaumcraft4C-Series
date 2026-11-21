package tc4validation.bugs;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Observes real screen, inventory and synchronized block state; no product behavior is replaced. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public final class BugClientProbe {
    static String previous="";static int ticks;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(!Boolean.getBoolean("tc4.bugValidation")||event.phase!=TickEvent.Phase.END||++ticks%5!=0)return;
        var mc=Minecraft.getInstance();var root=mc.gameDirectory.toPath();var path=root.resolve("bug-client-request.json");
        if(!Files.isRegularFile(path))return;JsonObject out=new JsonObject();
        try {
            String raw=Files.readString(path);if(raw.equals(previous))return;previous=raw;
            JsonObject request=JsonParser.parseString(raw).getAsJsonObject();out.add("request",request);
            if(request.has("openKeys"))mc.setScreen(new net.minecraft.client.gui.screens.controls.KeyBindsScreen(mc.screen,mc.options));
            if(request.has("closeScreen"))mc.setScreen(null);
            // Fallback for MC Pilot cursor events that do not reach this Forge screen.
            // Invoke the real Screen callbacks; research actions still travel over its normal packets.
            if(request.has("screenInput")) {
                var input=request.getAsJsonObject("screenInput");var active=Objects.requireNonNull(mc.screen,"No active screen");
                double x=input.get("x").getAsDouble(),y=input.get("y").getAsDouble();
                int button=input.has("button")?input.get("button").getAsInt():0;
                String action=input.get("action").getAsString();
                active.mouseMoved(x,y);
                if(!action.equals("move")) {
                    active.mouseClicked(x,y,button);
                    if(action.equals("drag")) {
                        double tx=input.get("toX").getAsDouble(),ty=input.get("toY").getAsDouble();
                        active.mouseDragged(tx,ty,button,tx-x,ty-y);x=tx;y=ty;
                    }
                    active.mouseReleased(x,y,button);
                }
                net.minecraftforge.fml.util.ObfuscationReflectionHelper.setPrivateValue(net.minecraft.client.MouseHandler.class,mc.mouseHandler,x*mc.getWindow().getScreenWidth()/active.width,"f_91507_");
                net.minecraftforge.fml.util.ObfuscationReflectionHelper.setPrivateValue(net.minecraft.client.MouseHandler.class,mc.mouseHandler,y*mc.getWindow().getScreenHeight()/active.height,"f_91508_");
                out.add("screenInput",input);
            }
            out.addProperty("mouseX",mc.mouseHandler.xpos());out.addProperty("mouseY",mc.mouseHandler.ypos());
            if(request.has("language")) {
                String lang=request.get("language").getAsString();mc.getLanguageManager().setSelected(lang);mc.options.languageCode=lang;
                mc.reloadResourcePacks();
            }
            out.addProperty("time",java.time.Instant.now().toString());out.addProperty("profile",tc4revival.config.TCConfig.SERVER.revival.profile().name());
            out.add("blocks",BugProbe.blocks(mc.level,request));
            JsonArray keys=new JsonArray();for(var key:mc.options.keyMappings) {
                JsonObject row=new JsonObject();row.addProperty("name",key.getName());row.addProperty("category",key.getCategory());
                row.addProperty("key",key.getKey().getName());row.addProperty("identity",System.identityHashCode(key));row.addProperty("label",net.minecraft.client.resources.language.I18n.get(key.getName()));keys.add(row);
            }out.add("keys",keys);
            var screen=mc.screen;out.addProperty("screen",screen==null?"none":screen.getClass().getName());
            if(screen!=null) {
                out.addProperty("width",screen.width);out.addProperty("height",screen.height);out.addProperty("title",screen.getTitle().getString());
                if(screen instanceof AbstractContainerScreen<?> c){out.addProperty("left",c.getGuiLeft());out.addProperty("top",c.getGuiTop());}
                JsonArray widgets=new JsonArray();for(var child:screen.children())if(child instanceof AbstractWidget widget) {
                    JsonObject row=new JsonObject();row.addProperty("class",child.getClass().getName());row.addProperty("label",widget.getMessage().getString());
                    row.addProperty("x",widget.getX());row.addProperty("y",widget.getY());row.addProperty("width",widget.getWidth());row.addProperty("height",widget.getHeight());
                    row.addProperty("visible",widget.visible);row.addProperty("active",widget.active);if(widget instanceof EditBox box)row.addProperty("value",box.getValue());widgets.add(row);
                }out.add("widgets",widgets);
                JsonObject fields=new JsonObject();if(screen.getClass().getName().startsWith("tc4revival."))for(var f:screen.getClass().getDeclaredFields()) {
                    if(java.lang.reflect.Modifier.isStatic(f.getModifiers()))continue;f.setAccessible(true);
                    Object value=f.get(screen);if(value!=null&&!(value instanceof Minecraft))fields.addProperty(f.getName(),String.valueOf(value));
                }out.add("fields",fields);
            }
            if(mc.player!=null) {
                out.addProperty("revealing",tc4revival.content.RevealingGear.visible(mc.player,false));
                JsonObject risk=new JsonObject();for(var f:tc4revival.client.InfusionRiskOverlay.class.getDeclaredFields())if(java.lang.reflect.Modifier.isStatic(f.getModifiers())){f.setAccessible(true);risk.addProperty(f.getName(),String.valueOf(f.get(null)));}out.add("infusionOverlay",risk);
                out.addProperty("position",mc.player.position().toString());out.addProperty("health",mc.player.getHealth());
                JsonArray slots=new JsonArray();for(var slot:mc.player.containerMenu.slots){JsonObject s=BugProbe.stack(slot.getItem());s.addProperty("slot",slot.index);s.addProperty("x",slot.x);s.addProperty("y",slot.y);slots.add(s);}out.add("slots",slots);
            }
            if(request.has("translate")) {JsonObject labels=new JsonObject();for(var key:request.getAsJsonArray("translate"))labels.addProperty(key.getAsString(),net.minecraft.client.resources.language.I18n.get(key.getAsString()));out.add("translations",labels);}
            out.addProperty("success",true);
            if(request.has("shutdown"))mc.stop();
        }catch(Throwable error){out.addProperty("success",false);out.addProperty("error",error.toString());org.slf4j.LoggerFactory.getLogger("BUG_CLIENT_PROBE").error("Isolated client probe failure",error);}
        try{Files.writeString(root.resolve("bug-client-response.json"),BugProbe.GSON.toJson(out));}catch(Exception error){throw new IllegalStateException(error);}
    }
}
