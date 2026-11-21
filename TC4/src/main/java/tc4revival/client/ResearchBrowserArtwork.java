package tc4revival.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import tc4revival.research.ResearchBrowser;
import tc4revival.research.ResearchBrowserLines;
import tc4revival.research.ResearchCatalog;
import tc4revival.research.ResearchDefinition;
import tc4revival.research.ResearchBrowserLayout;

/** Original book atlas and animated tree drawing. The screen supplies resolved, real item icons. */
public  class ResearchBrowserArtwork {
    private static final ResourceLocation GUI = texture("gui/gui_research");
    private static final ResourceLocation NODES = texture("misc/nodes"), PARTICLES = texture("misc/particles");
    @FunctionalInterface public interface ItemIcon {
        void draw(GuiGraphics graphics, ResearchDefinition.Stack stack, int x, int y, boolean locked);
    }
    private ResearchBrowserArtwork() {}
    private static ResourceLocation texture(String path) { return ResourceLocation.parse("thaumcraft:textures/" + path + ".png"); }

    public static void draw(GuiGraphics g, ResearchBrowser.View view, List<ResearchCatalog.Category> categories,
                            int left, int top, double panX, double panY, float ticks, long milliseconds,
                            Set<String> highlighted, ItemIcon itemIcon) {
        draw(g,view,categories,left,top,panX,panY,ticks,milliseconds,highlighted,itemIcon,ResearchBrowserLayout.NORMAL);
    }
    public static void draw(GuiGraphics g,ResearchBrowser.View view,List<ResearchCatalog.Category> categories,
                            int left,int top,double panX,double panY,float ticks,long milliseconds,
                            Set<String> highlighted,ItemIcon itemIcon,ResearchBrowserLayout layout) {
        java.util.Objects.requireNonNull(itemIcon);
        int scrollX = (int) Math.floor(view.bounds().clampX(panX)), scrollY = (int) Math.floor(view.bounds().clampY(panY));
        int x = left + 16, y = top + 17;
        RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc();
        g.setColor(1, 1, 1, 1);
        g.enableScissor(x, y, x + layout.viewportWidth(), y + layout.viewportHeight());
        try {
            var bounds = view.bounds();
            int u = backgroundOffset(scrollX, bounds.minX(), bounds.maxXExclusive(), 288);
            int v = backgroundOffset(scrollY, bounds.minY(), bounds.maxYExclusive(), 316);
            g.pose().pushPose();
            try {
                g.pose().scale(2, 2, 1);
                int bw=(layout.viewportWidth()+1)/2,bh=(layout.viewportHeight()+1)/2;
                // Preserve the normal view's 2x pixel scale at every window size.
                // Mirrored repetition joins even non-tileable addon backgrounds without seams.
                for(var horizontal:MirroredBackground.spans(u/2,bw,256))
                    for(var vertical:MirroredBackground.spans(v/2,bh,256))
                        LegacyGui.blit(g,ResourceLocation.parse(thaumcraft.api.research.ResearchPresentation.background(view.category().key(),view.category().background())),
                                x/2+horizontal.offset(),y/2+vertical.offset(),horizontal.length(),vertical.length(),
                                horizontal.textureStart(),vertical.textureStart(),horizontal.textureLength(),vertical.textureLength(),256,256);
            } finally { g.pose().popPose(); }
            g.pose().pushPose();
            try {
                g.pose().translate(x - scrollX, y - scrollY, 0);
                for (var link : view.links()) {
                    var source=view.nodes().stream().filter(node -> node.research().key().equals(link.source())).findFirst();
                    var target=view.nodes().stream().filter(node -> node.research().key().equals(link.target())).findFirst();
                    if(source.map(node -> node.research().tutorial()).orElse(false)
                            && target.map(node -> node.research().mirror()).orElse(false))
                        dashed(g,link.x(),link.y(),link.targetX(),link.targetY(),0xff8b8374);
                    else line(g, ResearchBrowserLines.sample(link, ticks));
                }
            } finally { g.pose().popPose(); }
            RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc();
            for (var node : view.nodes()) {
                int nx = node.x() - scrollX, ny = node.y() - scrollY;
                if (nx < -24 || ny < -24 || nx > layout.viewportWidth() || ny > layout.viewportHeight()) continue;
                drawNode(g, node, x + nx, y + ny, ticks, milliseconds, highlighted, itemIcon);
            }
        } finally { g.setColor(1, 1, 1, 1); g.disableScissor(); }
        tabs(g, categories, view.category().key(), left, top, milliseconds, highlighted,layout);
        if(layout.equals(ResearchBrowserLayout.NORMAL))LegacyGui.blit(g,GUI,left,top,0,0,256,230);
        else {
            int[] sx={0,16,240},sy={0,17,213},sw={16,224,16},sh={17,196,17};
            int[] dx={0,16,layout.width()-16},dy={0,17,layout.height()-17};
            int[] dw={16,layout.viewportWidth(),16},dh={17,layout.viewportHeight(),17};
            for(int row=0;row<3;row++)for(int col=0;col<3;col++)
                LegacyGui.blit(g,GUI,left+dx[col],top+dy[row],dw[col],dh[row],sx[col],sy[row],sw[col],sh[row],256,256);
        }
        RenderSystem.disableBlend();
    }
    private static int backgroundOffset(int value, int minimum, int maximum, int extent) {
        int range = Math.abs(maximum - minimum);
        return range == 0 ? 0 : (int) ((float) (value - minimum) / range * extent);
    }
    private static void drawNode(GuiGraphics g, ResearchBrowser.Node node, int x, int y, float ticks, long milliseconds,
                                 Set<String> highlighted, ItemIcon itemIcon) {
        var entry = node.original();
        float opacity=node.research().mirror()?.5f:1;
        if (entry.warp() > 0) {
            g.setColor(68 / 255f, 0, 85 / 255f, .66f);
            int frame = 31 - Math.floorMod((int) ticks, 32);
            LegacyGui.blit(g,NODES, x + 11 - 40, y + 11 - 40, 80, 80, frame, 5, 1, 1, 32, 32);
        }
        float brightness = switch (node.state()) {
            case COMPLETE -> 1;
            case LOCKED -> .3f;
            case AVAILABLE -> (float) Math.sin(Math.floorMod(milliseconds, 600) / 600.0 * Math.PI * 2) * .25f + .75f;
        };
        g.setColor(brightness, brightness, brightness, opacity);
        LegacyGui.blit(g,GUI, x - 2, y - 2, node.frame().u(), 230, 26, 26);
        if (entry.has(ResearchDefinition.Flag.SPECIAL)) LegacyGui.blit(g,GUI, x - 2, y - 2, 26, 230, 26, 26);
        if (highlighted.contains(entry.key())) sparkle(g, x - 5, y - 5, milliseconds);
        boolean locked = node.iconLocked();
        // Legacy highlights leave unlocked icons fully bright; locked resource icons still use their dark tint below.
        float iconTint = highlighted.contains(entry.key()) ? 1 : !locked ? brightness : .1f;
        g.setColor(iconTint, iconTint, iconTint, opacity);
        var icon=entry.display().icon(milliseconds);
        if (icon.isPresent() && icon.get().item().isPresent()) {
            var display=thaumcraft.api.client.ResearchPresentationClient.icon(new thaumcraft.api.client.ResearchPresentationClient.IconContext(node.research().key(),milliseconds,locked),icon.get().item().orElseThrow());
            itemIcon.draw(g,display.stack(),x+3,y+3,locked);
            for(var overlay:display.overlays()){
                RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();
                LegacyGui.blit(g,overlay,x+3,y+3,0,0,16,16,16,16);
            }
        }
        else icon.flatMap(ResearchDefinition.Icon::resource).ifPresent(resource -> {
            if (locked) g.setColor(.2f, .2f, .2f, opacity);
            full(g, ResourceLocation.parse(resource), x + 3, y + 3);
        });
        g.setColor(1, 1, 1, 1);
    }
    private static void tabs(GuiGraphics g, List<ResearchCatalog.Category> categories, String selected, int x, int y,
                             long milliseconds, Set<String> highlighted,ResearchBrowserLayout layout) {
        for (int index = 0; index < categories.size(); index++) {
            var category = categories.get(index); boolean right = index >= layout.rows(), active = category.key().equals(selected);
            int tabX = x + layout.tabX(index), tabY = y + layout.tabY(index);
            g.setColor(1, 1, 1, 1);
            tab(g, tabX, tabY, active ? 152 : 176, right);
            int iconX = x - 19 + (right ? layout.width()+8 + (active ? 14 : 6) : active ? 0 : 8);
            if (highlighted.contains(category.key())) sparkle(g, iconX - 8, tabY - 4, milliseconds);
            full(g, ResourceLocation.parse(category.icon()), iconX, tabY + 4);
            if (!active) tab(g, tabX, tabY, 200, right);
        }
    }
    private static void tab(GuiGraphics g, int x, int y, int u, boolean reversed) {
        // Reverse UVs without reversing face winding under modern culling.
        LegacyGui.blit(g,GUI, x, y, 24, 24, u + (reversed ? 24 : 0), 232, reversed ? -24 : 24, 24, 256, 256);
    }
    private static void full(GuiGraphics g, ResourceLocation texture, int x, int y) { LegacyGui.blit(g,texture, x, y, 0, 0, 16, 16, 16, 16); }
    private static void sparkle(GuiGraphics g, int x, int y, long milliseconds) {
        g.setColor(1, 1, 1, 1);
        LegacyGui.blit(g,PARTICLES, x, y, (int) Math.floorMod(milliseconds / 50, 16) * 16, 80, 16, 16);
    }
    private static void line(GuiGraphics g, List<ResearchBrowserLines.Point> points) {
        if (points.size() < 2) return;
        g.flush(); RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        var buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 1; i < points.size(); i++) {
            var a = points.get(i - 1); var b = points.get(i);
            float dx = b.x() - a.x(), dy = b.y() - a.y(), length = (float) Math.hypot(dx, dy);
            if (length == 0) continue;
            float normalX = -dy / length * .5f, normalY = dx / length * .5f;
            for (int corner = 0; corner < 4; corner++) {
                var point = corner == 0 || corner == 3 ? a : b; float side = corner < 2 ? 1 : -1;
                buffer.vertex(g.pose().last().pose(), point.x() + normalX * side, point.y() + normalY * side, 0)
                        .color(point.red(), point.green(), point.blue(), point.alpha()).endVertex();
            }
        }
        BufferUploader.drawWithShader(buffer.end()); RenderSystem.disableBlend();
    }
    public static void dashed(GuiGraphics g,int x,int y,int targetX,int targetY,int color) {
        double distance=Math.hypot(targetX-x,targetY-y);
        if(distance==0)return;
        for(int step=0;step<=distance;step+=5) {
            int px=x+(int)Math.round((targetX-x)*step/distance),py=y+(int)Math.round((targetY-y)*step/distance);
            g.fill(px,py,px+2,py+2,color);
        }
    }
}
