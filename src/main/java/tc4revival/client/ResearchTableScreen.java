package tc4revival.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import java.util.LinkedHashMap;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Inventory;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.TCAspects;
import tc4revival.config.TCConfig;
import tc4revival.content.ResearchTableBlockEntity.Action;
import tc4revival.content.ResearchTableMenu;
import tc4revival.content.ScribingToolsItem;
import tc4revival.content.TCSounds;
import tc4revival.network.ResearchTableActionPacket;
import tc4revival.network.TCNetwork;
import tc4revival.research.*;

/** Legacy research desk layout and interaction; knowledge displayed here is a read-only server snapshot. */
public  class ResearchTableScreen extends AbstractContainerScreen<ResearchTableMenu> {
    private static final ResourceLocation GUI = texture("gui/guiresearchtable2");
    private static final ResourceLocation PARTICLES = texture("misc/particles");
    private record Rune(ResearchHex hex, long start, long end, int glyph) {}
    private final java.util.Map<ResearchHex, Rune> runes = new LinkedHashMap<>();
    private PlayerKnowledge knowledge;
    private ResearchNote note,completedPreview;
    private boolean sorting;
    private List<Aspect> aspects = List.of();
    private Aspect first, second, dragged;
    private int page;
    private EditBox search;
    private ResearchColumnSelection columns;
    private boolean shortcuts, autoConnect, shortcutsPreference, autoConnectPreference, localPreferencesChanged;
    private ResearchTableAccess.Access access = new ResearchTableAccess.Access(false,false,false,false,false,false,false);
    private int searchStrategy;
    private boolean searchStrategyChanged;
    private List<Aspect> unfiltered = List.of();
    private List<Aspect> filterSource;
    private String filterLanguage;
    private tc4revival.aspect.AspectCatalog filterCatalog;
    private boolean filterDirty = true;
    private long combineUntil, nextRune;
    private ResearchDragSolver dragSolver;
    private ResearchDragSolver.Plan dragPlan;
    private ResearchMiddleGesture middleGesture;
    private Aspect middleSearch;
    private final java.util.ArrayDeque<java.util.Map.Entry<ResearchHex,String>> plannedWrites=new java.util.ArrayDeque<>();
    private ResearchNote awaitingWrite;
    private java.util.Map.Entry<ResearchHex,String> sentWrite;
    private int writeWait;
    private float layoutScale=1;
    public ResearchTableScreen(ResearchTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageWidth = 255; imageHeight = 255;
    }
    @Override protected void init() {
        layoutScale=Math.min(1,Math.min(width/263f,height/263f));
        width=(int)Math.ceil(width/layoutScale);height=(int)Math.ceil(height/layoutScale);
        imageWidth=255;super.init();
        if (columns == null) {
            columns = new ResearchColumnSelection(TCConfig.CLIENT.researchTableColumn.get());
            shortcutsPreference = TCConfig.CLIENT.researchTableShortcuts.get();
            autoConnectPreference = TCConfig.CLIENT.researchTableAutoConnect.get();
            searchStrategy = TCConfig.CLIENT.searchStrategy();
        }
        String previous = search == null ? "" : search.getValue();
        search = new EditBox(font, leftPos+10, topPos+30, 63, 9, Component.translatable("thaumcraft.research.aspect_search"));
        search.setBordered(false); search.setMaxLength(80); search.setTextColor(0xffffffff);
        search.setHint(Component.translatable("thaumcraft.research.aspect_search"));
        search.setValue(previous);
        search.setResponder(value -> { filterDirty=true; refreshAspects(); columns.queryChanged(!value.isBlank(),aspects.size()); updatePage(); });
        search.setFilter(value -> ResearchTableAccess.queryAllowed(value, access));
        addRenderableWidget(search);
        refreshAccess();
        // This is a container screen: typing starts only after the player selects the search field.
        setFocused(null);
        search.setFocused(false);
    }
    private void refreshAccess() {
        knowledge = ClientKnowledge.displaySnapshot().orElse(null);
        var next = ResearchTableAccess.resolve(knowledge);
        if (!next.equals(access)) filterDirty=true;
        access=next;
        shortcuts=shortcutsPreference && access.basic();
        autoConnect=autoConnectPreference && access.connect();
        if (!access.sorting()) sorting=false;
        if (search!=null) {
            search.visible=access.search() || access.related();
            search.active=search.visible;
            if (!ResearchTableAccess.queryAllowed(search.getValue(),access)) search.setValue("");
            if (!search.visible) search.setFocused(false);
        }
        if (!autoConnect) {dragSolver=null;dragPlan=null;plannedWrites.clear();awaitingWrite=null;sentWrite=null;}
    }
    private void refreshAspects() {
        var catalog = TCAspects.catalog();
        String language = minecraft.getLanguageManager().getSelected();
        if (filterDirty || !unfiltered.equals(filterSource) || !language.equals(filterLanguage) || catalog != filterCatalog) {
            aspects = ResearchAspectSearch.filter(unfiltered,catalog,search==null?"":search.getValue(),searchStrategy,
                    a->Component.translatable(thaumcraft.api.client.ClientAspectViews.view(a).descriptionKey()).getString(),ClientTextSearch::contains);
            filterSource=unfiltered;filterLanguage=language;filterCatalog=catalog;filterDirty=false;
        }
    }
    private void updatePage() {
        if(shortcuts) { if(!aspects.isEmpty())columns.clamp(aspects.size()); page=columns.windowStart(aspects.size()); }
        else page=Math.max(0,Math.min(page,ResearchTableView.lastPage(aspects.size())));
    }
    @Override protected void containerTick() { super.containerTick(); search.tick(); advancePlannedWrites(); }
    @Override public void removed() {
        if(localPreferencesChanged) {
            TCConfig.CLIENT.researchTableShortcuts.set(shortcutsPreference);
            TCConfig.CLIENT.researchTableAutoConnect.set(autoConnectPreference);
        }
        if(searchStrategyChanged)TCConfig.CLIENT.researchTableSearchStrategy.set(TCConfig.SearchStrategy.values()[searchStrategy+1]);
        TCConfig.CLIENT.researchTableColumn.set(columns.remembered());
        TCConfig.CLIENT_SPEC.save();
        super.removed();
    }
    private static ResourceLocation texture(String path) { return ResourceLocation.parse("thaumcraft:textures/" + path + ".png"); }
    private boolean research(String key) { return knowledge != null && knowledge.researchMarkers().contains(key); }
    private boolean known(String tag) { return knowledge != null && knowledge.knows(TCAspects.catalog().require(tag)); }
    private boolean available(Aspect aspect) { return aspect != null && knowledge != null && (knowledge.poolAmount(aspect) > 0 || menu.bonuses().contains(aspect.tag())); }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        graphics.pose().pushPose();graphics.pose().scale(layoutScale,layoutScale,1);
        renderFitted(graphics,(int)(mouseX/layoutScale),(int)(mouseY/layoutScale),partial);
        graphics.flush();graphics.pose().popPose();
    }
    private void renderFitted(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        refreshAccess();
        unfiltered = knowledge == null ? List.of() : (access.sorting()?menu.preferences():ResearchPreferences.DEFAULT).sort(knowledge,TCAspects.catalog());
        refreshAspects();
        note = ResearchNoteNbt.read(menu.getSlot(1).getItem().getTag(), TCAspects.catalog()).note().orElse(null);
        if(dragSolver!=null && !dragSolver.snapshot().equals(note)) {dragSolver=null;dragPlan=null;}
        if(access.refill() && menu.preferences().autoRefill()) {
            if(note!=null)completedPreview=note.complete()?note:null;
            else if(menu.getSlot(1).getItem().isEmpty())note=menu.completedPreview()!=null?menu.completedPreview():completedPreview;
        }else completedPreview=null;
        updatePage();
        if (!available(first)) first = null;
        if (!available(second)) second = null;
        renderBackground(graphics); super.render(graphics, mouseX, mouseY, partial);
        renderDragPlan(graphics);
        renderTooltip(graphics, mouseX, mouseY);
        Aspect cursor = dragged != null ? dragged : pickedAspect();
        if (cursor != null) orb(graphics, mouseX - 8, mouseY - 8, cursor.color());
        branchControls(graphics,mouseX,mouseY);
        searchControls(graphics,mouseX,mouseY);
        ClientScanFeedback.render(graphics, width, height);
        // Finish all desk content before drawing native tooltip components (including ModernUI).
        graphics.flush();
        drawTooltips(graphics, mouseX, mouseY);
    }
    @Override protected void renderLabels(GuiGraphics graphics, int x, int y) {} // The old texture contains the labels.
    @Override protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        g.setColor(1, 1, 1, 1);
        LegacyGui.blit(g,GUI, leftPos, topPos, 0, 0, 255, 167);
        LegacyGui.blit(g,GUI, leftPos + 40, topPos + 167, 0, 166, 184, 88);
        if(search.visible)g.fill(leftPos+7,topPos+28,leftPos+74,topPos+40,0x80000000);
        if (canMove(-1)) LegacyGui.blit(g,GUI, leftPos + 27, topPos + 121, 184, 208, 24, 8);
        if (canMove(1)) LegacyGui.blit(g,GUI, leftPos + 51, topPos + 121, 208, 208, 24, 8);
        if (first != null && second != null) {
            LegacyGui.blit(g,GUI, leftPos + 35, topPos + 139, 184, 184, 32, 16);
            if (System.nanoTime() < combineUntil) LegacyGui.blit(g,GUI, leftPos + 35, topPos + 139, 184, 168, 32, 16);
            else orb(g, leftPos + 43, topPos + 139, orbColor(leftPos + 43, topPos + 139));
        }
        if (note != null && note.complete() && !menu.getSlot(1).getItem().isEmpty() && research("RESEARCHDUPE")) LegacyGui.blit(g,GUI, leftPos + 37, topPos + 5, 232, 200, 24, 24);
        drawColumn(g);
        for (int i = 0; i < 25 && page * 5 + i < aspects.size(); i++) {
            var aspect = aspects.get(page * 5 + i);
            tag(g, leftPos + 10 + i / 5 * 16, topPos + 40 + i % 5 * 16, aspect,
                    knowledge.poolAmount(aspect), menu.bonuses().contains(aspect.tag()), available(aspect) ? 1 : 0.33f, false);
        }
        drawRowNumbers(g);
        if (first != null) tag(g, leftPos + 13, topPos + 139, first, 0, false, 1, false);
        if (second != null) tag(g, leftPos + 71, topPos + 139, second, 0, false, 1, false);
        sheet(g, mx, my);
    }
    private void sheet(GuiGraphics g, int mx, int my) {
        if (note == null) { runes.clear(); return; }
        LegacyGui.blit(g,texture("misc/parchment3"), leftPos + 94, topPos + 8, 0, 0, 150, 150);
        long now = System.currentTimeMillis();
        if (now > nextRune && minecraft.level != null) {
            nextRune = now + 250;
            var random = minecraft.level.random;
            var hex = ResearchTableView.hex(random.nextInt(120) - 60, random.nextInt(120) - 60);
            if (!note.grid().containsKey(hex) && !runes.containsKey(hex))
                runes.put(hex, new Rune(hex, now, nextRune + 15000 + random.nextInt(10000), random.nextInt(16)));
        }
        runes.values().removeIf(rune -> rune.end() < now);
        for (var rune : runes.values()) {
            float progress = (float) (now - rune.start()) / (rune.end() - rune.start());
            float alpha = (progress < 0.25f ? progress * 2 : progress > 0.5f ? 1 - progress : 0.5f) * 0.66f;
            var point = ResearchTableView.pixel(rune.hex());
            g.pose().pushPose(); g.pose().translate(leftPos + 169 + point.x(), topPos + 83 + point.y(), 0);
            g.pose().mulPose(Axis.ZP.rotationDegrees(-90));
            uvQuad(g, texture("misc/script"), -5, -5, 10, 10, rune.glyph() / 16f, 0, (rune.glyph() + 1) / 16f, 1, 0, alpha, true);
            g.pose().popPose();
        }
        var connections = ResearchTableView.connections(note, TCAspects.catalog(), this::known);
        for (var edge : connections.edges()) {
            var a = ResearchTableView.pixel(edge.from()); var b = ResearchTableView.pixel(edge.to());
            double x = leftPos + 169 + a.x(), y = topPos + 83 + a.y();
            double dx = b.x() - a.x(), dy = b.y() - a.y();
            float alpha = 0.6f + (float) Math.sin(minecraft.player.tickCount + x) * 0.3f;
            g.pose().pushPose(); g.pose().translate(x, y, 0); g.pose().mulPose(Axis.ZP.rotation((float) Math.atan2(dy, dx)));
            // Three physical pixels, as in the old GL line renderer.
            float half = (float) (1.5 / minecraft.getWindow().getGuiScale());
            additiveLine(g, (float) Math.hypot(dx, dy), half, alpha, note.grid().get(edge.to()).negative());
            g.pose().popPose();
        }
        var hover = ResearchTableView.hex(mx - leftPos - 169, my - topPos - 83);
        for (var entry : note.grid().entrySet()) {
            var point = ResearchTableView.pixel(entry.getKey()); var cell = entry.getValue();
            double x = leftPos + 161 + point.x(), y = topPos + 75 + point.y();
            if (!note.complete()) {
                if (cell.negative()) { starHex(g,x,y); orb(g,x,y,0xff2020); }
                else if (cell.type() == 1) orb(g, x, y, orbColor(x, y));
                else {
                    if (entry.getKey().equals(hover)) uvQuad(g, texture("gui/hex2"), x, y, 16, 16, 0, 0, 1, 1, 0xffffff, 1, false);
                    uvQuad(g, texture("gui/hex1"), x, y, 16, 16, 0, 0, 1, 1, 0xffffff, 0.25f, false);
                }
            }
            if (cell.aspect() == null) continue;
            if (!known(cell.aspect())) uvQuad(g, texture("aspects/_unknown"), x, y, 16, 16, 0, 0, 1, 1, 0, 0.5f, false, cell.negative());
            else if (cell.negative()) {
                var aspect = TCAspects.catalog().require(cell.aspect());
                uvQuad(g, thaumcraft.api.client.ClientAspectViews.view(aspect).texture(), x, y, 16, 16, 0, 0, 1, 1, thaumcraft.api.client.ClientAspectViews.view(aspect).color(), 1, false, true);
            }
            else {
                boolean dim = cell.type() == 2 && !connections.lit().contains(entry.getKey());
                tag(g, x, y, TCAspects.catalog().require(cell.aspect()), 0, false, dim ? 0.66f : 1, dim);
            }
        }
    }
    private static void starHex(GuiGraphics g,double x,double y) {
        g.flush(); RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0,texture("gui/gui_researchbackeldritch"));
        var buffer=Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.TRIANGLE_FAN,DefaultVertexFormat.POSITION_TEX_COLOR);
        // Real filled geometry clips the same star field used by the Eldritch category.
        // Sample a distinct 64px patch per cell rather than stretching a hollow hex outline.
        float u=(float)Math.floorMod((int)x,192)/256f,v=(float)Math.floorMod((int)y,192)/256f;
        var points=new java.util.ArrayList<ResearchTableView.Point>();
        points.add(new ResearchTableView.Point(8,8));
        points.addAll(ResearchTableView.cellOutline());points.add(points.get(1));
        for(var p:points)
            buffer.vertex(g.pose().last().pose(),(float)(x+p.x()),(float)(y+p.y()),0)
                    .uv(u+(float)p.x()/64f,v+(float)p.y()/64f).color(255,255,255,255).endVertex();
        com.mojang.blaze3d.vertex.BufferUploader.drawWithShader(buffer.end());
        RenderSystem.disableBlend();
    }
    private void tag(GuiGraphics g, double x, double y, Aspect aspect, int amount, boolean bonus, float alpha, boolean monochrome) {
        uvQuad(g, thaumcraft.api.client.ClientAspectViews.view(aspect).texture(), x, y, 16, 16, 0, 0, 1, 1,
                monochrome ? 0x191919 : thaumcraft.api.client.ClientAspectViews.view(aspect).color(), monochrome ? alpha * 0.8f : alpha, false);
        if (amount > 0) {
            g.pose().pushPose(); g.pose().scale(0.5f, 0.5f, 1);
            String text = Integer.toString(amount); int tx = (int) x * 2 + 32 - font.width(text), ty = (int) y * 2 + 32 - font.lineHeight;
            for (int[] delta : new int[][] {{-1,0},{1,0},{0,-1},{0,1}}) g.drawString(font, text, tx + delta[0], ty + delta[1], 0xff000000, false);
            g.drawString(font, text, tx, ty, 0xffffffff, false); g.pose().popPose();
        }
        if (bonus) LegacyGui.blit(g,PARTICLES, (int) x - 4, (int) y - 4, minecraft.player.tickCount % 16 * 16, 80, 16, 16);
    }
    private int orbColor(double x, double y) {
        int ticks = minecraft.player.tickCount;
        int red = (int) ((0.85 + Math.sin((ticks + x) / 10) * 0.15) * 255);
        int green = (int) ((0.85 + Math.sin((ticks + x + y) / 11) * 0.15) * 255);
        int blue = (int) ((0.85 + Math.sin((ticks + y) / 12) * 0.15) * 255);
        return red << 16 | green << 8 | blue;
    }
    private void orb(GuiGraphics g, double x, double y, int color) {
        if (TCConfig.CLIENT.colorBlind.get()) color = (int) ((color >> 16 & 255) / 1.8) << 16
                | (int) ((color >> 8 & 255) / 1.8) << 8 | (int) ((color & 255) / 1.8);
        float u = 0.5f + minecraft.player.tickCount % 8 / 8f;
        uvQuad(g, PARTICLES, x, y, 16, 16, u, 0.5f, u + 0.0624375f, 0.5624375f, color, 1, true);
    }
    private static void uvQuad(GuiGraphics g, ResourceLocation texture, double x, double y, float width, float height,
            float u0, float v0, float u1, float v1, int color, float alpha, boolean rotatedUV) {
        uvQuad(g, texture, x, y, width, height, u0, v0, u1, v1, color, alpha, rotatedUV, false);
    }
    private static void uvQuad(GuiGraphics g, ResourceLocation texture, double x, double y, float width, float height,
            float u0, float v0, float u1, float v1, int color, float alpha, boolean rotatedUV, boolean negative) {
        g.flush(); RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc();
        if (texture.getPath().equals("textures/gui/hex2.png"))
            RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA, com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
        RenderSystem.setShader(negative ? () -> LegacyLightmapShaders.researchNegative : GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, texture);
        var buffer = Tesselator.getInstance().getBuilder(); buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        float[][] positions = {{0,height},{width,height},{width,0},{0,0}};
        float[][] uv = rotatedUV ? new float[][] {{u1,v1},{u1,v0},{u0,v0},{u0,v1}} : new float[][] {{u0,v1},{u1,v1},{u1,v0},{u0,v0}};
        for (int i = 0; i < 4; i++) buffer.vertex(g.pose().last().pose(), (float) x + positions[i][0], (float) y + positions[i][1], 0)
                .uv(uv[i][0], uv[i][1]).color((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f, alpha).endVertex();
        com.mojang.blaze3d.vertex.BufferUploader.drawWithShader(buffer.end()); RenderSystem.defaultBlendFunc(); RenderSystem.disableBlend();
    }
    private static void additiveLine(GuiGraphics g, float length, float halfWidth, float alpha, boolean negative) {
        g.flush(); RenderSystem.enableBlend();
        if (negative) RenderSystem.defaultBlendFunc();
        else RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA, com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        var buffer = Tesselator.getInstance().getBuilder(); buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (float[] point : new float[][] {{0,halfWidth},{length,halfWidth},{length,-halfWidth},{0,-halfWidth}})
            buffer.vertex(g.pose().last().pose(), point[0], point[1], 0).color(negative ? 1 : 0, negative ? 0 : 0.6f, negative ? 0 : 0.8f, alpha).endVertex();
        com.mojang.blaze3d.vertex.BufferUploader.drawWithShader(buffer.end());
        RenderSystem.defaultBlendFunc(); RenderSystem.disableBlend();
    }
    private Aspect aspectAt(double mx, double my, boolean ignoreZero) {
        int index = ResearchTableView.aspectIndex(page, mx - leftPos - 10, my - topPos - 40);
        return index >= 0 && index < aspects.size() && (ignoreZero || available(aspects.get(index))) ? aspects.get(index) : null;
    }
    private boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= leftPos + x && mx < leftPos + x + w && my >= topPos + y && my < topPos + y + h;
    }
    private void send(Action action, ResearchHex hex, Aspect a, Aspect b) {
        TCNetwork.CHANNEL.sendToServer(new ResearchTableActionPacket(menu.containerId, action, hex.q(), hex.r(),
                a == null ? "" : a.tag(), b == null ? "" : b.tag()));
    }
    private void sound(SoundEvent sound, float volume, boolean vary) {
        minecraft.player.playSound(sound, volume, 1 + (vary ? minecraft.player.getRandom().nextFloat() * 0.1f : 0));
    }
    private void branchControls(GuiGraphics g,int mx,int my) {
        var prefs=menu.preferences();Component tooltip=null;
        if(access.sorting()) {
            ResearchWidgets.smallButton(g,leftPos+76,topPos+121,8,8,inside(mx,my,76,121,8,8));
            ResearchWidgets.smallButton(g,leftPos+85,topPos+121,8,8,inside(mx,my,85,121,8,8));
            buttonLetter(g,"S",76,0xffb8d9de);buttonLetter(g,"R",85,prefs.descending()?0xffffff80:0xffb8d9de);
            if(inside(mx,my,76,121,8,8))tooltip=Component.translatable("thaumcraft.research.sort."+prefs.sort().name().toLowerCase(java.util.Locale.ROOT));
            if(inside(mx,my,85,121,8,8))tooltip=Component.translatable(prefs.descending()?"thaumcraft.research.descending":"thaumcraft.research.ascending");
        }
        int x=automationX();
        if(access.refill()) {
            ResearchWidgets.smallButton(g,leftPos+x,topPos+175,20,20,prefs.autoRefill()||inside(mx,my,x,175,20,20));
            g.renderItem(new net.minecraft.world.item.ItemStack(tc4revival.content.TCItems.SCRIBING_TOOLS.get()),leftPos+x+2,topPos+177);
            if(inside(mx,my,x,175,20,20))tooltip=Component.translatable("thaumcraft.research.auto_refill").append(": ").append(Component.translatable(prefs.autoRefill()?"options.on":"options.off"));
        }
        if(access.random()) {
            ResearchWidgets.smallButton(g,leftPos+x,topPos+199,20,20,inside(mx,my,x,199,20,20));
            for(int[] dot:new int[][]{{5,5},{12,5},{8,8},{5,12},{12,12}})
                g.fill(leftPos+x+dot[0],topPos+199+dot[1],leftPos+x+dot[0]+2,topPos+201+dot[1],0xffb8d9de);
            if(inside(mx,my,x,199,20,20))tooltip=Component.translatable("thaumcraft.research.random");
        }
        if(sorting && access.sorting()) {
            tooltip=null;g.pose().pushPose();g.pose().translate(0,0,450);
            for(int i=0;i<3;i++) {
                int y=topPos+131+i*18;
                g.fill(leftPos+75,y,leftPos+175,y+18,inside(mx,my,75,131+i*18,100,18)?0xD0403048:0xC0000000);
                var label=Component.translatable("thaumcraft.research.sort."+ResearchPreferences.Sort.values()[i].name().toLowerCase(java.util.Locale.ROOT));
                g.drawString(font,font.plainSubstrByWidth(label.getString(),92),leftPos+79,y+5,0xffffffff,false);
            }
            g.pose().popPose();
        }
        if(tooltip!=null)g.renderTooltip(font,tooltip,mx,my);
    }
    private void buttonLetter(GuiGraphics g,String text,int x,int color) {
        buttonLetter(g,text,x,121,color);
    }
    private void buttonLetter(GuiGraphics g,String text,int x,int y,int color) {
        float scale=.75f;
        g.pose().pushPose();
        g.pose().translate(leftPos+x+(8-font.width(text)*scale)/2f,topPos+y+(8-7*scale)/2f,0);
        g.pose().scale(scale,scale,1);g.drawString(font,text,0,0,color,false);g.pose().popPose();
    }
    private int automationX() { return 228; }
    private boolean branchClick(double mx,double my,int button) {
        if(button!=0)return false;
        if(sorting && access.sorting()) {
            for(int i=0;i<3;i++)if(inside(mx,my,75,131+i*18,100,18)) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId,20+i);page=0;sorting=false;return true;
            }
            sorting=false;
            // Close the popup without clicking through to a research cell or inventory slot.
            return true;
        }
        if(access.sorting() && inside(mx,my,76,121,8,8)) { sorting=true;return true; }
        int x=automationX();
        int action=access.sorting()&&inside(mx,my,85,121,8,8)?23:access.refill()&&inside(mx,my,x,175,20,20)?24:
                access.random()&&inside(mx,my,x,199,20,20)?25:-1;
        if(action>=0) { minecraft.gameMode.handleInventoryButtonClick(menu.containerId,action);return true; }
        return false;
    }
    @Override public boolean mouseClicked(double mx, double my, int button) {
        mx/=layoutScale;my/=layoutScale;
        refreshAccess();
        if (!search.visible || !search.isMouseOver(mx,my)) {
            setFocused(null);
            search.setFocused(false);
        }
        if(button==2) {
            var hex=ResearchTableView.hex(mx-leftPos-169,my-topPos-83);
            boolean sheet=note!=null && inside(mx,my,94,8,150,150);
            Aspect aspect=aspectAt(mx,my,true);
            if(aspect==null && sheet) {
                var cell=note.grid().get(hex);
                if(cell!=null && cell.aspect()!=null && known(cell.aspect()))aspect=TCAspects.catalog().require(cell.aspect());
            }
            boolean connect=autoConnect && sheet && !note.complete() && knowledge!=null && note.grid().containsKey(hex);
            if(connect || (access.related() && aspect!=null)) {
                middleGesture=new ResearchMiddleGesture(mx,my);middleSearch=access.related()?aspect:null;
                dragSolver=null;dragPlan=null;
                if(connect) {
                    var stock=new LinkedHashMap<String,Integer>();
                    for(var a:TCAspects.catalog().all())if(knowledge.knows(a))stock.put(a.tag(),(int)knowledge.poolAmount(a));
                    dragSolver=new ResearchDragSolver(note,TCAspects.catalog(),hex,stock,menu.bonuses());
                }
                plannedWrites.clear();awaitingWrite=null;sentWrite=null;
                dragged=null;columns.cancelPick();search.setFocused(false);return true;
            }
        } else {middleGesture=null;middleSearch=null;dragSolver=null;dragPlan=null;}
        if(!plannedWrites.isEmpty() || sentWrite!=null) {plannedWrites.clear();awaitingWrite=null;sentWrite=null;}
        if(button==1) {
            boolean cleared=clearSearchAndSelection();
            // Erasing takes priority over cursor selection, inventory handling and search clearing.
            if(!sorting && note!=null && !note.complete() && inside(mx,my,94,8,150,150)) {
                var hex=ResearchTableView.hex(mx-leftPos-169,my-topPos-83);
                var cell=note.grid().get(hex);
                if(cell!=null && cell.type()==2) {
                    // Releasing an in-progress drag must not put its aspect straight back into this cell.
                    dragged=null;
                    sound(TCSounds.HH_ON.get(),.3f,false);sound(TCSounds.ERASE.get(),.2f,true);
                    send(Action.ERASE,hex,null,null);return true;
                }
            }
            if(cleared)return true;
        }
        if(button==0 && search.visible && inside(mx,my,76,29,8,8)) {
            searchStrategy=(searchStrategy+1)%3;searchStrategyChanged=true;filterDirty=true;refreshAspects();
            if(!search.getValue().isBlank())columns.queryChanged(true,aspects.size());updatePage();return true;
        }
        if(button==0 && access.connect() && inside(mx,my,automationX()+24,shortcutY(),20,20)) {
            autoConnectPreference=!autoConnectPreference;localPreferencesChanged=true;refreshAccess();
            dragSolver=null;dragPlan=null;plannedWrites.clear();awaitingWrite=null;sentWrite=null;
            return true;
        }
        if(button==0 && access.basic() && inside(mx,my,shortcutX(),shortcutY(),shortcutWidth(),20)) {
            shortcutsPreference=!shortcutsPreference;localPreferencesChanged=true;refreshAccess();columns.cancelPick();dragged=null;updatePage();return true;
        }
        if(branchClick(mx,my,button))return true;
        if(button==0 && search.visible && search.isMouseOver(mx,my)) { setFocused(search); search.mouseClicked(mx,my,button); return true; }
        search.setFocused(false);
        // A real drag takes priority over sticky keyboard placement.
        if(button==0 && aspectAt(mx,my,false)!=null)columns.cancelPick();
        Aspect picked=pickedAspect();
        if(button==0 && picked!=null && note!=null && inside(mx,my,94,8,150,150)) {
            var hex=ResearchTableView.hex(mx-leftPos-169,my-topPos-83);var cell=note.grid().get(hex);
            if(!note.complete() && cell!=null && cell.type()==0 && available(picked)) {
                sound(TCSounds.WRITE.get(),.2f,false);send(Action.PLACE,hex,picked,null);
                columns.cancelPick();
            }
            return true;
        }
        boolean handled = super.mouseClicked(mx, my, button);
        if (button != 0 || System.nanoTime() < combineUntil) return handled;
        if (inside(mx, my, 35, 139, 32, 16) && first != null && second != null) {
            combineUntil = System.nanoTime() + 200_000_000; sound(TCSounds.CAMERA_CLACK.get(), 0.4f, false); sound(TCSounds.HH_ON.get(), 0.3f, false);
            send(Action.COMBINE, new ResearchHex(0, 0), first, second); return true;
        }
        if (button==0 && inside(mx, my, 27, 121, 24, 8) && canMove(-1)) { moveColumn(-1); sound(TCSounds.KEY.get(), 0.3f, false); return true; }
        if (button==0 && inside(mx, my, 51, 121, 24, 8) && canMove(1)) { moveColumn(1); sound(TCSounds.KEY.get(), 0.3f, false); return true; }
        if (first != null && inside(mx, my, 11, 137, 16, 16)) { first = null; sound(TCSounds.HH_OFF.get(), 0.2f, true); return true; }
        if (second != null && inside(mx, my, 71, 137, 16, 16)) { second = null; sound(TCSounds.HH_OFF.get(), 0.2f, true); return true; }
        if (note != null) {
            var hex = ResearchTableView.hex(mx - leftPos - 169, my - topPos - 83); var cell = note.grid().get(hex);
            if (cell != null && cell.type() == 2) {
                sound(TCSounds.HH_ON.get(), 0.3f, false); sound(TCSounds.ERASE.get(), 0.2f, true); send(Action.ERASE, hex, null, null); return true;
            }
            if (note.complete() && research("RESEARCHDUPE") && inside(mx, my, 37, 5, 24, 24)) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 5); sound(TCSounds.CAMERA_CLACK.get(), 0.4f, false); return true;
            }
        }
        var selected = aspectAt(mx, my, hasShiftDown());
        if (selected != null && hasShiftDown() && research("RESEARCHER2") && !selected.isPrimal()) {
            var plan = AspectSynthesis.plan(selected, TCAspects.catalog(), knowledge::poolAmount, menu.bonuses());
            if (plan.affordable()) {
                combineUntil = System.nanoTime() + 200_000_000;
                sound(TCSounds.HH_ON.get(), 0.3f, false); send(Action.SYNTHESIZE, new ResearchHex(0, 0), selected, null);
            }
            return true;
        }
        if (button == 0 && available(selected)) { dragged = selected; sound(TCSounds.HH_OFF.get(), 0.2f, true); return true; }
        return handled;
    }
    @Override public boolean mouseReleased(double mx, double my, int button) {
        mx/=layoutScale;my/=layoutScale;
        if(button==2 && middleGesture!=null) {
            if(middleGesture.click(mx,my)) {
                if(middleSearch!=null && access.related()) {
                    // The inserted query is an original name, even in translation-only mode.
                    searchStrategy=0;filterDirty=true;
                    search.setValue("~"+thaumcraft.api.client.ClientAspectViews.view(middleSearch).displayName().toLowerCase(java.util.Locale.ROOT));
                    refreshAspects();columns.queryChanged(true,aspects.size());updatePage();
                }
            } else if(dragSolver!=null) {
                updateDragPlan(mx,my);
                if(dragPlan!=null && dragSolver.snapshot().equals(note)) {
                    plannedWrites.addAll(dragPlan.placements().entrySet());advancePlannedWrites();
                }
            }
            middleGesture=null;middleSearch=null;dragSolver=null;dragPlan=null;return true;
        }
        if (button == 0 && dragged != null) {
            var hex = ResearchTableView.hex(mx - leftPos - 169, my - topPos - 83);
            var cell = note == null ? null : note.grid().get(hex);
            if (cell != null && cell.type() == 0) {
                sound(TCSounds.HH_ON.get(), 0.3f, false); sound(TCSounds.WRITE.get(), 0.2f, false); send(Action.PLACE, hex, dragged, null);
            } else if (inside(mx, my, 4, 130, 32, 32)) { first = dragged; sound(TCSounds.HH_OFF.get(), 0.2f, true); }
            else if (inside(mx, my, 63, 130, 32, 32)) { second = dragged; sound(TCSounds.HH_OFF.get(), 0.2f, true); }
            else if (aspectAt(mx, my, false) == dragged) { if (first == null) first = dragged; else if (second == null) second = dragged; }
            dragged = null; return true;
        }
        return super.mouseReleased(mx, my, button);
    }
    @Override public boolean mouseDragged(double mx,double my,int button,double dx,double dy) {
        mx/=layoutScale;my/=layoutScale;dx/=layoutScale;dy/=layoutScale;
        if(button==2 && middleGesture!=null) {
            if(middleGesture.moved(mx,my) && dragSolver!=null)updateDragPlan(mx,my);
            return true;
        }
        return super.mouseDragged(mx,my,button,dx,dy);
    }
    private void updateDragPlan(double mx,double my) {
        dragPlan=inside(mx,my,94,8,150,150)?dragSolver.preview(ResearchTableView.hex(mx-leftPos-169,my-topPos-83)).orElse(null):null;
    }
    private void renderDragPlan(GuiGraphics g) {
        if(dragPlan==null)return;
        for(var entry:dragPlan.placements().entrySet()) {
            var point=ResearchTableView.pixel(entry.getKey());
            tag(g,leftPos+161+point.x(),topPos+75+point.y(),TCAspects.catalog().require(entry.getValue()),0,false,.6f,false);
        }
    }
    private void advancePlannedWrites() {
        refreshAccess();
        if(plannedWrites.isEmpty() && sentWrite==null)return;
        var current=ResearchNoteNbt.read(menu.getSlot(1).getItem().getTag(),TCAspects.catalog()).note().orElse(null);
        if(current==null || current.complete()) {plannedWrites.clear();sentWrite=null;awaitingWrite=null;return;}
        if(sentWrite!=null) {
            if(current.equals(awaitingWrite)) {
                if(++writeWait<100)return;
                plannedWrites.clear();sentWrite=null;awaitingWrite=null;return;
            }
            var expected=new LinkedHashMap<>(awaitingWrite.grid());
            expected.put(sentWrite.getKey(),new ResearchNote.Cell(2,sentWrite.getValue()));
            if(!current.equals(new ResearchNote(awaitingWrite.key(),awaitingWrite.color(),false,awaitingWrite.copies(),expected,awaitingWrite.layoutSize()))) {
                plannedWrites.clear();sentWrite=null;awaitingWrite=null;return;
            }
            sentWrite=null;awaitingWrite=null;
        }
        if(plannedWrites.isEmpty())return;
        var next=plannedWrites.removeFirst();var cell=current.grid().get(next.getKey());
        if(cell==null || cell.occupied()) {plannedWrites.clear();return;}
        sentWrite=next;awaitingWrite=current;writeWait=0;
        send(Action.PLACE,next.getKey(),TCAspects.catalog().require(next.getValue()),null);
        sound(TCSounds.WRITE.get(),.2f,false);
    }
    private Aspect pickedAspect() {
        return shortcuts && columns.picked()!=null ? TCAspects.catalog().find(columns.picked()).orElse(null) : null;
    }
    private boolean clearSearchAndSelection() {
        boolean changed=!search.getValue().isEmpty() || columns.picked()!=null || dragged!=null;
        search.setValue("");columns.cancelPick();dragged=null;
        return changed;
    }
    private boolean canMove(int delta) {
        int current=shortcuts?columns.selected():page;
        return delta<0 ? current>0 : current<(shortcuts?ResearchColumnSelection.lastColumn(aspects.size()):ResearchTableView.lastPage(aspects.size()));
    }
    private void moveColumn(int delta) {
        if(shortcuts)columns.move(delta,aspects.size());else page+=delta;
        updatePage();
    }
    @Override public boolean mouseScrolled(double x,double y,double delta) {
        x/=layoutScale;y/=layoutScale;
        if(shortcuts && delta!=0) { moveColumn(delta>0?-1:1);return true; }
        return super.mouseScrolled(x,y,delta);
    }
    @Override public void mouseMoved(double x,double y) { super.mouseMoved(x/layoutScale,y/layoutScale); }
    @Override public boolean keyPressed(int key,int scan,int modifiers) {
        if(key==GLFW.GLFW_KEY_ESCAPE) { onClose();return true; }
        if(key==GLFW.GLFW_KEY_SPACE) { clearSearchAndSelection();return true; }
        if(shortcuts && key>=GLFW.GLFW_KEY_1 && key<=GLFW.GLFW_KEY_5 && !hasControlDown() && !hasAltDown()) {
            int index=columns.index(key-GLFW.GLFW_KEY_1,aspects.size());
            if(dragged==null)columns.pick(index<0?null:aspects.get(index).tag());
            return true;
        }
        if(search.isFocused()) { search.keyPressed(key,scan,modifiers);return true; }
        return super.keyPressed(key,scan,modifiers);
    }
    @Override public boolean charTyped(char character,int modifiers) {
        if(character==' ' || shortcuts && character>='1' && character<='5')return true;
        return search.isFocused()?search.charTyped(character,modifiers):super.charTyped(character,modifiers);
    }
    private void drawColumn(GuiGraphics g) {
        if(!shortcuts || aspects.isEmpty())return;
        int x=leftPos+10+(columns.selected()-page)*16,y=topPos+40;
        for(int i=0;i<3;i++) {
            int color=new int[]{0xffffe65a,0xc09b791d,0x60594013}[i];
            g.fill(x-1+i,y-1+i,x+17-i,y+i,color);g.fill(x-1+i,y+80-i,x+17-i,y+81-i,color);
            g.fill(x-1+i,y+i,x+i,y+80-i,color);g.fill(x+16-i,y+i,x+17-i,y+80-i,color);
        }
    }
    private void drawRowNumbers(GuiGraphics g) {
        if(!shortcuts || aspects.isEmpty())return;
        int x=leftPos+10+(columns.selected()-page)*16;
        g.pose().pushPose();g.pose().scale(.5f,.5f,1);
        for(int row=0;row<5;row++)if(columns.index(row,aspects.size())>=0) {
            String label=Integer.toString(row+1);int tx=x*2,ty=(topPos+40+row*16)*2;
            for(int[] d:new int[][]{{-1,0},{1,0},{0,-1},{0,1}})g.drawString(font,label,tx+d[0],ty+d[1],0xff000000,false);
            g.drawString(font,label,tx,ty,0xffffe65a,false);
        }
        g.pose().popPose();
    }
    // On narrow windows the toggle fits below the parchment, clear of the inventory slots.
    private int shortcutWidth() { return 20; }
    private int shortcutX() { return automationX(); }
    private int shortcutY() { return 223; }
    private Component shortcutLabel() { return Component.translatable("thaumcraft.research.shortcuts",Component.translatable(shortcuts?"options.on":"options.off")); }
    private void searchControls(GuiGraphics g,int mx,int my) {
        if(search.visible) {
            ResearchWidgets.smallButton(g,leftPos+76,topPos+29,8,8,inside(mx,my,76,29,8,8));
            buttonLetter(g,new String[]{"A","T","+"}[searchStrategy],76,29,0xffb8d9de);
            if(inside(mx,my,76,29,8,8))g.renderTooltip(font,Component.translatable("thaumcraft.research.search_mode."+searchStrategy),mx,my);
            if(search.isMouseOver(mx,my))g.renderTooltip(font,font.split(Component.translatable("thaumcraft.research.aspect_search.help"),220),mx,my);
        }
        int x=shortcutX(),y=shortcutY(),w=shortcutWidth();
        if(access.basic()) {
            ResearchWidgets.smallButton(g,leftPos+x,topPos+y,w,20,inside(mx,my,x,y,w,20));
            g.drawCenteredString(font,shortcuts?"1–5":"×",leftPos+x+10,topPos+y+6,shortcuts?0xffffe65a:0xffaaaaaa);
            if(inside(mx,my,x,y,w,20))g.renderTooltip(font,font.split(shortcutLabel().copy().append("\n").append(Component.translatable("thaumcraft.research.shortcuts.help")),220),mx,my);
        }
        if(access.connect()) {
            int ax=automationX()+24;
            ResearchWidgets.smallButton(g,leftPos+ax,topPos+y,20,20,autoConnect||inside(mx,my,ax,y,20,20));
            int linkColor=autoConnect?0xffffe65a:0xffaaaaaa;
            g.fill(leftPos+ax+5,topPos+y+9,leftPos+ax+15,topPos+y+11,linkColor);
            g.fill(leftPos+ax+3,topPos+y+7,leftPos+ax+7,topPos+y+13,linkColor);
            g.fill(leftPos+ax+13,topPos+y+7,leftPos+ax+17,topPos+y+13,linkColor);
            if(inside(mx,my,ax,y,20,20))g.renderTooltip(font,Component.translatable("thaumcraft.research.auto_connect").append(": ").append(Component.translatable(autoConnect?"options.on":"options.off")),mx,my);
        }
    }
    private void drawTooltips(GuiGraphics g, int mx, int my) {
        var hovered = aspectAt(mx, my, true);
        boolean list = hovered != null;
        if (hovered == null && inside(mx, my, 13, 139, 16, 16)) hovered = first;
        if (hovered == null && inside(mx, my, 71, 139, 16, 16)) hovered = second;
        if (hovered != null) {
            java.util.Optional<net.minecraft.world.inventory.tooltip.TooltipComponent> image=java.util.Optional.empty();
            if(list && research("RESEARCHER1") && !hovered.isPrimal())
                image=java.util.Optional.of(new AspectTooltips.Data(List.of(new AspectTooltips.Row(Component.empty(),
                        hovered.components().stream().map(tag->new AspectTooltips.Cell(tag,"")).toList())),80));
            // Components belong to the tooltip's layout and pose, not the desk's z=0 layer.
            // This also keeps them within the tooltip when ModernUI repositions/animates it.
            var elements = new java.util.ArrayList<com.mojang.datafixers.util.Either<net.minecraft.network.chat.FormattedText,net.minecraft.world.inventory.tooltip.TooltipComponent>>();
            elements.add(com.mojang.datafixers.util.Either.left(Component.literal(thaumcraft.api.client.ClientAspectViews.view(hovered).displayName()).withStyle(ChatFormatting.AQUA)));
            elements.add(com.mojang.datafixers.util.Either.left(Component.translatable(thaumcraft.api.client.ClientAspectViews.view(hovered).descriptionKey()).withStyle(ChatFormatting.GRAY)));
            image.ifPresent(value -> elements.add(com.mojang.datafixers.util.Either.right(value)));
            if (list && hasShiftDown() && research("RESEARCHER2") && !hovered.isPrimal()) {
                var plan = AspectSynthesis.plan(hovered, TCAspects.catalog(), knowledge::poolAmount, menu.bonuses());
                if (plan.valid()) {
                    elements.add(com.mojang.datafixers.util.Either.left(Component.translatable("thaumcraft.research.synthesize")));
                    var cells = plan.costs().entrySet().stream().map(e -> new AspectTooltips.Cell(e.getKey().tag(), Integer.toString(e.getValue()),
                            plan.missing().contains(e.getKey()) ? 0xffff5555 : 0xffffffff)).toList();
                    elements.add(com.mojang.datafixers.util.Either.right(new AspectTooltips.Data(List.of(new AspectTooltips.Row(Component.empty(), cells)), Math.max(24, Math.min(240, width - 24)))));
                }
            }
            g.renderComponentTooltipFromElements(font, elements, mx, my - 8, net.minecraft.world.item.ItemStack.EMPTY);
        }
        if (note != null && note.complete() && research("RESEARCHDUPE") && inside(mx, my, 37, 5, 24, 24)) {
            LegacyGui.blit(g,GUI, leftPos + 100, topPos + 21, 184, 224, 48, 16);
            g.drawString(font, Component.translatable("tc.research.copy"), leftPos + 100, topPos + 12, 0xffffffff);
            var definition = ResearchCatalogs.bundled().find(note.key());
            if (definition.isPresent()) {
                int i = 0;
                for (var entry : new java.util.TreeMap<>(definition.orElseThrow().aspects()).entrySet())
                    tag(g, leftPos + 148 + i++ * 16, topPos + 21, TCAspects.catalog().require(entry.getKey()), entry.getValue() + note.copies(), false, 1, false);
            }
        }
        if (note != null && ScribingToolsItem.ink(menu.getSlot(0).getItem()) == 0) {
            List<Component> lines = List.of(Component.translatable("tile.researchtable.noink.0").withStyle(ChatFormatting.AQUA), Component.translatable("tile.researchtable.noink.1").withStyle(ChatFormatting.GRAY));
            int half = lines.stream().mapToInt(font::width).max().orElse(0) / 2;
            g.renderTooltip(font, lines, java.util.Optional.empty(), leftPos + 157 - half, topPos + 84);
        }
    }
}
