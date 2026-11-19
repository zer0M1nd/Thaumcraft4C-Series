package tc4revival.client;

import java.util.concurrent.CompletableFuture;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.resources.*;

/** Uses only Minecraft widgets. Created after vanilla fonts and shaders have loaded. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public class OriginalResourceScreen extends Screen {
    private final Screen parent;
    private java.util.List<net.minecraft.util.FormattedCharSequence> lines=java.util.List.of();
    private int scroll;
    private boolean busy;
    private volatile String progress="";
    public OriginalResourceScreen(Screen parent) { super(Component.literal("原版资源 / Original resources"));this.parent=parent; }
    @SubscribeEvent public static void opening(net.minecraftforge.client.event.ScreenEvent.Opening event) {
        if(event.getNewScreen() instanceof net.minecraft.client.gui.screens.TitleScreen&&ResourceLoadingState.needsScreen())
            event.setNewScreen(new OriginalResourceScreen(event.getNewScreen()));
    }
    @Override protected void init() {
        lines=font.split(Component.literal(ResourceLoadingState.details()),Math.max(150,width-48));
        var path=new EditBox(font,24,52,width-48,20,Component.literal("资源文件夹 / Resource directory"));
        path.setMaxLength(4096);path.setValue(net.minecraftforge.fml.loading.FMLPaths.GAMEDIR.get().toAbsolutePath().resolve("thaumcraft").toString());path.setEditable(false);addRenderableWidget(path);
        int y=height-28;int buttonWidth=Math.min(140,(width-56)/3);
        addRenderableWidget(Button.builder(Component.literal("重试 / Retry"),b->retry()).bounds(20,y,buttonWidth,20).build()).active=!busy;
        addRenderableWidget(Button.builder(Component.literal("复制详情 / Copy"),b->minecraft.keyboardHandler.setClipboard(path.getValue()+"\n"+ResourceLoadingState.details())).bounds(28+buttonWidth,y,buttonWidth,20).build());
        addRenderableWidget(Button.builder(Component.literal("退出 / Quit"),b->minecraft.stop()).bounds(36+buttonWidth*2,y,buttonWidth,20).build());
    }
    private void retry() {
        if(busy)return;busy=true;progress="正在检查与下载 / Checking and downloading…";rebuildWidgets();
        CompletableFuture.runAsync(()->OriginalDownloadBootstrap.retry(message->progress=message))
            .whenComplete((unused,error)->minecraft.execute(()->{
                if(error!=null)ResourceLoadingState.failure(String.valueOf(error.getMessage()));
                minecraft.reloadResourcePacks().whenComplete((done,reloadError)->minecraft.execute(()->{
                    if(reloadError!=null)ResourceLoadingState.failure("Resource reload: "+reloadError.getMessage());
                    busy=false;progress="";scroll=0;
                    if(!ResourceLoadingState.needsScreen())minecraft.setScreen(parent);else rebuildWidgets();
                }));
            }));
    }
    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean mouseScrolled(double x,double y,double delta) {
        scroll=Math.max(0,Math.min(Math.max(0,lines.size()-(height-130)/font.lineHeight),scroll-(int)(delta*3)));return true;
    }
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partial) {
        renderBackground(graphics);
        graphics.drawCenteredString(font,title,width/2,12,0xffffff);
        graphics.drawCenteredString(font,Component.literal(busy?"正在检查、下载并重新加载资源…":"下载失败：可重试，或放入原版 JAR 后重试。"),width/2,32,0xffcc66);
        graphics.enableScissor(20,80,width-20,height-52);
        for(int i=scroll;i<lines.size()&&80+(i-scroll)*font.lineHeight<height-52;i++)graphics.drawString(font,lines.get(i),24,80+(i-scroll)*font.lineHeight,0xeeeeee);
        graphics.disableScissor();
        if(busy)graphics.drawString(font,font.plainSubstrByWidth(progress,width-40),20,height-44,0xffffff);
        super.render(graphics,mouseX,mouseY,partial);
    }
}
