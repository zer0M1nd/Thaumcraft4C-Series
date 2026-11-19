package dev.tc4.updaterui;

import com.google.gson.*;
import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.IModuleLayerManager;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Minecraft widgets are available only after mod loading; approved JARs apply on the next launch. */
@Mod.EventBusSubscriber(modid="thaumcraftupdater", value=Dist.CLIENT)
public final class UpdateScreen extends Screen {
    private static boolean shown;
    private final Screen parent;
    private final Function<String,String> bridge;
    private JsonObject state;
    private int first;
    private boolean busy;
    private volatile String progress="";
    private String error="";
    private static final int TOP=62, ROW=60;

    private UpdateScreen(Screen parent, Function<String,String> bridge, JsonObject state) {
        super(Component.literal("神秘时代系列更新")); this.parent=parent; this.bridge=bridge; this.state=state;
    }
    @SuppressWarnings("unchecked")
    @SubscribeEvent public static void opening(ScreenEvent.Opening event) {
        if (shown || !(event.getNewScreen() instanceof TitleScreen)) return;
        shown=true;
        try {
            var layers=Launcher.INSTANCE.findLayerManager().orElseThrow();
            Module module=null;
            for (var layer : IModuleLayerManager.Layer.values()) {
                var found=layers.getLayer(layer).flatMap(value->value.findModule("dev.tc4.updater.bootstrap"));
                if (found.isPresent()) { module=found.get(); break; }
            }
            if (module==null) throw new IllegalStateException("Updater service layer missing");
            var type=Class.forName(module,"dev.tc4.updater.bootstrap.UpdaterService");
            var bridge=(Function<String,String>)type.getMethod("clientBridge").invoke(null);
            var state=JsonParser.parseString(bridge.apply("{\"action\":\"snapshot\"}")).getAsJsonObject();
            if (!state.getAsJsonArray("offers").isEmpty()) event.setNewScreen(new UpdateScreen(event.getNewScreen(),bridge,state));
        } catch (Exception failure) {
            org.slf4j.LoggerFactory.getLogger("ThaumcraftUpdater").error("Cannot open update review",failure);
        }
    }
    private int visibleRows() { return Math.max(1,(height-TOP-70)/ROW); }
    private JsonArray offers() { return state.getAsJsonArray("offers"); }
    private JsonObject choices() { return state.getAsJsonObject("choices"); }
    private boolean complete() { return choices().size()==offers().size(); }
    private Button button(String text,int x,int y,int w,Runnable action) {
        var result=addRenderableWidget(Button.builder(Component.literal(text),b->action.run()).bounds(x,y,w,20).build());
        result.active=!busy; return result;
    }
    @Override protected void init() {
        first=Math.max(0,Math.min(first,Math.max(0,offers().size()-visibleRows())));
        button("↑",width-60,38,20,()->scroll(-1)).active=!busy&&first>0;
        button("↓",width-36,38,20,()->scroll(1)).active=!busy&&first+visibleRows()<offers().size();
        int actionWidth=(width-48)/3;
        for (int i=first;i<Math.min(offers().size(),first+visibleRows());i++) {
            JsonObject offer=offers().get(i).getAsJsonObject(); String id=offer.get("modid").getAsString(); int y=TOP+(i-first)*ROW;
            button("更新日志 ↗",width-100,y,84,()->Util.getPlatform().openUri(offer.get("changelog").getAsString()));
            boolean undecided=!choices().has(id);
            button("更新",16,y+30,actionWidth,()->choose("update",List.of(id))).active=!busy&&undecided;
            button("跳过",24+actionWidth,y+30,actionWidth,()->choose("skip",List.of(id))).active=!busy&&undecided;
            button("忽略此版本",32+actionWidth*2,y+30,actionWidth,()->choose("ignore",List.of(id))).active=!busy&&undecided;
        }
        if (!complete()) {
            button("全部更新",16,height-46,actionWidth,()->bulk("update"));
            button("全部跳过",24+actionWidth,height-46,actionWidth,()->bulk("skip"));
            button("全部忽略此版本",32+actionWidth*2,height-46,actionWidth,()->bulk("ignore"));
        } else {
            button("继续游戏",width/2-124,height-46,120,()->minecraft.setScreen(parent));
            button("退出游戏",width/2+4,height-46,120,()->minecraft.stop());
        }
    }
    private void bulk(String action) {
        List<String> ids=new ArrayList<>();
        for (var element:offers()) { String id=element.getAsJsonObject().get("modid").getAsString(); if (!choices().has(id)) ids.add(id); }
        choose(action,ids);
    }
    private void choose(String action,List<String> ids) {
        if (busy) return;
        busy=true; error=""; rebuildWidgets();
        CompletableFuture.supplyAsync(()->{
            JsonObject response=null;
            for (String id:ids) {
                progress=(action.equals("update")?"正在下载并校验：":"正在保存选择：")+id;
                var request=new JsonObject(); request.addProperty("action",action); request.addProperty("modid",id);
                response=JsonParser.parseString(bridge.apply(request.toString())).getAsJsonObject();
                if (!response.get("error").getAsString().isEmpty()) break;
            }
            return response;
        }).whenComplete((response,failure)->minecraft.execute(()->{
            if (response!=null) { state=response; error=response.get("error").getAsString(); }
            if (failure!=null) error=String.valueOf(failure.getMessage());
            busy=false; progress=""; rebuildWidgets();
        }));
    }
    private void scroll(int amount) { if (!busy) { first+=amount; rebuildWidgets(); } }
    @Override public boolean mouseScrolled(double x,double y,double delta) { scroll(delta>0?-1:1); return true; }
    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partial) {
        renderBackground(graphics);
        graphics.drawCenteredString(font,title,width/2,12,0xffffff);
        graphics.drawCenteredString(font,Component.literal("选中更新会先下载，下次启动生效"),width/2,29,0xffcc66);
        graphics.drawString(font,"已选择 "+choices().size()+" / "+offers().size()+" · 滚轮翻页",16,44,0xaaaaaa);
        for (int i=first;i<Math.min(offers().size(),first+visibleRows());i++) {
            var offer=offers().get(i).getAsJsonObject(); String id=offer.get("modid").getAsString(); int y=TOP+(i-first)*ROW;
            graphics.drawString(font,font.plainSubstrByWidth(offer.get("name").getAsString(),width-124),16,y+2,0xffffff);
            String choice=choices().has(id)?switch(choices().get(id).getAsString()) { case "update"->" · 已下载"; case "ignore"->" · 已忽略"; default->" · 已跳过"; }:"";
            graphics.drawString(font,font.plainSubstrByWidth(offer.get("currentVersion").getAsString()+" → "+offer.get("version").getAsString()+choice,width-32),16,y+17,0x99ddff);
        }
        String status=busy?progress:!error.isEmpty()?"失败，可重试："+error:complete()?"选择已保存。已下载的更新需要退出并重新启动。":"批量按钮只处理尚未选择的 mod";
        graphics.drawString(font,font.plainSubstrByWidth(status,width-32),16,height-60,error.isEmpty()?0xdddddd:0xff7777);
        super.render(graphics,mouseX,mouseY,partial);
    }
}
